package rpgcombat.discovery.ui.render;

import rpgcombat.utils.ui.Ansi;

/** Constants visuals i de mida del visor de descobriments. */
public final class DiscoveryUiStyle {
    public static final String BOLD = Ansi.BOLD;
    public static final String CYAN = Ansi.CYAN;
    public static final String DARK_GRAY = Ansi.DARK_GRAY;
    public static final String GREEN = Ansi.GREEN;
    public static final String RED = Ansi.RED;
    public static final String RESET = Ansi.RESET;

    public static final int MIN_WIDTH = 72;
    public static final int MIN_HEIGHT = 16;

    public static final int HEADER_LINES = 6;
    public static final int FOOTER_LINES = 3;

    public static final int OUTER_BORDER_WIDTH = 2;
    public static final int HEADER_VISIBLE_SIDE_PADDING = 4;
    public static final int MIN_SPACING = 1;

    public static final int COLUMN_SEPARATOR_WIDTH = 3;
    public static final int COLUMN_SEPARATOR_TOTAL_WIDTH = COLUMN_SEPARATOR_WIDTH * 2;

    public static final int COLLAPSED_CATEGORY_WIDTH = 6;
    public static final int CATEGORY_WIDTH_RATIO = 4;
    public static final int CATEGORY_MIN_WIDTH = 20;
    public static final int CATEGORY_MAX_WIDTH = 30;

    public static final int ENTRY_WIDTH_RATIO = 3;
    public static final int ENTRY_MIN_WIDTH = 24;
    public static final int ENTRY_MAX_WIDTH = 36;
    public static final int COLLAPSED_ENTRY_MIN_WIDTH = 26;
    public static final int COLLAPSED_ENTRY_MAX_WIDTH = 44;

    public static final int DETAIL_MIN_WIDTH = 20;
    public static final int DETAIL_HORIZONTAL_PADDING = 2;
    public static final int DETAIL_CONTENT_MIN_WIDTH = 1;

    public static final int CATEGORY_PROGRESS_WIDTH = 5;
    public static final int HEADER_PROGRESS_SMALL_WIDTH = 8;
    public static final int HEADER_PROGRESS_MEDIUM_WIDTH = 14;
    public static final int HEADER_PROGRESS_LARGE_WIDTH = 20;
    public static final int HEADER_MEDIUM_BREAKPOINT = 90;
    public static final int HEADER_LARGE_BREAKPOINT = 120;

    public static final int MASK_MIN_WORDS = 3;
    public static final int MASK_MAX_WORDS = 8;

    public static final int ANSI_EXTRA_CAPACITY = 32;

    public static final String YELLOW = "\033[33m";
    public static final String BLUE = "\033[34m";
    public static final String MAGENTA = "\033[35m";
    public static final String WHITE = "\033[37m";
    public static final String BG_SELECTED = "\033[48;5;238m";

    public static final String SELECTED = BG_SELECTED + WHITE + BOLD;
    public static final String BORDER = DARK_GRAY;
    public static final String MUTED = DARK_GRAY;
    public static final String LOCKED = DARK_GRAY;

    public static final String COLUMN_SEPARATOR = MUTED + " │ " + RESET;
    public static final String BULLET = "• ";
    public static final String LOCKED_VALUE = "???";
    public static final String EMPTY_INFORMATION = "Sense informació disponible.";
    public static final String TRUNCATED_MARKER = "...";

    public static final String TITLE = "✦ WIKI DE DESCOBRIMENTS";
    public static final String FILTER_PREFIX = "Filtre: ";
    public static final int FILTER_RIGHT_PADDING = 2;
    public static final int FILTER_LEFT_GAP = 2;


    public static final String HELP_FILTER_NEXT = "F filtre";
    public static final String HELP_FILTER_PREVIOUS = "Maj+F filtre anterior";

    public static final String COLUMN_CATEGORIES = "CATEGORIES";
    public static final String COLUMN_CATEGORY_COMPACT = "CAT";
    public static final String COLUMN_DISCOVERIES = "DESCOBRIMENTS";
    public static final String COLUMN_DETAIL = "DETALL";

    public static final String SECTION_DATA = "Dades";
    public static final String SECTION_DISCOVERED = "Descoberta";
    public static final String SECTION_HINT = "Pista";

    public static final String STATUS_DISCOVERED = " DESCOBERTA ";
    public static final String STATUS_LOCKED = " BLOQUEJADA ";

    public static final String TOO_SMALL = "El terminal és massa petit per mostrar els descobriments.";
    public static final String RESIZE_WINDOW = "Augmenta la mida de la finestra.";

    public static final String NO_CATALOG = "No hi ha cap catàleg de descobriments carregat.";
    public static final String OPEN_ERROR = "No s'ha pogut obrir el visor de descobriments: ";

    public static final String NO_CATEGORIES = "  No hi ha categories.";
    public static final String NO_ENTRIES = "  No hi ha entrades.";
    public static final String SELECT_ENTRY = "Selecciona una entrada per veure'n la fitxa.";
    public static final String KEEP_EXPLORING = "Continua explorant per revelar aquesta fitxa.";
    public static final String NO_FILTER_RESULTS = "  Cap entrada amb el filtre: ";

    public static final String FOOTER_CATEGORY = "↑↓/W/S categoria   Enter/D entrar   Q sortir";
    public static final String FOOTER_COLLAPSED = "↑↓/W/S entrada   A/← desplegar categories   Q sortir";
    public static final String FOOTER_ENTRIES = "↑↓/W/S entrada   A/← categories   Enter/D ampliar detall   Q sortir";

    public static final String CATEGORY_WEAPONS = "armes";
    public static final String CATEGORY_RACES = "races";
    public static final String CATEGORY_RACE = "raça";
    public static final String CATEGORY_PERKS = "perks";
    public static final String CATEGORY_DIVINE_PERKS = "perks divines";
    public static final String CATEGORY_MISSIONS = "missions";
    public static final String CATEGORY_ACTIONS = "accions";
    public static final String CATEGORY_EFFECTS = "efectes";
    public static final String CATEGORY_SYNERGIES = "sinergies";

    public static final String KEY_UP_1 = "w";
    public static final String KEY_UP_2 = "W";
    public static final String KEY_UP_3 = "k";
    public static final String KEY_UP_4 = "K";
    public static final String KEY_DOWN_1 = "s";
    public static final String KEY_DOWN_2 = "S";
    public static final String KEY_DOWN_3 = "j";
    public static final String KEY_DOWN_4 = "J";
    public static final String KEY_LEFT_1 = "a";
    public static final String KEY_LEFT_2 = "A";
    public static final String KEY_LEFT_3 = "h";
    public static final String KEY_LEFT_4 = "H";
    public static final String KEY_RIGHT_1 = "d";
    public static final String KEY_RIGHT_2 = "D";
    public static final String KEY_RIGHT_3 = "l";
    public static final String KEY_RIGHT_4 = "L";
    public static final String KEY_ENTER_CR = "\r";
    public static final String KEY_ENTER_LF = "\n";
    public static final String KEY_EXIT_1 = "q";
    public static final String KEY_EXIT_2 = "Q";
    public static final String KEY_ESCAPE = "\033";
    public static final String KEY_FILTER_NEXT = "f";
    public static final String KEY_FILTER_PREVIOUS = "F";

    private DiscoveryUiStyle() {
    }
}
