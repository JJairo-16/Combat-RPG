package rpgcombat.discovery.persistence;

import java.util.List;

/** Format de desament del progrés global de descobriments. */
public record DiscoverySaveData(
        int version,
        List<DiscoverySavedEntry> discovered) {
}
