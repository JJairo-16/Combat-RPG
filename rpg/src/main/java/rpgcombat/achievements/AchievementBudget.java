package rpgcombat.achievements;

import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rpgcombat.achievements.config.AchievementObjective;
import rpgcombat.achievements.config.AchievementObjectiveType;
import rpgcombat.combat.models.Action;

/**
 * Índex auxiliar d'assoliments pendents per reduir actualitzacions irrellevants.
 *
 * <p>
 * Aquest objecte no és la font de veritat del progrés. El sistema d'assoliments
 * continua posseint els {@link AchievementProgress}; el budget només conserva
 * referències indexades dels progressos pendents que poden observar una
 * {@link AchievementUpdate}. Quan un assoliment ja està completat no entra als
 * índexs i, si es completa mentre s'avalua, es pot retirar amb
 * {@link #remove(AchievementProgress)}.
 * </p>
 *
 * <p>
 * La indexació es genera des de la definició de cada objectiu. Els objectius
 * lligats a esdeveniments entren als buckets d'esdeveniment corresponents i els
 * objectius que han de reaccionar a qualsevol actualització queden en un bucket
 * transversal. Això permet afegir noves definicions sense repetir recorreguts
 * globals al camí calent.
 * </p>
 */
public final class AchievementBudget {
    private final Map<AchievementEvent, CandidateBucket> byEvent =
            new EnumMap<>(AchievementEvent.class);
    private final CandidateBucket everyUpdate = new CandidateBucket();

    /**
     * Construeix l'índex a partir dels progressos existents i descarta els ja
     * completats.
     *
     * @param progressItems progressos coneguts pel sistema d'assoliments
     */
    public AchievementBudget(Collection<AchievementProgress> progressItems) {
        rebuild(progressItems);
    }

    /**
     * Regenera tots els buckets a partir de la font de veritat externa.
     *
     * @param progressItems progressos que s'han de tornar a indexar
     */
    public void rebuild(Collection<AchievementProgress> progressItems) {
        byEvent.clear();
        everyUpdate.clear();
        if (progressItems == null) {
            return;
        }

        for (AchievementProgress progress : progressItems) {
            add(progress);
        }
    }

    /**
     * Retorna només els assoliments pendents que poden consumir una
     * actualització.
     *
     * @param update actualització que s'avaluarà
     * @return instantània immutable de candidats sense duplicats
     */
    public List<AchievementProgress> candidates(AchievementUpdate update) {
        Set<AchievementEvent> events = update == null ? null : update.events();
        List<AchievementProgress> always = everyUpdate.snapshot();

        if (events == null || events.isEmpty()) {
            return always;
        }

        if (events.size() == 1) {
            CandidateBucket indexed = byEvent.get(events.iterator().next());
            if (indexed == null || indexed.isEmpty()) {
                return always;
            }
            if (always.isEmpty()) {
                return indexed.snapshot();
            }
        }

        LinkedHashSet<AchievementProgress> result = new LinkedHashSet<>(always);
        for (AchievementEvent event : events) {
            CandidateBucket indexed = byEvent.get(event);
            if (indexed != null) {
                indexed.addTo(result);
            }
        }
        return List.copyOf(result);
    }

    /**
     * Retira un progrés dels índexs quan ja no ha d'observar actualitzacions.
     *
     * @param progress progrés completat o descartat
     */
    public void remove(AchievementProgress progress) {
        everyUpdate.remove(progress);
        for (CandidateBucket indexed : byEvent.values()) {
            indexed.remove(progress);
        }
    }

    private void add(AchievementProgress progress) {
        if (progress == null || progress.completed() || progress.definition() == null) {
            return;
        }

        AchievementObjective objective = progress.definition().objective();
        AchievementObjectiveType type = objective == null ? null : objective.type();
        if (type == AchievementObjectiveType.ACTION_SEQUENCE) {
            indexActionSequence(progress);
            return;
        }

        if (type == null || observesEveryUpdate(type) || objective.event() == null) {
            everyUpdate.add(progress);
            return;
        }

        index(objective.event(), progress);
    }

    private boolean observesEveryUpdate(AchievementObjectiveType type) {
        return type == AchievementObjectiveType.AVOID_EVENT_FOR_TURNS
                || type == AchievementObjectiveType.CONSECUTIVE_EVENT
                || type == AchievementObjectiveType.STATE_MAINTAINED;
    }

    private void indexActionSequence(AchievementProgress progress) {
        for (Action action : Action.values()) {
            index(actionEvent(action), progress);
        }
    }

    private AchievementEvent actionEvent(Action action) {
        return switch (action) {
            case ATTACK -> AchievementEvent.ACTION_ATTACK;
            case DEFEND -> AchievementEvent.ACTION_DEFEND;
            case DODGE -> AchievementEvent.ACTION_DODGE;
            case CHARGE -> AchievementEvent.ACTION_CHARGE;
        };
    }

    private void index(AchievementEvent event, AchievementProgress progress) {
        if (event == null) {
            everyUpdate.add(progress);
            return;
        }
        byEvent.computeIfAbsent(event, ignored -> new CandidateBucket()).add(progress);
    }

    /**
     * Bucket mutable amb una instantània immutable reutilitzable pel camí calent.
     *
     * <p>
     * Les mutacions invaliden la instantània i les consultes d'un sol bucket
     * poden retornar-la sense construir unions intermèdies.
     * </p>
     */
    private static final class CandidateBucket {
        private final LinkedHashSet<AchievementProgress> items = new LinkedHashSet<>();
        private List<AchievementProgress> snapshot = List.of();
        private boolean dirty;

        void clear() {
            if (items.isEmpty() && snapshot.isEmpty() && !dirty) {
                return;
            }
            items.clear();
            snapshot = List.of();
            dirty = false;
        }

        void add(AchievementProgress progress) {
            if (items.add(progress)) {
                dirty = true;
            }
        }

        void remove(AchievementProgress progress) {
            if (items.remove(progress)) {
                dirty = true;
            }
        }

        boolean isEmpty() {
            return items.isEmpty();
        }

        List<AchievementProgress> snapshot() {
            if (dirty) {
                snapshot = List.copyOf(items);
                dirty = false;
            }
            return snapshot;
        }

        void addTo(Set<AchievementProgress> target) {
            target.addAll(items);
        }
    }
}
