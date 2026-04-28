package rpgcombat.utils.cinematic.typing;

/**
 * Defineix tons d'escriptura per modular el ritme.
 */
public enum TypingMood {
    NORMAL(1.0, 1.0, 1.0),
    WHISPER(1.20, 1.35, 1.15),
    TENSE(1.10, 1.50, 1.35),
    URGENT(0.62, 0.70, 0.70),
    DRAMATIC(1.28, 1.75, 1.45),
    FAST(0.70, 0.85, 0.85),
    SLOW(1.45, 1.55, 1.25);

    private final double speedMultiplier;
    private final double pauseMultiplier;
    private final double variationMultiplier;

    /**
     * Crea un to amb els seus multiplicadors.
     */
    TypingMood(double speedMultiplier, double pauseMultiplier, double variationMultiplier) {
        this.speedMultiplier = speedMultiplier;
        this.pauseMultiplier = pauseMultiplier;
        this.variationMultiplier = variationMultiplier;
    }

    /**
     * Retorna el multiplicador de velocitat.
     */
    public double speedMultiplier() {
        return speedMultiplier;
    }

    /**
     * Retorna el multiplicador de pauses.
     */
    public double pauseMultiplier() {
        return pauseMultiplier;
    }

    /**
     * Retorna el multiplicador de variació.
     */
    public double variationMultiplier() {
        return variationMultiplier;
    }

    /**
     * Converteix un nom de tag en un to.
     */
    public static TypingMood fromTag(String tagName) {
        if (tagName == null) return null;

        return switch (tagName.toLowerCase()) {
            case "normal" -> NORMAL;
            case "whisper" -> WHISPER;
            case "tense" -> TENSE;
            case "urgent" -> URGENT;
            case "dramatic" -> DRAMATIC;
            case "fast" -> FAST;
            case "slow" -> SLOW;
            default -> null;
        };
    }
}