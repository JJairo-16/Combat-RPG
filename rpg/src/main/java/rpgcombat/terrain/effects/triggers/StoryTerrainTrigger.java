package rpgcombat.terrain.effects.triggers;

import rpgcombat.combat.models.Action;
import rpgcombat.combat.ui.messages.MessageColor;
import rpgcombat.combat.ui.messages.MessageSymbol;
import rpgcombat.models.characters.Character;
import rpgcombat.models.effects.EffectResult;
import rpgcombat.models.effects.triggers.Trigger;
import rpgcombat.weapons.passives.HitContext;

/**
 * Base petita per triggers de terreny que reaccionen a les accions triades.
 */
abstract class StoryTerrainTrigger extends Trigger {
    private static final Action[] COMMON_ACTIONS = {
            Action.ATTACK,
            Action.DEFEND,
            Action.DODGE
    };

    /**
     * Crea una base de trigger amb la clau interna indicada.
     *
     * @param key clau estable de l'efecte
     */
    StoryTerrainTrigger(String key) {
        super(key);
    }

    /**
     * Indica si el propietari és l'actor que està resolent el torn.
     *
     * @param ctx context del torn
     * @param owner combatent propietari del trigger
     * @return {@code true} quan el propietari és l'atacant del context
     */
    protected boolean isActor(HitContext ctx, Character owner) {
        return ctx != null && owner != null && owner == ctx.attacker();
    }

    /**
     * Indica si els dos combatents han triat la mateixa acció.
     *
     * @param ctx context del torn
     * @param action acció que tots dos han d'haver triat
     * @return {@code true} quan les dues accions coincideixen
     */
    protected boolean bothChose(HitContext ctx, Action action) {
        return ctx != null
                && action != null
                && ctx.attackerAction() == action
                && ctx.defenderAction() == action;
    }

    /**
     * Indica si els combatents han triat accions diferents.
     *
     * @param ctx context del torn
     * @return {@code true} quan hi ha dues accions no iguals
     */
    protected boolean actionsDiffer(HitContext ctx) {
        return ctx != null
                && ctx.attackerAction() != null
                && ctx.defenderAction() != null
                && ctx.attackerAction() != ctx.defenderAction();
    }

    /**
     * Tria una acció estable a partir d'una ronda i una llavor del terreny.
     *
     * @param round ronda consultada
     * @param salt desplaçament propi del terreny
     * @return acció visible per aquesta ronda
     */
    protected Action roundAction(int round, int salt) {
        return COMMON_ACTIONS[Math.floorMod(round + salt, COMMON_ACTIONS.length)];
    }

    /**
     * Construeix un missatge de terreny amb estil de mode de joc.
     *
     * @param symbol símbol visual
     * @param text text del missatge
     * @return resultat visible del trigger
     */
    protected EffectResult terrainResult(MessageSymbol symbol, String text) {
        return EffectResult.gamemode(MessageColor.CYAN, symbol, text);
    }
}
