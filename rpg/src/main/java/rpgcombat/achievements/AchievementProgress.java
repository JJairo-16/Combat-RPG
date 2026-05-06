package rpgcombat.achievements;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import rpgcombat.achievements.config.AchievementCondition;
import rpgcombat.achievements.config.AchievementDefinition;
import rpgcombat.achievements.config.AchievementObjective;
import rpgcombat.achievements.config.AchievementObjectiveType;
import rpgcombat.combat.models.Action;

/** Manté el progrés persistent d'un assoliment global. */
public final class AchievementProgress {
    private final AchievementDefinition definition;
    private double progress;
    private int sequenceIndex;
    private boolean completed;
    private Instant completedAt;
    private final Map<String, Double> valueProgress = new LinkedHashMap<>();

    /** Crea un progrés nou. */
    public AchievementProgress(AchievementDefinition definition) {
        this.definition = definition;
    }

    /** Restaura un progrés desat. */
    public AchievementProgress(AchievementDefinition definition, double progress, int sequenceIndex, boolean completed,
            Instant completedAt, Map<String, Double> valueProgress) {
        this.definition = definition;
        this.progress = Math.max(0.0, progress);
        this.sequenceIndex = Math.max(0, sequenceIndex);
        if (valueProgress != null) {
            valueProgress.forEach((key, value) -> {
                if (key != null && value != null) this.valueProgress.put(normalizeValue(key), Math.max(0.0, value));
            });
        }
        this.completed = completed || this.progress >= effectiveTarget();
        this.completedAt = completedAt;
        if (this.completed && this.completedAt == null) this.completedAt = Instant.now();
    }

    public AchievementDefinition definition() { return definition; }
    public double progress() { return progress; }
    public int sequenceIndex() { return sequenceIndex; }
    public boolean completed() { return completed; }
    public Instant completedAt() { return completedAt; }
    public Map<String, Double> valueProgress() { return Map.copyOf(valueProgress); }

    /** Actualitza el progrés i indica si ha canviat. */
    public boolean update(AchievementUpdate update) {
        if (completed || update == null || definition == null || definition.objective() == null) return false;

        double before = progress;
        int sequenceBefore = sequenceIndex;
        boolean completedBefore = completed;
        Map<String, Double> valuesBefore = Map.copyOf(valueProgress);

        AchievementObjective objective = definition.objective();
        if (!matchesConditions(update, objective.conditions())) return false;

        switch (objective.type()) {
            case COUNT_EVENT, CONDITIONAL_COUNT -> addIf(update.has(objective.event()), 1);
            case SUM_VALUE, CONDITIONAL_SUM -> addIf(update.has(objective.event()), update.amountFor(objective.valueKey(), objective.event()));
            case CONSECUTIVE_EVENT -> updateConsecutive(update, objective);
            case AVOID_EVENT_FOR_TURNS -> updateAvoid(update, objective);
            case ACTION_SEQUENCE -> updateSequence(update.ownerAction(), objective.sequence());
            case STATE_REACHED -> addIf(update.has(objective.event()), effectiveTarget());
            case STATE_MAINTAINED -> updateMaintained(update, objective);
            case REACT_TO_EVENT -> updateReact(update, objective);
            case RISK_REWARD -> updateRiskReward(update, objective);
            case ALL_UNIQUE_VALUES -> updateAllUniqueValues(update, objective);
            case EACH_UNIQUE_VALUE_COUNT -> updateEachUniqueValueCount(update, objective);
            case MAX_VALUE_REACHED -> updateMaxValue(update, objective);
        }

        if (progress >= effectiveTarget()) complete();
        return before != progress || sequenceBefore != sequenceIndex || completedBefore != completed
                || !valuesBefore.equals(valueProgress);
    }

    /** Text curt de progrés per al visor. */
    public int viewProgress() {
        return (int) Math.min(viewTarget(), Math.floor(progress));
    }

    /** Objectiu visual, que pot diferir de l'objectiu intern en col·leccions. */
    public int viewGoal() {
        return (int) Math.max(1, Math.ceil(viewTarget()));
    }

    /** Gestiona esdeveniments consecutius. */
    private void updateConsecutive(AchievementUpdate update, AchievementObjective objective) {
        if (update.has(objective.successEvent())) {
            add(1);
            return;
        }
        if (update.has(objective.resetEvent())) {
            progress = 0;
        }
    }

    /** Gestiona evitar esdeveniments durant torns. */
    private void updateAvoid(AchievementUpdate update, AchievementObjective objective) {
        if (update.has(objective.event())) {
            progress = 0;
        } else {
            add(1);
        }
    }

    /** Gestiona seqüències d'accions. */
    private void updateSequence(Action action, List<Action> sequence) {
        if (action == null || sequence == null || sequence.isEmpty()) return;
        if (sequenceIndex < 0 || sequenceIndex >= sequence.size()) sequenceIndex = 0;

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
    private void updateMaintained(AchievementUpdate update, AchievementObjective objective) {
        if (update.has(objective.event())) {
            add(1);
        } else {
            progress = 0;
        }
    }

    /** Gestiona reaccions condicionades a dos esdeveniments. */
    private void updateReact(AchievementUpdate update, AchievementObjective objective) {
        if (update.has(objective.successEvent()) && update.has(objective.event())) {
            add(1);
        }
    }

    /** Gestiona objectius de risc-recompensa. */
    private void updateRiskReward(AchievementUpdate update, AchievementObjective objective) {
        if (update.has(objective.event()) && update.amountFor(objective.valueKey(), objective.successEvent()) >= objective.value()) {
            add(1);
        }
    }

    /** Gestiona col·leccions on cada valor únic només compta una vegada. */
    private void updateAllUniqueValues(AchievementUpdate update, AchievementObjective objective) {
        if (!update.has(objective.event())) return;
        String value = uniqueValue(update, objective);
        if (value == null) return;
        if (!isRequiredOrOpen(value, objective)) return;
        valueProgress.putIfAbsent(value, 1.0);
        progress = collectedRequiredCount(objective);
    }

    /** Gestiona col·leccions on cada valor requerit ha d'arribar a X repeticions. */
    private void updateEachUniqueValueCount(AchievementUpdate update, AchievementObjective objective) {
        if (!update.has(objective.event())) return;
        String value = uniqueValue(update, objective);
        if (value == null) return;
        if (!isRequiredOrOpen(value, objective)) return;

        valueProgress.merge(value, 1.0, Double::sum);
        double perValueTarget = Math.max(1.0, objective.target());
        progress = objective.requiredValues() == null || objective.requiredValues().isEmpty()
                ? cappedSum(perValueTarget)
                : objective.requiredValues().stream()
                        .map(this::normalizeValue)
                        .mapToDouble(key -> Math.min(perValueTarget, valueProgress.getOrDefault(key, 0.0)))
                        .sum();
    }

    /** Gestiona marques de valor màxim. */
    private void updateMaxValue(AchievementUpdate update, AchievementObjective objective) {
        double amount = update.amountFor(objective.valueKey(), objective.event());
        if (amount > progress) progress = amount;
    }

    /** Avalua totes les condicions declaratives. */
    private boolean matchesConditions(AchievementUpdate update, List<AchievementCondition> conditions) {
        if (conditions == null || conditions.isEmpty()) return true;
        return conditions.stream().allMatch(condition -> condition.matches(update));
    }

    /** Obté el valor que identifica un element únic. */
    private String uniqueValue(AchievementUpdate update, AchievementObjective objective) {
        String key = objective.uniqueKey() == null || objective.uniqueKey().isBlank() ? objective.valueKey() : objective.uniqueKey();
        String value = update.text(key);
        return value == null || value.isBlank() ? null : normalizeValue(value);
    }

    /** Indica si el valor és acceptable per a la col·lecció. */
    private boolean isRequiredOrOpen(String value, AchievementObjective objective) {
        List<String> required = objective.requiredValues();
        return required == null || required.isEmpty() || required.stream().map(this::normalizeValue).anyMatch(value::equals);
    }

    /** Suma limitada per objectiu individual. */
    private double cappedSum(double perValueTarget) {
        return valueProgress.values().stream().mapToDouble(value -> Math.min(perValueTarget, value)).sum();
    }

    /** Comptador d'elements requerits ja presents. */
    private double collectedRequiredCount(AchievementObjective objective) {
        List<String> required = objective.requiredValues();
        if (required == null || required.isEmpty()) return valueProgress.size();
        return required.stream().map(this::normalizeValue).filter(valueProgress::containsKey).count();
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

    /** Marca l'assoliment com a completat. */
    private void complete() {
        completed = true;
        progress = Math.max(progress, effectiveTarget());
        if (completedAt == null) completedAt = Instant.now();
    }

    /** Objectiu real de l'assoliment. */
    private double effectiveTarget() {
        if (definition == null || definition.objective() == null) return 1.0;
        AchievementObjective objective = definition.objective();
        if (objective.type() == AchievementObjectiveType.ACTION_SEQUENCE) {
            List<Action> sequence = objective.sequence();
            if (sequence != null && !sequence.isEmpty()) return sequence.size();
        }
        if (objective.type() == AchievementObjectiveType.ALL_UNIQUE_VALUES) {
            List<String> required = objective.requiredValues();
            if (required != null && !required.isEmpty()) return required.size();
        }
        if (objective.type() == AchievementObjectiveType.EACH_UNIQUE_VALUE_COUNT) {
            List<String> required = objective.requiredValues();
            if (required != null && !required.isEmpty()) return required.size() * Math.max(1.0, objective.target());
        }
        return Math.max(1.0, objective.target());
    }

    /** Objectiu que es mostra al visor. */
    private double viewTarget() {
        return effectiveTarget();
    }

    private String normalizeValue(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }
}
