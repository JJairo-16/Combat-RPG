package rpgcombat.terrain.effects.triggers;

import java.util.Map;
import java.util.Random;

import rpgcombat.combat.models.Action;
import rpgcombat.combat.ui.messages.MessageColor;
import rpgcombat.combat.ui.messages.MessageSymbol;
import rpgcombat.models.characters.Character;
import rpgcombat.models.effects.EffectResult;
import rpgcombat.models.effects.triggers.Trigger;
import rpgcombat.weapons.passives.HitContext;

/**
 * Trigger de prova que valida la delegació personalitzada dels terrenys.
 */
public final class TestTerrainTrigger extends Trigger {
    public static final String INTERNAL_EFFECT_KEY = "TERRAIN_TEST";
    public static final double DEFAULT_ATTACK_DAMAGE_MULTIPLIER = 5.0;
    public static final double DEFAULT_DEFEND_DAMAGE_MULTIPLIER = 0.95;

    private final double attackDamageMultiplier;
    private final double defendDamageMultiplier;

    /**
     * Crea el trigger amb els paràmetres numèrics declarats al JSON.
     *
     * @param parameters paràmetres configurats pel terreny
     */
    public TestTerrainTrigger(Map<String, Double> parameters) {
        this(number(parameters, "attackDamageMultiplier", DEFAULT_ATTACK_DAMAGE_MULTIPLIER),
                number(parameters, "defendDamageMultiplier", DEFAULT_DEFEND_DAMAGE_MULTIPLIER));
    }

    /**
     * Crea el trigger amb multiplicadors ja resolts.
     *
     * @param attackDamageMultiplier multiplicador dels cops d'atac
     * @param defendDamageMultiplier multiplicador del dany rebut en defensa
     */
    public TestTerrainTrigger(double attackDamageMultiplier, double defendDamageMultiplier) {
        super(INTERNAL_EFFECT_KEY);
        this.attackDamageMultiplier = Math.clamp(attackDamageMultiplier, 0.0, 10.0);
        this.defendDamageMultiplier = Math.clamp(defendDamageMultiplier, 0.0, 2.0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EffectResult modifyDamage(HitContext ctx, Random rng, Character owner) {
        if (ctx == null || owner == null) {
            return EffectResult.none();
        }
        if (owner == ctx.attacker()) {
            ctx.multiplyDamage(attackDamageMultiplier);
            return EffectResult.gamemode(
                    MessageColor.CYAN,
                    MessageSymbol.INFO,
                    "El terreny de prova amplifica el cop.");
        }
        if (owner == ctx.defender() && ctx.defenderAction() == Action.DEFEND) {
            ctx.multiplyDamage(defendDamageMultiplier);
            return EffectResult.gamemode(
                    MessageColor.CYAN,
                    MessageSymbol.POSITIVE,
                    "El terreny de prova reforça la defensa.");
        }
        return EffectResult.none();
    }

    /**
     * Llegeix un paràmetre numèric del trigger amb un valor alternatiu segur.
     *
     * @param params paràmetres configurats
     * @param key clau a cercar
     * @param fallback valor usat quan la clau no existeix
     * @return valor resolt
     */
    private static double number(Map<String, Double> params, String key, double fallback) {
        if (params == null || key == null) {
            return fallback;
        }
        return params.getOrDefault(key, fallback);
    }
}
