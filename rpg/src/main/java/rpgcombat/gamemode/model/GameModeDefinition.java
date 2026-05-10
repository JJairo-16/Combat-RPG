package rpgcombat.gamemode.model;

import java.util.List;

import rpgcombat.achievements.AchievementSystem;
import rpgcombat.discovery.DiscoverySystem;
import rpgcombat.gamemode.cinematics.ModeCinematics;
import rpgcombat.unlocks.UnlockEvaluator;
import rpgcombat.unlocks.UnlockMode;
import rpgcombat.unlocks.UnlockRule;

/** Definició immutable d'un mode de joc seleccionable. */
public record GameModeDefinition(
        String id,
        String name,
        String description,
        UnlockRule unlockRule,
        GameModeRules rules,
        ModeCinematics cinematics) {

    public GameModeDefinition {
        id = requireText(id, "id").toUpperCase();
        name = fallback(name, id);
        description = description == null ? "" : description;
        unlockRule = unlockRule == null ? new UnlockRule(UnlockMode.ALL, List.of()) : unlockRule;
        rules = rules == null ? GameModeRules.unrestricted() : rules;
        cinematics = cinematics == null ? ModeCinematics.normal() : cinematics;
    }

    public static GameModeDefinition normal() {
        return new GameModeDefinition(
                "NORMAL",
                "Normal",
                "Experiència completa amb totes les mecàniques activades.",
                new UnlockRule(UnlockMode.ALL, List.of()),
                GameModeRules.unrestricted(),
                ModeCinematics.normal());
    }

    public static GameModeDefinition beginner() {
        return new GameModeDefinition(
                "BEGINNER",
                "Principiant",
                "Combat reduït sense càrrega, accions especials, armes desbloquejables ni Caos.",
                new UnlockRule(UnlockMode.ALL, List.of()),
                GameModeRules.beginner(),
                ModeCinematics.beginner());
    }

    public boolean isUnlocked(AchievementSystem achievements, DiscoverySystem discoveries) {
        return UnlockEvaluator.isUnlocked(unlockRule, achievements, discoveries);
    }

    private static String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("El mode de joc necessita " + field + ".");
        }
        return value.trim();
    }
}
