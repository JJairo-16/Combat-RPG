package rpgcombat.achievements;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
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
            Instant completedAt) {
        this(definition, progress, sequenceIndex, completed, completedAt, Map.of());
    }

    /** Restaura un progrés desat amb dades per valor únic. */
    public AchievementProgress(AchievementDefinition definition, double progress, int sequenceIndex, boolean completed,
            Instant completedAt, Map<String, Double> valueProgress) {
        this.definition = definition;
        this.progress = Math.max(0.0, progress);
        this.sequenceIndex = Math.max(0, sequenceIndex);
        if (valueProgress != null) {
            valueProgress.forEach((key, value) -> {
                if (key != null && value != null) this.valueProgress.put(key, Math.max(0.0, value));
            });
        }
        recomputeCollectionProgress();
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
        switch (objective.type()) {
            case COUNT_EVENT -> addIf(update.has(objective.event()), 1);
            case SUM_VALUE -> add(update.amountFor(objective));
            case CONDITIONAL_COUNT -> addIf(update.has(objective.event()) && matches(update, objective), 1);
            case CONDITIONAL_SUM -> addIf(matchesEventOrNoEvent(update, objective) && matches(update, objective), update.amountFor(objective));
            case CONSECUTIVE_EVENT -> updateConsecutive(update, objective);
            case AVOID_EVENT_FOR_TURNS -> updateAvoidForTurns(update, objective);
            case ACTION_SEQUENCE -> updateSequence(update.ownerAction(), objective.sequence());
            case STATE_REACHED -> addIf(matches(update, objective) && matchesEventOrNoEvent(update, objective), effectiveTarget());
            case STATE_MAINTAINED -> updateMaintained(update, objective);
            case ALL_UNIQUE_VALUES -> updateAllUnique(update, objective);
            case EACH_UNIQUE_VALUE_COUNT -> updateEachUniqueCount(update, objective);
            case MAX_VALUE_REACHED -> updateMaxValue(update, objective);
        }

        recomputeCollectionProgress();
        if (progress >= effectiveTarget()) complete();
        return before != progress || sequenceBefore != sequenceIndex || completedBefore != completed || !valuesBefore.equals(valueProgress);
    }

    /** Text curt de progrés per al visor. */
    public int viewProgress() {
        return (int) Math.min(effectiveTarget(), Math.floor(progress));
    }

    /** Objectiu curt per al visor. */
    public int viewGoal() {
        return (int) Math.ceil(effectiveTarget());
    }

    /** Gestiona esdeveniments consecutius. */
    private void updateConsecutive(AchievementUpdate update, AchievementObjective objective) {
        if (!matches(update, objective)) return;
        if (update.has(objective.successEvent() == null ? objective.event() : objective.successEvent())) {
            add(1);
            return;
        }
        if (objective.resetEvent() == null || update.has(objective.resetEvent())) {
            progress = 0;
        }
    }

    /** Gestiona evitar un esdeveniment durant torns consecutius. */
    private void updateAvoidForTurns(AchievementUpdate update, AchievementObjective objective) {
        if (!matches(update, objective)) return;
        if (update.has(objective.resetEvent() == null ? objective.event() : objective.resetEvent())) {
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
        if (matchesEventOrNoEvent(update, objective) && matches(update, objective)) {
            add(1);
        } else {
            progress = 0;
        }
    }

    /** Gestiona col·leccions on cada valor només compta una vegada. */
    private void updateAllUnique(AchievementUpdate update, AchievementObjective objective) {
        if (!matchesEventOrNoEvent(update, objective) || !matches(update, objective)) return;
        String key = update.stringField(fieldOrDefault(objective.uniqueField(), "weaponId"));
        if (key == null || key.isBlank()) return;
        if (objective.requiredValues() != null && !objective.requiredValues().isEmpty()
                && !objective.requiredValues().contains(key)) return;
        valueProgress.putIfAbsent(key, 1.0);
    }

    /** Gestiona col·leccions on cada valor ha d'arribar a un mínim. */
    private void updateEachUniqueCount(AchievementUpdate update, AchievementObjective objective) {
        if (!matchesEventOrNoEvent(update, objective) || !matches(update, objective)) return;
        String key = update.stringField(fieldOrDefault(objective.uniqueField(), "weaponId"));
        if (key == null || key.isBlank()) return;
        if (objective.requiredValues() != null && !objective.requiredValues().isEmpty()
                && !objective.requiredValues().contains(key)) return;
        valueProgress.merge(key, 1.0, Double::sum);
    }

    /** Gestiona assolir un valor màxim puntual. */
    private void updateMaxValue(AchievementUpdate update, AchievementObjective objective) {
        if (!matchesEventOrNoEvent(update, objective) || !matches(update, objective)) return;
        progress = Math.max(progress, update.numericField(fieldOrDefault(objective.valueField(), "damage")));
    }

    /** Recalcula progrés visual de col·leccions. */
    private void recomputeCollectionProgress() {
        if (definition == null || definition.objective() == null) return;
        AchievementObjective objective = definition.objective();
        if (objective.type() == AchievementObjectiveType.ALL_UNIQUE_VALUES) {
            progress = countRequiredCollected(objective);
        } else if (objective.type() == AchievementObjectiveType.EACH_UNIQUE_VALUE_COUNT) {
            double targetPerValue = Math.max(1.0, objective.targetPerValue());
            if (objective.requiredValues() == null || objective.requiredValues().isEmpty()) {
                progress = valueProgress.values().stream().filter(v -> v >= targetPerValue).count();
            } else {
                progress = objective.requiredValues().stream()
                        .filter(v -> valueProgress.getOrDefault(v, 0.0) >= targetPerValue)
                        .count();
            }
        }
    }

    private long countRequiredCollected(AchievementObjective objective) {
        if (objective.requiredValues() == null || objective.requiredValues().isEmpty()) return valueProgress.size();
        return objective.requiredValues().stream().filter(valueProgress::containsKey).count();
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

    private boolean matchesEventOrNoEvent(AchievementUpdate update, AchievementObjective objective) {
        return objective.event() == null || update.has(objective.event());
    }

    private boolean matches(AchievementUpdate update, AchievementObjective objective) {
        if (objective.conditions() == null || objective.conditions().isEmpty()) return true;
        for (AchievementCondition condition : objective.conditions()) {
            if (!matchesCondition(update, condition)) return false;
        }
        return true;
    }

    private boolean matchesCondition(AchievementUpdate update, AchievementCondition condition) {
        String field = condition.field();
        String op = condition.operator() == null || condition.operator().isBlank() ? "==" : condition.operator();
        String expected = condition.value();
        Object actual = update.field(field);
        if (actual instanceof Number || isNumber(expected)) {
            double left = update.numericField(field);
            double right = parseDouble(expected, 0.0);
            return switch (op) {
                case "==" -> Double.compare(left, right) == 0;
                case "!=" -> Double.compare(left, right) != 0;
                case ">" -> left > right;
                case ">=" -> left >= right;
                case "<" -> left < right;
                case "<=" -> left <= right;
                default -> false;
            };
        }
        if (actual instanceof Boolean || "true".equalsIgnoreCase(expected) || "false".equalsIgnoreCase(expected)) {
            boolean left = update.booleanField(field);
            boolean right = Boolean.parseBoolean(expected);
            return switch (op) {
                case "==" -> left == right;
                case "!=" -> left != right;
                default -> false;
            };
        }
        String left = actual == null ? null : String.valueOf(actual);
        return switch (op) {
            case "==" -> expected == null ? left == null : expected.equals(left);
            case "!=" -> expected == null ? left != null : !expected.equals(left);
            default -> false;
        };
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
        if ((objective.type() == AchievementObjectiveType.ALL_UNIQUE_VALUES
                || objective.type() == AchievementObjectiveType.EACH_UNIQUE_VALUE_COUNT)
                && objective.requiredValues() != null && !objective.requiredValues().isEmpty()) {
            return objective.requiredValues().size();
        }
        return Math.max(1.0, objective.target());
    }

    private String fieldOrDefault(String value, String def) {
        return value == null || value.isBlank() ? def : value;
    }

    private boolean isNumber(String value) {
        if (value == null) return false;
        try { Double.parseDouble(value); return true; } catch (NumberFormatException ignored) { return false; }
    }

    private double parseDouble(String value, double def) {
        if (value == null) return def;
        try { return Double.parseDouble(value); } catch (NumberFormatException ignored) { return def; }
    }
}
