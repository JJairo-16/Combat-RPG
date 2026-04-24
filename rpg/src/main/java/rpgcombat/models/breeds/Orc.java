package rpgcombat.models.breeds;

import rpgcombat.models.characters.Character;
import rpgcombat.models.characters.Stat;
import rpgcombat.models.characters.Statistics;
import rpgcombat.weapons.Weapon;
import rpgcombat.weapons.attack.AttackResult;
import rpgcombat.weapons.config.WeaponType;

public class Orc extends Character {
    private final Statistics modStats;
    private static final double ATTACK_PHISICAL_BONUS = 1.2;

    public Orc(String name, int age, int[] stats) {
        super(name, age, stats, Breed.ORC);
        
        int[] tmp = applyBreed(stats, Breed.ORC);
        tmp[Stat.STRENGTH.ordinal()] *= ATTACK_PHISICAL_BONUS;
        this.modStats = new Statistics(tmp);
    }


    @Override
    public boolean setWeapon(Weapon w) {
        if (!w.canEquip(stats)) {
            return false;
        }

        if (w.getType() == WeaponType.MAGICAL) {
            return false;
        }

        this.weapon = w;
        return true;
    }

    @Override
    public AttackResult attack() {
        if (weapon == null)
            return unarmedAttack.attackUnarmed(15, modStats);

        Statistics statsToUse = isPhisicalWeapon(weapon) ? modStats : stats;
        return weapon.attack(statsToUse, rng);
    }

    private boolean isPhisicalWeapon(Weapon w) {
        if (w == null)
            return true;

        return w.getType() == WeaponType.PHYSICAL;
    }
}
