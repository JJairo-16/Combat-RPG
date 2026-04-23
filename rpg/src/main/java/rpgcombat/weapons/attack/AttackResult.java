package rpgcombat.weapons.attack;

/**
 * Representa el resultat d'una acció d'atac.
 *
 * @param damage quantitat de dany generat per l'atac
 * @param message missatge descriptiu associat a l'acció
 * @param target objectiu real del dany ({@link Target})
 * @param failKind causa opcional de fallada de l'atac/habilitat. És {@code null}
 *                 quan l'atac s'ha resolt amb normalitat.
 *
 * <p>És un record immutable que encapsula tota la informació necessària
 * perquè el sistema de combat resolgui l'aplicació del dany i mostri
 * el missatge corresponent.</p>
 */
public record AttackResult(double damage, String message, Target target, String failKind) {

    public static final String FAIL_KIND_RESOURCE = "RESOURCE";
    public static final String FAIL_KIND_SKILL = "SKILL";

    /** Crea un resultat d'atac assumint que l'objectiu és l'enemic i sense cap fallada especial. */
    public AttackResult(double damage, String message) {
        this(damage, message, Target.ENEMY, null);
    }

    /** Crea un resultat d'atac amb objectiu explícit i sense cap fallada especial. */
    public AttackResult(double damage, String message, Target target) {
        this(damage, message, target, null);
    }

    /** Factoria per a fallades per manca de recursos. */
    public static AttackResult resourceFail(String message) {
        return new AttackResult(0, message, Target.ENEMY, FAIL_KIND_RESOURCE);
    }

    /** Factoria per a fallades pròpies de l'habilitat/arma. */
    public static AttackResult skillFail(String message) {
        return new AttackResult(0, message, Target.ENEMY, FAIL_KIND_SKILL);
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
}
