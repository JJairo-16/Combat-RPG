package rpgcombat.gamemode.model;

import java.util.List;
import java.util.Map;
import java.util.Set;

import rpgcombat.achievements.AchievementSystem;
import rpgcombat.discovery.DiscoverySystem;
import rpgcombat.gamemode.cinematics.ModeCinematics;
import rpgcombat.gamemode.effects.ModeEffectDefinition;
import rpgcombat.gamemode.effects.ModeEffectTarget;
import rpgcombat.models.effects.triggers.UniversalLifeStealTrigger;
import rpgcombat.unlocks.UnlockEvaluator;
import rpgcombat.unlocks.UnlockMode;
import rpgcombat.unlocks.UnlockRequirement;
import rpgcombat.unlocks.UnlockRequirementType;
import rpgcombat.unlocks.UnlockRule;

/** Definició immutable d'un mode de joc seleccionable. */
public record GameModeDefinition(
        String id,
        String name,
        String description,
        UnlockRule unlockRule,
        GameModeRules rules,
        GameModePresentation presentation,
        ModeCinematics cinematics) {

    public GameModeDefinition {
        id = requireText(id, "id").toUpperCase();
        name = fallback(name, id);
        description = description == null ? "" : description;
        unlockRule = unlockRule == null ? new UnlockRule(UnlockMode.ALL, List.of()) : unlockRule;
        rules = rules == null ? GameModeRules.unrestricted() : rules;
        presentation = presentation == null ? GameModePresentation.fallback(description) : presentation;
        cinematics = cinematics == null ? ModeCinematics.normal() : cinematics;
    }

    public static GameModeDefinition normal() {
        return new GameModeDefinition(
                "NORMAL",
                "Normal",
                "Experiència completa amb totes les mecàniques activades.",
                new UnlockRule(UnlockMode.ALL, List.of()),
                GameModeRules.unrestricted(),
                new GameModePresentation(
                        "El duel sense concessions.",
                        List.of(
                                "El combat conserva totes les formes conegudes",
                                "Les armes trobades poden tornar a aparèixer",
                                "Les benediccions encara poden formar una corona ampla",
                                "Els pactes divins romanen desperts",
                                "El Caos pot escoltar"),
                        "???",
                        "Pacte encara sense nom",
                        List.of("El primer camí sempre roman obert.")),
                ModeCinematics.normal());
    }

    public static GameModeDefinition beginner() {
        return new GameModeDefinition(
                "BEGINNER",
                "Principiant",
                "Combat reduït sense càrrega, accions especials, armes desbloquejables ni Caos.",
                new UnlockRule(UnlockMode.ALL, List.of()),
                GameModeRules.beginner(),
                new GameModePresentation(
                        "El primer llindar.",
                        List.of(
                                "La càrrega roman segellada",
                                "No acudeixen veus externes",
                                "Només una benedicció menor pot arrelar",
                                "Cap pacte diví desperta en aquest llindar",
                                "El Caos no travessa la porta"),
                        "???",
                        "Llindar ocult",
                        List.of("La primera lliçó no demana cap tribut.")),
                ModeCinematics.beginner());
    }

    public static GameModeDefinition bloodHunger() {
        return new GameModeDefinition(
                "BLOOD_HUNGER",
                "Fam eterna",
                "Com el mode normal, però la vida ja no torna sola i cada atac pot reclamar sang.",
                new UnlockRule(UnlockMode.ALL, List.of(
                        new UnlockRequirement(UnlockRequirementType.ACHIEVEMENT, "ETERNAL_HUNGER", null, 0),
                        new UnlockRequirement(UnlockRequirementType.ACHIEVEMENT, "FLESH_BENDS", null, 0))),
                new GameModeRules(
                        Set.of(),
                        true,
                        GameModeRules.DEFAULT_MAX_PERKS,
                        true,
                        true,
                        null,
                        List.of(new ModeEffectDefinition(
                                UniversalLifeStealTrigger.INTERNAL_EFFECT_KEY,
                                ModeEffectTarget.BOTH,
                                Map.of(
                                        "lifeStealPct", UniversalLifeStealTrigger.DEFAULT_LIFE_STEAL_PCT,
                                        "maxHealPct", UniversalLifeStealTrigger.DEFAULT_MAX_HEAL_PCT,
                                        "suppressPassiveHealthRegen", 1.0)))),
                new GameModePresentation(
                        "La sang no espera el descans.",
                        List.of(
                                "El combat conserva totes les formes conegudes",
                                "La vida no torna sola al final de la ronda",
                                "Cada ferida retorna un glop mesurat",
                                "Les armes trobades poden tornar a aparèixer",
                                "El Caos pot escoltar"),
                        "???",
                        "Fam sense nom",
                        List.of("Quan la gana no s'atura i la carn ja sap doblegar-se, una porta s'obrirà.")),
                ModeCinematics.normal());
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
