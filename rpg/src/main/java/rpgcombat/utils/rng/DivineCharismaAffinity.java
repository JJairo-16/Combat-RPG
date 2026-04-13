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
 * Aquesta classe només s'encarrega de generar i consultar la preferència.
 * No aplica cap efecte directe sobre probabilitats ni tirades.
 */
public final class DivineCharismaAffinity {

    private DivineCharismaAffinity() {
    }

    /**
     * Classificació resumida del carisma davant dels déus.
     */
    public enum Standing {
        DISLIKED("Els déus et miren amb recel; no esperis que els seus designis et siguin favorables."),
        NEUTRAL("Els déus no et rebutgen ni et beneeixen; avui, el seu judici serà distant."),
        FAVORED("Els déus et miren amb certa benevolència; potser avui els seus signes t'acompanyaran.");

        private final String msg;

        private Standing(String message) {
            this.msg = message;
        }

        @Override
        public String toString() {
            return msg;
        }
    }

    /**
     * Tram detallat dins del perfil de preferència.
     */
    public enum Band {
        DISLIKED_LOW("Els déus aparten la mirada del teu esperit; avui no sembla que vulguin escoltar-te."),
        NEUTRAL_LOW("Els déus et toleren amb fredor; no esperis massa de la seva mà."),
        FAVORED("Els déus et reconeixen amb certa simpatia; potser et concediran un senyal favorable."),
        NEUTRAL_HIGH("Els déus et contemplen sense hostilitat, però encara sense devoció."),
        DISLIKED_HIGH("Els déus veuen en tu una presència que no els complau; vigila amb allò que demanes.");

        private final String msg;

        private Band(String message) {
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
            if (favoredCenter < 1 || favoredCenter > 20) {
                throw new IllegalArgumentException("favoredCenter ha d'estar entre 1 i 20");
            }
            if (favoredRadius < 0) {
                throw new IllegalArgumentException("favoredRadius ha de ser >= 0");
            }
            if (neutralRadius < favoredRadius) {
                throw new IllegalArgumentException("neutralRadius ha de ser >= favoredRadius");
            }
        }
    }

    private static Profile currentProfile;

    /**
     * Genera una nova preferència divina per a la partida actual.
     *
     * <p>
     * El centre preferit es genera dins del rang 1..20.
     * Els radis actuals es deixen fixos per mantenir un comportament estable:
     *
     * <ul>
     * <li>favoredRadius = 2</li>
     * <li>neutralRadius = 5</li>
     * </ul>
     */
    public static void rollForRun(Random rng) {
        Objects.requireNonNull(rng, "El RNG no pot ser nul.");

        int favoredCenter = rng.nextInt(20) + 1;
        currentProfile = new Profile(favoredCenter, 2, 5);
    }

    /**
     * Estableix manualment el perfil actual.
     *
     * <p>
     * Útil per tests, debug o partides deterministes.
     */
    public static void setProfile(Profile profile) {
        currentProfile = Objects.requireNonNull(profile, "El perfil no pot ser nul.");
    }

    /**
     * Elimina el perfil actual.
     *
     * <p>
     * Després d'això, qualsevol consulta requerirà tornar a inicialitzar-lo.
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

    /**
     * Indica si ja existeix una preferència divina carregada.
     */
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

        int center = profile.favoredCenter();
        int favoredRadius = profile.favoredRadius();
        int neutralRadius = profile.neutralRadius();

        int delta = charisma - center;
        int abs = Math.abs(delta);

        if (abs <= favoredRadius) {
            return Band.FAVORED;
        }

        if (abs <= neutralRadius) {
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
     */
    public static int distanceFromFavorite(int charisma) {
        return distanceFromFavorite(charisma, currentProfile());
    }

    /**
     * Retorna una "distància social divina" respecte al centre favorit.
     */
    public static int distanceFromFavorite(int charisma, Profile profile) {
        Objects.requireNonNull(profile, "El perfil no pot ser nul.");
        return Math.abs(charisma - profile.favoredCenter());
    }
}