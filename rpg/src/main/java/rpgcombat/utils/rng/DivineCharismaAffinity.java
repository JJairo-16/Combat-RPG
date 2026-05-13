package rpgcombat.utils.rng;

import java.util.Objects;
import java.util.Random;

/**
 * Gestiona les preferències ocultes dels déus respecte al carisma.
 *
 * <p>
 * A cada partida es genera un perfil ocult que determina quin rang de
 * carisma agrada als déus. A partir d'aquest perfil, qualsevol personatge
 * pot ser classificat com a desfavorit, neutral o afavorit.
 *
 * <p>
 * La forma base és:
 *
 * <pre>
 * cau malament - normal - cau bé - normal - cau malament
 * </pre>
 *
 * <p>
 * Prop dels extrems del rang real, els trams neutrals o desfavorits poden
 * quedar parcialment retallats. Això és intencionat: permet que el punt de
 * gràcia divina pugui aparèixer prop de carismes baixos o alts sense quedar
 * excessivament amortit pel radi neutral.
 */
public final class DivineCharismaAffinity {

    private DivineCharismaAffinity() {
    }

    /**
     * Rang real esperat del carisma.
     *
     * <p>
     * Amb 7 stats i 140 punts totals, el generador base treballa sobre una
     * mitjana de 20 punts per stat i estableix:
     * </p>
     *
     * <ul>
     * <li>mínim aproximat: 8</li>
     * <li>màxim aproximat: 20</li>
     * </ul>
     *
     * <p>
     * Aquest rang és el que es fa servir per generar i validar la preferència
     * divina, evitant perfils desalineats amb el sistema real d'estadístiques.
     * </p>
     */
    private static final int MIN_CHARISMA = 8;
    private static final int MAX_CHARISMA = 20;

    private static final int DEFAULT_FAVORED_RADIUS = 1;
    private static final int DEFAULT_NEUTRAL_RADIUS = 3;

    /** Classificació resumida del carisma davant dels déus. */
    public enum Standing {
        DISLIKED("Els déus et miren amb recel; no esperis que els seus designis et siguin favorables."),
        NEUTRAL("Els déus no et rebutgen ni et beneeixen; avui, el seu judici serà distant."),
        FAVORED("Els déus et miren amb certa benevolència; potser avui els seus signes t'acompanyaran.");

        private final String msg;

        Standing(String message) {
            this.msg = message;
        }

        @Override
        public String toString() {
            return msg;
        }
    }

    /** Tram detallat dins del perfil de preferència. */
    public enum Band {
        DISLIKED_LOW("Els déus aparten la mirada del teu esperit; avui no sembla que vulguin escoltar-te."),
        NEUTRAL_LOW("Els déus et toleren amb fredor; no esperis massa de la seva mà."),
        FAVORED("Els déus et reconeixen amb certa simpatia; potser et concediran un senyal favorable."),
        NEUTRAL_HIGH("Els déus et contemplen sense hostilitat, però encara sense devoció."),
        DISLIKED_HIGH("Els déus veuen en tu una presència que no els complau; vigila amb allò que demanes.");

        private final String msg;

        Band(String message) {
            this.msg = message;
        }

        @Override
        public String toString() {
            return msg;
        }
    }

    /**
     * Perfil ocult de preferència divina per a una partida.
     *
     * @param favoredCenter centre del rang preferit
     * @param favoredRadius radi del rang "cau bé"
     * @param neutralRadius radi total del rang neutral al voltant del centre
     */
    public record Profile(int favoredCenter, int favoredRadius, int neutralRadius) {

        public Profile {
            if (favoredCenter < MIN_CHARISMA || favoredCenter > MAX_CHARISMA) {
                throw new IllegalArgumentException(
                        "favoredCenter ha d'estar entre " + MIN_CHARISMA + " i " + MAX_CHARISMA);
            }

            if (favoredRadius < 0) {
                throw new IllegalArgumentException("favoredRadius ha de ser >= 0");
            }

            if (neutralRadius < favoredRadius) {
                throw new IllegalArgumentException("neutralRadius ha de ser >= favoredRadius");
            }

            if (favoredCenter - favoredRadius < MIN_CHARISMA
                    || favoredCenter + favoredRadius > MAX_CHARISMA) {
                throw new IllegalArgumentException(
                        "El tram afavorit ha de cabre dins del rang real de carisma.");
            }
        }
    }

    private static Profile currentProfile;

    /**
     * Genera una nova preferència divina per a la partida actual.
     *
     * <p>
     * El centre preferit només deixa marge perquè el tram afavorit complet
     * càpiga dins del rang real. El radi neutral pot sobresortir dels extrems
     * conceptualment; en consultar carisma, el valor es limita al rang real.
     * </p>
     *
     * <pre>
     * Amb carisma 10..22 i favoredRadius = 2:
     * centre possible = 12..20
     * </pre>
     */
    public static void rollForRun(Random rng) {
        Objects.requireNonNull(rng, "El RNG no pot ser nul.");

        int favoredRadius = DEFAULT_FAVORED_RADIUS;
        int neutralRadius = DEFAULT_NEUTRAL_RADIUS;

        int minCenter = MIN_CHARISMA + favoredRadius;
        int maxCenter = MAX_CHARISMA - favoredRadius;

        int favoredCenter = minCenter + rng.nextInt(maxCenter - minCenter + 1);

        currentProfile = new Profile(favoredCenter, favoredRadius, neutralRadius);
    }

    /**
     * Estableix manualment el perfil actual.
     *
     * <p>
     * Útil per tests, debug o partides deterministes.
     * </p>
     */
    public static void setProfile(Profile profile) {
        currentProfile = Objects.requireNonNull(profile, "El perfil no pot ser nul.");
    }

    /**
     * Elimina el perfil actual.
     *
     * <p>
     * Després d'això, qualsevol consulta requerirà tornar a inicialitzar-lo.
     * </p>
     */
    public static void clear() {
        currentProfile = null;
    }

    /**
     * Retorna el perfil actual.
     *
     * @throws IllegalStateException si encara no s'ha inicialitzat la preferència
     */
    public static Profile currentProfile() {
        Profile profile = currentProfile;
        if (profile == null) {
            throw new IllegalStateException("La preferència divina encara no ha estat inicialitzada.");
        }
        return profile;
    }

    /** Indica si ja existeix una preferència divina carregada. */
    public static boolean isInitialized() {
        return currentProfile != null;
    }

    /**
     * Classifica un valor de carisma dins del tram detallat del perfil actual.
     *
     * @param charisma valor de carisma del personatge
     * @return el tram detallat corresponent
     */
    public static Band classifyBand(int charisma) {
        return classifyBand(charisma, currentProfile());
    }

    /**
     * Classifica un valor de carisma dins del tram detallat d'un perfil concret.
     *
     * @param charisma valor de carisma del personatge
     * @param profile  perfil de preferència divina
     * @return el tram detallat corresponent
     */
    public static Band classifyBand(int charisma, Profile profile) {
        Objects.requireNonNull(profile, "El perfil no pot ser nul.");

        int safeCharisma = clampCharisma(charisma);

        int delta = safeCharisma - profile.favoredCenter();
        int abs = Math.abs(delta);

        if (abs <= profile.favoredRadius()) {
            return Band.FAVORED;
        }

        if (abs <= profile.neutralRadius()) {
            return delta < 0 ? Band.NEUTRAL_LOW : Band.NEUTRAL_HIGH;
        }

        return delta < 0 ? Band.DISLIKED_LOW : Band.DISLIKED_HIGH;
    }

    /**
     * Classifica un valor de carisma dins la categoria resumida del perfil actual.
     *
     * @param charisma valor de carisma del personatge
     * @return classificació resumida
     */
    public static Standing classifyStanding(int charisma) {
        return classifyStanding(charisma, currentProfile());
    }

    /**
     * Classifica un valor de carisma dins la categoria resumida d'un perfil
     * concret.
     *
     * @param charisma valor de carisma del personatge
     * @param profile  perfil de preferència divina
     * @return classificació resumida
     */
    public static Standing classifyStanding(int charisma, Profile profile) {
        Band band = classifyBand(charisma, profile);

        return switch (band) {
            case FAVORED -> Standing.FAVORED;
            case NEUTRAL_LOW, NEUTRAL_HIGH -> Standing.NEUTRAL;
            case DISLIKED_LOW, DISLIKED_HIGH -> Standing.DISLIKED;
        };
    }

    /**
     * Retorna una "distància social divina" respecte al centre favorit.
     *
     * <p>
     * Pot ser útil si en el futur vols graduar la intensitat d'un efecte.
     * </p>
     */
    public static int distanceFromFavorite(int charisma) {
        return distanceFromFavorite(charisma, currentProfile());
    }

    /** Retorna una "distància social divina" respecte al centre favorit. */
    public static int distanceFromFavorite(int charisma, Profile profile) {
        Objects.requireNonNull(profile, "El perfil no pot ser nul.");
        return Math.abs(clampCharisma(charisma) - profile.favoredCenter());
    }

    /** Retorna el mínim de carisma esperat pel sistema. */
    public static int minCharisma() {
        return MIN_CHARISMA;
    }

    /** Retorna el màxim de carisma esperat pel sistema. */
    public static int maxCharisma() {
        return MAX_CHARISMA;
    }

    private static int clampCharisma(int charisma) {
        return Math.clamp(charisma, MIN_CHARISMA, MAX_CHARISMA);
    }
}