package rpgcombat.achievements;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import rpgcombat.combat.models.Action;
import rpgcombat.combat.models.Winner;
import rpgcombat.combat.turnservice.TurnResult;
import rpgcombat.models.characters.Character;
import rpgcombat.weapons.Weapon;

/** Informació global d'un esdeveniment que pot fer avançar assoliments. */
public record AchievementUpdate(
        Character owner,
        Character opponent,
        Action ownerAction,
        Action opponentAction,
        TurnResult result,
        Winner winner,
        int roundNumber,
        Set<AchievementEvent> events,
        Map<String, String> text,
        Map<String, Double> numbers) {

    /** Crea una actualització a partir d'un torn de combat. */
    public static AchievementUpdate fromTurn(Character owner, Character opponent, Action ownerAction,
            Action opponentAction, TurnResult result, int roundNumber) {
        EnumSet<AchievementEvent> events = EnumSet.noneOf(AchievementEvent.class);
        Map<String, String> text = new LinkedHashMap<>();
        Map<String, Double> numbers = new LinkedHashMap<>();

        putCharacter(text, numbers, "owner", owner);
        putCharacter(text, numbers, "opponent", opponent);
        putAction(text, "ownerAction", ownerAction);
        putAction(text, "opponentAction", opponentAction);
        numbers.put("round", (double) roundNumber);

        if (ownerAction == Action.ATTACK) {
            events.add(AchievementEvent.ACTION_ATTACK);
            if (owner != null && owner.getWeapon() != null) events.add(AchievementEvent.WEAPON_USED);
        }
        if (ownerAction == Action.DEFEND) events.add(AchievementEvent.ACTION_DEFEND);
        if (ownerAction == Action.DODGE) events.add(AchievementEvent.ACTION_DODGE);
        if (ownerAction == Action.CHARGE) events.add(AchievementEvent.ACTION_CHARGE);

        if (result != null) {
            boolean dealt = result.damageDealt() > 0;
            boolean successfulAttack = ownerAction == Action.ATTACK && dealt && !result.selfHit();
            boolean successfulDodge = ownerAction == Action.DODGE
                    && opponentAction == Action.ATTACK
                    && !dealt
                    && !result.selfHit();

            if (successfulAttack) events.add(AchievementEvent.ACTION_SUCCESSFUL_ATTACK);
            if (successfulDodge) events.add(AchievementEvent.ACTION_SUCCESSFUL_DODGE);
            if (dealt) {
                events.add(AchievementEvent.HIT);
                events.add(AchievementEvent.DAMAGE_DEALT);
            }
            if (result.critical()) events.add(AchievementEvent.CRIT);
            if (result.chargedHit()) events.add(AchievementEvent.CHARGED_HIT);
            if (result.selfHit()) events.add(AchievementEvent.SELF_HIT);

            numbers.put("damage", Math.max(0.0, result.damageDealt()));
            numbers.put("damageDealt", Math.max(0.0, result.damageDealt()));
            numbers.put("incomingDamage", Math.max(0.0, result.damageDealt()));
            text.put("critical", Boolean.toString(result.critical()));
            text.put("chargedHit", Boolean.toString(result.chargedHit()));
            text.put("selfHit", Boolean.toString(result.selfHit()));
        } else {
            numbers.put("damage", 0.0);
            numbers.put("damageDealt", 0.0);
            numbers.put("incomingDamage", 0.0);
        }

        if (owner != null) {
            if (owner.healthRatio() <= 0.35) events.add(AchievementEvent.LOW_HEALTH);
            if (owner.getMomentumStacks() >= 2) events.add(AchievementEvent.HIGH_MOMENTUM);
            double staminaRatio = owner.getStatistics().getStamina() / Math.max(1.0, owner.getStatistics().getMaxStamina());
            if (staminaRatio <= 0.40) events.add(AchievementEvent.LOW_STAMINA);
            if (owner.isAlive()) events.add(AchievementEvent.SURVIVE_TURN);
            numbers.put("healthRatio", owner.healthRatio());
            numbers.put("staminaRatio", staminaRatio);
            numbers.put("momentum", (double) owner.getMomentumStacks());
            text.put("alive", Boolean.toString(owner.isAlive()));
        }

        return new AchievementUpdate(owner, opponent, ownerAction, opponentAction, result, Winner.NONE, roundNumber,
                Set.copyOf(events), Map.copyOf(text), Map.copyOf(numbers));
    }

    /** Crea una actualització global de final de combat. */
    public static AchievementUpdate fromMatchFinished(Character player1, Character player2, Winner winner,
            int roundNumber) {
        EnumSet<AchievementEvent> events = EnumSet.noneOf(AchievementEvent.class);
        Map<String, String> text = new LinkedHashMap<>();
        Map<String, Double> numbers = new LinkedHashMap<>();

        events.add(AchievementEvent.MATCH_FINISHED);
        if (winner == Winner.TIE) {
            events.add(AchievementEvent.MATCH_TIED);
        } else if (winner == Winner.PLAYER1 || winner == Winner.PLAYER2) {
            events.add(AchievementEvent.MATCH_WON);
            events.add(AchievementEvent.MATCH_LOST);
        }

        putCharacter(text, numbers, "player1", player1);
        putCharacter(text, numbers, "player2", player2);
        text.put("winner", winner == null ? Winner.NONE.name() : winner.name());
        numbers.put("round", (double) roundNumber);

        return new AchievementUpdate(player1, player2, null, null, null, winner, roundNumber, Set.copyOf(events),
                Map.copyOf(text), Map.copyOf(numbers));
    }

    /** Crea una actualització simple d'un sol esdeveniment. */
    public static AchievementUpdate simple(Character owner, AchievementEvent event) {
        EnumSet<AchievementEvent> events = EnumSet.noneOf(AchievementEvent.class);
        if (event != null) events.add(event);
        Map<String, String> text = new LinkedHashMap<>();
        Map<String, Double> numbers = new LinkedHashMap<>();
        putCharacter(text, numbers, "owner", owner);
        numbers.put("amount", 1.0);
        return new AchievementUpdate(owner, null, null, null, null, Winner.NONE, 0, Set.copyOf(events),
                Map.copyOf(text), Map.copyOf(numbers));
    }

    /** Indica si s'ha produït un esdeveniment. */
    public boolean has(AchievementEvent event) {
        return event != null && events != null && events.contains(event);
    }

    /** Permet condicions sobre el nom textual d'un esdeveniment. */
    public boolean hasEventNamed(String eventName) {
        if (eventName == null || events == null) return false;
        return events.stream().anyMatch(event -> event.name().equalsIgnoreCase(eventName));
    }

    /** Retorna el valor associat a un esdeveniment. */
    public double amountFor(AchievementEvent event) {
        if (event == AchievementEvent.DAMAGE_DEALT && result != null) return Math.max(0.0, result.damageDealt());
        return has(event) ? 1.0 : 0.0;
    }

    /** Retorna un valor numèric configurat per clau. */
    public double amountFor(String key, AchievementEvent fallbackEvent) {
        Double value = number(key);
        if (value != null) return Math.max(0.0, value);
        return amountFor(fallbackEvent);
    }

    /** Retorna un atribut textual normalitzat. */
    public String text(String key) {
        if (key == null || text == null) return null;
        String normalized = normalizeKey(key);
        String value = text.get(normalized);
        if (value != null) return value;
        value = text.get(key);
        if (value != null) return value;
        for (Map.Entry<String, String> entry : text.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(key)) return entry.getValue();
        }
        return null;
    }

    /** Retorna un atribut numèric. */
    public Double number(String key) {
        if (key == null || numbers == null) return null;
        String normalized = normalizeKey(key);
        Double value = numbers.get(normalized);
        if (value != null) return value;
        value = numbers.get(key);
        if (value != null) return value;
        for (Map.Entry<String, Double> entry : numbers.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(key)) return entry.getValue();
        }
        return null;
    }

    private static void putCharacter(Map<String, String> text, Map<String, Double> numbers, String prefix, Character character) {
        if (character == null) return;
        text.put(normalizeKey(prefix + ".name"), character.getName());
        text.put(normalizeKey(prefix + ".breed"), character.getBreed() == null ? null : character.getBreed().getClass().getSimpleName());
        text.put(normalizeKey(prefix + ".alive"), Boolean.toString(character.isAlive()));
        numbers.put(normalizeKey(prefix + ".healthRatio"), character.healthRatio());
        numbers.put(normalizeKey(prefix + ".momentum"), (double) character.getMomentumStacks());
        Weapon weapon = character.getWeapon();
        if (weapon != null) {
            text.put(normalizeKey(prefix + ".weapon.id"), weapon.getId());
            text.put(normalizeKey(prefix + ".weapon.name"), weapon.getName());
            text.put(normalizeKey(prefix + ".weapon.type"), weapon.getType() == null ? null : weapon.getType().name());
            text.put(normalizeKey("weapon.id"), weapon.getId());
            text.put(normalizeKey("weapon.name"), weapon.getName());
            text.put(normalizeKey("weapon.type"), weapon.getType() == null ? null : weapon.getType().name());
        }
    }

    private static void putAction(Map<String, String> text, String key, Action action) {
        if (action != null) text.put(normalizeKey(key), action.name());
    }

    private static String normalizeKey(String key) {
        return key == null ? null : key.trim().toLowerCase(Locale.ROOT);
    }
}
