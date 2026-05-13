package rpgcombat.gamemode.cinematics;

import java.util.List;
import java.util.Random;

/** Selecció declarativa de cinemàtiques per mode de joc. */
public record ModeCinematics(
        List<String> postCreationPool,
        String chaosPostCreation) {

    public static final String DEFAULT_RANDOM = "DEFAULT_RANDOM";
    public static final String CHAOS_MIND = "CHAOS_MIND";

    public ModeCinematics {
        postCreationPool = normalize(postCreationPool);
        chaosPostCreation = blankToNull(chaosPostCreation);
    }

    public static ModeCinematics normal() {
        return new ModeCinematics(List.of(DEFAULT_RANDOM), CHAOS_MIND);
    }

    public static ModeCinematics beginner() {
        return new ModeCinematics(List.of("BEGINNER_INTRO"), null);
    }

    public String choosePostCreation(Random rng, boolean chaosActive) {
        if (chaosActive && chaosPostCreation != null) {
            return chaosPostCreation;
        }
        if (postCreationPool.isEmpty()) {
            return DEFAULT_RANDOM;
        }
        Random effectiveRng = rng == null ? new Random() : rng;
        return postCreationPool.get(effectiveRng.nextInt(postCreationPool.size()));
    }

    private static List<String> normalize(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return List.of(DEFAULT_RANDOM);
        }
        List<String> normalized = keys.stream()
                .filter(key -> key != null && !key.isBlank())
                .map(String::trim)
                .toList();
        return normalized.isEmpty() ? List.of(DEFAULT_RANDOM) : List.copyOf(normalized);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
