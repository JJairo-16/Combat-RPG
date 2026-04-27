package rpgcombat.perks.effect;

import rpgcombat.models.effects.EffectResult;

/** Acció reutilitzable d'una perk. */
@FunctionalInterface
interface PerkAction {
    EffectResult apply(PerkContext ctx);
}
