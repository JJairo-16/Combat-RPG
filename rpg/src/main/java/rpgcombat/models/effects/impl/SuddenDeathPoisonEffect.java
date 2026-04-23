package rpgcombat.models.effects.impl;

import java.util.Random;

import rpgcombat.models.characters.Character;
import rpgcombat.models.effects.templates.ConstantDamageEffect;
import rpgcombat.weapons.passives.HitContext;

public class SuddenDeathPoisonEffect extends ConstantDamageEffect {
    public static final String INTERNAL_EFFECT_KEY = "SUDDEN_DEATH_POISON";

    public SuddenDeathPoisonEffect(double damagePerTurn) {
        super(INTERNAL_EFFECT_KEY, Integer.MAX_VALUE, damagePerTurn);
    }

    @Override
    protected double resolveTickDamage(HitContext ctx, Random rng, Character owner) {
        return damagePerTurn;
    }

    @Override
    protected String buildMessage(double appliedDamage, Character owner) {
        return "[RED|!]" + owner.getName() + " pateix " + appliedDamage + " de dany per verí letal.";
    }
}
