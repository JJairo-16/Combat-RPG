package rpgcombat.creator.editor;

/** Camps navegables del formulari. */
enum FormField {
    NAME(EditorAction.EDIT_NAME),
    AGE(EditorAction.EDIT_AGE),
    BREED(EditorAction.EDIT_BREED),
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

    private final EditorAction action;

    /** Assigna l'acció del camp. */
    FormField(EditorAction action) {
        this.action = action;
    }

    /** Retorna l'acció associada. */
    EditorAction action() {
        return action;
    }
}
