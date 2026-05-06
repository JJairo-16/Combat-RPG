package rpgcombat.perks;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Random;

import rpgcombat.achievements.AchievementSystem;
import rpgcombat.combat.models.Action;
import rpgcombat.combat.turnservice.TurnResult;
import rpgcombat.models.characters.Character;
import rpgcombat.models.effects.Effect;
import rpgcombat.perks.divine.DivineAwakeningView;
import rpgcombat.models.effects.triggers.Chaos;
import rpgcombat.perks.divine.DivinePerkDefinition;
import rpgcombat.perks.divine.DivinePerkRegistry;
import rpgcombat.perks.effect.PerkEffectFactory;
import rpgcombat.perks.mission.MissionDefinition;
import rpgcombat.perks.mission.MissionProgress;
import rpgcombat.perks.mission.MissionRegistry;
import rpgcombat.perks.mission.MissionUpdate;
import rpgcombat.perks.synergy.SynergyDisplayInfo;
import rpgcombat.perks.synergy.SynergyPreview;
import rpgcombat.perks.synergy.SynergyRegistry;
import rpgcombat.perks.synergy.SynergySystem;
import rpgcombat.perks.synergy.SynergyType;
import rpgcombat.utils.ui.Ansi;

/** Coordina missions, perks, perks divines i sinergies durant el combat. */
public final class CombatPerkSystem {
    private final Map<Character, PlayerPerkState> states = new IdentityHashMap<>();
    private final Random rng = new Random();
    private final SynergySystem synergySystem = new SynergySystem(SynergyRegistry.all());
    private final AchievementSystem achievementSystem;
    private int currentRoundNumber;

    /** Assigna l'estat inicial de perks a cada jugador. */
    public CombatPerkSystem(Character player1, Character player2) {
        this(player1, player2, null);
    }

    /** Assigna l'estat inicial de perks a cada jugador i connecta assoliments. */
    public CombatPerkSystem(Character player1, Character player2, AchievementSystem achievementSystem) {
        this.achievementSystem = achievementSystem;
        states.put(player1, initialStateFor(player1));
        states.put(player2, initialStateFor(player2));
        registerInitialDivinePerk(player1);
        registerInitialDivinePerk(player2);
    }

    /** Crea l'estat inicial d'un jugador. */
    private PlayerPerkState initialStateFor(Character player) {
        PlayerPerkState state = new PlayerPerkState(new MissionProgress(MissionRegistry.roll(rng)));
        DivinePerkRegistry.activeFor(player).ifPresent(state::setDivinePerk);
        return state;
    }

    /** Actualitza les missions del personatge després d'un torn. */
    public void afterTurn(Character actor, Character opponent, Action actorAction, Action opponentAction,
            TurnResult result, int roundNumber) {
        currentRoundNumber = roundNumber;
        PlayerPerkState state = states.get(actor);
        if (state == null)
            return;

        MissionUpdate update = MissionUpdate.from(actor, opponent, actorAction, opponentAction, result, roundNumber);
        for (MissionProgress mission : state.missions()) {
            if (!mission.rewardClaimed()) {
                boolean wasCompleted = mission.completed();
                mission.update(update);
                if (!wasCompleted && mission.completed()) {
                    registerPerkMissionCompleted(actor, state, mission, roundNumber);
                }
            }
        }
        state.updatePendingChoice();
    }

    /** Retorna el resum visible de progrés del jugador. */
    public String missionSummary(Character player) {
        PlayerPerkState state = states.get(player);
        if (state == null)
            return "";

        StringBuilder sb = new StringBuilder();

        if (!state.missions().isEmpty()) {
            sb.append("Missions");

            for (MissionProgress progress : state.missions()) {
                if (progress.definition() == null)
                    continue;
                MissionDefinition mission = progress.definition();

                String status;
                if (!progress.completed())
                    status = progress.progressText();
                else if (progress.rewardClaimed())
                    status = "Reclamada";
                else
                    status = "Completada";

                sb.append("\n\n").append(mission.name())
                        .append("\n").append(mission.description())
                        .append("\nProgrés: ").append(status);
            }
        }

        DivinePerkDefinition divinePerk = state.divinePerk();
        if (divinePerk != null) {
            if (!sb.isEmpty())
                sb.append("\n---\n");

            DivineAwakeningView awakening = divineAwakeningFor(player, divinePerk.id());
            String name = awakening == null ? coloredDivineName(divinePerk) : awakening.awakenedDisplayName();
            String description = awakening == null ? divineDescriptionFallback(divinePerk)
                    : awakening.awakenedDescription();

            sb.append("Perk divina")
                    .append("\n\n").append(name)
                    .append("\n").append(description)
                    .append("\nActivació: ").append(triggerLabel(divinePerk.perk().trigger()));
        }

        if (state.perks().isEmpty())
            return sb.toString();

        if (!sb.isEmpty())
            sb.append("\n---\n");

        sb.append("Perks");
        for (PerkDefinition chosen : state.perks()) {
            List<String> alteredDescriptions = state.synergyDescriptionsFor(chosen.id());
            sb.append("\n").append(chosen.name()).append(alteredDescriptions.isEmpty() ? "" : " ✦")
                    .append("\n").append(chosen.description());
            for (String description : alteredDescriptions) {
                sb.append("\nSinergia activa: ").append(description);
            }
            sb.append("\nActivació: ").append(triggerLabel(chosen.trigger()));
        }

        List<SynergyDisplayInfo> activeSynergies = synergySystem.activeDisplayInfo(state);

        if (!activeSynergies.isEmpty()) {
            if (!sb.isEmpty())
                sb.append("\n---\n");

            sb.append("Sinergies");

            for (SynergyDisplayInfo synergy : activeSynergies) {
                sb.append("\n")
                        .append(synergy.name())
                        .append("\n")
                        .append(synergy.description())
                        .append("\nTipus: ")
                        .append(synergy.type() == SynergyType.BONUS_EXTRA
                                ? "Bonus extra"
                                : "Perk modificada");
            }
        }

        return sb.toString();
    }

    /** Cerca la vista de despertar diví activa al personatge. */
    private DivineAwakeningView divineAwakeningFor(Character player, String divinePerkId) {
        if (player == null || divinePerkId == null)
            return null;

        for (Effect effect : player.getEffects()) {
            if (effect instanceof DivineAwakeningView awakening && divinePerkId.equals(awakening.divinePerkId())) {
                return awakening;
            }
        }
        return null;
    }

    /** Retorna la descripció base si l'efecte encara no exposa despertar. */
    private String divineDescriptionFallback(DivinePerkDefinition divinePerk) {
        if (divinePerk.awakeningMaxCharge() <= 0)
            return divinePerk.description();

        String wake = divinePerk.awakeningDescription().isBlank()
                ? ""
                : "\nDespertar: " + divinePerk.awakeningDescription();
        return divinePerk.description() + wake;
    }

    /** Resol l'elecció de perk pendent, si n'hi ha. */
    public void resolvePendingChoices(Character player) {
        PlayerPerkState state = states.get(player);
        if (state == null || !state.pendingChoice())
            return;

        if (!state.canGainMorePerks()) {
            state.clearPendingChoice();
            return;
        }

        boolean corruptedOnly = player.hasEffect(Chaos.INTERNAL_EFFECT_KEY);

        List<PerkDefinition> options = PerkRegistry.rollOptions(corruptedOnly, 24, rng).stream()
                .filter(perk -> !state.hasPerk(perk.id()))
                .filter(perk -> !player.hasEffect(PerkEffectFactory.keyFor(perk)))
                .limit(3)
                .toList();

        if (options.isEmpty()) {
            state.clearPendingChoice();
            return;
        }

        Map<String, SynergyPreview> previews = synergySystem.previewAll(state, options);
        PerkDefinition chosen = PerkChoiceMenu.choose(player, options, previews);

        if (chosen != null && !state.hasPerk(chosen.id())) {
            Effect effect = PerkEffectFactory.create(chosen);

            player.removeEffect(effect.key());
            player.addEffect(effect);

            Set<String> previousSynergies = new HashSet<>(state.activeSynergyIds());
            state.addPerk(chosen);
            synergySystem.refresh(player, state);
            registerPerkGained(player, state, chosen, currentRoundNumber);
            registerNewSynergies(player, state, previousSynergies, currentRoundNumber);
            state.clearPendingChoice();

            if (state.canGainMorePerks()) {
                MissionDefinition nextMission = MissionRegistry.rollExcluding(rng, state.missionIds());
                if (nextMission != null) {
                    state.addMission(new MissionProgress(nextMission));
                }
            }
        } else {
            state.clearPendingChoice();
        }
    }

    /** Registra la perk divina inicial, si existeix. */
    private void registerInitialDivinePerk(Character player) {
        if (achievementSystem == null || player == null) return;
        PlayerPerkState state = states.get(player);
        DivinePerkDefinition divine = state == null ? null : state.divinePerk();
        if (divine == null) return;
        achievementSystem.onDivinePerkAssigned(player, divine.id(), divine.name(), divine.god(), currentRoundNumber);
    }

    /** Registra una missió de perk completada. */
    private void registerPerkMissionCompleted(Character player, PlayerPerkState state, MissionProgress mission,
            int roundNumber) {
        if (achievementSystem == null || mission == null || mission.definition() == null) return;
        PerkDefinition chosen = state == null ? null : state.chosenPerk();
        achievementSystem.onPerkMissionCompleted(player, mission.definition().id(),
                chosen == null ? null : chosen.id(),
                state == null ? 0 : state.completedMissionCount(),
                state == null ? 0 : state.missions().size(),
                roundNumber);
    }

    /** Registra una perk obtinguda. */
    private void registerPerkGained(Character player, PlayerPerkState state, PerkDefinition chosen, int roundNumber) {
        if (achievementSystem == null || state == null || chosen == null) return;
        achievementSystem.onPerkGained(player, chosen.id(), chosen.name(),
                chosen.family() == null ? null : chosen.family().name(),
                chosen.tags(), state.perkCount(), PlayerPerkState.MAX_PERKS, roundNumber);
    }

    /** Registra les sinergies activades per primera vegada després de triar una perk. */
    private void registerNewSynergies(Character player, PlayerPerkState state, Set<String> previousSynergies,
            int roundNumber) {
        if (achievementSystem == null || state == null) return;
        Set<String> before = previousSynergies == null ? Set.of() : previousSynergies;
        for (String synergyId : state.activeSynergyIds()) {
            if (!before.contains(synergyId)) {
                achievementSystem.onSynergyActivated(player, synergyId, state.activeSynergyName(synergyId),
                        state.activeSynergyIds().size(), roundNumber);
            }
        }
    }

    /** Retorna el nom diví acolorit pel déu. */
    private static String coloredDivineName(DivinePerkDefinition divinePerk) {
        return godColor(divinePerk.god()) + divinePerk.name() + Ansi.RESET;
    }

    /** Retorna el color associat a un déu. */
    private static String godColor(String god) {
        if (god == null)
            return Ansi.BOLD;
        return switch (god.toLowerCase()) {
            case "ares", "morrigan", "hecate" -> Ansi.RED + Ansi.BOLD;
            case "artemis", "thoth", "athena" -> Ansi.CYAN + Ansi.BOLD;
            case "brigid", "hephaestus", "hestia" -> Ansi.YELLOW + Ansi.BOLD;
            case "cernunnos", "hermes" -> Ansi.GREEN + Ansi.BOLD;
            case "loki", "janus" -> Ansi.MAGENTA + Ansi.BOLD;
            case "thor" -> Ansi.BLUE + Ansi.BOLD;
            default -> Ansi.BOLD;
        };
    }

    /** Etiqueta visible de la fase que activa una perk. */
    private static String triggerLabel(rpgcombat.weapons.passives.HitContext.Phase trigger) {
        if (trigger == null)
            return "Desconeguda";
        return switch (trigger) {
            case START_TURN -> "Inici de torn";
            case BEFORE_ATTACK -> "Abans d'atacar";
            case ROLL_CRIT -> "Tirada crítica";
            case MODIFY_DAMAGE -> "Modificació de dany";
            case BEFORE_DEFENSE -> "Abans de defensar";
            case AFTER_DEFENSE -> "Després de defensar";
            case AFTER_HIT -> "Després d'impactar";
            case END_TURN -> "Final de torn";
        };
    }
}
