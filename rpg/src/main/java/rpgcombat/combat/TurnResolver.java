package rpgcombat.combat;

import static rpgcombat.combat.Action.ATTACK;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import rpgcombat.models.characters.Character;
import rpgcombat.models.characters.Result;
import rpgcombat.models.characters.Statistics;
import rpgcombat.models.weapons.Arsenal;
import rpgcombat.models.weapons.AttackResult;
import rpgcombat.models.weapons.Weapon;
import rpgcombat.models.weapons.passives.HitContext;
import rpgcombat.models.weapons.passives.HitContext.Event;
import rpgcombat.models.weapons.passives.HitContext.Phase;

public class TurnResolver {
    private final AttackResolver attackResolver;
    private final EffectPipeline effectPipeline;
    private final RoundRecoveryService recoveryService;

    public TurnResolver(
            AttackResolver attackResolver,
            EffectPipeline effectPipeline,
            RoundRecoveryService recoveryService) {
        this.attackResolver = attackResolver;
        this.effectPipeline = effectPipeline;
        this.recoveryService = recoveryService;
    }

    public TurnResult resolveTurn(
            Character attacker,
            Character defender,
            Action attackerAction,
            Action defenderAction,
            EndRoundRegenBonus defenderBonus) {

        if (attackerAction != ATTACK) {
            return resolveNonAttackTurn(defender, defenderAction);
        }

        List<String> startMessages = new ArrayList<>();
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
            if (damage > 0) {
                attacker.getDamage(damage);
            }

            return new TurnResult(
                    attacker.getName(),
                    attackerMessage,
                    startMessages,
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    damage,
                    false);
        }

        Random attackerRng = attacker.rng();
        Random defenderRng = defender.rng();

        HitContext ctx = new HitContext(attacker, defender, weapon, attackerRng, attackerAction, defenderAction);
        ctx.setAttackResult(attackResult);

        configureHitContext(ctx, attacker, attackResult, weapon);

        effectPipeline.runAttackerOnly(ctx, Phase.START_TURN, attacker, attackerRng, startMessages);

        effectPipeline.runPhase(ctx, Phase.BEFORE_ATTACK, attacker, defender, weapon, attackerRng, defenderRng, preDefenseMessages);
        effectPipeline.runPhase(ctx, Phase.ROLL_CRIT, attacker, defender, weapon, attackerRng, defenderRng, preDefenseMessages);

        boolean critical = ctx.resolveCritical();
        if (critical) {
            preDefenseMessages.add("cop crític!");
        }

        effectPipeline.runPhase(ctx, Phase.MODIFY_DAMAGE, attacker, defender, weapon, attackerRng, defenderRng, preDefenseMessages);
        effectPipeline.runPhase(ctx, Phase.BEFORE_DEFENSE, attacker, defender, weapon, attackerRng, defenderRng, preDefenseMessages);

        double damageToResolve = ctx.damageToResolve();

        Result defenderResult = attackResolver.resolveAttack(damageToResolve, defender, defenderAction);
        ctx.setDefenderResult(defenderResult);
        ctx.setDamageDealt(defenderResult.recived());

        recoveryService.registerDefenseBonus(defenderAction, defenderResult, damageToResolve, defenderBonus);

        if (hasWeapon) {
            weapon.registerResolvedAttack(ctx.wasCritical(), damageToResolve);
        }

        registerCombatEvents(ctx, defender, defenderAction);

        String defenseMessage = null;
        if (defenderResult.recived() != -1) {
            String msg = defenderResult.message();
            if (msg != null && !msg.isBlank()) {
                defenseMessage = msg;
            }
        }

        effectPipeline.runPhase(ctx, Phase.AFTER_DEFENSE, attacker, defender, weapon, attackerRng, defenderRng, postDefenseMessages);

        if (ctx.damageDealt() > 0) {
            effectPipeline.runPhase(ctx, Phase.AFTER_HIT, attacker, defender, weapon, attackerRng, defenderRng, postDefenseMessages);
        }

        effectPipeline.runAttackerOnly(ctx, Phase.END_TURN, attacker, attackerRng, endTurnMessages);

        return new TurnResult(
                attacker.getName(),
                attackerMessage,
                startMessages,
                preDefenseMessages,
                defenseMessage,
                postDefenseMessages,
                endTurnMessages,
                ctx.damageDealt(),
                critical);
    }

    private TurnResult resolveNonAttackTurn(Character defender, Action defenderAction) {
        Result defenderResult = attackResolver.resolveAttack(0, defender, defenderAction);
        String defenseMessage = defenderResult.message();

        return new TurnResult(
                defender.getName(),
                null,
                List.of(),
                List.of(),
                defenseMessage,
                List.of(),
                List.of(),
                0,
                false);
    }

    private void configureHitContext(
            HitContext ctx,
            Character attacker,
            AttackResult attackResult,
            Weapon weapon) {

        if (weapon != null) {
            Statistics attackerStats = attacker.getStatistics();

            double rolledDamage = Math.max(0.0001, weapon.lastAttackDamage());
            double nonCritDamage = Math.max(0.0, weapon.lastNonCriticalDamage());

            double skillMultiplier = (rolledDamage > 0.0)
                    ? attackResult.damage() / rolledDamage
                    : 1.0;

            double rebuiltBaseDamage = round2(nonCritDamage * skillMultiplier);
            if (rebuiltBaseDamage <= 0 && attackResult.damage() > 0) {
                rebuiltBaseDamage = attackResult.damage();
            }

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

            if (!defender.isAlive()) {
                ctx.registerEvent(Event.ON_KILL);
            }
        }
    }

    private static boolean isGrimori(Weapon w) {
        try {
            return w != null && w.getId() == Arsenal.GRIMORIE;
        } catch (Exception e) {
            return false;
        }
    }

    private static double round2(double n) {
        return Math.round(n * 100.0) / 100.0;
    }
}