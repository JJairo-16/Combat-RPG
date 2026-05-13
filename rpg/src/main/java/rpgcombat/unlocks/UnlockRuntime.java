package rpgcombat.unlocks;

import rpgcombat.achievements.AchievementSystem;
import rpgcombat.discovery.DiscoverySystem;
import rpgcombat.weapons.Arsenal;
import rpgcombat.weapons.config.WeaponDefinition;

/** Punt global lleuger perquè UI i catàlegs consultin la disponibilitat actual. */
public final class UnlockRuntime {
    private static AchievementSystem achievements;
    private static DiscoverySystem discoveries;

    private UnlockRuntime() {}

    public static void configure(AchievementSystem achievementSystem, DiscoverySystem discoverySystem) {
        achievements = achievementSystem;
        discoveries = discoverySystem;
    }

    public static AchievementSystem achievements() {
        return achievements;
    }

    public static DiscoverySystem discoveries() {
        return discoveries;
    }

    public static boolean isWeaponAvailable(String weaponId) {
        if (weaponId == null || weaponId.isBlank() || !Arsenal.isLoaded()) {
            return true;
        }
        try {
            WeaponDefinition definition = Arsenal.getDefinition(weaponId);
            return UnlockEvaluator.isUnlocked(definition.getUnlockRule(), achievements, discoveries);
        } catch (IllegalArgumentException ignored) {
            return true;
        }
    }
}
