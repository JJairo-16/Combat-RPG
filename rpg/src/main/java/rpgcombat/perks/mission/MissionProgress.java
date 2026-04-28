package rpgcombat.perks.mission;

import java.util.List;

import rpgcombat.combat.models.Action;

/**
 * Manté l'estat i el progrés d'una missió assignada.
 */
public final class MissionProgress {
    private final MissionDefinition definition;
    private double progress;
    private int sequenceIndex;
    private boolean completed;
    private boolean rewardClaimed;

    /**
     * Crea el progrés per a una missió.
     *
     * @param definition definició de la missió
     */
    public MissionProgress(MissionDefinition definition) {
        this.definition = definition;
    }

    /** @return definició de la missió */
    public MissionDefinition definition() {
        return definition;
    }

    /** @return progrés actual */
    public double progress() {
        return progress;
    }

    /** @return si la missió està completada */
    public boolean completed() {
        return completed;
    }

    /** @return si la recompensa s'ha reclamat */
    public boolean rewardClaimed() {
        return rewardClaimed;
    }

    /** Marca la recompensa com a reclamada. */
    public void markRewardClaimed() {
        rewardClaimed = true;
    }

    /**
     * Actualitza el progrés segons un esdeveniment.
     *
     * @param update actualització de missió
     */
    public void update(MissionUpdate update) {
        if (completed || update == null || definition == null) return;

        switch (definition.type()) {
            case COUNT_EVENT -> addIf(update.has(definition.event()), 1);
            case SUM_VALUE -> add(update.amountFor(definition.event()));
            case CONSECUTIVE_EVENT -> updateConsecutive(update);
            case AVOID_EVENT_FOR_TURNS -> updateAvoid(update);
            case ACTION_SEQUENCE -> updateSequence(update.ownerAction());
            case STATE_REACHED -> addIf(update.has(definition.event()), definition.target());
            case STATE_MAINTAINED -> updateMaintained(update);
            case REACT_TO_EVENT -> updateReact(update);
            case RISK_REWARD -> updateRiskReward(update);
        }

        if (progress >= definition.target()) complete();
    }

    /**
     * Text representatiu del progrés.
     */
    public String progressText() {
        double target = definition.target();
        if (target <= 1.0) return completed ? "Completada" : "Pendent";
        return Math.min(target, Math.floor(progress)) + "/" + Math.floor(target);
    }

    /** Gestiona esdeveniments consecutius. */
    private void updateConsecutive(MissionUpdate update) {
        if (update.has(definition.successEvent())) {
            add(1);
            return;
        }
        if (update.has(definition.resetEvent())) {
            progress = 0;
        }
    }

    /** Gestiona evitar esdeveniments durant torns. */
    private void updateAvoid(MissionUpdate update) {
        if (update.has(definition.event())) {
            progress = 0;
        } else {
            add(1);
        }
    }

    /** Gestiona seqüències d'accions. */
    private void updateSequence(Action action) {
        List<Action> sequence = definition.sequence();
        if (sequence == null || sequence.isEmpty()) return;

        if (action == sequence.get(sequenceIndex)) {
            sequenceIndex++;
            progress = sequenceIndex;
            if (sequenceIndex >= sequence.size()) complete();
            return;
        }

        sequenceIndex = action == sequence.get(0) ? 1 : 0;
        progress = sequenceIndex;
    }

    /** Gestiona mantenir un estat. */
    private void updateMaintained(MissionUpdate update) {
        if (update.has(definition.event())) {
            add(1);
        } else {
            progress = 0;
        }
    }

    /** Gestiona reaccions a esdeveniments. */
    private void updateReact(MissionUpdate update) {
        if (update.has(definition.successEvent()) && update.has(definition.event())) {
            add(1);
        }
    }

    /** Gestiona mecànica risc-recompensa. */
    private void updateRiskReward(MissionUpdate update) {
        if (update.has(definition.event()) && update.amountFor(definition.successEvent()) >= definition.value()) {
            add(1);
        }
    }

    /** Afegeix progrés si es compleix una condició. */
    private void addIf(boolean condition, double amount) {
        if (condition) add(amount);
    }

    /** Incrementa el progrés. */
    private void add(double amount) {
        if (amount <= 0) return;
        progress += amount;
    }

    /** Marca la missió com a completada. */
    private void complete() {
        completed = true;
        progress = Math.max(progress, definition.target());
    }
}