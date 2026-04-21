package rpgcombat.weapons.passives;

import java.util.Random;

import rpgcombat.models.characters.Character;
import rpgcombat.models.characters.Statistics;
import rpgcombat.models.effects.impl.BlindEffect;
import rpgcombat.models.effects.impl.PoisonEffect;
import rpgcombat.utils.ui.Ansi;
import rpgcombat.weapons.Weapon;

/**
 * Fàbrica d'efectes passius d'arma.
 * Conté helpers per crear {@link WeaponPassive} reutilitzables.
 */
public final class Passives {
    private Passives() {
        // Classe utilitària: no instanciable.
    }

    private static final String HP = Ansi.RED + "HP" + Ansi.RESET;

    /**
     * Crea un passiu que cura l'atacant un percentatge del dany real infligit.
     *
     * @param pct percentatge de robatori de vida
     * @return passiu que s'aplica després d'encertar
     */
    public static WeaponPassive lifeSteal(double pct) {
        return new WeaponPassive() {
            @Override
            public String afterHit(Weapon weapon, HitContext ctx, Random rng) {
                double healAmount = ctx.damageDealt() * pct;
                double realHealed = ctx.attacker().getStatistics().heal(healAmount);

                if (realHealed <= 0) {
                    return null;
                }

                return String.format("%s roba %.1f %s",
                        ctx.attacker().getName(),
                        realHealed,
                        HP);
            }
        };
    }

    /**
     * Crea un passiu que aplica dany verdader després d'un impacte real.
     *
     * @param pct percentatge de vida màxima convertit en dany
     */
    public static WeaponPassive trueHarm(double pct) {
        return new WeaponPassive() {
            @Override
            public String afterHit(Weapon weapon, HitContext ctx, Random rng) {
                double opponentMaxHealth = ctx.defender().getStatistics().getMaxHealth();
                double extra = opponentMaxHealth * pct;

                if (extra <= 0) {
                    return null;
                }

                ctx.defender().getStatistics().damage(extra);

                return String.format("%s connecta un dany verdader del %.2f%%",
                        ctx.attacker().getName(),
                        roundPercent(pct));
            }
        };
    }

    /**
     * Passiu d'execució: quan l'enemic està per sota d'un llindar de vida,
     * augmenta el dany abans de defensar.
     *
     * @param thresholdLife ratio de vida
     * @param damageBonus bonus multiplicatiu extra
     */
    public static WeaponPassive executor(double thresholdLife, double damageBonus) {
        return new WeaponPassive() {
            @Override
            public String modifyDamage(Weapon weapon, HitContext ctx, Random rng) {
                Character defender = ctx.defender();
                Statistics defenderStats = defender.getStatistics();

                double ratio = defenderStats.getHealth() / defenderStats.getMaxHealth();
                if (ratio > thresholdLife) {
                    return null;
                }

                ctx.multiplyDamage(1.0 + damageBonus);

                return String.format("%s prepara una execució (+%d%% de dany)",
                        ctx.attacker().getName(),
                        roundPercent(damageBonus));
            }
        };
    }

    public static WeaponPassive blindOnHit(double applyProb, double missProb, int duration) {
        return new WeaponPassive() {
            @Override
            public String afterHit(Weapon weapon, HitContext ctx, Random rng) {
                if (ctx.damageDealt() <= 0) {
                    return null;
                }

                if (rng.nextDouble() > applyProb) {
                    return null;
                }

                ctx.defender().addEffect(new BlindEffect(duration, missProb));
                return ctx.defender().getName() + " queda encegat temporalment.";
            }
        };
    }

    /**
     * Passiu de verí acumulatiu.
     *
     * <p>
     * El cop actual llegeix els stacks de verí existents sobre el defensor
     * i hi suma dany extra. Si l'impacte connecta, afegeix 1 stack nou.
     * Si la cadena es trenca, el verí desapareix.
     * </p>
     */
    public static WeaponPassive poisonChain(double extraDamagePerStack, int softCapStart, double falloff) {
        return new WeaponPassive() {
            @Override
            public String modifyDamage(Weapon weapon, HitContext ctx, Random rng) {
                PoisonEffect poison = PoisonEffect.from(ctx.defender());
                if (poison == null) {
                    return null;
                }

                double bonus = poison.bonusDamage();
                if (bonus <= 0) {
                    return null;
                }

                ctx.addFlatDamage(bonus);

                return String.format(
                        "El verí amplifica el cop (+%.2f de dany amb %d càrregues).",
                        bonus,
                        poison.stacks());
            }

            @Override
            public String afterDefense(Weapon weapon, HitContext ctx, Random rng) {
                if (ctx.damageDealt() > 0) {
                    return null;
                }

                boolean removed = ctx.defender().removeEffect(PoisonEffect.INTERNAL_EFFECT_KEY);
                if (!removed) {
                    return null;
                }

                return "La cadena del verí es trenca i el verí s'esvaeix.";
            }

            @Override
            public String afterHit(Weapon weapon, HitContext ctx, Random rng) {
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

                return String.format(
                        "%s acumula verí (%d càrregues).",
                        ctx.defender().getName(),
                        stacks);
            }
        };
    }

    private static double roundPercent(double n) {
        return Math.round(n * 100.0);
    }
}