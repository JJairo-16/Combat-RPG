package rpgcombat.weapons.passives;

import java.util.Random;

import rpgcombat.combat.ui.messages.CombatMessage;
import rpgcombat.combat.ui.messages.MessageColor;
import rpgcombat.combat.ui.messages.MessageSymbol;
import rpgcombat.models.characters.Character;
import rpgcombat.models.characters.Statistics;
import rpgcombat.models.effects.impl.BlindEffect;
import rpgcombat.models.effects.impl.elemental.BurnEffect;
import rpgcombat.models.effects.impl.elemental.ChilledEffect;
import rpgcombat.models.effects.impl.elemental.FrozenEffect;
import rpgcombat.models.effects.impl.elemental.PoisonEffect;
import rpgcombat.combat.models.Action;
import rpgcombat.weapons.Weapon;

/**
 * Fàbrica de passius d'arma.
 */
public final class Passives {
    private Passives() {
    }

    /**
     * Cura l'atacant segons el dany real infligit.
     */
    public static WeaponPassive lifeSteal(double pct) {
        return new WeaponPassive() {
            @Override
            public CombatMessage afterHit(Weapon weapon, HitContext ctx, Random rng) {
                double healAmount = ctx.damageDealt() * pct;
                if (ctx.damageDealt() > 0 && pct > 0) {
                    ctx.putMeta("lifeStealTriggered", true);
                    ctx.putMeta("lifeStealPct", pct);
                }
                double realHealed = ctx.attacker().getStatistics().heal(healAmount);
                if (realHealed > 0) {
                    double previous = ctx.getMeta("LIFE_STOLEN", Double.class, 0.0);
                    ctx.putMeta("LIFE_STOLEN", previous + realHealed);
                }

                if (realHealed <= 0) {
                    return null;
                }

                return CombatMessage.of(
                        MessageSymbol.POSITIVE,
                        MessageColor.GREEN,
                        String.format("%s roba %.1f HP",
                                ctx.attacker().getName(),
                                realHealed));
            }
        };
    }

    /**
     * Aplica dany verdader després d'un impacte real.
     */
    public static WeaponPassive trueHarm(double pct) {
        return new WeaponPassive() {
            @Override
            public CombatMessage afterHit(Weapon weapon, HitContext ctx, Random rng) {
                double opponentMaxHealth = ctx.defender().getStatistics().getMaxHealth();
                double extra = opponentMaxHealth * pct;

                if (extra <= 0) {
                    return null;
                }

                ctx.defender().getStatistics().damage(extra);

                return CombatMessage.of(
                        MessageSymbol.WARNING,
                        MessageColor.RED,
                        String.format("%s connecta un dany verdader del %.2f%%",
                                ctx.attacker().getName(),
                                round2(pct * 100.0)));
            }
        };
    }

    /**
     * Augmenta el dany si l'enemic té poca vida.
     */
    public static WeaponPassive executor(double thresholdLife, double damageBonus) {
        return new WeaponPassive() {
            @Override
            public CombatMessage modifyDamage(Weapon weapon, HitContext ctx, Random rng) {
                Character defender = ctx.defender();
                Statistics defenderStats = defender.getStatistics();

                double ratio = defenderStats.getHealth() / defenderStats.getMaxHealth();
                if (ratio > thresholdLife) {
                    return null;
                }

                ctx.multiplyDamage(1.0 + damageBonus);

                return CombatMessage.of(
                        MessageSymbol.WARNING,
                        MessageColor.YELLOW,
                        String.format("%s prepara una execució (+%d%% de dany)",
                                ctx.attacker().getName(),
                                roundPercent(damageBonus)));
            }
        };
    }

    /**
     * Pot aplicar ceguesa després d'un impacte.
     */
    public static WeaponPassive blindOnHit(double applyProb, double missProb, int duration) {
        return new WeaponPassive() {
            @Override
            public CombatMessage afterHit(Weapon weapon, HitContext ctx, Random rng) {
                if (ctx.damageDealt() <= 0) {
                    return null;
                }

                if (rng.nextDouble() > applyProb) {
                    return null;
                }

                ctx.defender().addEffect(new BlindEffect(duration, missProb));

                return CombatMessage.of(
                        MessageSymbol.NEGATIVE,
                        MessageColor.RED,
                        ctx.defender().getName() + " queda encegat temporalment.");
            }
        };
    }

    /**
     * Verí acumulatiu amb dany extra i trencament de cadena.
     */
    public static WeaponPassive poisonChain(double extraDamagePerStack, int softCapStart, double falloff) {
        return new WeaponPassive() {
            @Override
            public CombatMessage modifyDamage(Weapon weapon, HitContext ctx, Random rng) {
                PoisonEffect poison = PoisonEffect.from(ctx.defender());
                if (poison == null) {
                    return null;
                }

                double bonus = poison.bonusDamage();
                if (bonus <= 0) {
                    return null;
                }

                ctx.addFlatDamage(bonus);

                return CombatMessage.of(
                        MessageSymbol.POSITIVE,
                        MessageColor.GREEN,
                        String.format("El verí amplifica el cop (+%.2f de dany amb %d càrregues).",
                                bonus,
                                poison.stacks()));
            }

            @Override
            public CombatMessage afterDefense(Weapon weapon, HitContext ctx, Random rng) {
                if (ctx.damageDealt() > 0) {
                    return null;
                }

                boolean removed = ctx.defender().removeEffect(PoisonEffect.INTERNAL_EFFECT_KEY);
                if (!removed) {
                    return null;
                }

                return CombatMessage.statusEffect(
                        MessageSymbol.NEGATIVE,
                        MessageColor.DARK_GREEN,
                        "La cadena del verí es trenca i el verí s'esvaeix");
            }

            @Override
            public CombatMessage afterHit(Weapon weapon, HitContext ctx, Random rng) {
                if (ctx.damageDealt() <= 0) {
                    return null;
                }

                ctx.defender().addEffect(new PoisonEffect(
                        extraDamagePerStack,
                        softCapStart,
                        falloff,
                        1));

                PoisonEffect updated = PoisonEffect.from(ctx.defender());
                int stacks = (updated == null) ? 1 : updated.stacks();

                return CombatMessage.statusEffect(
                        MessageSymbol.POSITIVE,
                        MessageColor.DARK_GREEN,
                        String.format("Acumula verí (%d càrregues)",
                                stacks));
            }
        };
    }

    /**
     * Dualitat elemental: després d'un impacte real, aplica foc o gel segons el
     * mode calculat per l'habilitat de l'arma. El mode viatja per metadades per
     * evitar acoblar la passiva a una subclasse concreta d'arma.
     */
    public static WeaponPassive elementalDuality(
            double burnApplyProb,
            int burnTurns,
            double burnDamagePerTurn,
            double frozenApplyProb,
            int frozenTurns,
            double frozenOutgoingMultiplier,
            double frozenIncomingMultiplier) {
        return new WeaponPassive() {
            @Override
            public CombatMessage afterHit(Weapon weapon, HitContext ctx, Random rng) {
                if (ctx.damageDealt() <= 0) {
                    return null;
                }

                Object rawMode = ctx.attackResult() == null ? null : ctx.attackResult().meta("elementalMode");
                String mode = rawMode == null ? null : String.valueOf(rawMode);
                if (mode == null || mode.isBlank()) {
                    return null;
                }

                boolean fireMode = "FIRE".equals(mode);
                boolean frostMode = "FROST".equals(mode);
                if (!fireMode && !frostMode) {
                    return null;
                }

                double chance = fireMode ? burnApplyProb : frozenApplyProb;
                chance = Math.clamp(chance, 0.0, 1.0);

                boolean applied = rng.nextDouble() < chance;
                ctx.putMeta("elementalDuality", true);
                ctx.putMeta("elementalMode", mode);
                ctx.putMeta("elementalApplied", applied);
                ctx.putMeta("elementalApplyChance", chance);

                if (!applied) {
                    ctx.putMeta("elementalEffect", "NONE");
                    return CombatMessage.of(
                            MessageSymbol.EQUAL,
                            fireMode ? MessageColor.RED : MessageColor.CYAN,
                            fireMode
                                    ? "La flama no arriba a encendre el rival."
                                    : "El gebre no arriba a fixar-se al rival.");
                }

                if (fireMode) {
                    ctx.defender().addEffect(new BurnEffect(burnTurns, burnDamagePerTurn));
                    ctx.putMeta("elementalEffect", BurnEffect.INTERNAL_EFFECT_KEY);
                    ctx.putMeta("elementalBurnApplied", true);
                    return CombatMessage.statusEffect(
                            MessageSymbol.NEGATIVE,
                            MessageColor.RED,
                            "Queda marcat per una cremada elemental");
                }

                ctx.defender().addEffect(new FrozenEffect(
                        frozenTurns,
                        frozenOutgoingMultiplier,
                        frozenIncomingMultiplier));
                ctx.putMeta("elementalEffect", FrozenEffect.INTERNAL_EFFECT_KEY);
                ctx.putMeta("elementalFrozenApplied", true);
                return CombatMessage.statusEffect(
                        MessageSymbol.NEGATIVE,
                        MessageColor.CYAN,
                        "Queda congelat per la dualitat elemental");
            }
        };
    }

    /** Bonus moderat si tots dos combatents ataquen alhora. */
    public static WeaponPassive firstOathClash(double damageBonus) {
        return new WeaponPassive() {
            @Override
            public CombatMessage modifyDamage(Weapon weapon, HitContext ctx, Random rng) {
                if (ctx.attackerAction() != Action.ATTACK || ctx.defenderAction() != Action.ATTACK) {
                    return null;
                }
                ctx.multiplyDamage(1.0 + damageBonus);
                ctx.putMeta("firstOathClash", true);
                return CombatMessage.of(MessageSymbol.POSITIVE, MessageColor.YELLOW,
                        "El jurament respon quan dues ofensives xoquen.");
            }
        };
    }

    /** Recompensa un atac després d'haver acumulat guàrdia defensiva. */
    public static WeaponPassive guardCounter(double damageBonus, int minGuardStacks) {
        return new WeaponPassive() {
            @Override
            public CombatMessage modifyDamage(Weapon weapon, HitContext ctx, Random rng) {
                if (ctx.attacker().getGuardStacks() < minGuardStacks) {
                    return null;
                }
                ctx.multiplyDamage(1.0 + damageBonus);
                ctx.putMeta("guardCounter", true);
                return CombatMessage.of(MessageSymbol.POSITIVE, MessageColor.CYAN,
                        "La guàrdia acumulada torna el cop.");
            }
        };
    }

    /** Ajuda de comeback limitada quan el portador està en estat desesperat. */
    public static WeaponPassive ancestralBell(double damageBonus, double healAmount, int cooldownTurns) {
        return new WeaponPassive() {
            @Override
            public CombatMessage modifyDamage(Weapon weapon, HitContext ctx, Random rng) {
                if (!ctx.attacker().isDesperate()) {
                    return null;
                }
                ctx.multiplyDamage(1.0 + damageBonus);
                ctx.putMeta("ancestralBellDesperate", true);
                return CombatMessage.of(MessageSymbol.WARNING, MessageColor.CYAN,
                        "La campana ressona al límit.");
            }

            @Override
            public CombatMessage afterHit(Weapon weapon, HitContext ctx, Random rng) {
                if (ctx.damageDealt() <= 0 || !ctx.attacker().isDesperate()) {
                    tickCooldown(weapon, "ancestralBell.cooldown");
                    return null;
                }
                int cooldown = weapon.getMeta("ancestralBell.cooldown", Integer.class, 0);
                if (cooldown > 0) {
                    tickCooldown(weapon, "ancestralBell.cooldown");
                    return null;
                }
                double healed = ctx.attacker().getStatistics().heal(healAmount);
                weapon.putMeta("ancestralBell.cooldown", Math.max(1, cooldownTurns));
                ctx.putMeta("ancestralBellHeal", healed);
                if (healed <= 0) {
                    return null;
                }
                return CombatMessage.of(MessageSymbol.POSITIVE, MessageColor.GREEN,
                        ctx.attacker().getName() + " recupera " + round2(healed) + " HP amb l'eco ancestral.");
            }
        };
    }

    /** Castiga patrons repetits del rival. */
    public static WeaponPassive tacticalMirror(double damageBonus) {
        return new WeaponPassive() {
            @Override
            public CombatMessage modifyDamage(Weapon weapon, HitContext ctx, Random rng) {
                Action previous = weapon.getMeta("tacticalMirror.lastDefenderAction", Action.class, null);
                Action current = ctx.defenderAction();
                weapon.putMeta("tacticalMirror.lastDefenderAction", current);
                if (previous == null || current == null || previous != current) {
                    return null;
                }
                ctx.multiplyDamage(1.0 + damageBonus);
                ctx.putMeta("tacticalMirrorRead", true);
                return CombatMessage.of(MessageSymbol.POSITIVE, MessageColor.MAGENTA,
                        "El mirall llegeix la repetició del rival.");
            }
        };
    }

    /** Castiga una defensa llegida en el mateix torn. */
    public static WeaponPassive retaliationAgainstDefend(double damageBonus) {
        return new WeaponPassive() {
            @Override
            public CombatMessage modifyDamage(Weapon weapon, HitContext ctx, Random rng) {
                if (ctx.defenderAction() != Action.DEFEND) {
                    return null;
                }
                ctx.multiplyDamage(1.0 + damageBonus);
                ctx.putMeta("retaliationAgainstDefend", true);
                return CombatMessage.of(MessageSymbol.POSITIVE, MessageColor.YELLOW,
                        "La corda troba una guàrdia massa quieta.");
            }
        };
    }

    /**
     * Converteix ratxes sense crític en una petita probabilitat crítica temporal.
     */
    public static WeaponPassive badOmenCrit(double bonusPerStack, int maxStacks) {
        return new WeaponPassive() {
            @Override
            public CombatMessage rollCrit(Weapon weapon, HitContext ctx, Random rng) {
                int stacks = weapon.getMeta("badOmen.stacks", Integer.class, 0);
                double bonus = Math.clamp(stacks, 0, maxStacks) * bonusPerStack;
                if (bonus <= 0) {
                    return null;
                }
                ctx.setCriticalChance(ctx.criticalChance() + bonus);
                ctx.putMeta("badOmenCritBonus", bonus);
                return CombatMessage.of(MessageSymbol.WARNING, MessageColor.YELLOW,
                        "El mal presagi fa més probable el cop afortunat.");
            }

            @Override
            public CombatMessage endTurn(Weapon weapon, HitContext ctx, Random rng) {
                if (ctx.wasCritical()) {
                    weapon.putMeta("badOmen.stacks", 0);
                    ctx.putMeta("badOmenReset", true);
                } else {
                    int stacks = weapon.getMeta("badOmen.stacks", Integer.class, 0);
                    int next = Math.min(maxStacks, stacks + 1);
                    weapon.putMeta("badOmen.stacks", next);
                    ctx.putMeta("badOmenStacks", next);
                }
                
                return null;
            }
        };
    }

    /** Aplica fred menor després d'un impacte real. */
    public static WeaponPassive chillOnHit(double applyProb, int turns, double outgoingMultiplier) {
        return new WeaponPassive() {
            @Override
            public CombatMessage afterHit(Weapon weapon, HitContext ctx, Random rng) {
                if (ctx.damageDealt() <= 0 || rng.nextDouble() >= applyProb) {
                    return null;
                }
                ctx.defender().addEffect(new ChilledEffect(turns, outgoingMultiplier));
                ctx.putMeta("chilledApplied", true);
                return CombatMessage.of(MessageSymbol.NEGATIVE, MessageColor.CYAN,
                        ctx.defender().getName() + " queda alentit per un fred menor.");
            }
        };
    }

    private static void tickCooldown(Weapon weapon, String key) {
        int cooldown = weapon.getMeta(key, Integer.class, 0);
        if (cooldown > 0) {
            weapon.putMeta(key, cooldown - 1);
        }
    }

    private static double round2(double n) {
        return Math.round(n * 100.0) / 100.0;
    }

    private static int roundPercent(double n) {
        return (int) Math.round(n * 100.0);
    }
}
