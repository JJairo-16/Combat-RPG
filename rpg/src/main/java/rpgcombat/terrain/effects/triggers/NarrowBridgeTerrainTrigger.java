package rpgcombat.terrain.effects.triggers;

import java.util.Random;

import rpgcombat.combat.models.Action;
import rpgcombat.combat.ui.messages.MessageSymbol;
import rpgcombat.models.characters.Character;
import rpgcombat.models.effects.EffectResult;
import rpgcombat.weapons.passives.HitContext;

/**
 * Converteix els xocs d'atac contra atac en creuaments sense contenció.
 */
public final class NarrowBridgeTerrainTrigger extends StoryTerrainTrigger {
    public static final String INTERNAL_EFFECT_KEY = "TERRAIN_NARROW_BRIDGE";

    /** Crea el trigger del pont estret. */
    public NarrowBridgeTerrainTrigger() {
        super(INTERNAL_EFFECT_KEY);
    }

    /**
     * Força un crític quan els dos combatents s'han llançat a atacar.
     */
    @Override
    public EffectResult rollCrit(HitContext ctx, Random rng, Character owner) {
        if (!isActor(ctx, owner) || !bothChose(ctx, Action.ATTACK)) {
            return EffectResult.none();
        }

        ctx.forceCritical();
        return terrainResult(MessageSymbol.WARNING,
                "El Pont Estret obliga " + owner.getName() + " a creuar sense reservar el cop.");
    }
}
