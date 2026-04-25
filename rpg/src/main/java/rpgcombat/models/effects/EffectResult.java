package rpgcombat.models.effects;

import rpgcombat.combat.ui.messages.CombatMessage;
import rpgcombat.combat.ui.messages.MessageColor;
import rpgcombat.combat.ui.messages.MessageSymbol;

/**
 * Resultat d'una execució d'efecte.
 */
public record EffectResult(
        CombatMessage message,
        boolean consumedCharge,
        boolean changedState
) {
    public static EffectResult none() {
        return new EffectResult(null, false, false);
    }

    public static EffectResult msg(CombatMessage message) {
        return new EffectResult(message, false, false);
    }

    public static EffectResult positive(String text) {
        return msg(CombatMessage.positive(text));
    }

    public static EffectResult negative(String text) {
        return msg(CombatMessage.negative(text));
    }

    public static EffectResult warning(String text) {
        return msg(CombatMessage.warning(text));
    }

    public static EffectResult chaos(String text) {
        return msg(CombatMessage.chaos(text));
    }

    public static EffectResult hit(String text) {
        return msg(CombatMessage.hit(text));
    }

    public static EffectResult styled(MessageColor color, MessageSymbol symbol, String text) {
        return msg(CombatMessage.of(symbol, color, text));
    }

    public static EffectResult consumed(CombatMessage message) {
        return new EffectResult(message, true, true);
    }

    public static EffectResult changed(CombatMessage message) {
        return new EffectResult(message, false, true);
    }
}