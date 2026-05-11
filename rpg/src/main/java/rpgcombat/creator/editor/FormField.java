package rpgcombat.creator.editor;

/** Camps navegables del formulari. */
enum FormField {
    NAME(EditorAction.EDIT_NAME),
    AGE(EditorAction.EDIT_AGE),
    BREED(EditorAction.EDIT_BREED),
    DIVINE_PERK(EditorAction.EDIT_DIVINE_PERK),
    STRENGTH(EditorAction.EDIT_STRENGTH),
    DEXTERITY(EditorAction.EDIT_DEXTERITY),
    CONSTITUTION(EditorAction.EDIT_CONSTITUTION),
    INTELLIGENCE(EditorAction.EDIT_INTELLIGENCE),
    WISDOM(EditorAction.EDIT_WISDOM),
    CHARISMA(EditorAction.EDIT_CHARISMA),
    LUCK(EditorAction.EDIT_LUCK),
    RANDOMIZE(EditorAction.RANDOMIZE),
    CONFIRM(EditorAction.CONFIRM);

    static final FormField[] VALUES = values();
    private static final FormField[] WITHOUT_DIVINE_PERK = {
            NAME,
            AGE,
            BREED,
            STRENGTH,
            DEXTERITY,
            CONSTITUTION,
            INTELLIGENCE,
            WISDOM,
            CHARISMA,
            LUCK,
            RANDOMIZE,
            CONFIRM
    };

    private final EditorAction action;

    /** Assigna l'acció del camp. */
    FormField(EditorAction action) {
        this.action = action;
    }

    /** Retorna l'acció associada. */
    EditorAction action() {
        return action;
    }

    /** Retorna els camps visibles segons les opcions de creació. */
    static FormField[] valuesFor(boolean divinePerksEnabled) {
        return divinePerksEnabled ? VALUES : WITHOUT_DIVINE_PERK;
    }
}
