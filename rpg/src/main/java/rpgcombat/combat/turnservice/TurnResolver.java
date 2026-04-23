package rpgcombat.combat.turnservice;

import static rpgcombat.combat.models.Action.ATTACK;
import static rpgcombat.combat.models.Action.CHARGE;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import rpgcombat.balance.CombatBalanceRegistry;
import rpgcombat.balance.config.CombatBalanceConfig;
import rpgcombat.balance.config.MomentumConfig;
import rpgcombat.combat.AttackResolver;
import rpgcombat.combat.models.Action;
import rpgcombat.combat.models.EffectPipeline;
import rpgcombat.combat.services.EndRoundRegenBonus;
import rpgcombat.combat.services.RoundRecoveryService;
import rpgcombat.models.characters.Character;
import rpgcombat.models.characters.Result;
import rpgcombat.models.characters.Statistics;
import rpgcombat.models.effects.impl.PoisonEffect;
import rpgcombat.weapons.Weapon;
import rpgcombat.weapons.attack.AttackResult;
import rpgcombat.weapons.passives.HitContext;
import rpgcombat.weapons.passives.HitContext.Event;
import rpgcombat.weapons.passives.HitContext.Phase;

import rpgcombat.combat.services.CombatRhythmService;

/**
 * Resol un torn de combat entre dos personatges.
 */
public class TurnResolver {
    private final AttackResolver attackResolver;
    private final EffectPipeline effectPipeline;
    private final RoundRecoveryService recoveryService;
    private final CombatRhythmService rhythmService = new CombatRhythmService();

    private final CombatBalanceConfig balance = CombatBalanceRegistry.get();
    private final MomentumConfig momentumConfig = balance.momentum();

    /**
     * Crea el resolvedor de torns.
     *
     * @param attackResolver servei de resolució d'atacs
     * @param effectPipeline pipeline d'efectes
     * @param recoveryService servei de recuperació
     */
    public TurnResolver(
            AttackResolver attackResolver,
            EffectPipeline effectPipeline,
            RoundRecoveryService recoveryService) {
        this.attackResolver = attackResolver;
        this.effectPipeline = effectPipeline;
        this.recoveryService = recoveryService;
    }

    /**
     * Resol l'acció d'un torn complet.
     *
     * @param attacker atacant
     * @param defender defensor
     * @param attackerAction acció de l'atacant
     * @param defenderAction acció del defensor
     * @param defenderBonus bonus final de regeneració
     * @return resultat del torn
     */
    public TurnResult resolveTurn(
            Character attacker,
            Character defender,
            Action attackerAction,
            Action defenderAction,
            EndRoundRegenBonus defenderBonus) {

        List<String> startMessages = new ArrayList<>();
        rhythmService.onActionStart(attacker, attackerAction);
        attacker.onTurnStart(attackerAction, startMessages);

        if (!attacker.isAlive()) {
            return new TurnResult(attacker.getName(), null, startMessages, List.of(), null, List.of(), List.of(), 0,
                    false);
        }

        if (attackerAction != ATTACK) {
            return resolveNonAttackTurn(attacker, defender, attackerAction, defenderAction, startMessages);
        }

        List<String> preDefenseMessages = new ArrayList<>();
        List<String> postDefenseMessages = new ArrayList<>();
        List<String> endTurnMessages = new ArrayList<>();

        Weapon preWeapon = attacker.getWeapon();
        if (isGrimori(preWeapon)) {
            startMessages.add("… el Grimori s'activa …");
        }

        AttackResult attackResult = attacker.attack();
        String attackerMessage = attacker.getName() + " " + attackResult.message();

        Character realTarget = attackResolver.chooseTarget(attacker, defender, attackResult);
        Weapon weapon = attacker.getWeapon();
        boolean hasWeapon = weapon != null;

        if (realTarget == attacker) {
            double damage = attackResult.damage();
            if (damage > 0)
                attacker.getDamage(damage);
            return new TurnResult(attacker.getName(), attackerMessage, startMessages, List.of(), null, List.of(),
                    List.of(), damage, false);
        }

        Random attackerRng = attacker.rng();
        Random defenderRng = defender.rng();
        HitContext ctx = new HitContext(attacker, defender, weapon, attackerRng, attackerAction, defenderAction);
        ctx.setAttackResult(attackResult);

        configureHitContext(ctx, attacker, attackResult, weapon);

        effectPipeline.runAttackerOnly(ctx, Phase.START_TURN, attacker, attackerRng, startMessages);
        effectPipeline.runPhase(ctx, Phase.BEFORE_ATTACK, attacker, defender, weapon, attackerRng, defenderRng,
                preDefenseMessages);
        effectPipeline.runPhase(ctx, Phase.ROLL_CRIT, attacker, defender, weapon, attackerRng, defenderRng,
                preDefenseMessages);

        boolean critical = ctx.resolveCritical();
        if (critical)
            preDefenseMessages.add("cop crític!");

        if (attacker.isDesperate()) {
            preDefenseMessages.add("[GREEN|!] " + attacker.getName() + " lluita al límit i troba força extra.");
        }

        if (attacker.getMomentumStacks() > 0) {
            preDefenseMessages.add("[CYAN|+] " + attacker.getName() + " aprofita l'impuls del combat.");
        }

        boolean chargedStrike = attacker.consumeChargedAttack();
        if (chargedStrike) {
            ctx.multiplyDamage(attacker.chargedAttackMultiplier());
            ctx.putMeta("CHARGED_HIT", true);
            preDefenseMessages.add("[CYAN|+] L'atac carregat esclata amb més força.");
        }

        ctx.multiplyDamage(attacker.getAttackModifierThisTurn());
        ctx.multiplyDamage(attacker.comebackAttackMultiplierAgainst(defender));
        ctx.multiplyDamage(attacker.momentumAttackMultiplierAgainst(defender));
        rhythmService.applyOffensivePressure(attacker, ctx::multiplyDamage);
        rhythmService.applyDefensivePressure(defender, ctx::multiplyDamage);
        ctx.multiplyDamage(defender.consumeIncomingDamageMultiplier());
        ctx.multiplyDamage(defender.comebackIncomingDamageMultiplierAgainst(attacker));
        effectPipeline.runPhase(ctx, Phase.MODIFY_DAMAGE, attacker, defender, weapon, attackerRng, defenderRng,
                preDefenseMessages);
        effectPipeline.runPhase(ctx, Phase.BEFORE_DEFENSE, attacker, defender, weapon, attackerRng, defenderRng,
                preDefenseMessages);

        double damageToResolve = ctx.damageToResolve();
        if (defender.isDesperate()) {
            preDefenseMessages
                    .add("[GREEN|!] " + defender.getName() + " aguanta com pot i redueix part de la pressió rebuda.");
        }
        rhythmService.onDefenseReaction(defender, defenderAction, damageToResolve);

        Result defenderResult = attackResolver.resolveAttack(damageToResolve, defender, defenderAction);
        ctx.setDefenderResult(defenderResult);
        ctx.setDamageDealt(defenderResult.recived());

        recoveryService.registerDefenseBonus(defenderAction, defenderResult, damageToResolve, defenderBonus);
        if (hasWeapon)
            weapon.registerResolvedAttack(ctx.wasCritical(), damageToResolve);
        registerCombatEvents(ctx, defender, defenderAction);
        applyPhaseThreeStates(defender, defenderAction, ctx, defenderResult, critical,
                postDefenseMessages);
        updateMomentum(attacker, defender, defenderAction, damageToResolve, defenderResult, postDefenseMessages);

        defender.tryTriggerAdrenalineSurge(attacker, defenderBonus, postDefenseMessages);

        rhythmService.onDefenseResolved(defender, defenderAction, damageToResolve, endTurnMessages);
        rhythmService.onAttackResolved(attacker, attackerAction, damageToResolve, endTurnMessages);

        String defenseMessage = null;
        if (defenderResult.recived() != -1) {
            String msg = defenderResult.message();
            if (msg != null && !msg.isBlank())
                defenseMessage = msg;
        }

        effectPipeline.runPhase(ctx, Phase.AFTER_DEFENSE, attacker, defender, weapon, attackerRng, defenderRng,
                postDefenseMessages);
        if (ctx.damageDealt() > 0) {
            effectPipeline.runPhase(ctx, Phase.AFTER_HIT, attacker, defender, weapon, attackerRng, defenderRng,
                    postDefenseMessages);
        }
        effectPipeline.runAttackerOnly(ctx, Phase.END_TURN, attacker, attackerRng, endTurnMessages);

        return new TurnResult(attacker.getName(), attackerMessage, startMessages, preDefenseMessages, defenseMessage,
                postDefenseMessages, endTurnMessages, ctx.damageDealt(), critical);
    }

    /**
     * Resol un torn sense atac directe.
     */
    private TurnResult resolveNonAttackTurn(
            Character attacker,
            Character defender,
            Action attackerAction,
            Action defenderAction,
            List<String> startMessages) {

        List<String> endTurnMessages = new ArrayList<>();

        if (attackerAction == CHARGE) {
            if (attacker.hasChargedAttack()) {
                endTurnMessages.add("[CYAN|!] " + attacker.getName() + " manté la càrrega; no s'acumula més.");
            } else {
                attacker.prepareChargedAttack();
                endTurnMessages.add("[CYAN|+] " + attacker.getName() + " concentra forces per al següent atac.");
            }
        }

        decayMomentumOnPassiveTurn(attacker, attackerAction, endTurnMessages);

        if (breakAttackChains(attacker, defender)) {
            endTurnMessages.add("La cadena del verí es trenca i el verí s'esvaeix.");
        }

        Result defenderResult = attackResolver.resolveAttack(0, defender, defenderAction);
        String defenseMessage = defenderResult.message();

        Random attackerRng = attacker.rng();
        HitContext ctx = new HitContext(
                attacker,
                defender,
                attacker.getWeapon(),
                attackerRng,
                attackerAction,
                defenderAction);

        effectPipeline.runAttackerOnly(ctx, Phase.END_TURN, attacker, attackerRng, endTurnMessages);

        return new TurnResult(
                attacker.getName(),
                null,
                startMessages,
                List.of(),
                defenseMessage,
                List.of(),
                endTurnMessages,
                0,
                false);
    }

    /**
     * Prepara el context d'impacte amb les dades de l'atac.
     */
    private void configureHitContext(HitContext ctx, Character attacker, AttackResult attackResult, Weapon weapon) {
        if (weapon != null) {
            Statistics attackerStats = attacker.getStatistics();
            double rolledDamage = Math.max(0.0001, weapon.lastAttackDamage());
            double nonCritDamage = Math.max(0.0, weapon.lastNonCriticalDamage());
            double skillMultiplier = (rolledDamage > 0.0) ? attackResult.damage() / rolledDamage : 1.0;
            double rebuiltBaseDamage = round2(nonCritDamage * skillMultiplier);
            if (rebuiltBaseDamage <= 0 && attackResult.damage() > 0)
                rebuiltBaseDamage = attackResult.damage();
            ctx.setBaseDamage(rebuiltBaseDamage);
            ctx.setCriticalChance(weapon.resolveCriticalChance(attackerStats));
            ctx.setCriticalMultiplier(weapon.resolveCriticalMultiplier(attackerStats));
            ctx.putMeta("WEAPON_NAME", weapon.getName());
            ctx.putMeta("RAW_DAMAGE", rebuiltBaseDamage);
            ctx.putMeta("ORIGINAL_WEAPON_CRIT", weapon.lastWasCritic());
        } else {
            ctx.setBaseDamage(attackResult.damage());
            ctx.setCriticalChance(0.0);
            ctx.setCriticalMultiplier(1.0);
            ctx.putMeta("WEAPON_NAME", "Fists");
            ctx.putMeta("RAW_DAMAGE", attackResult.damage());
        }
        ctx.putMeta("CRIT", false);
    }

    /**
     * Registra els esdeveniments del combat per a l'impacte actual.
     */
    private void registerCombatEvents(HitContext ctx, Character defender, Action defenderAction) {
        if (defenderAction == Action.DODGE) {
            ctx.registerEvent(Event.ON_DODGE);
        } else if (defenderAction == Action.DEFEND) {
            ctx.registerEvent(Event.ON_DEFEND);
        }

        if (ctx.damageDealt() > 0) {
            ctx.registerEvent(Event.ON_HIT);
            ctx.registerEvent(Event.ON_DAMAGE_DEALT);
            ctx.registerEvent(Event.ON_DAMAGE_TAKEN);
            if (!defender.isAlive())
                ctx.registerEvent(Event.ON_KILL);
        }
    }

    /**
     * Aplica estats addicionals després de resoldre la defensa.
     */
    private void applyPhaseThreeStates(
            Character defender,
            Action defenderAction,
            HitContext ctx,
            Result defenderResult,
            boolean critical,
            List<String> out) {

        if (defenderResult.recived() <= 0)
            return;

        if (defenderAction == Action.DEFEND && defender.isVulnerable()) {
            out.add("[RED|!] La defensa trencada deixa " + defender.getName() + " exposat.");
        }

        if (critical) {
            defender.applyBleed(2);
            out.add("[RED|+] El cop crític obre una ferida: s'aplica sagnat.");
        }

        Object rawDamageInput = ctx.getMeta("RAW_DAMAGE");
        if (rawDamageInput instanceof Number rawDamage && ctx.damageDealt() >= rawDamage.doubleValue() * 0.90
                && defenderAction == Action.DODGE) {
            defender.applyBleed(1);
            out.add("[RED|+] L'esquiva fallida deixa un tall superficial.");
        }

        if (ctx.getMeta("CHARGED_HIT") instanceof Boolean charged && Boolean.TRUE.equals(charged)) {
            defender.applyStagger(1);
            out.add("[YELLOW|+] L'impacte carregat desequilibra el rival.");
        }
    }

    /**
     * Actualitza l'impuls dels combatents segons el resultat.
     */
    private void updateMomentum(
            Character attacker,
            Character defender,
            Action defenderAction,
            double damageToResolve,
            Result defenderResult,
            List<String> out) {

        boolean successfulHit = defenderResult.recived() > 0;
        boolean successfulDodge = defenderAction == Action.DODGE && damageToResolve > 0
                && defenderResult.recived() <= 0;
        boolean defenderUnderHeavyPressure = defender.isDesperate()
                || defender.healthRatio() + momentumConfig.suppressionHealthOffset() < attacker.healthRatio();

        if (successfulHit) {
            if (!defenderUnderHeavyPressure) {
                int before = attacker.getMomentumStacks();
                attacker.gainMomentum();
                if (attacker.getMomentumStacks() > before && out != null) {
                    out.add("[CYAN|+] " + attacker.getName() + " guanya impuls.");
                }
            } else if (out != null && attacker.getMomentumStacks() > 0) {
                out.add("[CYAN|=] L'avantatge de " + attacker.getName()
                        + " no accelera més davant un rival acorralat.");
            }

            if (defender.getMomentumStacks() > 0) {
                defender.loseMomentum();
                if (out != null)
                    out.add("[CYAN|-] " + defender.getName() + " perd impuls sota la pressió rival.");
            }
            return;
        }

        if (successfulDodge) {
            int before = defender.getMomentumStacks();
            defender.gainMomentum();
            if (defender.isDesperate() || defender.healthRatio() + momentumConfig.suppressionHealthOffset() < attacker.healthRatio()) {
                defender.gainMomentum();
            }

            if (defender.getMomentumStacks() > before && out != null) {
                out.add("[CYAN|+] " + defender.getName() + " llegeix el ritme i guanya impuls.");
            }
            if (attacker.getMomentumStacks() > 0) {
                attacker.loseMomentum();
                if (out != null)
                    out.add("[CYAN|-] " + attacker.getName() + " perd impuls després de fallar.");
            }
            return;
        }

        if (attacker.getMomentumStacks() > 0) {
            attacker.loseMomentum();
            if (out != null)
                out.add("[CYAN|-] " + attacker.getName() + " perd part de l'impuls.");
        }
    }

    /**
     * Redueix l'impuls en torns passius.
     */
    private void decayMomentumOnPassiveTurn(Character actor, Action action, List<String> out) {
        if (actor == null || action == null || actor.getMomentumStacks() <= 0) {
            return;
        }

        if (action == Action.DEFEND || action == Action.CHARGE) {
            if (action == Action.CHARGE && actor.isDesperate()) {
                return;
            }
            actor.loseMomentum();
            if (out != null) {
                out.add("[CYAN|-] L'impuls de " + actor.getName() + " es refreda una mica.");
            }
        }
    }

    /**
     * Trenca cadenes d'atac especials si escau.
     */
    private boolean breakAttackChains(Character attacker, Character defender) {
        Weapon weapon = attacker.getWeapon();
        if (weapon == null)
            return false;
        if (!"WASP_HARPOON".equals(weapon.getId()))
            return false;

        PoisonEffect poison = PoisonEffect.from(defender);
        if (poison == null)
            return false;
        if (poison.stacks() <= 0)
            return false;

        defender.removeEffect(PoisonEffect.INTERNAL_EFFECT_KEY);
        return true;
    }

    /**
     * Indica si l'arma és un Grimori.
     */
    private boolean isGrimori(Weapon weapon) {
        return weapon != null && "GRIMORI".equals(weapon.getId());
    }

    /**
     * Arrodoneix a dues xifres decimals.
     */
    private static double round2(double n) {
        return Math.round(n * 100.0) / 100.0;
    }
}