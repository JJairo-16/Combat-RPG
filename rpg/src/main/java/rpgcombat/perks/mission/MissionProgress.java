package rpgcombat.perks.mission;

import java.util.List;

import rpgcombat.combat.models.Action;

/** Estat mutable d'una missió assignada. */
public final class MissionProgress {
    private final MissionDefinition definition;
    private double progress;
    private int sequenceIndex;
    private boolean completed;
    private boolean rewardClaimed;

    public MissionProgress(MissionDefinition definition) {
        this.definition = definition;
    }

    public MissionDefinition definition() {
        return definition;
    }

    public double progress() {
        return progress;
    }

    public boolean completed() {
        return completed;
    }

    public boolean rewardClaimed() {
        return rewardClaimed;
    }

    public void markRewardClaimed() {
        rewardClaimed = true;
    }

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

    public String progressText() {
        double target = definition.target();
        if (target <= 1.0) return completed ? "Completada" : "Pendent";
        return Math.min(target, Math.floor(progress)) + "/" + Math.floor(target);
    }

    private void updateConsecutive(MissionUpdate update) {
        if (update.has(definition.successEvent())) {
            add(1);
            return;
        }
        if (update.has(definition.resetEvent())) {
            progress = 0;
        }
    }

    private void updateAvoid(MissionUpdate update) {
        if (update.has(definition.event())) {
            progress = 0;
        } else {
            add(1);
        }
    }

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

    private void updateMaintained(MissionUpdate update) {
        if (update.has(definition.event())) {
            add(1);
        } else {
            progress = 0;
        }
    }

    private void updateReact(MissionUpdate update) {
        if (update.has(definition.successEvent()) && update.has(definition.event())) {
            add(1);
        }
    }

    private void updateRiskReward(MissionUpdate update) {
        if (update.has(definition.event()) && update.amountFor(definition.successEvent()) >= definition.value()) {
            add(1);
        }
    }

    private void addIf(boolean condition, double amount) {
        if (condition) add(amount);
    }

    private void add(double amount) {
        if (amount <= 0) return;
        progress += amount;
    }

    private void complete() {
        completed = true;
        progress = Math.max(progress, definition.target());
    }
}
