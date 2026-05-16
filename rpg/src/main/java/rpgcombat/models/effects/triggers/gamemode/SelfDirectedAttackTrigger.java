package rpgcombat.models.effects.triggers.gamemode;

import java.util.Random;

import rpgcombat.models.characters.Character;
import rpgcombat.models.effects.EffectResult;
import rpgcombat.models.effects.triggers.Trigger;
import rpgcombat.weapons.passives.HitContext;

/**
 * Trigger de mode que redirigeix qualsevol atac del portador contra si mateix.
 */
public final class SelfDirectedAttackTrigger extends Trigger {
    public static final String INTERNAL_EFFECT_KEY = "SELF_DIRECTED_ATTACK";
    public static final double DEFAULT_DAMAGE_MULTIPLIER = 1.0;
    public static final boolean DEFAULT_CAN_KILL = true;

    private final double damageMultiplier;
    private final boolean canKill;

    public SelfDirectedAttackTrigger(double damageMultiplier, boolean canKill) {
        super(INTERNAL_EFFECT_KEY);
        this.damageMultiplier = Math.max(0.0, damageMultiplier);
        this.canKill = canKill;
    }

    @Override
    public EffectResult modifyDamage(HitContext ctx, Random rng, Character owner) {
        if (ctx == null || owner == null || ctx.attacker() != owner || ctx.attackerAction() == null) {
            return EffectResult.none();
        }

        ctx.putMeta(Chaos.META_SELF_HIT, true);
        ctx.putMeta(Chaos.META_SELF_HIT_MULTIPLIER, damageMultiplier);
        ctx.putMeta(Chaos.META_SELF_HIT_CAN_KILL, canKill);
        ctx.putMeta("selfDirectedAttack", true);
        ctx.putMeta("selfDirectedAttackMultiplier", damageMultiplier);
        ctx.putMeta("selfDirectedAttackCanKill", canKill);
        return EffectResult.gamemode(rpgcombat.combat.ui.messages.MessageColor.DEFAULT, rpgcombat.combat.ui.messages.MessageSymbol.WARNING, owner.getName() + " redirigeix l'atac contra si mateix.");
    }
}
