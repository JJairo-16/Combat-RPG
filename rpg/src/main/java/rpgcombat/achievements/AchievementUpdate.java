package rpgcombat.achievements;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.LinkedHashSet;

import rpgcombat.combat.models.Action;
import rpgcombat.combat.models.Winner;
import rpgcombat.combat.turnservice.TurnResult;
import rpgcombat.models.characters.Character;
import rpgcombat.models.characters.Statistics;
import rpgcombat.weapons.Weapon;
import rpgcombat.models.effects.triggers.Chaos;
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
            boolean grimoireMinMultiplier = result.booleanMeta("grimoireMinMultiplier") || result.booleanMeta("grimoireMultiplierMin");
            boolean grimoireMaxMultiplier = result.booleanMeta("grimoireMaxMultiplier") || result.booleanMeta("grimoireMultiplierMax");
            boolean arcaneDisruptionMiss = missed && "ARCANE_DISRUPTION_STAFF".equals(result.weaponId());
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
            fields.put("grimoireMinMultiplier", grimoireMinMultiplier);
            fields.put("grimoireMaxMultiplier", grimoireMaxMultiplier);
            fields.put("arcaneDisruptionMiss", arcaneDisruptionMiss);
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
            addPerkActivationFields(fields);

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
            if (arcaneDisruptionMiss) events.add(AchievementEvent.ARCANE_DISRUPTION_MISS);
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
                events.add(AchievementEvent.GRIMOIRE_MULTIPLIER_ROLLED);
                if (result.grimoireMultiplier() >= 1.0) events.add(AchievementEvent.GRIMOIRE_MULTIPLIER_AT_LEAST_ONE);
                if (grimoireMinMultiplier) events.add(AchievementEvent.GRIMOIRE_MIN_MULTIPLIER);
                if (grimoireMaxMultiplier) events.add(AchievementEvent.GRIMOIRE_MAX_MULTIPLIER);
            }
            if (grimoireSolved) {
                events.add(AchievementEvent.GRIMOIRE_CODE_SOLVED);
                if (result.critical()) events.add(AchievementEvent.GRIMOIRE_CODE_SOLVED_CRIT);
            }
            if (ballistaProjectiles > 0) events.add(AchievementEvent.BALLISTA_PROJECTILES);
            if (hasTextField(fields, "activatedPerkIds")) events.add(AchievementEvent.PERK_ACTIVATED);
            if (hasTextField(fields, "divineAwakeningPerkIds")) events.add(AchievementEvent.DIVINE_PERK_AWAKENING_PROGRESS);
            if (booleanValue(fields.get("divinePerkAwakened"))) {
                events.add(AchievementEvent.DIVINE_PERK_AWAKENED);
                events.add(AchievementEvent.DIVINE_PERK_MISSION_COMPLETED);
            }
            if (booleanValue(fields.get("divinePerkFullPower"))) events.add(AchievementEvent.DIVINE_PERK_FULL_POWER);
            if (hasTextField(fields, "triggeredSynergyIds")) events.add(AchievementEvent.SYNERGY_TRIGGERED);
            addChaosEvents(events, fields);
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
            if (owner.hasEffect(Chaos.INTERNAL_EFFECT_KEY)) events.add(AchievementEvent.CHAOS_ACTIVE_TURN);
            if (owner.isAlive()) events.add(AchievementEvent.SURVIVE_TURN);
        }

        return new AchievementUpdate(owner, opponent, ownerAction, opponentAction, result, Winner.NONE, roundNumber,
                Set.copyOf(events), safeFields(fields));
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
        boolean chaosMatch = booleanValue(fields.get("chaosMode"));
        if (chaosMatch) events.add(AchievementEvent.CHAOS_MATCH_STARTED);
        if (winner == Winner.TIE) {
            events.add(AchievementEvent.MATCH_TIED);
        } else if (winner == Winner.PLAYER1 || winner == Winner.PLAYER2) {
            events.add(AchievementEvent.MATCH_WON);
            events.add(AchievementEvent.MATCH_LOST);
            if (chaosMatch) events.add(AchievementEvent.CHAOS_MATCH_WON);
        }
        return new AchievementUpdate(owner, opponent, null, null, null, winner, roundNumber, Set.copyOf(events), safeFields(fields));
    }

    /** Crea una còpia de l'actualització afegint esdeveniments i camps derivats. */
    public AchievementUpdate withAdditional(Set<AchievementEvent> extraEvents, Map<String, Object> extraFields) {
        EnumSet<AchievementEvent> mergedEvents = EnumSet.noneOf(AchievementEvent.class);
        if (events != null) mergedEvents.addAll(events);
        if (extraEvents != null) {
            for (AchievementEvent event : extraEvents) {
                if (event != null) mergedEvents.add(event);
            }
        }

        Map<String, Object> mergedFields = new HashMap<>();
        if (fields != null) mergedFields.putAll(fields);
        if (extraFields != null) {
            extraFields.forEach((key, value) -> {
                if (key != null && value != null) mergedFields.put(key, value);
            });
        }

        return new AchievementUpdate(owner, opponent, ownerAction, opponentAction, result, winner, roundNumber,
                Set.copyOf(mergedEvents), safeFields(mergedFields));
    }

    /** Crea una actualització simple d'un sol esdeveniment. */
    public static AchievementUpdate simple(Character owner, AchievementEvent event) {
        Map<String, Object> fields = baseFields(owner, null, null, null, 0);
        return new AchievementUpdate(owner, null, null, null, null, Winner.NONE, 0, Set.of(event), safeFields(fields));
    }

    /** Crea una actualització quan s'ha triat un mode de joc per començar partida. */
    public static AchievementUpdate gameModeSelected(String modeId) {
        Map<String, Object> fields = baseFields(null, null, null, null, 0);
        fields.put("modeId", modeId == null ? "" : modeId);
        return new AchievementUpdate(null, null, null, null, null, Winner.NONE, 0,
                Set.of(AchievementEvent.GAME_MODE_SELECTED), safeFields(fields));
    }

    /** Crea una actualització de progrés d'una missió de perk encara no completada. */
    public static AchievementUpdate perkMissionProgress(Character owner, String missionId, String perkId,
            double progressBefore, double progressAfter, double target, int activeMissionCount, int roundNumber) {
        Map<String, Object> fields = baseFields(owner, null, null, null, roundNumber);
        fields.put("missionId", missionId);
        fields.put("perkMissionId", missionId);
        fields.put("perkId", perkId);
        fields.put("perkMissionProgressBefore", progressBefore);
        fields.put("perkMissionProgressAfter", progressAfter);
        fields.put("perkMissionTarget", target);
        fields.put("activePerkMissionCount", activeMissionCount);
        fields.put("perkMissionCompleted", progressAfter >= target);
        return new AchievementUpdate(owner, null, null, null, null, Winner.NONE, roundNumber,
                Set.of(AchievementEvent.PERK_MISSION_PROGRESS), safeFields(fields));
    }

    /** Crea una actualització de missió de perk completada. */
    public static AchievementUpdate perkMissionCompleted(Character owner, String missionId, String perkId,
            int completedMissionCount, int activeMissionCount, int roundNumber) {
        Map<String, Object> fields = baseFields(owner, null, null, null, roundNumber);
        fields.put("missionId", missionId);
        fields.put("perkMissionId", missionId);
        fields.put("perkId", perkId);
        fields.put("completedPerkMissionCount", completedMissionCount);
        fields.put("activePerkMissionCount", activeMissionCount);
        return new AchievementUpdate(owner, null, null, null, null, Winner.NONE, roundNumber,
                Set.of(AchievementEvent.PERK_MISSION_COMPLETED), safeFields(fields));
    }

    /** Crea una actualització de perk guanyada. */
    public static AchievementUpdate perkGained(Character owner, String perkId, String perkName, String family,
            List<String> tags, int perkCount, int maxPerks, int roundNumber) {
        Map<String, Object> fields = baseFields(owner, null, null, null, roundNumber);
        fields.put("perkId", perkId);
        fields.put("perkName", perkName);
        fields.put("perkFamily", family);
        fields.put("perkTags", joinTokens(tags));
        fields.put("perkCount", perkCount);
        fields.put("maxPerks", maxPerks);
        fields.put("perkLimitReached", perkCount >= maxPerks);
        EnumSet<AchievementEvent> events = EnumSet.of(AchievementEvent.PERK_GAINED);
        if (perkCount >= maxPerks) events.add(AchievementEvent.PERK_LIMIT_REACHED);
        return new AchievementUpdate(owner, null, null, null, null, Winner.NONE, roundNumber,
                Set.copyOf(events), safeFields(fields));
    }

    /** Crea una actualització de perk divina assignada al combat. */
    public static AchievementUpdate divinePerkAssigned(Character owner, String divinePerkId, String divinePerkName,
            String god, int roundNumber) {
        Map<String, Object> fields = baseFields(owner, null, null, null, roundNumber);
        fields.put("divinePerkId", divinePerkId);
        fields.put("divinePerkName", divinePerkName);
        fields.put("god", god);
        fields.put("divinePerkDormant", true);
        fields.put("divinePerkPartialPower", true);
        fields.put("divinePowerRatio", 0.40);
        return new AchievementUpdate(owner, null, null, null, null, Winner.NONE, roundNumber,
                Set.of(AchievementEvent.DIVINE_PERK_ASSIGNED, AchievementEvent.DIVINE_PERK_DORMANT_ASSIGNED), safeFields(fields));
    }

    /** Crea una actualització de sinergia activada. */
    public static AchievementUpdate synergyActivated(Character owner, String synergyId, String synergyName,
            int synergyCount, int roundNumber) {
        Map<String, Object> fields = baseFields(owner, null, null, null, roundNumber);
        fields.put("synergyId", synergyId);
        fields.put("synergyName", synergyName);
        fields.put("synergyCount", synergyCount);
        EnumSet<AchievementEvent> events = EnumSet.of(AchievementEvent.SYNERGY_ACTIVATED);
        if (synergyCount > 0) events.add(AchievementEvent.SYNERGY_COUNT_REACHED);
        return new AchievementUpdate(owner, null, null, null, null, Winner.NONE, roundNumber,
                Set.copyOf(events), safeFields(fields));
    }

    /** Crea una actualització quan s'afegeix el trigger de Caos. */
    public static AchievementUpdate chaosTriggerAdded(Character owner, int roundNumber) {
        Map<String, Object> fields = baseFields(owner, null, null, null, roundNumber);
        fields.put("chaosTriggerAdded", true);
        fields.put("triggerId", Chaos.INTERNAL_EFFECT_KEY);
        return new AchievementUpdate(owner, null, null, null, null, Winner.NONE, roundNumber,
                Set.of(AchievementEvent.CHAOS_TRIGGER_ADDED), safeFields(fields));
    }

    /** Crea una actualització quan el combat comença amb Caos actiu. */
    public static AchievementUpdate chaosMatchStarted(Character player1, Character player2, int roundNumber) {
        Map<String, Object> fields = baseFields(player1, player2, null, null, roundNumber);
        boolean player1Chaos = player1 != null && player1.hasEffect(Chaos.INTERNAL_EFFECT_KEY);
        boolean player2Chaos = player2 != null && player2.hasEffect(Chaos.INTERNAL_EFFECT_KEY);
        fields.put("player1HasChaos", player1Chaos);
        fields.put("player2HasChaos", player2Chaos);
        fields.put("bothPlayersHaveChaos", player1Chaos && player2Chaos);
        fields.put("chaosMode", player1Chaos && player2Chaos);
        return new AchievementUpdate(player1, player2, null, null, null, Winner.NONE, roundNumber,
                Set.of(AchievementEvent.CHAOS_MATCH_STARTED), safeFields(fields));
    }

    /** Crea una actualització del Pacte de Sang. */
    public static AchievementUpdate bloodPactUsed(Character owner, double manaRestored, double hpCost,
            double hpCostPercent, double hpBeforePercent, double hpAfterPercent,
            double manaBeforePercent, double manaAfterPercent, int roundNumber) {
        Map<String, Object> fields = baseFields(owner, null, null, null, roundNumber);
        boolean effective = manaRestored > 0.0 && hpCost > 0.0;
        fields.put("specialAction", "bloodPact");
        fields.put("bloodPactAttempted", true);
        fields.put("bloodPactUsed", effective);
        fields.put("manaRestored", manaRestored);
        fields.put("bloodPactManaRestored", manaRestored);
        fields.put("hpCost", hpCost);
        fields.put("bloodPactHpCost", hpCost);
        fields.put("bloodPactHpCostPercent", hpCostPercent * 100.0);
        fields.put("bloodPactHpCostRatio", hpCostPercent);
        fields.put("bloodPactHpBeforePercent", hpBeforePercent);
        fields.put("bloodPactHpAfterPercent", hpAfterPercent);
        fields.put("bloodPactManaBeforePercent", manaBeforePercent);
        fields.put("bloodPactManaAfterPercent", manaAfterPercent);
        fields.put("bloodPactLowHealth", hpBeforePercent <= 25.0);
        fields.put("bloodPactCriticalHealth", hpBeforePercent <= 10.0);
        fields.put("bloodPactFullRestore", manaAfterPercent >= 100.0 && manaBeforePercent < 100.0);
        EnumSet<AchievementEvent> events = EnumSet.of(AchievementEvent.SPECIAL_ACTION_USED,
                AchievementEvent.BLOOD_PACT_ATTEMPTED);
        if (effective) {
            events.add(AchievementEvent.BLOOD_PACT_USED);
            events.add(AchievementEvent.BLOOD_PACT_LIFE_PAID);
            events.add(AchievementEvent.BLOOD_PACT_MANA_RESTORED);
            if (hpBeforePercent <= 25.0) events.add(AchievementEvent.BLOOD_PACT_LOW_HEALTH_USED);
            if (hpBeforePercent <= 10.0) events.add(AchievementEvent.BLOOD_PACT_CRITICAL_HEALTH_USED);
            if (manaAfterPercent >= 100.0 && manaBeforePercent < 100.0) {
                events.add(AchievementEvent.BLOOD_PACT_FULL_RESTORE);
            }
        }
        return new AchievementUpdate(owner, null, null, null, null, Winner.NONE, roundNumber,
                Set.copyOf(events), safeFields(fields));
    }

    /** Crea una actualització de la Crida Espiritual. */
    public static AchievementUpdate spiritualCallingUsed(Character owner, int face, double healPercent,
            double healAmount, int roundNumber) {
        Map<String, Object> fields = baseFields(owner, null, null, null, roundNumber);
        fields.put("specialAction", "spiritualCalling");
        fields.put("spiritualCallingUsed", true);
        fields.put("spiritualCallingFace", face);
        fields.put("spiritualRoll", face);
        fields.put("spiritualCallingRoll", face);
        fields.put("spiritualCallingNatural1", face == 1);
        fields.put("spiritualCallingNatural20", face == 20);
        fields.put("spiritualCallingHealPercent", healPercent * 100.0);
        fields.put("spiritualCallingHealRatio", healPercent);
        fields.put("healAmount", healAmount);
        fields.put("spiritualCallingHealAmount", healAmount);
        EnumSet<AchievementEvent> events = EnumSet.of(AchievementEvent.SPECIAL_ACTION_USED,
                AchievementEvent.SPIRITUAL_CALLING_USED, AchievementEvent.SPIRITUAL_ROLL);
        if (face == 1) events.add(AchievementEvent.SPIRITUAL_ROLL_NATURAL_1);
        if (face == 20) events.add(AchievementEvent.SPIRITUAL_ROLL_NATURAL_20);
        if (healAmount > 0) events.add(AchievementEvent.SPIRITUAL_CALLING_HEALED);
        return new AchievementUpdate(owner, null, null, null, null, Winner.NONE, roundNumber,
                Set.copyOf(events), safeFields(fields));
    }

    /** Crea una actualització d'ús d'una ulti de segona etapa. */
    public static AchievementUpdate ultimateUsed(Character owner, String ultimateId, String ultimateName,
            String ultimateWeaponType, int roundNumber) {
        Map<String, Object> fields = baseFields(owner, null, null, null, roundNumber);
        fields.put("specialAction", "ultimate");
        fields.put("ultimateUsed", true);
        fields.put("ultimateId", ultimateId);
        fields.put("ultimateName", ultimateName);
        fields.put("ultimateType", ultimateWeaponType);
        fields.put("ultimateWeaponType", ultimateWeaponType);

        EnumSet<AchievementEvent> events = EnumSet.of(AchievementEvent.SPECIAL_ACTION_USED,
                AchievementEvent.ULTIMATE_USED);
        if ("ARCANE_OVERLOAD".equals(ultimateId)) events.add(AchievementEvent.ARCANE_OVERLOAD_USED);
        if ("COLOSSAL_BREAK".equals(ultimateId)) events.add(AchievementEvent.COLOSSAL_BREAK_USED);
        if ("ELVEN_OPENING_SHOT".equals(ultimateId)) events.add(AchievementEvent.ELVEN_OPENING_SHOT_USED);

        return new AchievementUpdate(owner, null, null, null, null, Winner.NONE, roundNumber,
                Set.copyOf(events), safeFields(fields));
    }

    private static Map<String, Object> safeFields(Map<String, Object> fields) {
        if (fields == null || fields.isEmpty()) return Map.of();
        Map<String, Object> clean = new HashMap<>();
        fields.forEach((key, value) -> {
            if (key != null && value != null) clean.put(key, value);
        });
        return Map.copyOf(clean);
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
        if (event == AchievementEvent.BLOOD_PACT_LIFE_PAID) return Math.max(0.0, numericField("bloodPactHpCost"));
        if (event == AchievementEvent.BLOOD_PACT_MANA_RESTORED) return Math.max(0.0, numericField("bloodPactManaRestored"));
        if (event == AchievementEvent.SPIRITUAL_CALLING_HEALED) return Math.max(0.0, numericField("spiritualCallingHealAmount"));
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

    /** Identificador estable de l'actor que ha generat aquesta actualització. */
    public String actorKey() {
        String explicit = stringField("actorKey");
        if (explicit != null && !explicit.isBlank()) return explicit;
        if (owner == null) return null;
        return owner.getName();
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
        if (owner != null) {
            fields.put("actorKey", owner.getName());
            fields.put("ownerName", owner.getName());
        }
        if (opponent != null) fields.put("opponentName", opponent.getName());
        if (owner != null) addCharacterFields(fields, "", owner);
        if (opponent != null) addCharacterFields(fields, "opponent", opponent);
        boolean ownerChaos = owner != null && owner.hasEffect(Chaos.INTERNAL_EFFECT_KEY);
        boolean opponentChaos = opponent != null && opponent.hasEffect(Chaos.INTERNAL_EFFECT_KEY);
        fields.put("actorHasChaos", ownerChaos);
        fields.put("opponentHasChaos", opponentChaos);
        fields.put("bothPlayersHaveChaos", ownerChaos && opponentChaos);
        fields.put("chaosMode", ownerChaos && opponentChaos);
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
        fields.put(p + sep + "hasChaos", character.hasEffect(Chaos.INTERNAL_EFFECT_KEY));
        if (p.isEmpty()) {
            fields.put("chaosActive", character.hasEffect(Chaos.INTERNAL_EFFECT_KEY));
        }
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

    private static void addPerkActivationFields(Map<String, Object> fields) {
        String perkIds = stringField(fields, "activatedPerkIds");
        if (perkIds != null && !perkIds.isBlank()) {
            fields.put("perkActivated", true);
            fields.put("activatedPerkId", firstToken(perkIds));
        }
        String synergyIds = stringField(fields, "triggeredSynergyIds");
        if (synergyIds != null && !synergyIds.isBlank()) {
            fields.put("synergyTriggered", true);
            fields.put("triggeredSynergyId", firstToken(synergyIds));
        }
        String divineAwakeningIds = stringField(fields, "divineAwakeningPerkIds");
        if (divineAwakeningIds != null && !divineAwakeningIds.isBlank()) {
            fields.put("divinePerkAwakeningProgress", true);
            fields.put("divinePerkId", firstToken(divineAwakeningIds));
        }
    }

    private static void addChaosEvents(EnumSet<AchievementEvent> events, Map<String, Object> fields) {
        if (booleanValue(fields.get("chaosTriggered"))) events.add(AchievementEvent.CHAOS_OUTCOME_ROLLED);
        if (booleanValue(fields.get("chaosActionChanged"))) events.add(AchievementEvent.CHAOS_ACTION_CHANGED);
        if (booleanValue(fields.get(Chaos.META_SELF_HIT))) events.add(AchievementEvent.CHAOS_SELF_HIT);
        if (booleanValue(fields.get("chaosForceCrit"))) events.add(AchievementEvent.CHAOS_FORCE_CRIT);
        if (booleanValue(fields.get("chaosForbidCrit"))) events.add(AchievementEvent.CHAOS_FORBID_CRIT);
        if ("DAMAGE_UP".equals(stringField(fields, "chaosOutcome"))) events.add(AchievementEvent.CHAOS_DAMAGE_UP);
        if ("DAMAGE_DOWN".equals(stringField(fields, "chaosOutcome"))) events.add(AchievementEvent.CHAOS_DAMAGE_DOWN);
        if ("FAIL_ACTION".equals(stringField(fields, "chaosOutcome"))) events.add(AchievementEvent.CHAOS_FAIL_ACTION);
        if ("MANA_SPIKE".equals(stringField(fields, "chaosOutcome"))) events.add(AchievementEvent.CHAOS_MANA_SPIKE);
        if ("BLOOD_RUSH".equals(stringField(fields, "chaosOutcome"))) events.add(AchievementEvent.CHAOS_BLOOD_RUSH);
        if ("PERFECT_CHAOS".equals(stringField(fields, "chaosOutcome"))
                || booleanValue(fields.get("chaosPerfect"))) events.add(AchievementEvent.CHAOS_PERFECT);
    }

    private static boolean hasTextField(Map<String, Object> fields, String key) {
        String value = stringField(fields, key);
        return value != null && !value.isBlank();
    }

    private static boolean booleanValue(Object value) {
        if (value instanceof Boolean b) return b;
        if (value instanceof Number n) return n.doubleValue() != 0.0;
        return value != null && Boolean.parseBoolean(String.valueOf(value));
    }

    private static String joinTokens(List<String> values) {
        if (values == null || values.isEmpty()) return "";
        LinkedHashSet<String> tokens = new LinkedHashSet<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) tokens.add(value.trim());
        }
        return String.join("|", tokens);
    }

    private static String firstToken(String text) {
        if (text == null || text.isBlank()) return null;
        String[] split = text.split("[|,]");
        return split.length == 0 ? text.trim() : split[0].trim();
    }

    private static String stringField(Map<String, Object> fields, String key) {
        Object value = fields.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private static String value(String value, String def) {
        return value == null || value.isBlank() ? def : value;
    }
}
