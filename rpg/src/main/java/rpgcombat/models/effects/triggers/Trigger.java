package rpgcombat.models.effects.triggers;

import rpgcombat.models.effects.Effect;
import rpgcombat.models.effects.EffectState;

/**
 * Classe base per a efectes de tipus trigger.
 * No expiren i mantenen un estat intern fix.
 */
public abstract class Trigger implements Effect {
    protected final String key;
    protected final EffectState state = new EffectState(0, 0, Integer.MAX_VALUE, 0);

    /**
     * @param key identificador únic del trigger
     */
    Trigger(String key) {
        this.key = key;
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public EffectState state() {
        return state;
    }

    /**
     * Els triggers no expiren.
     */
    @Override
    public boolean isExpired() {
        return false;
    }
}