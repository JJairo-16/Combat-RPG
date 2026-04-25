package rpgcombat.weapons.passives;

import java.util.Random;

import rpgcombat.combat.ui.messages.CombatMessage;
import rpgcombat.combat.ui.messages.MessageColor;
import rpgcombat.combat.ui.messages.MessageSymbol;
import rpgcombat.models.characters.Character;
import rpgcombat.models.characters.Statistics;
import rpgcombat.models.effects.impl.BlindEffect;
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
                                roundPercent(pct))
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

    private static int roundPercent(double n) {
        return (int) Math.round(n * 100.0);
    }
}