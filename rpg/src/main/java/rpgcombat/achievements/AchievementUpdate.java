package rpgcombat.achievements;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import rpgcombat.combat.models.Action;
import rpgcombat.combat.models.Winner;
import rpgcombat.combat.turnservice.TurnResult;
import rpgcombat.models.characters.Character;
import rpgcombat.models.characters.Statistics;
import rpgcombat.weapons.Weapon;
import rpgcombat.achievements.config.AchievementObjective;

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
        Map<String, Object> fields) {

    /** Constructor de compatibilitat. */
    public AchievementUpdate(
            Character owner,
            Character opponent,
            Action ownerAction,
            Action opponentAction,
            TurnResult result,
            Winner winner,
            int roundNumber,
            Set<AchievementEvent> events) {
        this(owner, opponent, ownerAction, opponentAction, result, winner, roundNumber, events, Map.of());
    }

    /** Crea una actualització a partir d'un torn de combat. */
    public static AchievementUpdate fromTurn(Character owner, Character opponent, Action ownerAction,
            Action opponentAction, TurnResult result, int roundNumber) {
        EnumSet<AchievementEvent> events = EnumSet.noneOf(AchievementEvent.class);
        Map<String, Object> fields = baseFields(owner, opponent, ownerAction, opponentAction, roundNumber);

        if (ownerAction == Action.ATTACK) events.add(AchievementEvent.ACTION_ATTACK);
        if (ownerAction == Action.DEFEND) events.add(AchievementEvent.ACTION_DEFEND);
        if (ownerAction == Action.DODGE) events.add(AchievementEvent.ACTION_DODGE);
        if (ownerAction == Action.CHARGE) events.add(AchievementEvent.ACTION_CHARGE);

        if (result != null) {
            boolean dealt = result.damageDealt() > 0;
            boolean failed = result.failed();
            boolean missed = result.missed() || (ownerAction == Action.ATTACK && !dealt && !result.selfHit());
            boolean successfulAttack = ownerAction == Action.ATTACK && dealt && !result.selfHit();
            boolean opponentDefeated = opponent != null && !opponent.isAlive();
            boolean killingBlow = successfulAttack && opponentDefeated;
            boolean chargedKillingBlow = killingBlow && result.chargedHit();
            boolean finalJudgementWin = killingBlow && "EXECUTIONERS_EDGE".equals(result.weaponId());
            boolean grimoireSolved = result.booleanMeta("grimoireCodeSolved") || result.booleanMeta("grimoireCorrect");
            int ballistaProjectiles = (int) Math.round(result.numericMeta("ballistaProjectiles"));
            boolean successfulDodge = ownerAction == Action.DODGE
                    && opponentAction == Action.ATTACK
                    && !dealt
                    && !result.selfHit();

            fields.putAll(result.meta());
            fields.put("damage", result.damageDealt());
            fields.put("damageDealt", result.damageDealt());
            fields.put("incomingDamage", result.damageToResolve());
            fields.put("damageToResolve", result.damageToResolve());
            fields.put("wasCritical", result.critical());
            fields.put("critical", result.critical());
            fields.put("missed", missed);
            fields.put("failed", failed);
            fields.put("failKind", result.failKind());
            fields.put("selfHit", result.selfHit());
            fields.put("chargedHit", result.chargedHit());
            fields.put("grimoireMultiplier", result.grimoireMultiplier());
            fields.put("lifeStolen", result.lifeStolen());
            fields.put("weaponId", value(result.weaponId(), stringField(fields, "weaponId")));
            fields.put("weaponName", value(result.weaponName(), stringField(fields, "weaponName")));
            fields.put("opponentDefeated", opponentDefeated);
            fields.put("enemyDefeated", opponentDefeated);
            fields.put("killingBlow", killingBlow);
            fields.put("chargedKillingBlow", chargedKillingBlow);
            fields.put("wonByCurrentAttack", killingBlow);
            fields.put("wonByChargedAttack", chargedKillingBlow);
            fields.put("matchWinningBlow", killingBlow);
            fields.put("chargedMatchWinningBlow", chargedKillingBlow);
            fields.put("finalJudgementWin", finalJudgementWin);
            fields.put("chargedFinalBlowWin", finalJudgementWin && result.chargedHit());
            fields.put("grimoireCodeSolved", grimoireSolved);
            fields.put("grimoireCorrect", grimoireSolved);
            fields.put("ballistaProjectiles", ballistaProjectiles);
            fields.put("projectiles", ballistaProjectiles);

            if (successfulAttack) events.add(AchievementEvent.ACTION_SUCCESSFUL_ATTACK);
            if (successfulDodge) events.add(AchievementEvent.ACTION_SUCCESSFUL_DODGE);
            if (dealt) {
                events.add(AchievementEvent.HIT);
                events.add(AchievementEvent.DAMAGE_DEALT);
            }
            if (missed) events.add(AchievementEvent.MISS);
            if (failed) {
                events.add(AchievementEvent.ATTACK_FAILED);
                if ("RESOURCE".equals(result.failKind())) events.add(AchievementEvent.ATTACK_FAILED_BY_RESOURCE);
                if ("SKILL".equals(result.failKind())) events.add(AchievementEvent.ATTACK_FAILED_BY_SKILL);
                if ("EFFECT".equals(result.failKind())) events.add(AchievementEvent.ATTACK_FAILED_BY_EFFECT);
            }
            if (result.critical()) events.add(AchievementEvent.CRIT);
            if (result.chargedHit()) events.add(AchievementEvent.CHARGED_HIT);
            if (opponentDefeated) events.add(AchievementEvent.ENEMY_DEFEATED);
            if (killingBlow) {
                events.add(AchievementEvent.KILLING_BLOW);
                events.add(AchievementEvent.MATCH_WON_BY_ATTACK);
                events.add(AchievementEvent.MATCH_WINNING_BLOW);
            }
            if (chargedKillingBlow) {
                events.add(AchievementEvent.CHARGED_KILLING_BLOW);
                events.add(AchievementEvent.CHARGED_MATCH_WINNING_BLOW);
            }
            if (finalJudgementWin) events.add(AchievementEvent.FINAL_JUDGEMENT_WIN);
            if (finalJudgementWin && result.chargedHit()) events.add(AchievementEvent.CHARGED_FINAL_BLOW_WIN);
            if (result.selfHit()) events.add(AchievementEvent.SELF_HIT);
            if (result.lifeStolen() > 0) events.add(AchievementEvent.LIFE_STEAL);
            if (result.grimoireMultiplier() > 0) {
                events.add(AchievementEvent.GRIMOIRE_USED);
                if (result.grimoireMultiplier() >= 1.0) events.add(AchievementEvent.GRIMOIRE_MULTIPLIER_AT_LEAST_ONE);
            }
            if (grimoireSolved) {
                events.add(AchievementEvent.GRIMOIRE_CODE_SOLVED);
                if (result.critical()) events.add(AchievementEvent.GRIMOIRE_CODE_SOLVED_CRIT);
            }
            if (ballistaProjectiles > 0) events.add(AchievementEvent.BALLISTA_PROJECTILES);
            if (result.weaponId() != null && !result.weaponId().isBlank()) events.add(AchievementEvent.WEAPON_USED);
        }

        if (owner != null) {
            if (owner.getStatistics().getHealth() <= 1.0 && owner.isAlive()) events.add(AchievementEvent.REACHED_ONE_HP);
            if (owner.getStatistics().getStamina() <= 0.0) events.add(AchievementEvent.ZERO_STAMINA);
            if (owner.healthRatio() <= 0.35) events.add(AchievementEvent.LOW_HEALTH);
            if (owner.getMomentumStacks() >= 2) events.add(AchievementEvent.HIGH_MOMENTUM);
            if (owner.getStatistics().getStamina() / Math.max(1.0, owner.getStatistics().getMaxStamina()) <= 0.40) {
                events.add(AchievementEvent.LOW_STAMINA);
            }
            if (owner.isAlive()) events.add(AchievementEvent.SURVIVE_TURN);
        }

        return new AchievementUpdate(owner, opponent, ownerAction, opponentAction, result, Winner.NONE, roundNumber,
                Set.copyOf(events), Map.copyOf(fields));
    }

    /** Crea una actualització global de final de combat. */
    public static AchievementUpdate fromMatchFinished(Character player1, Character player2, Winner winner,
            int roundNumber) {
        EnumSet<AchievementEvent> events = EnumSet.noneOf(AchievementEvent.class);
        Character winnerCharacter = winnerCharacter(player1, player2, winner);
        Character loserCharacter = loserCharacter(player1, player2, winner);
        Character owner = winnerCharacter == null ? player1 : winnerCharacter;
        Character opponent = loserCharacter == null ? player2 : loserCharacter;

        Map<String, Object> fields = baseFields(owner, opponent, null, null, roundNumber);
        fields.put("winner", winner == null ? Winner.NONE.name() : winner.name());
        fields.put("hasWinner", winner == Winner.PLAYER1 || winner == Winner.PLAYER2);
        fields.put("tie", winner == Winner.TIE);
        addWinnerFields(fields, winnerCharacter);
        addLoserFields(fields, loserCharacter);

        events.add(AchievementEvent.MATCH_FINISHED);
        if (winner == Winner.TIE) {
            events.add(AchievementEvent.MATCH_TIED);
        } else if (winner == Winner.PLAYER1 || winner == Winner.PLAYER2) {
            events.add(AchievementEvent.MATCH_WON);
            events.add(AchievementEvent.MATCH_LOST);
        }
        return new AchievementUpdate(owner, opponent, null, null, null, winner, roundNumber, Set.copyOf(events), Map.copyOf(fields));
    }

    /** Crea una actualització simple d'un sol esdeveniment. */
    public static AchievementUpdate simple(Character owner, AchievementEvent event) {
        Map<String, Object> fields = baseFields(owner, null, null, null, 0);
        return new AchievementUpdate(owner, null, null, null, null, Winner.NONE, 0, Set.of(event), Map.copyOf(fields));
    }

    private static Character winnerCharacter(Character player1, Character player2, Winner winner) {
        if (winner == Winner.PLAYER1) return player1;
        if (winner == Winner.PLAYER2) return player2;
        return null;
    }

    private static Character loserCharacter(Character player1, Character player2, Winner winner) {
        if (winner == Winner.PLAYER1) return player2;
        if (winner == Winner.PLAYER2) return player1;
        return null;
    }

    private static void addWinnerFields(Map<String, Object> fields, Character winner) {
        fields.put("winnerAlive", winner != null && winner.isAlive());
        if (winner == null) return;
        addCharacterFields(fields, "winner", winner);
        Weapon weapon = winner.getWeapon();
        if (weapon != null) {
            fields.put("winnerWeaponId", weapon.getId());
            fields.put("winnerWeaponName", weapon.getName());
            fields.put("winnerWeaponType", weapon.getType() == null ? null : weapon.getType().name());
        }
    }

    private static void addLoserFields(Map<String, Object> fields, Character loser) {
        fields.put("loserAlive", loser != null && loser.isAlive());
        if (loser == null) return;
        addCharacterFields(fields, "loser", loser);
        Weapon weapon = loser.getWeapon();
        if (weapon != null) {
            fields.put("loserWeaponId", weapon.getId());
            fields.put("loserWeaponName", weapon.getName());
            fields.put("loserWeaponType", weapon.getType() == null ? null : weapon.getType().name());
        }
    }

    /** Indica si s'ha produït un esdeveniment. */
    public boolean has(AchievementEvent event) {
        return event != null && events.contains(event);
    }

    /** Retorna el valor associat a un objectiu. */
    public double amountFor(AchievementObjective objective) {
        if (objective == null) return 0.0;
        if (objective.valueField() != null && !objective.valueField().isBlank()) {
            return numericField(objective.valueField());
        }
        return amountFor(objective.event());
    }

    /** Retorna el valor associat a un esdeveniment. */
    public double amountFor(AchievementEvent event) {
        if (event == AchievementEvent.DAMAGE_DEALT && result != null) return Math.max(0.0, result.damageDealt());
        if (event == AchievementEvent.DAMAGE_RECEIVED && result != null) return Math.max(0.0, result.damageToResolve());
        if (event == AchievementEvent.LIFE_STEAL && result != null) return Math.max(0.0, result.lifeStolen());
        if (event == AchievementEvent.BALLISTA_PROJECTILES && result != null) return Math.max(0.0, result.numericMeta("ballistaProjectiles"));
        return has(event) ? 1.0 : 0.0;
    }

    /** Retorna un camp flexible pel nom indicat. */
    public Object field(String name) {
        return name == null ? null : fields.get(name);
    }

    /** Retorna un camp numèric. */
    public double numericField(String name) {
        Object value = field(name);
        if (value instanceof Number n) return n.doubleValue();
        if (value instanceof Boolean b) return Boolean.TRUE.equals(b) ? 1.0 : 0.0;
        if (value != null) {
            try { return Double.parseDouble(String.valueOf(value)); } catch (NumberFormatException ignored) {}
        }
        return 0.0;
    }

    /** Retorna un camp booleà. */
    public boolean booleanField(String name) {
        Object value = field(name);
        if (value instanceof Boolean b) return b;
        if (value instanceof Number n) return n.doubleValue() != 0.0;
        return value != null && Boolean.parseBoolean(String.valueOf(value));
    }

    /** Retorna un camp textual. */
    public String stringField(String name) {
        Object value = field(name);
        return value == null ? null : String.valueOf(value);
    }

    private static Map<String, Object> baseFields(Character owner, Character opponent, Action ownerAction,
            Action opponentAction, int roundNumber) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("round", roundNumber);
        fields.put("roundNumber", roundNumber);
        fields.put("usedAction", ownerAction == null ? null : ownerAction.name());
        fields.put("ownerAction", ownerAction == null ? null : ownerAction.name());
        fields.put("enemyAction", opponentAction == null ? null : opponentAction.name());
        fields.put("opponentAction", opponentAction == null ? null : opponentAction.name());
        if (owner != null) addCharacterFields(fields, "", owner);
        if (opponent != null) addCharacterFields(fields, "opponent", opponent);
        return fields;
    }

    private static void addCharacterFields(Map<String, Object> fields, String prefix, Character character) {
        Statistics stats = character.getStatistics();
        String p = prefix == null || prefix.isBlank() ? "" : prefix;
        String sep = p.isEmpty() ? "" : ".";
        fields.put(p + sep + "hp", stats.getHealth());
        fields.put(p + sep + "maxHp", stats.getMaxHealth());
        fields.put(p + sep + "hpPercent", character.healthRatio() * 100.0);
        fields.put(p + sep + "stamina", stats.getStamina());
        fields.put(p + sep + "maxStamina", stats.getMaxStamina());
        fields.put(p + sep + "staminaPercent", stats.getStamina() / Math.max(1.0, stats.getMaxStamina()) * 100.0);
        fields.put(p + sep + "mana", stats.getMana());
        fields.put(p + sep + "maxMana", stats.getMaxMana());
        fields.put(p + sep + "momentum", character.getMomentumStacks());
        fields.put(p + sep + "alive", character.isAlive());
        Weapon weapon = character.getWeapon();
        if (weapon != null) {
            fields.put(p + sep + "weaponId", weapon.getId());
            fields.put(p + sep + "weaponName", weapon.getName());
            fields.put(p + sep + "weaponType", weapon.getType() == null ? null : weapon.getType().name());
            if (p.isEmpty()) {
                fields.put("equippedWeapon", weapon.getId());
            }
        }
    }

    private static String stringField(Map<String, Object> fields, String key) {
        Object value = fields.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private static String value(String value, String def) {
        return value == null || value.isBlank() ? def : value;
    }
}
