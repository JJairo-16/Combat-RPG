package rpgcombat.discovery.ui.filter;

import static rpgcombat.discovery.ui.render.DiscoveryText.containsAny;
import static rpgcombat.discovery.ui.render.DiscoveryText.hasText;
import static rpgcombat.discovery.ui.render.DiscoveryText.lower;
import static rpgcombat.discovery.ui.render.DiscoveryUiStyle.CATEGORY_PERKS;
import static rpgcombat.discovery.ui.render.DiscoveryUiStyle.CATEGORY_WEAPONS;

import java.util.ArrayList;
import java.util.List;

import rpgcombat.discovery.ui.models.DiscoveryCategoryView;
import rpgcombat.discovery.ui.models.DiscoveryEntryView;

/** Filtres disponibles per mostrar entrades del descobriment. */
public enum DiscoveryFilter {
    ALL("Totes", DiscoveryFilterGroup.GENERAL) {
        @Override
        public boolean matches(DiscoveryEntryView entry) {
            return true;
        }
    },

    WEAPON_PHYSICAL("Físiques", DiscoveryFilterGroup.WEAPONS) {
        @Override
        public boolean matches(DiscoveryEntryView entry) {
            String text = searchableText(entry);
            return containsAny(text,
                    "físic", "fisic", "física", "fisica",
                    "dany físic", "dany fisic",
                    "cos a cos", "melee",
                    "força", "forca");
        }
    },

    WEAPON_MAGIC("Màgiques", DiscoveryFilterGroup.WEAPONS) {
        @Override
        public boolean matches(DiscoveryEntryView entry) {
            String text = searchableText(entry);
            return containsAny(text,
                    "màgic", "magic", "màgica", "magica",
                    "mana", "mp",
                    "encanteri", "màgia", "magia",
                    "elemental", "arcà", "arca");
        }
    },

    WEAPON_RANGED("De rang", DiscoveryFilterGroup.WEAPONS) {
        @Override
        public boolean matches(DiscoveryEntryView entry) {
            String text = searchableText(entry);
            return containsAny(text,
                    "rang", "distància", "distancia",
                    "projectil", "fletxa", "arc",
                    "llançament", "llancament",
                    "arma de rang");
        }
    },

    PERK_STRATEGY("Estratègia", DiscoveryFilterGroup.PERKS) {
        @Override
        public boolean matches(DiscoveryEntryView entry) {
            String text = searchableText(entry);
            return containsAny(text,
                    "estratègia", "estrategia",
                    "tàctica", "tactica",
                    "planificació", "planificacio",
                    "control", "posició", "posicio");
        }
    },

    PERK_LUCK("Sort", DiscoveryFilterGroup.PERKS) {
        @Override
        public boolean matches(DiscoveryEntryView entry) {
            String text = searchableText(entry);
            return containsAny(text,
                    "sort", "atzar", "probabilitat",
                    "crític", "critic",
                    "aleatori", "random");
        }
    },

    PERK_CHAOS("Caos", DiscoveryFilterGroup.PERKS) {
        @Override
        public boolean matches(DiscoveryEntryView entry) {
            String text = searchableText(entry);
            return containsAny(text,
                    "caos", "caòtic", "caotic",
                    "inestable", "descontrol",
                    "aleatori", "imprevisible");
        }
    },

    PERK_CORRUPTED("Corruptes", DiscoveryFilterGroup.PERKS) {
        @Override
        public boolean matches(DiscoveryEntryView entry) {
            String text = searchableText(entry);
            return containsAny(text,
                    "corrupte", "corrupta", "corruptes",
                    "corrupció", "corrupcio",
                    "maledicció", "malediccio",
                    "sang", "ombra");
        }
    };

    private final String label;
    private final DiscoveryFilterGroup group;

    /** Crea un filtre amb etiqueta i grup. */
    DiscoveryFilter(String label, DiscoveryFilterGroup group) {
        this.label = label;
        this.group = group;
    }

    /** Indica si una entrada passa aquest filtre. */
    public abstract boolean matches(DiscoveryEntryView entry);

    /** Etiqueta visible del filtre. */
    public String label() {
        return label;
    }

    /** Indica si el filtre és aplicable a una categoria. */
    public boolean availableFor(DiscoveryCategoryView category) {
        return this == ALL || group == groupFor(category);
    }

    /** Retorna el filtre següent disponible. */
    public static DiscoveryFilter next(DiscoveryCategoryView category, DiscoveryFilter current) {
        return step(category, current, 1);
    }

    /** Retorna el filtre anterior disponible. */
    public static DiscoveryFilter previous(DiscoveryCategoryView category, DiscoveryFilter current) {
        return step(category, current, -1);
    }

    /** Llista els filtres disponibles per a una categoria. */
    public static List<DiscoveryFilter> availableFiltersFor(DiscoveryCategoryView category) {
        DiscoveryFilterGroup categoryGroup = groupFor(category);
        List<DiscoveryFilter> filters = new ArrayList<>();

        for (DiscoveryFilter filter : values()) {
            if (filter == ALL || filter.group == categoryGroup) {
                filters.add(filter);
            }
        }

        return filters;
    }

    /** Retorna el grup de filtres d'una categoria. */
    public static DiscoveryFilterGroup groupFor(DiscoveryCategoryView category) {
        if (category == null || category.title() == null) {
            return DiscoveryFilterGroup.GENERAL;
        }

        String title = lower(category.title());

        if (title.equals(CATEGORY_WEAPONS)) {
            return DiscoveryFilterGroup.WEAPONS;
        }

        if (title.equals(CATEGORY_PERKS)) {
            return DiscoveryFilterGroup.PERKS;
        }

        return DiscoveryFilterGroup.GENERAL;
    }

    /** Indica si una categoria té filtres específics. */
    public static boolean hasFilters(DiscoveryCategoryView category) {
        return groupFor(category) != DiscoveryFilterGroup.GENERAL;
    }

    /** Avança o retrocedeix dins els filtres disponibles. */
    private static DiscoveryFilter step(DiscoveryCategoryView category, DiscoveryFilter current, int delta) {
        List<DiscoveryFilter> filters = availableFiltersFor(category);
        if (filters.isEmpty()) {
            return ALL;
        }

        int index = filters.indexOf(current);
        if (index < 0) {
            return filters.get(0);
        }

        return filters.get((index + delta + filters.size()) % filters.size());
    }

    /** Construeix el text usat per filtrar una entrada. */
    private static String searchableText(DiscoveryEntryView entry) {
        if (entry == null) {
            return "";
        }

        StringBuilder text = new StringBuilder();

        appendSearchable(text, entry.title());
        appendSearchable(text, entry.shortDescription());
        appendSearchable(text, entry.hint());
        appendSearchable(text, entry.discoveredWhen());

        if (entry.details() != null) {
            for (String detail : entry.details()) {
                appendSearchable(text, detail);
            }
        }

        return lower(text.toString());
    }

    /** Afegeix text cercable si no és buit. */
    private static void appendSearchable(StringBuilder out, String text) {
        if (hasText(text)) {
            out.append(' ').append(text);
        }
    }
}