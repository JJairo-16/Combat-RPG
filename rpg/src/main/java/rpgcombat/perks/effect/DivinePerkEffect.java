package rpgcombat.perks.effect;

import java.util.Random;

import rpgcombat.combat.models.Action;
import rpgcombat.combat.ui.messages.CombatMessage;
import rpgcombat.combat.ui.messages.MessageSymbol;
import rpgcombat.models.characters.Character;
import rpgcombat.models.effects.Effect;
import rpgcombat.models.effects.EffectResult;
import rpgcombat.models.effects.EffectState;
import rpgcombat.models.effects.StackingRule;
import rpgcombat.perks.PerkDefinition;
import rpgcombat.perks.divine.DivineAwakeningView;
import rpgcombat.utils.ui.Ansi;
import rpgcombat.weapons.passives.HitContext;
import rpgcombat.weapons.passives.HitContext.Event;
import rpgcombat.weapons.passives.HitContext.Phase;

/**
 * Efecte permanent per a perks divines inicials, amb despertar progressiu dins del combat.
 */
final class DivinePerkEffect implements Effect, DivineAwakeningView {
    private static final double MIN_POWER = 0.40;

    private final PerkDefinition perk;
    private final EffectState state = new EffectState(0, 0, Integer.MAX_VALUE, 0);
    private int awakening;
    private boolean onceUsed;
    private boolean lokiInitialized;

    /**
     * Crea l'efecte per a una perk divina concreta.
     */
    DivinePerkEffect(PerkDefinition perk) {
        this.perk = perk;
    }

    /**
     * Retorna la clau única de l'efecte.
     */
    @Override
    public String key() {
        return ConfigurablePerkEffect.keyFor(perk);
    }

    /**
     * Evita apilar diverses còpies del mateix efecte.
     */
    @Override
    public StackingRule stackingRule() {
        return StackingRule.IGNORE;
    }

    /**
     * Retorna l'estat intern de l'efecte.
     */
    @Override
    public EffectState state() {
        return state;
    }

    /**
     * Aquest efecte no expira mai.
     */
    @Override
    public boolean isExpired() {
        return false;
    }

    /**
     * Retorna l'identificador de la perk divina.
     */
    @Override
    public String divinePerkId() {
        return perk.id();
    }

    /**
     * Retorna el nom mostrable amb l'estat de despertar.
     */
    @Override
    public String awakenedDisplayName() {
        int max = maxAwakeningFor(perk.id());
        String color = godColor();

        if (max <= 0) {
            return "✧ " + color + perk.name() + Ansi.RESET;
        }

        if (awakening >= max) {
            return "✦ " + color + perk.name() + Ansi.RESET;
        }

        return "✧ " + color + perk.name() + Ansi.RESET
                + Ansi.DARK_GRAY + " · despertar " + awakening + "/" + max + Ansi.RESET;
    }

    /**
     * Retorna la descripció actualitzada segons el despertar.
     */
    @Override
    public String awakenedDescription() {
        int max = maxAwakeningFor(perk.id());
        String description = compactEffectDescription(max);
        if (max > 0 && awakening < max) {
            String awakenBy = awakeningInstruction();
            if (!awakenBy.isBlank()) {
                description += "\nDespertar: " + awakenBy;
            }
        }
        return description;
    }

    /**
     * Aplica la lògica divina segons la fase del combat.
     */
    @Override
    public EffectResult onPhase(HitContext ctx, Phase phase, Random rng, Character owner) {
        return switch (perk.id()) {
            case "CERNUNNOS_WILD_PULSE" -> cernunnos(ctx, phase, owner);
            case "ARES_BLOOD_OATH" -> ares(ctx, phase, owner);
            case "MORRIGAN_RAVEN_OMEN" -> morrigan(ctx, phase, owner);
            case "ARTEMIS_MOON_HUNTER" -> artemis(ctx, phase, owner);
            case "BRIGID_OATH_FLAME" -> brigid(ctx, phase, owner);
            case "HEPHAESTUS_INNER_FORGE" -> hephaestus(ctx, phase, owner);
            case "THOR_HELD_THUNDER" -> thor(ctx, phase, owner);
            case "HESTIA_UNBROKEN_HEARTH" -> hestia(ctx, phase, owner);
            case "THOTH_FATE_SCRIPT" -> thoth(ctx, phase, owner);
            case "ATHENA_DIVINE_STRATEGIST" -> athena(ctx, phase, owner);
            case "HECATE_CURSED_THRESHOLD" -> hecate(ctx, phase, owner);
            case "LOKI_BROKEN_RULE" -> loki(ctx, phase, owner);
            case "JANUS_BETWEEN_DOORS" -> janus(ctx, phase, owner);
            case "HERMES_STOLEN_STEP" -> hermes(ctx, phase, owner);
            default -> EffectResult.none();
        };
    }

    /**
     * Gestiona el pols salvatge de Cernunnos.
     */
    private EffectResult cernunnos(HitContext ctx, Phase phase, Character owner) {
        if (phase == Phase.START_TURN && ctx.attacker() == owner) {
            state.addStacks(1, Integer.MAX_VALUE);
            String stance = state.stacks() % 2 == 0 ? "forma feral" : "forma guardiana";
            return msg("adopta la " + stance + ".");
        }
        boolean even = state.stacks() % 2 == 0;
        double p = power(4);
        if (phase == Phase.MODIFY_DAMAGE && ctx.attacker() == owner) {
            double bonus = even ? 0.18 * p : -0.12 * p;
            ctx.multiplyDamage(1.0 + bonus);
            return msg(even
                    ? "+" + pct(bonus) + " de dany, però queda exposat."
                    : "conté la fúria: " + pct(bonus) + " de dany.");
        }
        if (phase == Phase.BEFORE_DEFENSE && ctx.defender() == owner) {
            double bonus = even ? 0.15 * p : -0.18 * p;
            ctx.multiplyDamage(1.0 + bonus);
            return msg(even
                    ? "rep +" + pct(bonus) + " de dany per la fúria."
                    : "redueix el dany rebut un " + pct(-bonus) + ".");
        }
        return EffectResult.none();
    }

    /**
     * Gestiona el jurament de sang d'Ares.
     */
    private EffectResult ares(HitContext ctx, Phase phase, Character owner) {
        if (phase == Phase.MODIFY_DAMAGE && ctx.attacker() == owner && state.stacks() >= 2) {
            double bonus = 0.15 + 0.10 * Math.min(awakening, 2);
            ctx.multiplyDamage(1.0 + bonus);
            state.setStacks(0);
            return msg("el tercer atac consecutiu esclata amb +" + pct(bonus) + " de dany.");
        }
        if (phase == Phase.END_TURN && ctx.attacker() == owner) {
            if (ctx.attackerAction() == Action.ATTACK) {
                state.addStacks(1, 2);
            } else {
                state.setStacks(0);
            }
        }
        return EffectResult.none();
    }

    /**
     * Gestiona el presagi del corb de Morrigan.
     */
    private EffectResult morrigan(HitContext ctx, Phase phase, Character owner) {
        if (!onceUsed && phase == Phase.START_TURN && owner.healthRatio() <= 0.35) {
            onceUsed = true;
            setAwakening(ctx, 2, 2);
            state.setStacks(1);
            owner.addEffect(new rpgcombat.models.effects.impl.Fatigue(1));
            return msg("els corbs anuncien sang: el proper atac serà crític. El presagi desperta del tot.");
        }
        if (phase == Phase.ROLL_CRIT && ctx.attacker() == owner && state.stacks() > 0) {
            ctx.forceCritical();
            state.setStacks(0);
            return msg("el presagi força un crític.");
        }
        return EffectResult.none();
    }

    /**
     * Gestiona la caça lunar d'Artemisa.
     */
    private EffectResult artemis(HitContext ctx, Phase phase, Character owner) {
        if (phase == Phase.AFTER_DEFENSE && ctx.defender() == owner && ctx.hasEvent(Event.ON_DODGE)
                && ctx.damageDealt() <= 0) {
            state.setStacks(1);
            boolean awakened = awaken(ctx, 2);
            return msg("l'esquiva perfecta prepara una caça lunar" + awakeningText(awakened, 2) + ".");
        }
        if (phase == Phase.ROLL_CRIT && ctx.attacker() == owner && state.stacks() > 0) {
            double bonus = switch (Math.min(awakening, 2)) {
                case 0 -> 0.10;
                case 1 -> 0.18;
                default -> 0.25;
            };
            ctx.setCriticalChance(ctx.criticalChance() + bonus);
            return msg("+" + pct(bonus) + " de probabilitat crítica.");
        }
        if (phase == Phase.AFTER_HIT && ctx.attacker() == owner && state.stacks() > 0 && ctx.damageDealt() > 0) {
            if (awakening >= 2) {
                ctx.defender().applyBleed(1);
            }
            state.setStacks(0);
            return msg(awakening >= 2
                    ? "la presa sagna durant 1 torn."
                    : "la caça lunar encara no pot fer sagnar la presa.");
        }
        return EffectResult.none();
    }

    /**
     * Gestiona la flama del jurament de Brigid.
     */
    private EffectResult brigid(HitContext ctx, Phase phase, Character owner) {
        if (phase == Phase.END_TURN && ctx.attacker() == owner && ctx.attackerAction() != Action.ATTACK) {
            state.setStacks(1);
            return msg("una acció voluntària no ofensiva encén la propera ofensiva.");
        }
        if (phase == Phase.ROLL_CRIT && ctx.attacker() == owner && state.stacks() > 0) {
            ctx.setCriticalChance(ctx.criticalChance() + (0.04 + 0.02 * Math.min(awakening, 2)));
        }
        if (phase == Phase.MODIFY_DAMAGE && ctx.attacker() == owner && state.stacks() > 0) {
            double bonus = 0.08 + 0.04 * Math.min(awakening, 2);
            ctx.multiplyDamage(1.0 + bonus);
            state.setStacks(0);
            return msg("consumeix Inspiració: +" + pct(bonus) + " de dany i crític millorat.");
        }
        return EffectResult.none();
    }

    /**
     * Gestiona el tremp interior d'Hefest.
     */
    private EffectResult hephaestus(HitContext ctx, Phase phase, Character owner) {
        if (phase == Phase.AFTER_DEFENSE && ctx.defender() == owner && ctx.defenderAction() == Action.DEFEND) {
            state.addStacks(1, 3);
            boolean awakened = awaken(ctx, 3);
            return msg("acumula tremp de forja (" + state.stacks() + "/3)"
                    + awakeningText(awakened, 3) + ".");
        }
        if (phase == Phase.MODIFY_DAMAGE && ctx.attacker() == owner && state.stacks() >= 3) {
            double bonus = switch (Math.min(awakening, 3)) {
                case 0 -> 0.12;
                case 1 -> 0.18;
                case 2 -> 0.23;
                default -> 0.28;
            };
            ctx.multiplyDamage(1.0 + bonus);
            state.setStacks(0);
            return msg("el tremp acumulat perfora la defensa: +" + pct(bonus) + " de dany.");
        }
        return EffectResult.none();
    }

    /**
     * Gestiona el tro retingut de Thor.
     */
    private EffectResult thor(HitContext ctx, Phase phase, Character owner) {
        if (phase == Phase.END_TURN && ctx.attacker() == owner && ctx.attackerAction() == Action.CHARGE) {
            double reduction = switch (Math.min(awakening, 2)) {
                case 0 -> 0.94;
                case 1 -> 0.88;
                default -> 0.82;
            };
            owner.multiplyNextIncomingDamage(reduction);
            state.setStacks(1);
            return msg("carrega el tro i endureix el cos contra el proper cop.");
        }
        if (phase == Phase.AFTER_HIT && ctx.attacker() == owner && state.stacks() > 0 && ctx.damageDealt() > 0) {
            if (awakening >= 2) {
                ctx.defender().applyStagger(1);
            }
            state.setStacks(0);
            return msg(awakening >= 2
                    ? "el cop carregat desequilibra el rival."
                    : "el tro encara no és prou fort per desequilibrar.");
        }
        return EffectResult.none();
    }

    /**
     * Gestiona la llar intacta d'Hestia.
     */
    private EffectResult hestia(HitContext ctx, Phase phase, Character owner) {
        if (phase == Phase.END_TURN && ctx.attacker() == owner && ctx.attackerAction() != Action.ATTACK) {
            double reduction = switch (Math.min(awakening, 2)) {
                case 0 -> 0.92;
                case 1 -> 0.88;
                default -> 0.84;
            };
            owner.multiplyNextIncomingDamage(reduction);
            return msg("el refugi interior redueix el proper dany rebut.");
        }
        return EffectResult.none();
    }

    /**
     * Gestiona la lectura del destí de Thoth.
     */
    private EffectResult thoth(HitContext ctx, Phase phase, Character owner) {
        if (phase == Phase.MODIFY_DAMAGE && ctx.attacker() == owner && repeatedOpponentAction(ctx, owner)) {
            double bonus = switch (Math.min(awakening, 2)) {
                case 0 -> 0.08;
                case 1 -> 0.13;
                default -> 0.18;
            };
            ctx.multiplyDamage(1.0 + bonus);
            return msg("ha llegit la repetició enemiga: +" + pct(bonus) + " de dany.");
        }
        if (phase == Phase.BEFORE_DEFENSE && ctx.defender() == owner && repeatedOpponentAction(ctx, owner)) {
            double reduction = switch (Math.min(awakening, 2)) {
                case 0 -> 0.94;
                case 1 -> 0.90;
                default -> 0.86;
            };
            ctx.multiplyDamage(reduction);
            return msg("ha llegit la repetició enemiga: redueix el dany rebut.");
        }
        if (phase == Phase.END_TURN && ctx.attacker() == owner) {
            state.setStacks(ctx.defenderAction().ordinal() + 1);
        }
        return EffectResult.none();
    }

    /**
     * Gestiona l'estratègia divina d'Atena.
     */
    private EffectResult athena(HitContext ctx, Phase phase, Character owner) {
        if (phase == Phase.END_TURN && ctx.attacker() == owner) {
            int actionCode = ctx.attackerAction().ordinal() + 1;
            if (state.remainingTurns() == actionCode) {
                state.setStacks(0);
            } else {
                boolean awakened = awaken(ctx, 2);
                state.addStacks(1, 1 + Math.min(awakening, 2));
                if (awakened) {
                    return msg("reajusta la seva tàctica" + awakeningText(true, 2) + ".");
                }
            }
            state.setDuration(actionCode);
        }
        if (phase == Phase.MODIFY_DAMAGE && ctx.attacker() == owner && state.stacks() > 0) {
            double perStack = switch (Math.min(awakening, 2)) {
                case 0 -> 0.04;
                case 1 -> 0.05;
                default -> 0.06;
            };
            ctx.multiplyDamage(1.0 + perStack * state.stacks());
            return msg("la variació tàctica aporta +" + pct(perStack * state.stacks()) + " de dany.");
        }
        return EffectResult.none();
    }

    /**
     * Gestiona el llindar maleït d'Hècate.
     */
    private EffectResult hecate(HitContext ctx, Phase phase, Character owner) {
        if (phase == Phase.AFTER_DEFENSE && ctx.defender() == owner
                && ctx.damageDealt() >= owner.getStatistics().getMaxHealth() * 0.15) {
            state.setStacks(1);
            state.setDuration((int) Math.round(ctx.damageDealt()));
            return msg("marca el llindar: el proper cop retornarà part del dolor.");
        }
        if (phase == Phase.AFTER_HIT && ctx.attacker() == owner && state.stacks() > 0 && ctx.damageDealt() > 0) {
            double ratio = switch (Math.min(awakening, 2)) {
                case 0 -> 0.15;
                case 1 -> 0.25;
                default -> 0.35;
            };
            double echo = Math.max(1, state.remainingTurns() * ratio);
            ctx.defender().getDamage(echo);
            state.setStacks(0);
            state.setDuration(0);
            return msg("retorna " + round2(echo) + " de dany maleït.");
        }
        return EffectResult.none();
    }

    /**
     * Gestiona la regla trencada de Loki.
     */
    private EffectResult loki(HitContext ctx, Phase phase, Character owner) {
        if (phase == Phase.START_TURN && ctx.attacker() == owner) {
            if (!lokiInitialized) {
                lokiInitialized = true;
                state.setCooldown(lokiCooldown());
            } else {
                state.tickCooldown();
            }
            awaken(ctx, 2);
        }
        if (phase == Phase.MODIFY_DAMAGE && ctx.attacker() == owner
                && state.cooldownTurns() <= 0 && ctx.damageToResolve() <= 0) {
            ctx.addFlatDamage(4);
            state.setCooldown(lokiCooldown());
            return msg("trenca la regla: converteix el fallo en un cop feble. Proper favor en "
                    + lokiCooldown() + " torns.");
        }
        return EffectResult.none();
    }

    /**
     * Gestiona les portes de Janus.
     */
    private EffectResult janus(HitContext ctx, Phase phase, Character owner) {
        if (phase == Phase.END_TURN && ctx.attacker() == owner) {
            int current = ctx.attackerAction().ordinal() + 1;
            int previous = state.remainingTurns();
            if (previous > 0 && isOffensiveCode(previous) != isOffensiveCode(current)) {
                boolean awakened = awaken(ctx, 2);
                state.setStacks(isOffensiveCode(current) ? 1 : 2);
                state.setDuration(current);
                return msg("obre una porta entre dues decisions" + awakeningText(awakened, 2) + ".");
            }
            state.setDuration(current);
        }
        if (phase == Phase.MODIFY_DAMAGE && ctx.attacker() == owner && state.stacks() == 1) {
            double bonus = 0.06 + 0.04 * Math.min(awakening, 2);
            ctx.multiplyDamage(1.0 + bonus);
            state.setStacks(0);
            return msg("la porta oberta impulsa el cop: +" + pct(bonus) + " de dany.");
        }
        if (phase == Phase.BEFORE_DEFENSE && ctx.defender() == owner && state.stacks() == 2) {
            double reduction = 0.94 - 0.04 * Math.min(awakening, 2);
            ctx.multiplyDamage(reduction);
            state.setStacks(0);
            return msg("la porta oberta amorteix el cop rebut.");
        }
        return EffectResult.none();
    }

    /**
     * Gestiona el pas robat d'Hermes.
     */
    private EffectResult hermes(HitContext ctx, Phase phase, Character owner) {
        if (phase == Phase.AFTER_DEFENSE && ctx.defender() == owner && ctx.defenderAction() == Action.DODGE
                && ctx.damageDealt() <= 0) {
            state.setStacks(1);
            boolean awakened = awaken(ctx, 2);
            return msg("roba un pas al rival" + awakeningText(awakened, 2) + ".");
        }
        if (phase == Phase.END_TURN && ctx.attacker() == owner && ctx.attackerAction() != Action.ATTACK) {
            state.setStacks(1);
            boolean awakened = awaken(ctx, 2);
            return msg("guarda impuls per actuar amb avantatge" + awakeningText(awakened, 2) + ".");
        }
        if (phase == Phase.MODIFY_DAMAGE && ctx.attacker() == owner && state.stacks() > 0) {
            double bonus = 0.06 + 0.03 * Math.min(awakening, 2);
            ctx.multiplyDamage(1.0 + bonus);
            state.setStacks(0);
            return msg("el pas robat dona +" + pct(bonus) + " de dany al següent cop.");
        }
        return EffectResult.none();
    }

    /**
     * Retorna el despertar màxim d'una perk.
     */
    private int maxAwakeningFor(String id) {
        return switch (id) {
            case "CERNUNNOS_WILD_PULSE" -> 4;
            case "HEPHAESTUS_INNER_FORGE" -> 3;
            case "ARES_BLOOD_OATH", "ARTEMIS_MOON_HUNTER", "BRIGID_OATH_FLAME",
                    "THOR_HELD_THUNDER", "HESTIA_UNBROKEN_HEARTH", "THOTH_FATE_SCRIPT",
                    "ATHENA_DIVINE_STRATEGIST", "HECATE_CURSED_THRESHOLD", "LOKI_BROKEN_RULE",
                    "JANUS_BETWEEN_DOORS", "HERMES_STOLEN_STEP", "MORRIGAN_RAVEN_OMEN" ->
                2;
            default -> 0;
        };
    }

    /**
     * Construeix una descripció compacta de l'efecte actual.
     */
    private String compactEffectDescription(int max) {
        int level = Math.min(awakening, max);
        boolean full = max <= 0 || awakening >= max;
        return switch (perk.id()) {
            case "CERNUNNOS_WILD_PULSE" -> "Els torns parells donen "
                    + value(0.18 * power(4), 0.18, full)
                    + " de dany, però reps "
                    + value(0.15 * power(4), 0.15, full)
                    + " de dany. Els senars redueixen el dany fet "
                    + value(0.12 * power(4), 0.12, full)
                    + " i el rebut "
                    + value(0.18 * power(4), 0.18, full) + ".";
            case "ARES_BLOOD_OATH" -> "El tercer atac consecutiu guanya "
                    + value(0.15 + 0.10 * Math.min(level, 2), 0.35, full) + " de dany.";
            case "MORRIGAN_RAVEN_OMEN" -> full
                    ? "Quan caus prou a prop de la mort, el pròxim atac és crític i reps fatiga durant 1 torn."
                    : "Dorm fins que caiguis a 35% de vida o menys; llavors desperta completament.";
            case "ARTEMIS_MOON_HUNTER" -> "Després d'una esquiva perfecta, el següent atac guanya "
                    + value(switch (Math.min(level, 2)) {
                        case 0 -> 0.10;
                        case 1 -> 0.18;
                        default -> 0.25;
                    }, 0.25, full)
                    + " de crític"
                    + (full ? " i aplica sagnat si impacta." : " (i sagnat al completar-se).");
            case "BRIGID_OATH_FLAME" -> "Una acció no ofensiva voluntària encén Inspiració: el pròxim atac guanya "
                    + value(0.08 + 0.04 * Math.min(level, 2), 0.16, full)
                    + " de dany i "
                    + value(0.04 + 0.02 * Math.min(level, 2), 0.08, full)
                    + " de crític.";
            case "HEPHAESTUS_INNER_FORGE" -> "Defensar acumula tremp. A 3 càrregues, el pròxim atac guanya "
                    + value(switch (Math.min(level, 3)) {
                        case 0 -> 0.12;
                        case 1 -> 0.18;
                        case 2 -> 0.23;
                        default -> 0.28;
                    }, 0.28, full)
                    + " de dany.";
            case "THOR_HELD_THUNDER" -> "Carregar redueix el pròxim dany rebut "
                    + value(1.0 - (switch (Math.min(level, 2)) {
                        case 0 -> 0.94;
                        case 1 -> 0.88;
                        default -> 0.82;
                    }), 0.18, full)
                    + (full ? " i el cop carregat desequilibra si impacta." : " (i desequilibra al completar-se).");
            case "HESTIA_UNBROKEN_HEARTH" -> "Acabar el torn sense atacar redueix el pròxim dany rebut "
                    + value(1.0 - (switch (Math.min(level, 2)) {
                        case 0 -> 0.92;
                        case 1 -> 0.88;
                        default -> 0.84;
                    }), 0.16, full) + ".";
            case "THOTH_FATE_SCRIPT" -> "Si el rival repeteix acció, guanyes "
                    + value(switch (Math.min(level, 2)) {
                        case 0 -> 0.08;
                        case 1 -> 0.13;
                        default -> 0.18;
                    }, 0.18, full)
                    + " de dany o redueixes el rebut "
                    + value(1.0 - (switch (Math.min(level, 2)) {
                        case 0 -> 0.94;
                        case 1 -> 0.90;
                        default -> 0.86;
                    }), 0.14, full) + ".";
            case "ATHENA_DIVINE_STRATEGIST" -> "Canviar d'acció acumula fins a "
                    + currentValue(1 + Math.min(level, 2))
                    + (full ? "" : " (" + fullValue(3) + ")")
                    + " càrregues. Cada una dona "
                    + value(switch (Math.min(level, 2)) {
                        case 0 -> 0.04;
                        case 1 -> 0.05;
                        default -> 0.06;
                    }, 0.06, full) + " de dany.";
            case "HECATE_CURSED_THRESHOLD" -> "Rebre un cop fort marca l'enemic. El pròxim impacte retorna "
                    + value(switch (Math.min(level, 2)) {
                        case 0 -> 0.15;
                        case 1 -> 0.25;
                        default -> 0.35;
                    }, 0.35, full)
                    + " del dany marcat.";
            case "LOKI_BROKEN_RULE" -> "Un atac que no faria dany es converteix en un cop feble cada "
                    + currentValue(lokiCooldown())
                    + (full ? "" : " (" + fullValue(3) + ")")
                    + " torns propis.";
            case "JANUS_BETWEEN_DOORS" -> "Canviar entre atacar i no atacar obre una porta. El següent atac obté "
                    + value(0.06 + 0.04 * Math.min(level, 2), 0.14, full)
                    + " de dany, o el següent cop rebut es redueix "
                    + value(0.06 + 0.04 * Math.min(level, 2), 0.14, full) + ".";
            case "HERMES_STOLEN_STEP" -> "Esquivar o no atacar guarda impuls. El següent atac guanya "
                    + value(0.06 + 0.03 * Math.min(level, 2), 0.12, full) + " de dany.";
            default -> perk.description();
        };
    }

    /**
     * Indica com es desperta la perk actual.
     */
    private String awakeningInstruction() {
        return switch (perk.id()) {
            case "CERNUNNOS_WILD_PULSE" -> "guanya força a cada torn propi.";
            case "ARES_BLOOD_OATH" -> "completa cadenes de tres atacs consecutius.";
            case "MORRIGAN_RAVEN_OMEN" -> "comença un torn amb 35% de vida o menys.";
            case "ARTEMIS_MOON_HUNTER" -> "fes esquives perfectes.";
            case "BRIGID_OATH_FLAME" -> "acaba torns amb defensar, esquivar o carregar.";
            case "HEPHAESTUS_INNER_FORGE" -> "defensa per acumular tremp.";
            case "THOR_HELD_THUNDER" -> "carrega atacs.";
            case "HESTIA_UNBROKEN_HEARTH" -> "acaba torns sense atacar.";
            case "THOTH_FATE_SCRIPT" -> "aprofita accions repetides del rival.";
            case "ATHENA_DIVINE_STRATEGIST" -> "canvia d'acció respecte al teu torn anterior.";
            case "HECATE_CURSED_THRESHOLD" -> "rep cops forts i retorna el dolor marcat.";
            case "LOKI_BROKEN_RULE" -> "deixa que Loki trenqui un atac fallit.";
            case "JANUS_BETWEEN_DOORS" -> "alterna entre una acció ofensiva i una no ofensiva.";
            case "HERMES_STOLEN_STEP" -> "esquiva o acaba el torn sense atacar.";
            default -> "";
        };
    }

    /**
     * Mostra un valor actual i, si cal, el complet.
     */
    private String value(double current, double complete, boolean full) {
        String currentText = currentValue(pct(current) + "%");
        if (full)
            return currentText;
        return currentText + Ansi.DARK_GRAY + " (" + fullValue(pct(complete) + "%") + Ansi.DARK_GRAY + ")"
                + Ansi.RESET;
    }

    /**
     * Dona format al valor actual.
     */
    private String currentValue(Object value) {
        return Ansi.YELLOW + String.valueOf(value) + Ansi.RESET;
    }

    /**
     * Dona format al valor complet.
     */
    private String fullValue(Object value) {
        return Ansi.CYAN + String.valueOf(value) + Ansi.RESET;
    }

    /**
     * Retorna el color associat a la divinitat.
     */
    private String godColor() {
        return switch (perk.id()) {
            case "ARES_BLOOD_OATH", "MORRIGAN_RAVEN_OMEN", "HECATE_CURSED_THRESHOLD" -> Ansi.RED + Ansi.BOLD;
            case "ARTEMIS_MOON_HUNTER", "THOTH_FATE_SCRIPT", "ATHENA_DIVINE_STRATEGIST" -> Ansi.CYAN + Ansi.BOLD;
            case "BRIGID_OATH_FLAME", "HEPHAESTUS_INNER_FORGE", "HESTIA_UNBROKEN_HEARTH" -> Ansi.YELLOW + Ansi.BOLD;
            case "CERNUNNOS_WILD_PULSE", "HERMES_STOLEN_STEP" -> Ansi.GREEN + Ansi.BOLD;
            case "LOKI_BROKEN_RULE", "JANUS_BETWEEN_DOORS" -> Ansi.MAGENTA + Ansi.BOLD;
            case "THOR_HELD_THUNDER" -> Ansi.BLUE + Ansi.BOLD;
            default -> Ansi.BOLD;
        };
    }

    /**
     * Comprova si el rival ha repetit acció.
     */
    private boolean repeatedOpponentAction(HitContext ctx, Character owner) {
        Action action = owner == ctx.attacker() ? ctx.defenderAction() : ctx.attackerAction();
        return state.stacks() == action.ordinal() + 1;
    }

    /**
     * Incrementa el despertar fins al màxim indicat i el registra per als assoliments.
     */
    private boolean awaken(HitContext ctx, int max) {
        if (awakening >= max) {
            registerAwakeningMeta(ctx, max, false);
            return false;
        }
        awakening++;
        registerAwakeningMeta(ctx, max, true);
        return true;
    }

    /** Fixa explícitament el despertar i el registra per als assoliments. */
    private void setAwakening(HitContext ctx, int max, int level) {
        int previous = awakening;
        awakening = Math.clamp(level, awakening, max);
        registerAwakeningMeta(ctx, max, awakening > previous);
    }

    /** Registra el progrés de despertar diví al context flexible del torn. */
    private void registerAwakeningMeta(HitContext ctx, int max, boolean advanced) {
        if (ctx == null || perk == null || max <= 0) return;
        appendToken(ctx, "divineAwakeningPerkIds", perk.id());
        appendToken(ctx, "divineAwakeningPerkNames", perk.name());
        appendToken(ctx, "divineAwakeningGods", godName());
        appendToken(ctx, "divineAwakeningLevels", String.valueOf(Math.min(awakening, max)));
        appendToken(ctx, "divineAwakeningMaxLevels", String.valueOf(max));
        ctx.putMeta("divinePerkId", perk.id());
        ctx.putMeta("divinePerkName", perk.name());
        ctx.putMeta("god", godName());
        ctx.putMeta("divineAwakeningLevel", Math.min(awakening, max));
        ctx.putMeta("divineAwakeningMax", max);
        ctx.putMeta("divineAwakeningAdvanced", advanced);
        ctx.putMeta("divinePerkAwakened", awakening >= max);
        ctx.putMeta("divinePerkFullPower", awakening >= max);
        ctx.putMeta("divinePowerRatio", power(max));
    }

    /** Afegeix un token textual sense duplicats. */
    private void appendToken(HitContext ctx, String key, String value) {
        if (ctx == null || key == null || value == null || value.isBlank()) return;
        Object previous = ctx.getMeta(key);
        String token = value.trim();
        if (previous == null || String.valueOf(previous).isBlank()) {
            ctx.putMeta(key, token);
            return;
        }
        String text = String.valueOf(previous);
        for (String existing : text.split("[|,]")) {
            if (token.equals(existing.trim())) return;
        }
        ctx.putMeta(key, text + "|" + token);
    }

    /** Retorna el déu associat a la perk divina. */
    private String godName() {
        return switch (perk.id()) {
            case "CERNUNNOS_WILD_PULSE" -> "CERNUNNOS";
            case "ARES_BLOOD_OATH" -> "ARES";
            case "MORRIGAN_RAVEN_OMEN" -> "MORRIGAN";
            case "ARTEMIS_MOON_HUNTER" -> "ARTEMIS";
            case "BRIGID_OATH_FLAME" -> "BRIGID";
            case "HEPHAESTUS_INNER_FORGE" -> "HEPHAESTUS";
            case "THOR_HELD_THUNDER" -> "THOR";
            case "HESTIA_UNBROKEN_HEARTH" -> "HESTIA";
            case "THOTH_FATE_SCRIPT" -> "THOTH";
            case "ATHENA_DIVINE_STRATEGIST" -> "ATHENA";
            case "HECATE_CURSED_THRESHOLD" -> "HECATE";
            case "LOKI_BROKEN_RULE" -> "LOKI";
            case "JANUS_BETWEEN_DOORS" -> "JANUS";
            case "HERMES_STOLEN_STEP" -> "HERMES";
            default -> "DIVINE";
        };
    }

    /**
     * Calcula la potència segons el despertar.
     */
    private double power(int max) {
        if (max <= 0)
            return 1.0;
        return MIN_POWER + (1.0 - MIN_POWER) * Math.min(awakening, max) / max;
    }

    /**
     * Retorna el text visual de despertar si ha augmentat.
     */
    private String awakeningText(boolean awakened, int max) {
        if (!awakened) {
            return "";
        }

        return " " + awakeningSymbol(max);
    }

    /**
     * Retorna el símbol del despertar actual.
     */
    private String awakeningSymbol(int max) {
        if (max <= 0 || awakening >= max) {
            return Ansi.CYAN + "✦" + Ansi.RESET;
        }

        return Ansi.YELLOW + "✧" + Ansi.RESET;
    }

    /**
     * Calcula el temps de recàrrega de Loki.
     */
    private int lokiCooldown() {
        return Math.max(3, 5 - Math.min(awakening, 2));
    }

    /**
     * Indica si el codi d'acció és ofensiu.
     */
    private boolean isOffensiveCode(int actionCode) {
        return actionCode == Action.ATTACK.ordinal() + 1;
    }

    /**
     * Crea un resultat amb missatge diví.
     */
    private EffectResult msg(String text) {
        MessageSymbol symbol = isFullyAwakened()
                ? MessageSymbol.DIVINE_AWAKENED
                : MessageSymbol.DIVINE_DORMANT;

        return new EffectResult(
                CombatMessage.of(symbol, perk.family().color(), perk.name() + ": " + text),
                false,
                true);
    }

    /**
     * Indica si la perk està completament desperta.
     */
    private boolean isFullyAwakened() {
        return awakening >= maxAwakeningFor(perk.id());
    }

    /**
     * Converteix un decimal en percentatge enter absolut.
     */
    private static String pct(double n) {
        return String.valueOf((int) Math.round(Math.abs(n) * 100));
    }

    /**
     * Arrodoneix a dos decimals.
     */
    private static double round2(double n) {
        return Math.round(n * 100.0) / 100.0;
    }
}