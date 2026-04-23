package rpgcombat.models.effects.triggers;

import java.util.Random;

import rpgcombat.balance.CombatBalanceRegistry;
import rpgcombat.balance.config.FractureConfig;
import rpgcombat.models.characters.Character;
import rpgcombat.models.effects.EffectResult;
import rpgcombat.models.effects.impl.Fracture;
import rpgcombat.weapons.passives.HitContext;

/**
 * Trigger que pot aplicar l'efecte de fractura en rebre un cop crític.
 */
public class FractureTrigger extends Trigger {
    private FractureConfig config = CombatBalanceRegistry.get().fracture();

    public FractureTrigger() {
        super("FRACTURE_TRIGGER");
    }

    /**
     * S'executa després de rebre un cop. Si és crític, pot aplicar fractura.
     */
    @Override
    public EffectResult afterHit(HitContext ctx, Random rng, Character owner) {
        if (ctx.attacker().equals(owner))
            return EffectResult.none();

        if (!ctx.wasCritical())
            return EffectResult.none();

        double percent = getRatePercent(owner);
        if (rng.nextDouble() < percent) {
            owner.addEffect(new Fracture(config.duration()));
            return EffectResult.msg(owner.getName() + " ha rebut una fragtura.");
        }

        return EffectResult.none();
    }

    /**
     * Calcula la probabilitat d'aplicar fractura segons la constitució.
     */
    private double getRatePercent(Character player) {
        final double minRate = config.minRate();
        final double maxRate = config.maxRate();
        final int C = config.C();
        final int n = config.n();

        final int con = player.getStatistics().getConstitution();
        return minRate + (maxRate - minRate) / (1 + Math.pow(con / (double) C, n));
    }
}