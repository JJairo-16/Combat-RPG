package rpgcombat.discovery.config;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import rpgcombat.combat.models.Action;
import rpgcombat.discovery.DiscoveryCategory;
import rpgcombat.discovery.DiscoveryKey;
import rpgcombat.gamemode.model.GameModeDefinition;
import rpgcombat.gamemode.registry.GameModeRegistry;
import rpgcombat.models.breeds.Breed;
import rpgcombat.models.effects.triggers.UniversalLifeStealTrigger;
import rpgcombat.perks.PerkDefinition;
import rpgcombat.perks.PerkRegistry;
import rpgcombat.perks.divine.DivinePerkDefinition;
import rpgcombat.perks.divine.DivinePerkRegistry;
import rpgcombat.perks.mission.MissionDefinition;
import rpgcombat.perks.mission.MissionRegistry;
import rpgcombat.perks.synergy.SynergyDefinition;
import rpgcombat.perks.synergy.SynergyRegistry;
import rpgcombat.weapons.Arsenal;
import rpgcombat.weapons.config.WeaponDefinition;

/**
 * Catàleg final de descobriments, amb entrades automàtiques i overrides JSON.
 */
public final class DiscoveryCatalog {
    private final Map<DiscoveryCategory, DiscoveryCategoryDefinition> categories;
    private final Map<DiscoveryKey, DiscoveryEntryDefinition> entries;

    /** Crea un catàleg immutable. */
    private DiscoveryCatalog(Map<DiscoveryCategory, DiscoveryCategoryDefinition> categories,
            Map<DiscoveryKey, DiscoveryEntryDefinition> entries) {
        this.categories = Map.copyOf(categories);
        this.entries = Map.copyOf(entries);
    }

    /** Construeix el catàleg final a partir de la configuració carregada. */
    public static DiscoveryCatalog build(DiscoveryCatalogConfig config) {
        Map<DiscoveryCategory, DiscoveryCategoryDefinition> categories = defaultCategories();
        applyCategoryOverrides(categories, config == null ? null : config.categories());

        Map<DiscoveryKey, DiscoveryEntryDefinition> entries = new LinkedHashMap<>();
        addAutomaticEntries(entries);
        applyManualEntries(entries, config == null ? null : config.entries());

        return new DiscoveryCatalog(categories, entries);
    }

    /** Indica si una entrada existeix i pot ser descoberta. */
    public boolean contains(DiscoveryCategory category, String id) {
        if (category == null || id == null || id.isBlank())
            return false;
        return entries.containsKey(new DiscoveryKey(category, id));
    }

    /** Retorna una entrada si existeix. */
    public Optional<DiscoveryEntryDefinition> find(DiscoveryCategory category, String id) {
        if (category == null || id == null || id.isBlank())
            return Optional.empty();
        return Optional.ofNullable(entries.get(new DiscoveryKey(category, id)));
    }

    /** Categories ordenades per mostrar. */
    public List<DiscoveryCategoryDefinition> categories() {
        return categories.values().stream()
                .sorted(Comparator.comparingInt(DiscoveryCategoryDefinition::sortOrder))
                .toList();
    }

    /** Entrades d'una categoria, ordenades per ordre manual i títol. */
    public List<DiscoveryEntryDefinition> entries(DiscoveryCategory category) {
        return entries.values().stream()
                .filter(entry -> entry.category() == category)
                .sorted(Comparator
                        .comparingInt(DiscoveryEntryDefinition::sortOrder)
                        .thenComparing(DiscoveryEntryDefinition::title))
                .toList();
    }

    /** Total d'entrades del catàleg. */
    public int totalEntries() {
        return entries.size();
    }

    /** Crea les categories per defecte. */
    private static Map<DiscoveryCategory, DiscoveryCategoryDefinition> defaultCategories() {
        Map<DiscoveryCategory, DiscoveryCategoryDefinition> result = new EnumMap<>(DiscoveryCategory.class);
        int order = 10;
        for (DiscoveryCategory category : DiscoveryCategory.values()) {
            result.put(category, new DiscoveryCategoryDefinition(category, category.defaultTitle(), "", true, order));
            order += 10;
        }
        return result;
    }

    /** Aplica canvis JSON sobre les categories. */
    private static void applyCategoryOverrides(Map<DiscoveryCategory, DiscoveryCategoryDefinition> categories,
            List<DiscoveryCategoryConfig> overrides) {
        if (overrides == null)
            return;
        for (DiscoveryCategoryConfig override : overrides) {
            DiscoveryCategory id = parseCategory(override == null ? null : override.id());
            if (id == null)
                continue;
            DiscoveryCategoryDefinition base = categories.get(id);
            categories.put(id, new DiscoveryCategoryDefinition(
                    id,
                    textOr(override.title(), base.title()),
                    textOr(override.description(), base.description()),
                    override.showLockedEntries() == null ? base.showLockedEntries() : override.showLockedEntries(),
                    override.sortOrder() == null ? base.sortOrder() : override.sortOrder()));
        }
    }

    /** Afegeix les entrades generades des dels registres del joc. */
    private static void addAutomaticEntries(Map<DiscoveryKey, DiscoveryEntryDefinition> entries) {
        int order = 100;
        for (GameModeDefinition mode : GameModeRegistry.all()) {
            put(entries, new DiscoveryEntryDefinition(
                    DiscoveryCategory.GAME_MODES,
                    mode.id(),
                    mode.name(),
                    mode.presentation().lockedTitle(),
                    firstText(mode.description(), mode.presentation().shortDescription()),
                    gameModeDetails(mode),
                    "Es descobreix quan s'escull aquest mode abans d'una partida.",
                    firstText(mode.presentation().lockedHints().stream().findFirst().orElse(""),
                            "Tria aquest camí en començar."),
                    firstText(mode.presentation().lockedHints().stream().findFirst().orElse(""),
                            "Encara no saps quin pacte obre aquest camí."),
                    firstText(mode.presentation().shortDescription(), "Aquest camí ja pot ser provat."),
                    List.of("mode", "regles"),
                    false,
                    order++));
        }

        order = 100;
        for (WeaponDefinition weapon : Arsenal.values()) {
            put(entries, new DiscoveryEntryDefinition(
                    DiscoveryCategory.WEAPONS,
                    weapon.getId(),
                    weapon.getName(),
                    "???",
                    weapon.getDescription(),
                    weaponDetails(weapon),
                    "Es descobreix quan s'equipa per primera vegada.",
                    "Una silueta d'arma espera ser entesa.",
                    "Encara no pots triar aquesta arma; busca la pista que n'obre el camí.",
                    "La forma ja és a l'abast, però encara falta provar-la en combat.",
                    List.of("arma", weapon.getType().name().toLowerCase()),
                    true,
                    order++));
        }

        order = 100;
        for (Breed breed : Breed.values()) {
            put(entries, new DiscoveryEntryDefinition(
                    DiscoveryCategory.BREEDS,
                    breed.name(),
                    breed.getName(),
                    "???",
                    breed.getDescription(),
                    lines(breed.getDescription(), "Bonus racial: +" + Math.round(breed.bonus() * 100.0) + "% a "
                            + breed.bonusStat().getName()),
                    "Es descobreix quan un personatge d'aquesta raça entra en joc.",
                    "Crea o troba un personatge d'aquesta raça.",
                    "Crea o troba un personatge d'aquesta raça.",
                    "Crea o troba un personatge d'aquesta raça.",
                    List.of("raça"),
                    true,
                    order++));
        }

        order = 100;
        for (PerkDefinition perk : PerkRegistry.all()) {
            put(entries, new DiscoveryEntryDefinition(
                    DiscoveryCategory.PERKS,
                    perk.id(),
                    perk.name(),
                    "???",
                    perk.description(),
                    perkDetails(perk),
                    "Es descobreix quan el jugador obté aquesta perk.",
                    "Completa missions de perk per poder triar-la.",
                    "Completa missions de perk per poder triar-la.",
                    "Completa missions de perk per poder triar-la.",
                    perk.tags(),
                    true,
                    order++));
        }

        order = 100;
        for (DivinePerkDefinition divine : DivinePerkRegistry.all()) {
            put(entries, new DiscoveryEntryDefinition(
                    DiscoveryCategory.DIVINE_PERKS,
                    divine.id(),
                    divine.name(),
                    "???",
                    divine.shortDescription(),
                    lines(divine.description(), divine.god().isBlank() ? "" : "Déu: " + divine.god(),
                            divine.awakeningDescription().isBlank() ? ""
                                    : "Despertar: " + divine.awakeningDescription()),
                    "Es descobreix quan una perk divina és assignada a un personatge.",
                    "Crea un personatge amb aquesta benedicció divina.",
                    "Crea un personatge amb aquesta benedicció divina.",
                    "Crea un personatge amb aquesta benedicció divina.",
                    List.of("divina"),
                    true,
                    order++));
        }

        order = 100;
        for (MissionDefinition mission : MissionRegistry.all()) {
            put(entries, new DiscoveryEntryDefinition(
                    DiscoveryCategory.MISSIONS,
                    mission.id(),
                    mission.name(),
                    "???",
                    mission.description(),
                    List.of(),
                    "Es descobreix quan aquesta missió és assignada.",
                    "Avança en combat per rebre noves missions.",
                    "Avança en combat per rebre noves missions.",
                    "Avança en combat per rebre noves missions.",
                    List.of("missió"),
                    true,
                    order++));
        }

        order = 100;
        for (Action action : Action.values()) {
            put(entries, new DiscoveryEntryDefinition(
                    DiscoveryCategory.ACTIONS,
                    action.name(),
                    action.label(),
                    "???",
                    "Acció bàsica de combat.",
                    lines("Acció bàsica de combat."),
                    "Es descobreix quan s'utilitza en combat.",
                    "Utilitza aquesta acció durant un torn.",
                    "Utilitza aquesta acció durant un torn.",
                    "Utilitza aquesta acció durant un torn.",
                    List.of("acció", "bàsica"),
                    true,
                    order++));
        }

        order = 100;
        for (SynergyDefinition synergy : SynergyRegistry.all()) {
            put(entries, new DiscoveryEntryDefinition(
                    DiscoveryCategory.SYNERGIES,
                    synergy.id(),
                    synergy.name(),
                    "???",
                    synergy.description(),
                    synergyDetails(synergy),
                    "Es descobreix quan la sinergia s'activa per primera vegada.",
                    "Combina perks compatibles per revelar aquesta sinergia.",
                    "Combina perks compatibles per revelar aquesta sinergia.",
                    "Combina perks compatibles per revelar aquesta sinergia.",
                    List.of("sinergia"),
                    true,
                    order++));
        }
    }

    /** Aplica entrades manuals o modifica les automàtiques. */
    private static void applyManualEntries(Map<DiscoveryKey, DiscoveryEntryDefinition> entries,
            List<DiscoveryEntryConfig> configs) {
        if (configs == null)
            return;
        int manualOrder = 10_000;
        for (DiscoveryEntryConfig config : configs) {
            DiscoveryCategory category = parseCategory(config == null ? null : config.category());
            if (category == null || config.id() == null || config.id().isBlank())
                continue;
            DiscoveryKey key = new DiscoveryKey(category, config.id());
            DiscoveryEntryDefinition base = entries.get(key);
            if (base == null) {
                base = new DiscoveryEntryDefinition(
                        category,
                        config.id(),
                        textOr(config.title(), config.id()),
                        textOr(config.lockedTitle(), "???"),
                        textOr(config.shortDescription(), ""),
                        config.description() == null ? List.of() : config.description(),
                        textOr(config.discoveredWhen(), ""),
                        textOr(config.hint(), ""),
                        textOr(config.unlockHint(), config.hint()),
                        textOr(config.discoveryHint(), config.hint()),
                        config.tags() == null ? List.of() : config.tags(),
                        config.hiddenUntilDiscovered() == null || config.hiddenUntilDiscovered(),
                        config.sortOrder() == null ? manualOrder++ : config.sortOrder());
            } else {
                base = base.withOverride(config);
            }
            entries.put(key, base);
        }
    }

    /** Genera els detalls visibles d'una arma. */
    private static List<String> weaponDetails(WeaponDefinition weapon) {
        List<String> details = new ArrayList<>();
        details.add("Dany base: " + weapon.getBaseDamage());
        details.add("Tipus: " + weapon.getType().getName());
        details.add("Probabilitat crítica: " + formatPercent(weapon.getCriticalProb()));
        details.add("Dany crític: x" + formatNumber(weapon.getCriticalDamage()));
        if (!isEmptyAttackSkill(weapon.getAttackSkill())) {
            details.add("Atac especial: " + readableAttackSkill(weapon.getAttackSkill()));
        }
        if (weapon.getManaPrice() > 0) {
            details.add("Cost de mana: " + formatNumber(weapon.getManaPrice()));
        }
        if (!weapon.getPassiveConfigs().isEmpty()) {
            details.add("Passives: " + weapon.getPassiveConfigs().stream()
                    .map(DiscoveryCatalog::readablePassive)
                    .distinct()
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("cap"));
        }
        return List.copyOf(details);
    }

    /** Genera els detalls visibles d'una perk. */
    private static List<String> perkDetails(PerkDefinition perk) {
        List<String> details = new ArrayList<>();
        if (perk.family() != null) {
            details.add("Família: " + perk.family().label());
        }
        if (perk.trigger() != null) {
            details.add("S'activa: " + readablePhase(perk.trigger().name()));
        }
        // Les etiquetes internes serveixen per calcular sinergies, però són soroll per
        // al visor temporal.
        return List.copyOf(details);
    }

    /** Genera els detalls visibles d'una sinergia. */
    private static List<String> synergyDetails(SynergyDefinition synergy) {
        List<String> details = new ArrayList<>();
        if (synergy.type() != null) {
            details.add("Tipus: " + readableSynergyType(synergy.type().name()));
        }
        if (!synergy.requiredPerks().isEmpty()) {
            details.add("Perks requerides: " + readableTags(synergy.requiredPerks()));
        }
        if (!synergy.requiredTags().isEmpty()) {
            details.add("Condició: combina perks compatibles de " + readableTags(synergy.requiredTags()));
        }
        if (synergy.minMembers() > 0) {
            details.add("Membres mínims: " + synergy.minMembers());
        }
        return List.copyOf(details);
    }

    /** Genera els detalls visibles d'un mode de joc. */
    private static List<String> gameModeDetails(GameModeDefinition mode) {
        List<String> details = new ArrayList<>();
        if (mode.rules().hasActionRestrictions()) {
            details.add("Aquest camí estreny el combat fins als gestos essencials.");
        } else {
            details.add("Aquest camí deixa que el combat recordi totes les seves formes.");
        }
        if (hasModeEffect(mode, UniversalLifeStealTrigger.INTERNAL_EFFECT_KEY)) {
            details.add("La vida ja no torna per costum; només respon a la ferida oberta.");
        }
        if (mode.rules().maxPerks() <= 1) {
            details.add("Les benediccions no fan cor: només una pot arrelar.");
        } else {
            details.add("Les benediccions poden ramificar-se sense un límit estrany.");
        }
        if (!mode.rules().divinePerksEnabled() && mode.rules().chaos().activation().name().equals("DISABLED")) {
            details.add("Els pactes divins i el Caos resten fora d'aquest llindar.");
        } else if (!mode.rules().divinePerksEnabled()) {
            details.add("Els pactes divins resten adormits en aquest camí.");
        } else if (mode.rules().chaos().activation().name().equals("DISABLED")) {
            details.add("El Caos no troba porta per travessar.");
        } else {
            details.add("El Caos queda a l'aguait, no com a promesa sinó com a possibilitat.");
        }
        if (!mode.rules().showUnlockableWeapons()) {
            details.add("Les armes revelades esperen fora de la primera lliçó.");
        } else {
            details.add("Les armes que ja han deixat senyal poden tornar a respondre.");
        }
        return List.copyOf(details);
    }

    private static boolean hasModeEffect(GameModeDefinition mode, String effectId) {
        return mode != null
                && mode.rules() != null
                && effectId != null
                && mode.rules().modeEffects().stream().anyMatch(effect -> effectId.equals(effect.id()));
    }

    /** Desa una entrada pel seu identificador compost. */
    private static void put(Map<DiscoveryKey, DiscoveryEntryDefinition> entries, DiscoveryEntryDefinition entry) {
        entries.put(entry.key(), entry);
    }

    /** Converteix text en una categoria vàlida. */
    private static DiscoveryCategory parseCategory(String id) {
        if (id == null || id.isBlank())
            return null;
        try {
            return DiscoveryCategory.valueOf(id.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    /** Retorna només les línies amb text. */
    private static List<String> lines(String... values) {
        List<String> result = new ArrayList<>();
        if (values == null)
            return List.of();
        for (String value : values) {
            if (value != null && !value.isBlank())
                result.add(value.trim());
        }
        return List.copyOf(result);
    }

    /** Retorna el text o el valor alternatiu. */
    private static String textOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    /** Retorna el primer text no buit. */
    private static String firstText(String primary, String fallback) {
        return primary == null || primary.isBlank() ? fallback : primary;
    }

    /** Indica si l'atac especial està buit. */
    private static boolean isEmptyAttackSkill(String attackSkill) {
        return attackSkill == null || attackSkill.isBlank() || "nothing".equalsIgnoreCase(attackSkill.trim());
    }

    /** Fa llegible el nom d'un atac especial. */
    private static String readableAttackSkill(String key) {
        if (key == null || key.isBlank())
            return "";
        return switch (key.trim()) {
            case "explosiveShot" -> "Tret explosiu";
            case "arcaneDisruption" -> "Disrupció arcana";
            case "luckyBallista" -> "Ballista de la sort";
            case "grimoriCipher" -> "Xifrat del grimori";
            case "perforatingThrow" -> "Llançament perforant";
            case "chronoWeave" -> "Teixit temporal";
            case "crossCut" -> "Tall creuat";
            case "nothing" -> "Cap";
            default -> readableCode(key);
        };
    }

    /** Fa llegible una passiva d'arma. */
    private static String readablePassive(rpgcombat.weapons.config.PassiveConfig passive) {
        if (passive == null || passive.type() == null || passive.type().isBlank())
            return "";
        return switch (passive.type().trim()) {
            case "lifeSteal" -> "Robavida" + passiveParamPercent(passive, "pct");
            case "trueHarm" -> "Dany veritable" + passiveParamPercent(passive, "pct");
            case "executor" -> "Execució" + executorPassiveDetails(passive);
            case "blindOnHit" -> "Ceguesa en impactar" + blindPassiveDetails(passive);
            case "poisonChain" -> "Cadena de verí" + poisonPassiveDetails(passive);
            default -> readableCode(passive.type());
        };
    }

    /** Formata un paràmetre passiu percentual. */
    private static String passiveParamPercent(rpgcombat.weapons.config.PassiveConfig passive, String key) {
        Optional<Double> value = passiveDouble(passive, key);
        return value.map(v -> " (" + formatPercent(v) + ")").orElse("");
    }

    /** Genera els detalls de la passiva d'execució. */
    private static String executorPassiveDetails(rpgcombat.weapons.config.PassiveConfig passive) {
        Optional<Double> thresholdLife = passiveDouble(passive, "thresholdLife");
        Optional<Double> damageBonus = passiveDouble(passive, "damageBonus");
        List<String> parts = new ArrayList<>();
        thresholdLife.ifPresent(value -> parts.add("per sota del " + formatPercent(value) + " de vida"));
        damageBonus.ifPresent(value -> parts.add("+" + formatPercent(value) + " de dany"));
        return parts.isEmpty() ? "" : " (" + String.join(", ", parts) + ")";
    }

    /** Genera els detalls de la passiva de ceguesa. */
    private static String blindPassiveDetails(rpgcombat.weapons.config.PassiveConfig passive) {
        Optional<Double> applyProb = passiveDouble(passive, "applyProb");
        Optional<Integer> duration = passiveInteger(passive, "duration");
        List<String> parts = new ArrayList<>();
        applyProb.ifPresent(value -> parts.add(formatPercent(value) + " de probabilitat"));
        duration.ifPresent(value -> parts.add(value + " torn" + (value == 1 ? "" : "s")));
        return parts.isEmpty() ? "" : " (" + String.join(", ", parts) + ")";
    }

    /** Genera els detalls de la passiva de verí. */
    private static String poisonPassiveDetails(rpgcombat.weapons.config.PassiveConfig passive) {
        Optional<Double> extraDamagePerStack = passiveDouble(passive, "extraDamagePerStack");
        Optional<Integer> softCapStart = passiveInteger(passive, "softCapStart");
        List<String> parts = new ArrayList<>();
        extraDamagePerStack.ifPresent(value -> parts.add("+" + formatNumber(value) + " per càrrega"));
        softCapStart.ifPresent(value -> parts.add("suavitzat des de " + value + " càrregues"));
        return parts.isEmpty() ? "" : " (" + String.join(", ", parts) + ")";
    }

    /** Llegeix un paràmetre decimal d'una passiva. */
    private static Optional<Double> passiveDouble(rpgcombat.weapons.config.PassiveConfig passive, String key) {
        if (passive == null || passive.params() == null || !passive.params().containsKey(key))
            return Optional.empty();
        Object value = passive.params().get(key);
        return value instanceof Number number ? Optional.of(number.doubleValue()) : Optional.empty();
    }

    /** Llegeix un paràmetre enter d'una passiva. */
    private static Optional<Integer> passiveInteger(rpgcombat.weapons.config.PassiveConfig passive, String key) {
        if (passive == null || passive.params() == null || !passive.params().containsKey(key))
            return Optional.empty();
        Object value = passive.params().get(key);
        return value instanceof Number number ? Optional.of(number.intValue()) : Optional.empty();
    }

    /** Fa llegible una fase d'activació. */
    private static String readablePhase(String phase) {
        if (phase == null)
            return "";
        return switch (phase) {
            case "START_TURN" -> "a l'inici del torn";
            case "BEFORE_ATTACK" -> "abans d'atacar";
            case "ROLL_CRIT" -> "en calcular crítics";
            case "MODIFY_DAMAGE" -> "en modificar el dany";
            case "BEFORE_DEFENSE" -> "abans de defensar";
            case "AFTER_DEFENSE" -> "després de defensar";
            case "AFTER_HIT" -> "després d'un impacte";
            case "END_TURN" -> "al final del torn";
            default -> readableCode(phase);
        };
    }

    /** Fa llegible un tipus de sinergia. */
    private static String readableSynergyType(String type) {
        if (type == null)
            return "";
        return switch (type) {
            case "ALTER_MEMBERS" -> "modifica perks compatibles";
            case "BONUS_EXTRA" -> "bonificació addicional";
            default -> readableCode(type);
        };
    }

    /** Formata un decimal com a percentatge. */
    private static String formatPercent(double value) {
        return formatNumber(value * 100.0) + "%";
    }

    /** Fa llegible una llista d'etiquetes. */
    private static String readableTags(List<String> values) {
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(DiscoveryCatalog::readableCode)
                .distinct()
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }

    /** Converteix un codi tècnic en text llegible. */
    private static String readableCode(String value) {
        if (value == null || value.isBlank())
            return "";
        String cleaned = value.trim().replace('_', ' ');
        StringBuilder result = new StringBuilder(cleaned.length());
        boolean newWord = true;
        for (int i = 0; i < cleaned.length(); i++) {
            char c = cleaned.charAt(i);
            if (Character.isWhitespace(c) || c == '-' || c == '.') {
                result.append(c);
                newWord = true;
            } else if (newWord) {
                result.append(Character.toUpperCase(c));
                newWord = false;
            } else {
                result.append(Character.toLowerCase(c));
            }
        }
        return result.toString();
    }

    /** Formata un número sense decimals innecessaris. */
    private static String formatNumber(double value) {
        if (value == Math.rint(value))
            return Long.toString(Math.round(value));
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
