package rpgcombat.perks.mission;

import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rpgcombat.combat.models.Action;

/**
 * Índex auxiliar de missions pendents per limitar actualitzacions irrellevants.
 *
 * <p>
 * La llista de {@link MissionProgress} continua sent la font de veritat. Aquest
 * objecte conserva només les missions que encara poden avançar i les distribueix
 * segons els esdeveniments que poden observar; les missions completades o amb
 * recompensa consumida no entren al camí calent.
 * </p>
 */
public final class MissionBudget {
    private final Map<MissionEvent, CandidateBucket> byEvent = new EnumMap<>(MissionEvent.class);
    private final CandidateBucket everyUpdate = new CandidateBucket();

    /**
     * Construeix l'índex a partir de les missions conegudes.
     *
     * @param missions progressos de missió del jugador
     */
    public MissionBudget(Collection<MissionProgress> missions) {
        rebuild(missions);
    }

    /**
     * Regenera l'índex des de la font de veritat externa.
     *
     * @param missions progressos que s'han de reindexar
     */
    public void rebuild(Collection<MissionProgress> missions) {
        byEvent.clear();
        everyUpdate.clear();
        if (missions == null) {
            return;
        }
        for (MissionProgress mission : missions) {
            add(mission);
        }
    }

    /**
     * Afegeix una missió pendent als índexs corresponents.
     *
     * @param mission progrés que acaba d'entrar a l'estat del jugador
     */
    public void add(MissionProgress mission) {
        if (!isPending(mission)) {
            return;
        }

        MissionDefinition definition = mission.definition();
        ObjectiveType type = definition.type();
        if (type == null) {
            everyUpdate.add(mission);
            return;
        }

        switch (type) {
            case ACTION_SEQUENCE -> indexActionSequence(mission);
            case AVOID_EVENT_FOR_TURNS, STATE_MAINTAINED -> everyUpdate.add(mission);
            case CONSECUTIVE_EVENT -> indexConsecutive(definition, mission);
            default -> indexOrEvery(definition.event(), mission);
        }
    }

    /**
     * Retorna les missions pendents que poden observar una actualització.
     *
     * @param update informació del torn
     * @return instantània immutable sense duplicats
     */
    public List<MissionProgress> candidates(MissionUpdate update) {
        Set<MissionEvent> events = update == null ? null : update.events();
        List<MissionProgress> always = everyUpdate.snapshot();

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

        LinkedHashSet<MissionProgress> result = new LinkedHashSet<>(always);
        for (MissionEvent event : events) {
            CandidateBucket indexed = byEvent.get(event);
            if (indexed != null) {
                indexed.addTo(result);
            }
        }
        return List.copyOf(result);
    }

    /**
     * Retira una missió quan deixa de necessitar actualitzacions.
     *
     * @param mission missió completada o descartada
     */
    public void remove(MissionProgress mission) {
        everyUpdate.remove(mission);
        for (CandidateBucket indexed : byEvent.values()) {
            indexed.remove(mission);
        }
    }

    private boolean isPending(MissionProgress mission) {
        return mission != null
                && !mission.completed()
                && !mission.rewardClaimed()
                && mission.definition() != null;
    }

    private void indexConsecutive(MissionDefinition definition, MissionProgress mission) {
        boolean indexed = indexIfPresent(definition.successEvent(), mission);
        indexed |= indexIfPresent(definition.resetEvent(), mission);
        if (!indexed) {
            everyUpdate.add(mission);
        }
    }

    private void indexActionSequence(MissionProgress mission) {
        for (Action action : Action.values()) {
            index(actionEvent(action), mission);
        }
    }

    private MissionEvent actionEvent(Action action) {
        return switch (action) {
            case ATTACK -> MissionEvent.ACTION_ATTACK;
            case DEFEND -> MissionEvent.ACTION_DEFEND;
            case DODGE -> MissionEvent.ACTION_DODGE;
            case CHARGE -> MissionEvent.ACTION_CHARGE;
        };
    }

    private void indexOrEvery(MissionEvent event, MissionProgress mission) {
        if (!indexIfPresent(event, mission)) {
            everyUpdate.add(mission);
        }
    }

    private boolean indexIfPresent(MissionEvent event, MissionProgress mission) {
        if (event == null) {
            return false;
        }
        index(event, mission);
        return true;
    }

    private void index(MissionEvent event, MissionProgress mission) {
        byEvent.computeIfAbsent(event, ignored -> new CandidateBucket()).add(mission);
    }

    /** Bucket mutable amb snapshot immutable reutilitzable per consulta. */
    private static final class CandidateBucket {
        private final LinkedHashSet<MissionProgress> items = new LinkedHashSet<>();
        private List<MissionProgress> snapshot = List.of();
        private boolean dirty;

        void clear() {
            items.clear();
            snapshot = List.of();
            dirty = false;
        }

        void add(MissionProgress mission) {
            if (items.add(mission)) {
                dirty = true;
            }
        }

        void remove(MissionProgress mission) {
            if (items.remove(mission)) {
                dirty = true;
            }
        }

        boolean isEmpty() {
            return items.isEmpty();
        }

        List<MissionProgress> snapshot() {
            if (dirty) {
                snapshot = List.copyOf(items);
                dirty = false;
            }
            return snapshot;
        }

        void addTo(Set<MissionProgress> target) {
            target.addAll(items);
        }
    }
}
