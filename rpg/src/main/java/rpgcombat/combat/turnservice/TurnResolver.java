package rpgcombat.combat.turnservice;

import static rpgcombat.combat.models.Action.ATTACK;
import static rpgcombat.combat.models.Action.CHARGE;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import rpgcombat.balance.CombatBalanceRegistry;
import rpgcombat.balance.config.CombatBalanceConfig;
import rpgcombat.balance.config.character.MomentumConfig;
import rpgcombat.combat.AttackResolver;
import rpgcombat.combat.models.Action;
import rpgcombat.combat.models.EffectPipeline;
import rpgcombat.combat.services.EndRoundRegenBonus;
import rpgcombat.combat.services.RoundRecoveryService;
import rpgcombat.combat.ui.messages.CombatMessage;
import rpgcombat.combat.ui.messages.CombatMessageBuffer;
import rpgcombat.combat.ui.messages.MessageColor;
import rpgcombat.combat.ui.messages.MessageSymbol;
import rpgcombat.gamemode.model.GameModeRules;
import rpgcombat.models.characters.Character;
import rpgcombat.models.characters.Result;
import rpgcombat.models.characters.Statistics;
import rpgcombat.models.effects.impl.elemental.PoisonEffect;
import rpgcombat.models.effects.triggers.Chaos;
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
    private final GameModeRules rules;

    private final CombatBalanceConfig balance = CombatBalanceRegistry.get();
    private final MomentumConfig momentumConfig = balance.momentum();

    /**
     * Crea el resolvedor de torns.
     *
     * @param attackResolver  servei de resolució d'atacs
     * @param effectPipeline  pipeline d'efectes
     * @param recoveryService servei de recuperació
     */
    public TurnResolver(
            AttackResolver attackResolver,
            EffectPipeline effectPipeline,
            RoundRecoveryService recoveryService) {
        this(attackResolver, effectPipeline, recoveryService, GameModeRules.unrestricted());
    }

    public TurnResolver(
            AttackResolver attackResolver,
            EffectPipeline effectPipeline,
            RoundRecoveryService recoveryService,
            GameModeRules rules) {
        this.attackResolver = attackResolver;
        this.effectPipeline = effectPipeline;
        this.recoveryService = recoveryService;
        this.rules = rules == null ? GameModeRules.unrestricted() : rules;
    }

    /**
     * Resol l'acció d'un torn complet.
     *
     * @param attacker       atacant
     * @param defender       defensor
     * @param attackerAction acció de l'atacant
     * @param defenderAction acció del defensor
     * @param defenderBonus  bonus final de regeneració
     * @return resultat del torn
     */
    public TurnResult resolveTurn(
            Character attacker,
            Character defender,
            Action attackerAction,
            Action defenderAction,
            EndRoundRegenBonus defenderBonus) {

        CombatMessageBuffer startMessages = new CombatMessageBuffer();
        attackerAction = rules.requireAllowed(attackerAction);
        defenderAction = rules.requireAllowed(defenderAction);

        Action selectedAction = attackerAction;
        attackerAction = Chaos.applyStartTurn(attacker, defender, attackerAction, startMessages, rules::allowsAction);
        Map<String, Object> startTurnMeta = chaosStartMeta(attacker, selectedAction, attackerAction);
        rhythmService.onActionStart(attacker, attackerAction);
        attacker.onTurnStart(attackerAction, startMessages);

        if (!attacker.isAlive()) {
            return new TurnResult(attacker.getName(), null, startMessages.messages(), List.of(), null, List.of(),
                    List.of(), 0, false, false, false, false, null, 0.0, 0.0, 0.0,
                    attacker.getWeapon() == null ? null : attacker.getWeapon().getId(),
                    attacker.getWeapon() == null ? null : attacker.getWeapon().getName(),
                    startTurnMeta);
        }

        if (attackerAction != ATTACK) {
            return resolveNonAttackTurn(attacker, defender, attackerAction, defenderAction, startMessages, startTurnMeta);
        }

        CombatMessageBuffer preDefenseMessages = new CombatMessageBuffer();
        CombatMessageBuffer postDefenseMessages = new CombatMessageBuffer();
        CombatMessageBuffer endTurnMessages = new CombatMessageBuffer();

        Weapon preWeapon = attacker.getWeapon();
        if (isGrimori(preWeapon)) {
            startMessages.add(CombatMessage.info("… el Grimori s'activa …"));
        }

        AttackResult attackResult = attacker.attack();
        Map<String, Object> attackMeta = new HashMap<>(startTurnMeta);
        attackMeta.putAll(attackResult.meta());
        String attackerMessage = attacker.getName() + " " + attackResult.message();

        Character realTarget = attackResolver.chooseTarget(attacker, defender, attackResult);
        Weapon weapon = attacker.getWeapon();
        boolean hasWeapon = weapon != null;
        String weaponId = getWeaponId(weapon);
        String weaponName = getWeaponName(weapon);
        double grimoireMultiplier = resolveGrimoireMultiplier(weapon, attackResult);
        String failKind = attackResult == null ? null : attackResult.failKind();
        boolean attackFailed = attackResult != null && attackResult.failed();
        attackMeta.put("weaponId", weaponId);
        attackMeta.put("weaponName", weaponName);
        attackMeta.put("grimoireMultiplier", grimoireMultiplier);

        if (realTarget == attacker) {
            double damage = attackResult.damage();
            if (damage > 0)
                attacker.getDamage(damage);
            return new TurnResult(attacker.getName(), attackerMessage, startMessages.messages(), List.of(), null,
                    List.of(),
                    List.of(), damage, false, true, false, false, failKind, damage, grimoireMultiplier, 0.0,
                    weaponId, weaponName, attackMeta);
        }

        Random attackerRng = attacker.rng();
        Random defenderRng = defender.rng();
        HitContext ctx = new HitContext(attacker, defender, weapon, attackerRng, attackerAction, defenderAction);
        ctx.setAttackResult(attackResult);
        ctx.putMeta("WEAPON_ID", weaponId);
        ctx.putMeta("WEAPON_NAME", weaponName);
        ctx.putMeta("GRIMOIRE_MULTIPLIER", grimoireMultiplier);
        startTurnMeta.forEach(ctx::putMeta);
        if (attackFailed) ctx.putMeta("ATTACK_FAIL_KIND", failKind);

        configureHitContext(ctx, attacker, attackResult, weapon);

        effectPipeline.runAttackerOnly(ctx, Phase.START_TURN, attacker, attackerRng, startMessages);
        effectPipeline.runPhase(ctx, Phase.BEFORE_ATTACK, attacker, defender, weapon, attackerRng, defenderRng,
                preDefenseMessages);
        effectPipeline.runPhase(ctx, Phase.ROLL_CRIT, attacker, defender, weapon, attackerRng, defenderRng,
                preDefenseMessages);

        boolean critical = ctx.resolveCritical();
        if (critical)
            preDefenseMessages.warning("cop crític!");

        if (attacker.isDesperate()) {
            preDefenseMessages.styled(MessageColor.GREEN, MessageSymbol.WARNING,
                    attacker.getName() + " lluita al límit i troba força extra.");
        }

        if (attacker.getMomentumStacks() > 0) {
            preDefenseMessages.styled(MessageColor.CYAN, MessageSymbol.POSITIVE,
                    attacker.getName() + " aprofita l'impuls del combat.");
        }

        boolean chargedStrike = rules.allowsAction(CHARGE) && attacker.consumeChargedAttack();
        if (chargedStrike) {
            ctx.multiplyDamage(attacker.chargedAttackMultiplier());
            ctx.putMeta("CHARGED_HIT", true);
            preDefenseMessages.styled(MessageColor.CYAN, MessageSymbol.POSITIVE,
                    "L'atac carregat esclata amb més força.");
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

        if (Boolean.TRUE.equals(ctx.getMeta(Chaos.META_SELF_HIT, Boolean.class, false))) {
            double selfMultiplier = ctx.getMeta(Chaos.META_SELF_HIT_MULTIPLIER, Double.class, 1.0);
            boolean canKill = ctx.getMeta(Chaos.META_SELF_HIT_CAN_KILL, Boolean.class, false);
            double selfDamage = round2(Math.max(0.0, damageToResolve * selfMultiplier));
            if (!canKill)
                selfDamage = Math.clamp(0.0, selfDamage, attacker.getStatistics().getHealth() - 1.0);

            Result selfResult = selfDamage > 0
                    ? attacker.getDamage(selfDamage)
                    : new Result(0, attacker.getName() + " resisteix el pitjor del caos.");

            ctx.setDefenderResult(selfResult);
            ctx.setDamageDealt(selfDamage);
            postDefenseMessages.styled(MessageColor.MAGENTA, MessageSymbol.WARNING,
                    attacker.getName() + " es colpeja a si mateix per " + selfDamage + ".");
            rhythmService.onAttackResolved(attacker, attackerAction, selfDamage, endTurnMessages);
            effectPipeline.runAttackerOnly(ctx, Phase.END_TURN, attacker, attackerRng, endTurnMessages);

            return new TurnResult(attacker.getName(), attackerMessage, startMessages.messages(),
                    preDefenseMessages.messages(),
                    selfResult.message(), postDefenseMessages.messages(), endTurnMessages.messages(), selfDamage,
                    critical, true, Boolean.TRUE.equals(ctx.getMeta("CHARGED_HIT")), false, failKind, selfDamage,
                    grimoireMultiplier, ctx.getMeta("LIFE_STOLEN", Double.class, 0.0), weaponId, weaponName, mergeTurnMeta(attackMeta, ctx));
        }
        if (defender.isDesperate()) {
            preDefenseMessages.styled(MessageColor.GREEN, MessageSymbol.WARNING,
                    defender.getName() + " aguanta com pot i redueix part de la pressió rebuda.");
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

        return new TurnResult(attacker.getName(), attackerMessage, startMessages.messages(),
                preDefenseMessages.messages(), defenseMessage,
                postDefenseMessages.messages(), endTurnMessages.messages(), ctx.damageDealt(), critical,
                false, Boolean.TRUE.equals(ctx.getMeta("CHARGED_HIT")), isMiss(attackFailed, damageToResolve, ctx.damageDealt()),
                failKind, damageToResolve, grimoireMultiplier, ctx.getMeta("LIFE_STOLEN", Double.class, 0.0),
                weaponId, weaponName, mergeTurnMeta(attackMeta, ctx));
    }


    /**
     * Combina metadades pròpies de l'habilitat amb dades generades durant la resolució del cop.
     */
    private Map<String, Object> mergeTurnMeta(Map<String, Object> attackMeta, HitContext ctx) {
        Map<String, Object> merged = new HashMap<>();
        if (attackMeta != null) merged.putAll(attackMeta);
        if (ctx == null) return merged;
        merged.put("lifeStolen", ctx.getMeta("LIFE_STOLEN", Double.class, 0.0));
        copyMeta(ctx, merged, "lifeStealTriggered");
        copyMeta(ctx, merged, "lifeStealPct");
        copyMeta(ctx, merged, "modeLifeSteal");
        copyMeta(ctx, merged, "modeLifeStealPct");
        copyMeta(ctx, merged, "modeLifeStealHeal");
        merged.put("chargedHit", Boolean.TRUE.equals(ctx.getMeta("CHARGED_HIT")));
        merged.put("rawDamage", ctx.getMeta("RAW_DAMAGE"));
        merged.put("originalWeaponCrit", ctx.getMeta("ORIGINAL_WEAPON_CRIT"));
        copyMeta(ctx, merged, "activatedPerkIds");
        copyMeta(ctx, merged, "activatedPerkNames");
        copyMeta(ctx, merged, "activatedPerkFamilies");
        copyMeta(ctx, merged, "activatedPerkTags");
        copyMeta(ctx, merged, "triggeredSynergyIds");
        copyMeta(ctx, merged, "triggeredSynergyNames");
        copyMeta(ctx, merged, "divineAwakeningPerkIds");
        copyMeta(ctx, merged, "divineAwakeningPerkNames");
        copyMeta(ctx, merged, "divineAwakeningGods");
        copyMeta(ctx, merged, "divineAwakeningLevels");
        copyMeta(ctx, merged, "divineAwakeningMaxLevels");
        copyMeta(ctx, merged, "divinePerkAwakened");
        copyMeta(ctx, merged, "divinePerkFullPower");
        copyMeta(ctx, merged, "divineAwakeningLevel");
        copyMeta(ctx, merged, "divineAwakeningMax");
        copyMeta(ctx, merged, "divinePerkId");
        copyMeta(ctx, merged, "divinePerkName");
        copyMeta(ctx, merged, "god");
        copyMeta(ctx, merged, Chaos.META_SELF_HIT);
        copyMeta(ctx, merged, Chaos.META_SELF_HIT_MULTIPLIER);
        copyMeta(ctx, merged, Chaos.META_SELF_HIT_CAN_KILL);
        copyMeta(ctx, merged, "selfDirectedAttack");
        copyMeta(ctx, merged, "selfDirectedAttackMultiplier");
        copyMeta(ctx, merged, "selfDirectedAttackCanKill");
        copyMeta(ctx, merged, "chaosSelfHit");
        copyMeta(ctx, merged, "chaosForceCrit");
        copyMeta(ctx, merged, "chaosForbidCrit");
        copyMeta(ctx, merged, "chaosDamageMultiplier");
        copyMeta(ctx, merged, "chaosDamageUp");
        copyMeta(ctx, merged, "chaosDamageDown");
        copyMeta(ctx, merged, "chaosOverload");
        copyMeta(ctx, merged, "chaosUnstableGuard");
        copyMeta(ctx, merged, "chaosFailAction");
        copyMeta(ctx, merged, "elementalDuality");
        copyMeta(ctx, merged, "elementalMode");
        copyMeta(ctx, merged, "elementalNextMode");
        copyMeta(ctx, merged, "elementalModeLabel");
        copyMeta(ctx, merged, "elementalNextModeLabel");
        copyMeta(ctx, merged, "elementalApplied");
        copyMeta(ctx, merged, "elementalApplyChance");
        copyMeta(ctx, merged, "elementalEffect");
        copyMeta(ctx, merged, "elementalBurnApplied");
        copyMeta(ctx, merged, "elementalFrozenApplied");
        copyMeta(ctx, merged, "ultimateUsed");
        copyMeta(ctx, merged, "ultimateId");
        copyMeta(ctx, merged, "ultimateName");
        copyMeta(ctx, merged, "ultimateType");
        copyMeta(ctx, merged, "ultimateMultiplier");
        copyMeta(ctx, merged, "ultimateCritBonus");
        copyMeta(ctx, merged, "ultimateStaggerApplied");
        copyMeta(ctx, merged, "secondStageCharge");
        return merged;
    }

    /** Copia una metadada del context si existeix. */
    private void copyMeta(HitContext ctx, Map<String, Object> target, String key) {
        Object value = ctx.getMeta(key);
        if (value != null) target.put(key, value);
    }

    /**
     * Resol un torn sense atac directe.
     */
    private TurnResult resolveNonAttackTurn(
            Character attacker,
            Character defender,
            Action attackerAction,
            Action defenderAction,
            CombatMessageBuffer startMessages,
            Map<String, Object> startTurnMeta) {

        CombatMessageBuffer endTurnMessages = new CombatMessageBuffer();

        if (attackerAction == CHARGE) {
            if (attacker.hasChargedAttack()) {
                endTurnMessages.styled(MessageColor.CYAN, MessageSymbol.WARNING,
                        attacker.getName() + " manté la càrrega; no s'acumula més.");
            } else {
                attacker.prepareChargedAttack();
                endTurnMessages.styled(MessageColor.CYAN, MessageSymbol.POSITIVE,
                        attacker.getName() + " concentra forces per al següent atac.");
            }
        }

        decayMomentumOnPassiveTurn(attacker, attackerAction, endTurnMessages);

        if (breakAttackChains(attacker, defender)) {
            endTurnMessages.warning("La cadena del verí es trenca i el verí s'esvaeix.");
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
        Weapon weapon = attacker.getWeapon();

        return new TurnResult(
                attacker.getName(),
                null,
                startMessages.messages(),
                List.of(),
                defenseMessage,
                List.of(),
                endTurnMessages.messages(),
                0,
                false,
                false,
                false,
                false,
                null,
                0,
                0.0,
                0.0,
                getWeaponId(weapon),
                getWeaponName(weapon),
                mergeTurnMeta(startTurnMeta, ctx));
    }

    /**
     * Prepara el context d'impacte amb les dades de l'atac.
     */
    private void configureHitContext(HitContext ctx, Character attacker, AttackResult attackResult, Weapon weapon) {
        if (isFallbackAttack(attackResult)) {
            double damage = Math.max(0.0, attackResult.damage());

            ctx.setBaseDamage(damage);
            ctx.setCriticalChance(0.0);
            ctx.setCriticalMultiplier(1.0);

            ctx.putMeta("WEAPON_NAME", "Improvisació");
            ctx.putMeta("RAW_DAMAGE", damage);
            ctx.putMeta("ORIGINAL_WEAPON_CRIT", false);
            ctx.putMeta("CRIT", false);
            return;
        }

        if (weapon != null) {
            Statistics attackerStats = attacker.getStatistics();

            double rolledDamage = Math.max(0.0001, weapon.lastAttackDamage());
            double nonCritDamage = Math.max(0.0, weapon.lastNonCriticalDamage());

            double skillMultiplier = attackResult.damage() / rolledDamage;
            double rebuiltBaseDamage = round2(nonCritDamage * skillMultiplier);

            if (rebuiltBaseDamage <= 0.0 && attackResult.damage() > 0.0) {
                rebuiltBaseDamage = attackResult.damage();
            }

            ctx.setBaseDamage(rebuiltBaseDamage);
            ctx.setCriticalChance(weapon.resolveCriticalChance(attackerStats));
            ctx.setCriticalMultiplier(weapon.resolveCriticalMultiplier(attackerStats));

            ctx.putMeta("WEAPON_NAME", weapon.getName());
            ctx.putMeta("RAW_DAMAGE", rebuiltBaseDamage);
            ctx.putMeta("ORIGINAL_WEAPON_CRIT", weapon.lastWasCritic());
        } else {
            double damage = Math.max(0.0, attackResult.damage());

            ctx.setBaseDamage(damage);
            ctx.setCriticalChance(0.0);
            ctx.setCriticalMultiplier(1.0);

            ctx.putMeta("WEAPON_NAME", "Fists");
            ctx.putMeta("RAW_DAMAGE", damage);
            ctx.putMeta("ORIGINAL_WEAPON_CRIT", false);
        }

        ctx.putMeta("CRIT", false);
    }

    /**
     * Detecta atacs improvisats sense reconstruir el dany amb l'arma original.
     */
    private boolean isFallbackAttack(AttackResult attackResult) {
        return "FALLBACK".equals(attackResult.failKind());
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
            CombatMessageBuffer out) {

        if (defenderResult.recived() <= 0)
            return;

        if (defenderAction == Action.DEFEND && defender.isVulnerable()) {
            out.styled(MessageColor.RED, MessageSymbol.WARNING,
                    "La defensa trencada deixa " + defender.getName() + " exposat.");
        }

        if (critical) {
            defender.applyBleed(2);
            out.styled(MessageColor.RED, MessageSymbol.POSITIVE, "El cop crític obre una ferida: s'aplica sagnat.");
        }

        Object rawDamageInput = ctx.getMeta("RAW_DAMAGE");
        if (rawDamageInput instanceof Number rawDamage && ctx.damageDealt() >= rawDamage.doubleValue() * 0.90
                && defenderAction == Action.DODGE) {
            defender.applyBleed(1);
            out.styled(MessageColor.RED, MessageSymbol.POSITIVE, "L'esquiva fallida deixa un tall superficial.");
        }

        if (ctx.getMeta("CHARGED_HIT") instanceof Boolean charged && Boolean.TRUE.equals(charged)) {
            defender.applyStagger(1);
            out.styled(MessageColor.YELLOW, MessageSymbol.POSITIVE, "L'impacte carregat desequilibra el rival.");
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
            CombatMessageBuffer out) {

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
                    out.styled(MessageColor.CYAN, MessageSymbol.POSITIVE, attacker.getName() + " guanya impuls.");
                }
            } else if (out != null && attacker.getMomentumStacks() > 0) {
                out.styled(MessageColor.CYAN, MessageSymbol.EQUAL, "L'avantatge de " + attacker.getName()
                        + " no accelera més davant un rival acorralat.");
            }

            if (defender.getMomentumStacks() > 0) {
                defender.loseMomentum();
                if (out != null)
                    out.styled(MessageColor.CYAN, MessageSymbol.NEGATIVE,
                            defender.getName() + " perd impuls sota la pressió rival.");
            }
            return;
        }

        if (successfulDodge) {
            int before = defender.getMomentumStacks();
            defender.gainMomentum();
            if (defender.isDesperate()
                    || defender.healthRatio() + momentumConfig.suppressionHealthOffset() < attacker.healthRatio()) {
                defender.gainMomentum();
            }

            if (defender.getMomentumStacks() > before && out != null) {
                out.styled(MessageColor.CYAN, MessageSymbol.POSITIVE,
                        defender.getName() + " llegeix el ritme i guanya impuls.");
            }
            if (attacker.getMomentumStacks() > 0) {
                attacker.loseMomentum();
                if (out != null)
                    out.styled(MessageColor.CYAN, MessageSymbol.NEGATIVE,
                            attacker.getName() + " perd impuls després de fallar.");
            }
            return;
        }

        if (attacker.getMomentumStacks() > 0) {
            attacker.loseMomentum();
            if (out != null)
                out.styled(MessageColor.CYAN, MessageSymbol.NEGATIVE, attacker.getName() + " perd part de l'impuls.");
        }
    }

    /**
     * Redueix l'impuls en torns passius.
     */
    private void decayMomentumOnPassiveTurn(Character actor, Action action, CombatMessageBuffer out) {
        if (actor == null || action == null || actor.getMomentumStacks() <= 0) {
            return;
        }

        if (action == Action.DEFEND || action == Action.CHARGE) {
            if (action == Action.CHARGE && actor.isDesperate()) {
                return;
            }
            actor.loseMomentum();
            if (out != null) {
                out.styled(MessageColor.CYAN, MessageSymbol.NEGATIVE,
                        "L'impuls de " + actor.getName() + " es refreda una mica.");
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
     * Detecta si un atac ha fallat sense importar-ne l'origen.
     */
    private boolean isMiss(boolean attackFailed, double damageToResolve, double damageDealt) {
        return attackFailed || (damageToResolve > 0 && damageDealt <= 0);
    }

    /**
     * Estima el multiplicador propi del Grimori a partir del dany generat per l'arma.
     */
    private double resolveGrimoireMultiplier(Weapon weapon, AttackResult attackResult) {
        if (!isGrimori(weapon) || attackResult == null) return 0.0;
        double rolledDamage = Math.max(0.0001, weapon.lastAttackDamage());
        return round2(attackResult.damage() / rolledDamage);
    }

    /**
     * Indica si l'arma és un Grimori.
     */
    private boolean isGrimori(Weapon weapon) {
        return weapon != null && ("GRIMORI".equals(weapon.getId()) || "GRIMORIE".equals(weapon.getId()));
    }

    /**
     * Recull informació del trigger de Caos resolt a l'inici del torn.
     */
    private Map<String, Object> chaosStartMeta(Character attacker, Action selectedAction, Action finalAction) {
        Map<String, Object> meta = new HashMap<>();
        if (attacker == null || !attacker.hasEffect(Chaos.INTERNAL_EFFECT_KEY)) {
            return meta;
        }
        if (attacker.getEffect(Chaos.INTERNAL_EFFECT_KEY) instanceof Chaos chaos) {
            meta.put("chaosActive", true);
            meta.put("chaosTriggered", chaos.lastOutcomeName() != null);
            meta.put("chaosOutcome", chaos.lastOutcomeName());
            meta.put("chaosOutcomeLabel", chaos.lastOutcomeLabel());
            meta.put("chaosOutcomeSevere", chaos.lastOutcomeSevere());
            meta.put("chaosSelectedAction", selectedAction == null ? null : selectedAction.name());
            meta.put("chaosFinalAction", finalAction == null ? null : finalAction.name());
            meta.put("chaosActionChanged", selectedAction != null && finalAction != null && selectedAction != finalAction);
        }
        return meta;
    }

    /**
     * Arrodoneix a dues xifres decimals.
     */
    private static double round2(double n) {
        return Math.round(n * 100.0) / 100.0;
    }

    private static String getWeaponId(Weapon weapon) {
        if (weapon == null) return "UNARMED";
        return weapon.getId();
    }

    private static String getWeaponName(Weapon weapon) {
        if (weapon == null) return "Desarmat";
        return weapon.getName();
    }
}
