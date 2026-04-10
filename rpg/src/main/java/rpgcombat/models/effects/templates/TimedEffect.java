package rpgcombat.models.effects.templates;

import rpgcombat.models.effects.Effect;
import rpgcombat.models.effects.EffectState;

/**
 * Base per a efectes amb duració per torns.
 */
public abstract class TimedEffect implements Effect {

    private final String key;
    protected final EffectState state;

    protected TimedEffect(String key, int turns) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("La clau de l'efecte no pot ser nul·la");
        }
        if (turns <= 0) {
            throw new IllegalArgumentException("Els torns han de ser > 0");
        }

        this.key = key;
        this.state = EffectState.ofDuration(turns);
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public EffectState state() {
        return state;
    }

    @Override
    public boolean isExpired() {
        return state.remainingTurns() <= 0;
    }
}