package rpgcombat.perks.effect;

import java.util.Random;

import rpgcombat.models.characters.Character;
import rpgcombat.weapons.passives.HitContext;
import rpgcombat.weapons.passives.HitContext.Phase;

/** Context d'execució d'una perk. */
record PerkContext(HitContext hit, Phase phase, Random rng, Character owner) {}
