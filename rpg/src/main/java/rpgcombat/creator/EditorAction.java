package rpgcombat.creator;

/** Accions que pot executar el formulari. */
enum EditorAction {
    EDIT_NAME,
    EDIT_AGE,
    EDIT_BREED,
    EDIT_STRENGTH,
    EDIT_DEXTERITY,
    EDIT_CONSTITUTION,
    EDIT_INTELLIGENCE,
    EDIT_WISDOM,
    EDIT_CHARISMA,
    EDIT_LUCK,
    RANDOMIZE,
    CONFIRM;

    /** Indica si és una estadística editable. */
    boolean isStat() {
        return ordinal() >= EDIT_STRENGTH.ordinal() && ordinal() <= EDIT_LUCK.ordinal();
    }

    /** Retorna l'índex d'estadística o -1. */
    int statIndex() {
        return isStat() ? ordinal() - EDIT_STRENGTH.ordinal() : -1;
    }
}
