package rpgcombat.creator.editor;

import rpgcombat.creator.CharacterCreator;

/** Constants de disposició del formulari. */
final class EditorLayout {
    static final int SCREEN_START_ROW = 1;
    static final int SCREEN_START_COL = 1;
    static final int TITLE_ROW = 1;
    static final int LEFT_COL = 4;
    static final int RIGHT_COL = 66;
    static final int CONTENT_COL = 7;
    static final int LEFT_BOX_WIDTH = 56;
    static final int RIGHT_BOX_WIDTH = 68;
    static final int IDENTITY_ROW = 3;
    static final int STATS_ROW = 10;
    static final int ACTIONS_ROW = 22;
    static final int HELP_ROW = 33;
    static final int MESSAGE_ROW = 36;
    static final int LABEL_WIDTH = 16;
    static final int STAT_BAR_WIDTH = 18;
    static final int NAME_INPUT_WIDTH = CharacterCreator.MAX_NAME_LEN + 1;
    static final int AGE_INPUT_WIDTH = 11;
    static final int BOX_HORIZONTAL_PADDING = 6;
    static final int BOX_MIN_BREED_BOTTOM = 15;
    static final int BREED_HINT_GAP = 2;
    static final int AGE_MAX_DIGITS = 10;
    static final int ESC_READ_TIMEOUT_MS = 30;
    static final int FULL_LINE_WIDTH = 140;

    private static final int NAME_ROW = IDENTITY_ROW + 1;
    private static final int AGE_ROW = IDENTITY_ROW + 2;
    private static final int BREED_ROW = IDENTITY_ROW + 3;
    private static final int STRENGTH_ROW = STATS_ROW + 1;
    static final int BUILD_SCORE_ROW = ACTIONS_ROW + 1;

    private static final int RANDOMIZE_ROW = ACTIONS_ROW + 2;
    private static final int CONFIRM_ROW = ACTIONS_ROW + 3;

    private EditorLayout() {
    }

    /** Retorna la fila d'una acció. */
    static int rowFor(EditorAction action) {
        return switch (action) {
            case EDIT_NAME -> NAME_ROW;
            case EDIT_AGE -> AGE_ROW;
            case EDIT_BREED -> BREED_ROW;
            case EDIT_STRENGTH, EDIT_DEXTERITY, EDIT_CONSTITUTION, EDIT_INTELLIGENCE,
                    EDIT_WISDOM, EDIT_CHARISMA, EDIT_LUCK -> STRENGTH_ROW + action.statIndex();
            case RANDOMIZE -> RANDOMIZE_ROW;
            case CONFIRM -> CONFIRM_ROW;
        };
    }
}
