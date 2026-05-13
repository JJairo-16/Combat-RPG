package rpgcombat.weapons.attack;

import java.util.Map;

/**
 * Representa el resultat d'una acció d'atac.
 *
 * @param damage quantitat de dany generat per l'atac
 * @param message missatge descriptiu associat a l'acció
 * @param target objectiu real del dany ({@link Target})
 * @param failKind causa opcional de fallada de l'atac/habilitat. És {@code null}
 *                 quan l'atac s'ha resolt amb normalitat.
 * @param meta dades flexibles generades per l'arma o habilitat perquè altres
 *             sistemes, com els assoliments, puguin reaccionar sense acoblament.
 *
 * <p>És un record immutable que encapsula tota la informació necessària
 * perquè el sistema de combat resolgui l'aplicació del dany i mostri
 * el missatge corresponent.</p>
 */
public record AttackResult(double damage, String message, Target target, String failKind, Map<String, Object> meta) {

    public static final String FAIL_KIND_RESOURCE = "RESOURCE";
    public static final String FAIL_KIND_SKILL = "SKILL";
    public static final String FAIL_KIND_FALLBACK = "FALLBACK";

    /** Normalitza metadades nul·les. */
    public AttackResult {
        meta = meta == null ? Map.of() : Map.copyOf(meta);
    }

    /** Crea un resultat d'atac assumint que l'objectiu és l'enemic i sense cap fallada especial. */
    public AttackResult(double damage, String message) {
        this(damage, message, Target.ENEMY, null, Map.of());
    }

    /** Crea un resultat d'atac amb objectiu explícit i sense cap fallada especial. */
    public AttackResult(double damage, String message, Target target) {
        this(damage, message, target, null, Map.of());
    }

    /** Crea un resultat d'atac amb objectiu, fallada i sense metadades. */
    public AttackResult(double damage, String message, Target target, String failKind) {
        this(damage, message, target, failKind, Map.of());
    }

    /** Crea un resultat d'atac amb metadades flexibles. */
    public AttackResult(double damage, String message, Map<String, Object> meta) {
        this(damage, message, Target.ENEMY, null, meta);
    }

    /** Factoria per a fallades per manca de recursos. */
    public static AttackResult resourceFail(String message) {
        return new AttackResult(0, message, Target.ENEMY, FAIL_KIND_RESOURCE, Map.of());
    }

    /** Factoria per a fallades pròpies de l'habilitat/arma. */
    public static AttackResult skillFail(String message) {
        return new AttackResult(0, message, Target.ENEMY, FAIL_KIND_SKILL, Map.of());
    }

    public static AttackResult fallback(double damage, String message) {
        return new AttackResult(damage, message, Target.ENEMY, FAIL_KIND_FALLBACK, Map.of());
    }

    public boolean failed() {
        return failKind != null && !failKind.isBlank();
    }

    public boolean resourceFailed() {
        return FAIL_KIND_RESOURCE.equals(failKind);
    }

    public boolean skillFailed() {
        return FAIL_KIND_SKILL.equals(failKind);
    }

    /** Retorna una metadada concreta. */
    public Object meta(String key) {
        return key == null ? null : meta.get(key);
    }

    /** Retorna una metadada booleana. */
    public boolean booleanMeta(String key) {
        Object value = meta(key);
        if (value instanceof Boolean b) return b;
        if (value instanceof Number n) return n.doubleValue() != 0.0;
        return value != null && Boolean.parseBoolean(String.valueOf(value));
    }

    /** Retorna una metadada numèrica. */
    public double numericMeta(String key) {
        Object value = meta(key);
        if (value instanceof Number n) return n.doubleValue();
        if (value instanceof Boolean b) return Boolean.TRUE.equals(b) ? 1.0 : 0.0;
        if (value != null) {
            try { return Double.parseDouble(String.valueOf(value)); } catch (NumberFormatException ignored) {}
        }
        return 0.0;
    }
}
