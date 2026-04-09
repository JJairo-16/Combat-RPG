package rpgcombat.models.breeds;

import rpgcombat.models.characters.Character;
import rpgcombat.models.characters.Stat;
import rpgcombat.models.characters.Statistics;
import rpgcombat.weapons.attack.AttackResult;
import rpgcombat.weapons.config.WeaponType;

public class Halfling extends Character {
    private final Statistics modStats;
    private static final double ATTACK_LUCK_BONUS = 1.1;

    public Halfling(String name, int age, int[] stats) {
        super(name, age, stats, Breed.HALFLING);

        int[] tmp = applyBreed(stats, Breed.HALFLING);
        tmp[Stat.LUCK.ordinal()] *= ATTACK_LUCK_BONUS;
        this.modStats = new Statistics(tmp);
    }

    @Override
    public AttackResult attack() {
        if (weapon == null)
            return attackUnarmed(modStats);

        return weapon.attack(modStats, rng);
    }

    private AttackResult attackUnarmed(Statistics stats) {
        return new AttackResult(
                    WeaponType.PHYSICAL.getBasicDamage(5, stats),
                    "ataca amb les mans desnudes.");
    }
}
