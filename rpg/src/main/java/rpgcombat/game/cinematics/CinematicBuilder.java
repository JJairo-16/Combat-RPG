package rpgcombat.game.cinematics;

import java.util.Random;

import rpgcombat.combat.models.Winner;
import rpgcombat.config.ui.CinematicsOptions;
import rpgcombat.models.characters.Character;
import rpgcombat.models.effects.triggers.Chaos;
import rpgcombat.utils.cinematic.cinematic.TextCinematic;
import rpgcombat.utils.cinematic.scene.BlockBuilder;
import rpgcombat.utils.cinematic.scene.Scene;
import rpgcombat.utils.cinematic.scene.SceneBuilder;
import rpgcombat.utils.cinematic.scene.TextBlock;
import rpgcombat.utils.cinematic.typing.TypingMood;

/**
 * Crea i reprodueix les cinemàtiques principals del combat.
 */
public final class CinematicBuilder {
    private static final int ARROW_ANIMATION_DELAY = 140;
    private static final Random RNG = new Random();
    private static boolean chaosEnabled = false;

    private CinematicBuilder() {
    }

    /**
     * Reprodueix la cinemàtica prèvia a la creació de personatges.
     */
    public static void playPreCreation() {
        buildPreCreationCinematic().play();
    }

    /**
     * Reprodueix la cinemàtica inicial i aplica Caos si cal.
     */
    public static void playInit(CinematicsOptions options, Character player1, Character player2) {
        if (!options.postCreation()) {
            applyChaos(options, player1, player2);
            return;
        }

        TextCinematic cinematic = applyChaos(options, player1, player2)
                ? buildChaosMindInit()
                : buildRandomInit();

        cinematic.play();
    }

    /**
     * Reprodueix la cinemàtica contra l'estancament.
     */
    public static void playAntiStall() {
        buildAntiStallCinematic().play();
    }

    /**
     * Reprodueix la cinemàtica final segons el resultat i si hi ha Caos.
     *
     * @param winner resultat del combat
     */
    public static void playEnd(Winner winner) {
        int key = getKey(winner == Winner.TIE, chaosEnabled);

        TextCinematic cinematic = switch (key) {
            case 3 -> buildTieEndChaos();
            case 2 -> buildTieEnd();
            case 1 -> buildWinEndChaos();
            case 0 -> buildWinEnd();
            default -> throw new IllegalStateException("Unexpected key: " + key);
        };

        cinematic.play();
    }


    /**
     * Reprodueix la cinemàtica de crèdits.
     */
    public static void playCredits() {
        buildCreditsCinematic().play();
    }

    /**
     * Tria una cinemàtica inicial aleatòria.
     */
    private static TextCinematic buildRandomInit() {
        return switch (RNG.nextInt(3)) {
            case 0 -> buildStrategyInit();
            case 1 -> buildLuckInit();
            default -> buildChaosInit();
        };
    }

    /**
     * Aplica l'efecte Caos segons la probabilitat configurada.
     *
     * @return {@code true} si s'ha aplicat
     */
    private static boolean applyChaos(CinematicsOptions options, Character player1, Character player2) {
        if (RNG.nextDouble() >= options.chaos()) {
            return false;
        }

        player1.addEffect(new Chaos());
        player2.addEffect(new Chaos());
        chaosEnabled = true;
        return true;
    }

    /**
     * Calcula una clau de 2 bits a partir de dos flags.
     * El primer flag és el bit més significatiu.
     *
     * @param flag1 primer flag
     * @param flag2 segon flag
     * @return valor entre 0 i 3 que representa la combinació
     */
    private static int getKey(boolean flag1, boolean flag2) {
        return (flag1 ? 1 : 0) << 1 | (flag2 ? 1 : 0);
    }

    /**
     * Construeix la cinemàtica prèvia a la creació.
     */
    private static TextCinematic buildPreCreationCinematic() {
        TextBlock wakeUp = BlockBuilder.text("""
                <gray>La teva consciència emergeix lentament d’un abisme sense nom.</gray>
                <gray>Tot és fosc.</gray>
                <gray>Jeus sobre la pedra freda, vençut per un silenci antic.</gray>

                <gray>Ho intentes... però cap múscul respon. Ni un dit.</gray>
                <gray>No és dolor. No és cansament.</gray>

                <gray>És una ordre imposada sobre la teva carn.</gray>
                <gray>Una voluntat superior et manté immòbil, i el teu cos obeeix</gray>
                <gray>com si ja no et pertanyés.</gray>
                """)
                .mood(TypingMood.DRAMATIC)
                .build();

        TextBlock divineVoices = BlockBuilder
                .text("""
                        <gray>Al fons de la foscor, unes veus s’alcen com ecos dins d’un temple oblidat.</gray>

                        <bright_blue>Aurelion:</bright_blue> <blue>La consciència persisteix. Capacitat cognitiva intacta. Això simplificarà l'observació.</blue>

                        <bright_yellow>Lysara:</bright_yellow> <yellow>Oh... meravellós. Així podrà entendre-ho tot. Fa que el joc sigui molt més deliciós.</yellow>

                        <bright_red>Varkhul:</bright_red> <red>Que entengui, sí… és millor així. Quan entens… el trencament arriba més profund.</red>
                                    <red>M’agrada veure com aguanten. Alguns duren més. Altres… no tant.</red>

                        <gray>Parlen de tu com els déus parlen dels mortals:</gray>
                        <gray>no com d’un ésser viu, sinó com d’una peça sobre el tauler.</gray>
                        """)
                .mood(TypingMood.SLOW)
                .build();

        TextBlock combatDecree = BlockBuilder
                .text("""
                        <bright_blue>Aurelion:</bright_blue> <blue>Dos subjectes mortals. Condicions controlades. Resultat analitzable.</blue>

                        <bright_yellow>Lysara:</bright_yellow> <yellow>Que decideixi la fortuna... o potser un petit caprici meu. Veurem qui em diverteix més.</yellow>

                        <bright_red>Varkhul:</bright_red> <red>Trenqueu-vos… però lentament. Vull veure en quin moment deixeu de ser… vosaltres.</red>
                                    <red>Cridareu. O potser no. A vegades el silenci és millor… es trenca més net.</red>
                                    <red>No importa qui guanyi. Sempre queda alguna cosa… malmesa.</red>

                        <gray>El teu cor colpeja el pit com un tambor de guerra.</gray>
                        <gray>No pots moure’t.</gray>
                        <gray>No pots fugir.</gray>
                        <gray>I, tanmateix, saps que aviat et faran lluitar.</gray>
                        """)
                .mood(TypingMood.DRAMATIC)
                .build();

        return cinema(scene(wakeUp), scene(divineVoices), scene(combatDecree));
    }

    /**
     * Construeix la introducció d'Aurelion.
     */
    private static TextCinematic buildStrategyInit() {
        TextBlock combatStart = BlockBuilder
                .text("""
                        <bright_blue>Aurelion:</bright_blue> <blue>Les condicions són adequades. L’entorn és… interessant.</blue>

                        <blue>Cada moviment obre camins. Cada error… els tanca.</blue>

                        <blue>Ja he traçat més opcions de les que podríeu recórrer en tota una vida.</blue>

                        <blue>Però no busco certesa. Busco decisió.</blue>

                        <blue>Jo sóc Aurelion, mestre de l’estratègia. Mostreu-me si sabeu escollir.</blue>

                        <blue>Anticipeu. Adapteu-vos. Supereu.</blue>

                        <blue>Comenceu.</blue>
                        """)
                .mood(TypingMood.DRAMATIC)
                .build();

        return cinema(scene(combatStart));
    }

    /**
     * Construeix la introducció de Lysara.
     */
    private static TextCinematic buildLuckInit() {
        TextBlock combatStart = BlockBuilder.text("""
                <bright_yellow>Lysara:</bright_yellow> <yellow>Ah… així que ja hi som.</yellow>

                <yellow>Tot sembla tan clar al principi. Però només cal un instant… i tot canvia.</yellow>

                <yellow>M’agrada aquest moment. Quan encara creieu que ho controleu.</yellow>

                <yellow>Un petit gir… una casualitat… i ja no és tan senzill, oi?</yellow>

                <yellow>Jo sóc Lysara, la Dama del Velo Daurat. I m’encanta veure com es desfan les certeses.</yellow>

                <yellow>No cal que sigueu perfectes. Només… interessants.</yellow>

                <yellow>Som-hi. Deixeu que la sort decideixi… o potser jo.</yellow>
                """)
                .mood(TypingMood.DRAMATIC)
                .build();

        return cinema(scene(combatStart));
    }

    /**
     * Construeix la introducció de Varkhul.
     */
    private static TextCinematic buildChaosInit() {
        TextBlock combatStart = BlockBuilder.text("""
                <bright_red>Varkhul:</bright_red> <red>Ah… sí. Comença. O ja havia començat? Hm.</red>

                <red>Sempre igual… al principi. Drets. Sencers. Tranquils.</red>

                <red>Després… no. Mai no es mantenen igual.</red>

                <red>M’agrada aquest punt… quan alguna cosa falla. Petit. Quasi invisible… però creix.</red>

                <red>Un pensament fora de lloc. Un moviment tardà. I llavors… heh… tot segueix.</red>

                <red>Jo sóc Varkhul, el Cor Deslligat. No trenqueu de cop… no, no. Millor a poc a poc.</red>

                <red>Lluiteu. Aguanteu. O no.</red>

                <red>Al final… sempre hi ha alguna cosa que cedeix. Sempre.</red>
                """)
                .mood(TypingMood.DRAMATIC)
                .build();

        return cinema(scene(combatStart));
    }

    /**
     * Construeix la introducció especial quan Caos s'aplica a l'inici.
     */
    private static TextCinematic buildChaosMindInit() {
        TextBlock combatStart = BlockBuilder.text("""
                <bright_red>Varkhul:</bright_red> <red>No… avui no.</red>

                <red>No miraré. No esperaré. O… sí, però des de dins. Millor.</red>

                <red>Aquesta vegada… ho faré jo. Sí. Jo.</red>

                <red>La ment… mmm… es torça ràpid. Massa ràpid si saps on tocar.</red>

                <red>Aquí. No—més a prop. Heh… ho trobo sempre.</red>

                <red>Ho notareu. Primer petit. Un pensament estrany… que no marxa.</red>

                <red>Després dos. Després… ja no sabreu quin era el vostre.</red>

                <red>Intentareu mantenir-vos sencers. No ho feu. No funciona així.</red>

                <red>Jo sóc Varkhul, el Cor Deslligat. I avui… us trenco jo. A poc a poc… o de cop. Ja veurem.</red>

                <red>Lluiteu. Penseu. O intenteu-ho.</red>

                <red>Al final… no fallareu vosaltres.</red>

                <red>Fallarà… tot el que hi ha dins.</red>
                """)
                .mood(TypingMood.DRAMATIC)
                .build();

        return cinema(scene(combatStart));
    }

    /**
     * Construeix la cinemàtica contra l'estancament.
     */
    private static TextCinematic buildAntiStallCinematic() {
        TextBlock antistall = BlockBuilder.text("""
                <bright_red>Varkhul:</bright_red> <red>Mm… massa lent.</red>

                <red>Penseu, dubteu… m’avorreix.</red>

                <red>No. Prou.</red>

                <red>Ho accelero. Ara.</red>

                <red>Pensaments que xoquen… decisions que cauen.</red>

                <red>No tindreu temps.</red>

                <red>Jo sóc Varkhul, el Cor Deslligat. Trenqueu-vos més ràpid.</red>
                """)
                .mood(TypingMood.DRAMATIC)
                .build();

        return cinema(scene(antistall));
    }

    private static TextCinematic buildTieEnd() {
        TextBlock end = BlockBuilder.text("""
                <gray>El combat s’atura.</gray>
                <gray>No hi ha vencedor. No hi ha caiguda final.</gray>

                <bright_blue>Aurelion:</bright_blue> <blue>Cap resolució dominant.</blue>
                            <blue>Un equilibri sostingut fins al límit.</blue>
                            <blue>…prenc nota.</blue>

                <bright_yellow>Lysara:</bright_yellow> <yellow>Oh… quin final tan capritxós.</yellow>
                            <yellow>Ningú guanya, ningú perd… m’encanta.</yellow>
                            <yellow>Ja veurem qui té més sort… la propera vegada.</yellow>

                <bright_red>Varkhul:</bright_red> <red>Mm… encara no.</red>
                            <red>Però ja cedeix. Ho noto.</red>
                            <red>Heh… tard o d’hora.</red>

                <gray>Per a tu, el combat ha acabat.</gray>
                <gray>Per a ells… només era un més.</gray>
                <gray>Altres lluitaran. Altres cauran.</gray>
                <gray>I les seves veus… no callaran mai.</gray>
                """)
                .mood(TypingMood.DRAMATIC)
                .build();

        return cinema(scene(end));
    }

    private static TextCinematic buildWinEnd() {
        TextBlock end = BlockBuilder.text("""
                <gray>El combat arriba al seu final.</gray>
                <gray>Un s’imposa. L’altre cedeix.</gray>

                <bright_blue>Aurelion:</bright_blue> <blue>Resolució clara.</blue>
                            <blue>La seqüència dominant s’ha imposat com era previsible.</blue>
                            <blue>Un resultat… útil.</blue>

                <bright_yellow>Lysara:</bright_yellow> <yellow>Ah… ho has vist?</yellow>
                            <yellow>Un sol instant… i tot s’ha inclinat.</yellow>
                            <yellow>Quina sort més deliciosa.</yellow>

                <bright_red>Varkhul:</bright_red> <red>Sí… així.</red>
                            <red>Alguna cosa cedeix. Sempre ho fa.</red>
                            <red>Heh… encara es pot trencar més.</red>

                <gray>Per a tu, el combat ha acabat.</gray>
                <gray>Per a ells… només era un altre joc.</gray>
                <gray>Altres lluitaran. Altres cauran.</gray>
                <gray>I ells… continuaran mirant.</gray>
                """)
                .mood(TypingMood.DRAMATIC)
                .build();

        return cinema(scene(end));
    }

    private static TextCinematic buildTieEndChaos() {
        TextBlock end = BlockBuilder.text("""
                <gray>El combat es desfà més que acabar-se.</gray>
                <gray>No hi ha vencedor. Només restes del que havia de ser una lluita.</gray>

                <bright_blue>Aurelion:</bright_blue> <blue>…inestable.</blue>
                            <blue>Les decisions han perdut coherència en el tram final.</blue>
                            <blue>Això… distorsiona el resultat.</blue>

                <bright_yellow>Lysara:</bright_yellow> <yellow>Oh… això no era un simple caprici.</yellow>
                            <yellow>Tot s’ha torçat alhora. Tan imprevisible… tan viu.</yellow>
                            <yellow>M’ha encantat.</yellow>

                <bright_red>Varkhul:</bright_red> <red>Heh… sí… així.</red>
                            <red>Ho heu sentit, oi? Quan ja no encaixava res.</red>
                            <red>Quan no sabíeu si éreu vosaltres… o no.</red>

                            <red>No s’ha trencat del tot.</red>
                            <red>Però gairebé. Gairebé…</red>

                <gray>Per a tu, el combat ha acabat.</gray>
                <gray>Per a ells… només ha estat una prova més.</gray>
                <gray>Altres ments esperen. Altres es trencaran millor.</gray>
                <gray>I aquesta vegada… només era el principi.</gray>
                """)
                .mood(TypingMood.DRAMATIC)
                .build();

        return cinema(scene(end));
    }

    private static TextCinematic buildWinEndChaos() {
        TextBlock end = BlockBuilder.text("""
                <gray>El combat acaba… però no de forma neta.</gray>
                <gray>Un s’imposa. L’altre cau.</gray>
                <gray>I, tot i així, res sembla del tot correcte.</gray>

                <bright_blue>Aurelion:</bright_blue> <blue>Resolució obtinguda.</blue>
                            <blue>Però el camí fins al resultat ha estat… contaminat.</blue>
                            <blue>Estratègia interrompuda. Decisions deformades.</blue>

                <bright_yellow>Lysara:</bright_yellow> <yellow>Oh… quin final tan estrany.</yellow>
                            <yellow>Hi ha hagut victòria, sí… però no només per mèrit.</yellow>
                            <yellow>Alguna cosa ha mogut els fils massa fort.</yellow>

                <bright_red>Varkhul:</bright_red> <red>Heh… sí.</red>
                            <red>Un cau. L’altre queda dret.</red>
                            <red>Però tots dos han sentit l’esquerda.</red>

                            <red>Ho he fet jo. Una mica. O prou.</red>
                            <red>No importa qui ha guanyat… alguna cosa dins seu ja no torna igual.</red>

                <gray>Per a tu, el combat ha acabat.</gray>
                <gray>Per a ells… només ha estat una altra diversió.</gray>
                <gray>Altres lluitaran. Altres cauran.</gray>
                <gray>I Varkhul… tornarà a buscar la fissura.</gray>
                """)
                .mood(TypingMood.DRAMATIC)
                .build();

        return cinema(scene(end));
    }


    /**
     * Construeix els crèdits del joc.
     */
    private static TextCinematic buildCreditsCinematic() {
        TextBlock mainCredits = BlockBuilder.text("""
                <bright_white>CRÈDITS</bright_white>

                <gray>Disseny de personatges:</gray> <white>Jairo Linares</white>
                <gray>Sistema d'estadístiques:</gray> <white>Jairo Linares</white>
                <gray>Sistema d'efectes d'estat:</gray> <white>Jairo Linares</white>
                <gray>Disseny de races i atributs:</gray> <white>Jairo Linares</white>
                <gray>Interfície d'usuari:</gray> <white>Jairo Linares</white>
                <gray>Sistema de menús:</gray> <white>Jairo Linares</white>
                <gray>Editor de personatges:</gray> <white>Jairo Linares</white>
                <gray>Motor de joc:</gray> <white>Jairo Linares</white>
                <gray>Sistema d'accions i modificadors:</gray> <white>Jairo Linares</white>
                <gray>Sistema de missatges i text:</gray> <white>Jairo Linares</white>
                <gray>Arquitectura general del projecte:</gray> <white>Jairo Linares</white>
                <gray>Integració de sistemes:</gray> <white>Jairo Linares</white>
                """)
                .mood(TypingMood.FAST)
                .build();

        TextBlock impossibleCredits = BlockBuilder.text("""
                <bright_white>CRÈDITS ADICIONALS</bright_white>

                <gray>Disseny de realitats alternatives:</gray> <white>Jairo Linares</white>
                <gray>Gestió de línies temporals inestables:</gray> <white>Jairo Linares</white>
                <gray>Equilibri de decisions moralment qüestionables:</gray> <white>Jairo Linares</white>
                <gray>Invocació controlada del caos:</gray> <white>Jairo Linares</white>
                <gray>Simulació d'existència dubtosa:</gray> <white>Jairo Linares</white>
                <gray>Optimització de pensaments absurds:</gray> <white>Jairo Linares</white>
                <gray>Interpretació de comportaments inexplicables:</gray> <white>Jairo Linares</white>
                <gray>Enginyeria de situacions que semblaven bona idea:</gray> <white>Jairo Linares</white>
                <gray>Implementació de lògica discutible:</gray> <white>Jairo Linares</white>
                """)
                .mood(TypingMood.NORMAL)
                .build();

        TextBlock blameCredits = BlockBuilder.text("""
                <bright_white>INCIDÈNCIES TÈCNIQUES</bright_white>

                <gray>Generació de bugs crítics:</gray> <white>ChatGPT</white>
                <gray>Desincronització de sistemes:</gray> <white>GitHub Copilot</white>
                <gray>Errors de càlcul completament evitables:</gray> <white>Gemini</white>
                <gray>Comportaments imprevisibles del joc:</gray> <white>Claude</white>
                <gray>Decisions de disseny qüestionables:</gray> <white>Microsoft Copilot</white>
                <gray>Errors que només passen una vegada:</gray> <white>Perplexity</white>
                <gray>Codi que funciona per motius desconeguts:</gray> <white>Character.AI</white>
                <gray>Codi que deixa de funcionar sense motiu:</gray> <white>Replit Ghostwriter</white>
                <gray>Problemes que apareixen just abans d'entregar:</gray> <white>Amazon CodeWhisperer</white>
                <gray>Causant del 99.7% dels bugs:</gray> <white>ChatGPT</white>
                """)
                .mood(TypingMood.NORMAL)
                .build();

        TextBlock endCredits = BlockBuilder.text("""
                <bright_white>CONTROL FINAL</bright_white>

                <gray>Control de qualitat:</gray> <white>No aplicable</white>
                <gray>Testing:</gray> <white>Ocasional</white>
                <gray>Suport tècnic:</gray> <white>Reinicia i prova un altre cop</white>

                <bright_white>Gràcies per jugar.</bright_white>
                """)
                .mood(TypingMood.SLOW)
                .build();

        return cinema(
                scene(mainCredits),
                scene(impossibleCredits),
                scene(blameCredits),
                scene(endCredits));
    }

    /**
     * Envolta un bloc de text en una escena.
     */
    private static Scene scene(TextBlock textBlock) {
        return SceneBuilder.create()
                .clearBefore(true)
                .block(textBlock)
                .build();
    }

    /**
     * Crea una cinemàtica d'una sola escena.
     */
    private static TextCinematic cinema(Scene scene) {
        return TextCinematic.builder()
                .clearScreenOnEnd(true)
                .arrowAnimationDelay(ARROW_ANIMATION_DELAY)
                .scene(scene)
                .build();
    }

    /**
     * Crea una cinemàtica amb diverses escenes.
     */
    private static TextCinematic cinema(Scene... scenes) {
        return TextCinematic.builder()
                .clearScreenOnEnd(true)
                .arrowAnimationDelay(ARROW_ANIMATION_DELAY)
                .scenes(scenes)
                .build();
    }
}