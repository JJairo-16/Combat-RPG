package rpgcombat.perks.effect;

/** Condició reutilitzable d'una perk. */
@FunctionalInterface
interface PerkCondition {
    boolean matches(PerkContext ctx);
}
