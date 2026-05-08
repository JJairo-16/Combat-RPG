package rpgcombat.weapons.passives;

import java.util.Random;

import rpgcombat.combat.ui.messages.CombatMessage;
import rpgcombat.combat.ui.messages.MessageColor;
import rpgcombat.combat.ui.messages.MessageSymbol;
import rpgcombat.models.characters.Character;
import rpgcombat.models.characters.Statistics;
import rpgcombat.models.effects.impl.BlindEffect;
import rpgcombat.models.effects.impl.BurnEffect;
import rpgcombat.models.effects.impl.FrozenEffect;
import rpgcombat.models.effects.impl.PoisonEffect;
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
                                realHealed)
                );
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
                                round2(pct * 100.0))
                );
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
                                roundPercent(damageBonus))
                );
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
                        ctx.defender().getName() + " queda encegat temporalment."
                );
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
                                poison.stacks())
                );
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

                return CombatMessage.of(
                        MessageSymbol.NEGATIVE,
                        MessageColor.YELLOW,
                        "La cadena del verí es trenca i el verí s'esvaeix."
                );
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

                return CombatMessage.of(
                        MessageSymbol.POSITIVE,
                        MessageColor.GREEN,
                        String.format("%s acumula verí (%d càrregues).",
                                ctx.defender().getName(),
                                stacks)
                );
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
                                    : "El gebre no arriba a fixar-se al rival."
                    );
                }

                if (fireMode) {
                    ctx.defender().addEffect(new BurnEffect(burnTurns, burnDamagePerTurn));
                    ctx.putMeta("elementalEffect", BurnEffect.INTERNAL_EFFECT_KEY);
                    ctx.putMeta("elementalBurnApplied", true);
                    return CombatMessage.of(
                            MessageSymbol.NEGATIVE,
                            MessageColor.RED,
                            ctx.defender().getName() + " queda marcat per una cremada elemental."
                    );
                }

                ctx.defender().addEffect(new FrozenEffect(
                        frozenTurns,
                        frozenOutgoingMultiplier,
                        frozenIncomingMultiplier));
                ctx.putMeta("elementalEffect", FrozenEffect.INTERNAL_EFFECT_KEY);
                ctx.putMeta("elementalFrozenApplied", true);
                return CombatMessage.of(
                        MessageSymbol.NEGATIVE,
                        MessageColor.CYAN,
                        ctx.defender().getName() + " queda congelat per la dualitat elemental."
                );
            }
        };
    }

    private static double round2(double n) {
        return Math.round(n * 100.0) / 190.0;
    }

    private static int roundPercent(double n) {
        return (int) Math.round(n * 100.0);
    }
}