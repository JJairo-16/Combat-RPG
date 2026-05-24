package rpgcombat.combat.turnservice;

import java.util.List;
import java.util.Map;

import rpgcombat.combat.ui.messages.CombatMessage;
import rpgcombat.models.characters.Character;

/**
 * Representa el resultat complet d'un torn de combat.
 */
public record TurnResult(
        Character actor,
        String actorName,
        String attackerMessage,
        List<CombatMessage> startMessages,
        List<CombatMessage> preDefenseMessages,
        String defenseMessage,
        List<CombatMessage> postDefenseMessages,
        List<CombatMessage> endTurnMessages,
        double damageDealt,
        boolean critical,
        boolean selfHit,
        boolean chargedHit,
        boolean missed,
        String failKind,
        double damageToResolve,
        double grimoireMultiplier,
        double lifeStolen,
        String weaponId,
        String weaponName,
        Map<String, Object> meta) {

    public TurnResult {
        meta = cleanMeta(meta);
        if (actorName == null && actor != null) {
            actorName = actor.getName();
        }
    }

    public TurnResult(
            String actorName,
            String attackerMessage,
            List<CombatMessage> startMessages,
            List<CombatMessage> preDefenseMessages,
            String defenseMessage,
            List<CombatMessage> postDefenseMessages,
            List<CombatMessage> endTurnMessages,
            double damageDealt,
            boolean critical,
            boolean selfHit,
            boolean chargedHit,
            boolean missed,
            String failKind,
            double damageToResolve,
            double grimoireMultiplier,
            double lifeStolen,
            String weaponId,
            String weaponName,
            Map<String, Object> meta) {
        this(null, actorName, attackerMessage, startMessages, preDefenseMessages, defenseMessage,
                postDefenseMessages, endTurnMessages, damageDealt, critical, selfHit, chargedHit, missed,
                failKind, damageToResolve, grimoireMultiplier, lifeStolen, weaponId, weaponName, meta);
    }

    public TurnResult(
            String actorName,
            String attackerMessage,
            List<CombatMessage> startMessages,
            List<CombatMessage> preDefenseMessages,
            String defenseMessage,
            List<CombatMessage> postDefenseMessages,
            List<CombatMessage> endTurnMessages,
            double damageDealt,
            boolean critical) {
        this(actorName, attackerMessage, startMessages, preDefenseMessages, defenseMessage, postDefenseMessages,
                endTurnMessages, damageDealt, critical, false, false, damageDealt <= 0, null, damageDealt, 0.0, 0.0,
                null, null, Map.of());
    }

    public TurnResult(
            String actorName,
            String attackerMessage,
            List<CombatMessage> startMessages,
            List<CombatMessage> preDefenseMessages,
            String defenseMessage,
            List<CombatMessage> postDefenseMessages,
            List<CombatMessage> endTurnMessages,
            double damageDealt,
            boolean critical,
            boolean selfHit,
            boolean chargedHit) {
        this(actorName, attackerMessage, startMessages, preDefenseMessages, defenseMessage, postDefenseMessages,
                endTurnMessages, damageDealt, critical, selfHit, chargedHit, damageDealt <= 0, null, damageDealt, 0.0,
                0.0, null, null, Map.of());
    }

    public TurnResult(
            String actorName,
            String attackerMessage,
            List<CombatMessage> startMessages,
            List<CombatMessage> preDefenseMessages,
            String defenseMessage,
            List<CombatMessage> postDefenseMessages,
            List<CombatMessage> endTurnMessages,
            double damageDealt,
            boolean critical,
            boolean selfHit,
            boolean chargedHit,
            boolean missed,
            String failKind,
            double damageToResolve,
            double grimoireMultiplier,
            double lifeStolen,
            String weaponId,
            String weaponName) {
        this(actorName, attackerMessage, startMessages, preDefenseMessages, defenseMessage, postDefenseMessages,
                endTurnMessages, damageDealt, critical, selfHit, chargedHit, missed, failKind, damageToResolve,
                grimoireMultiplier, lifeStolen, weaponId, weaponName, Map.of());
    }

    /** Indica si l'atac ha fallat per qualsevol motiu conegut. */
    public boolean failed() {
        return failKind != null && !failKind.isBlank();
    }

    /** Retorna una metadada flexible del torn. */
    public Object meta(String key) {
        return key == null ? null : meta.get(key);
    }

    /** Retorna una metadada booleana. */
    public boolean booleanMeta(String key) {
        Object value = meta(key);
        if (value instanceof Boolean b)
            return b;
        if (value instanceof Number n)
            return n.doubleValue() != 0.0;
        return value != null && Boolean.parseBoolean(String.valueOf(value));
    }

    /** Retorna una metadada numèrica. */
    public double numericMeta(String key) {
        Object value = meta(key);
        if (value instanceof Number n)
            return n.doubleValue();
        if (value instanceof Boolean b)
            return Boolean.TRUE.equals(b) ? 1.0 : 0.0;
        if (value != null) {
            try {
                return Double.parseDouble(String.valueOf(value));
            } catch (NumberFormatException ignored) {
            }
        }
        return 0.0;
    }

    private static Map<String, Object> cleanMeta(Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }

        Map<String, Object> clean = new java.util.HashMap<>();

        for (Map.Entry<String, Object> entry : source.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                clean.put(entry.getKey(), entry.getValue());
            }
        }

        return Map.copyOf(clean);
    }
}
