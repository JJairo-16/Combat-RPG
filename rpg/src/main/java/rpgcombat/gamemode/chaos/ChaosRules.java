package rpgcombat.gamemode.chaos;

import java.util.Random;

import rpgcombat.config.ui.CinematicsOptions;

/** Regles de mode que decideixen si s'aplica Caos a una partida. */
public record ChaosRules(
        ChaosActivation activation,
        double probability) {

    public ChaosRules {
        activation = activation == null ? ChaosActivation.PROBABILITY : activation;
        probability = Math.clamp(probability, 0.0, 1.0);
        if (activation == ChaosActivation.DISABLED) {
            probability = 0.0;
        } else if (activation == ChaosActivation.FORCED) {
            probability = 1.0;
        }
    }

    public static ChaosRules defaultRules() {
        return new ChaosRules(ChaosActivation.PROBABILITY, CinematicsOptions.CHAOS_RATE);
    }

    public static ChaosRules disabled() {
        return new ChaosRules(ChaosActivation.DISABLED, 0.0);
    }

    public boolean shouldActivate(Random rng) {
        return switch (activation) {
            case DISABLED -> false;
            case FORCED -> true;
            case PROBABILITY -> {
                Random effectiveRng = rng == null ? new Random() : rng;
                yield effectiveRng.nextDouble() < probability;
            }
        };
    }
}
