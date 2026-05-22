package rpgcombat.game.ui;

import java.util.List;

import rpgcombat.combat.models.CombatantStatus;
import rpgcombat.combat.models.Winner;
import rpgcombat.combat.ui.CombatRenderer;
import rpgcombat.gamemode.model.MatchContext;
import rpgcombat.models.breeds.Breed;
import rpgcombat.models.characters.Character;
import rpgcombat.models.characters.Statistics;
import rpgcombat.utils.cache.TextWrapCache;
import rpgcombat.utils.rng.DivineCharismaAffinity;
import rpgcombat.utils.ui.Ansi;
import rpgcombat.weapons.Weapon;
import rpgcombat.weapons.config.WeaponType;

/**
 * Composa i mostra les pantalles de presentació de la partida.
 */
public final class MatchPresentation {
    private static final int WRAP_WIDTH = 76;
    private static final String SECTION_DIV = " " + Ansi.DARK_GRAY + "─".repeat(WRAP_WIDTH) + Ansi.RESET + "\n";

    private final MatchContext matchContext;
    private final TextWrapCache wrapCache = new TextWrapCache();
    private final CombatRenderer combatRenderer = new CombatRenderer();
    private final StringBuilder playerInfo = new StringBuilder(24_000);

    public MatchPresentation(MatchContext matchContext) {
        this.matchContext = matchContext == null ? new MatchContext(null, false) : matchContext;
    }

    /**
     * Mostra la fitxa del jugador en una pantalla interactiva.
     */
    public void showPlayerInfo(Character player) {
        PlayerInfoScreen.show(buildPlayerInfo(player));
    }

    /**
     * Mostra el resum final del combat abans de la cinemàtica de tancament.
     */
    public void showWinner(Winner winner, Character player1, Character player2) {
        VictoryScreen.show(buildWinnerSummary(winner, player1, player2));
    }

    private String buildPlayerInfo(Character player) {
        if (player == null) {
            return "No hi ha informació disponible.\n";
        }

        Statistics stats = player.getStatistics();
        Breed breed = player.getBreed();
        Weapon weapon = player.getWeapon();

        StringBuilder out = playerInfo;
        out.setLength(0);
        out.ensureCapacity(24_000);

        appendCharacterOverview(out, player, breed, stats, weapon);
        appendResourceSection(out, player);
        appendWeaponSection(out, weapon);
        appendTerrainSection(out);
        appendDivineAffinitySection(out, player);
        return out.toString();
    }

    private void appendCharacterOverview(StringBuilder out, Character player, Breed breed, Statistics stats,
            Weapon weapon) {
        appendSectionTitle(out, "Combatent");
        out.append(" ")
                .append(Ansi.WHITE).append(Ansi.BOLD).append(player.getName()).append(Ansi.RESET)
                .append("   ")
                .append(Ansi.DARK_GRAY).append("Edat ").append(Ansi.RESET)
                .append(Ansi.BOLD).append(player.getAge()).append(Ansi.RESET)
                .append('\n');

        out.append(" ")
                .append(Ansi.CYAN).append(Ansi.BOLD).append(breed.getName()).append(Ansi.RESET)
                .append("   ")
                .append(weapon == null
                        ? Ansi.DARK_GRAY + "Sense arma" + Ansi.RESET
                        : Ansi.GREEN + "Arma equipada" + Ansi.RESET)
                .append('\n');

        appendWrapped(out, breed.getDescription(), "   ", Ansi.DARK_GRAY);

        int bonusPct = (int) Math.round(breed.bonus() * 100.0);
        out.append("   ")
                .append(Ansi.GREEN).append("Bonus ").append(Ansi.RESET)
                .append(Ansi.BOLD).append('+').append(bonusPct).append('%').append(Ansi.RESET)
                .append(Ansi.DARK_GRAY).append(" a ").append(Ansi.RESET)
                .append(Ansi.BOLD).append(breed.bonusStat().getName()).append(Ansi.RESET)
                .append("\n\n");

        out.append("   ").append(Ansi.DARK_GRAY).append("Atributs").append(Ansi.RESET).append('\n');
        out.append("   ")
                .append(statChip("Força", stats.getStrength())).append("   ")
                .append(statChip("Destresa", stats.getDexterity())).append("   ")
                .append(statChip("Constitució", stats.getConstitution())).append("   ")
                .append(statChip("Intel·ligència", stats.getIntelligence()))
                .append('\n');
        out.append("   ")
                .append(statChip("Saviesa", stats.getWisdom())).append("   ")
                .append(statChip("Carisma", stats.getCharisma())).append("   ")
                .append(statChip("Sort", stats.getLuck()))
                .append('\n');
    }

    private void appendResourceSection(StringBuilder out, Character player) {
        appendSectionTitle(out, "Recursos");
        appendStatusBars(out, player);
    }

    private void appendWeaponSection(StringBuilder out, Weapon weapon) {
        appendSectionTitle(out, "Arma equipada");
        if (weapon == null) {
            out.append(" ")
                    .append(Ansi.DARK_GRAY).append("Cap").append(Ansi.RESET)
                    .append('\n');
            return;
        }

        WeaponType type = weapon.getType();
        String typeName = type == null ? "?" : type.getName();
        out.append(" ")
                .append(Ansi.WHITE).append(Ansi.BOLD).append(weapon.getName()).append(Ansi.RESET)
                .append("   ")
                .append(colorByWeaponType(type)).append('[').append(typeName).append(']').append(Ansi.RESET)
                .append('\n');
        appendWrapped(out, weapon.getDescription(), "   ", Ansi.DARK_GRAY);

        out.append("   ")
                .append(Ansi.GREEN).append("Dany ").append(Ansi.RESET).append(Ansi.BOLD)
                .append(weapon.getBaseDamage()).append(Ansi.RESET)
                .append("   ")
                .append(Ansi.YELLOW).append("Crit ").append(Ansi.RESET).append(Ansi.BOLD)
                .append(roundPercent(weapon.getCriticalProb())).append('%').append(Ansi.RESET)
                .append("   ")
                .append(Ansi.YELLOW).append("Mult ").append(Ansi.RESET).append(Ansi.BOLD)
                .append('x').append(roundTwo(weapon.getCriticalDamage())).append(Ansi.RESET)
                .append("   ");

        if (weapon.getManaPrice() > 0) {
            out.append(Ansi.BRIGHT_BLUE).append("Mana ").append(Ansi.RESET).append(Ansi.BOLD)
                    .append(Math.round(weapon.getManaPrice())).append(Ansi.RESET);
        } else {
            out.append(Ansi.DARK_GRAY).append("Mana -").append(Ansi.RESET);
        }
        out.append('\n');
    }

    private void appendTerrainSection(StringBuilder out) {
        var terrain = matchContext.terrain();
        if (terrain == null) {
            return;
        }

        appendSectionTitle(out, "Terreny");
        out.append(" ")
                .append(terrain.isNone() ? Ansi.DARK_GRAY : Ansi.CYAN)
                .append(Ansi.BOLD).append(terrain.name()).append(Ansi.RESET)
                .append('\n');

        if (!terrain.isNone()) {
            out.append("   ")
                    .append(Ansi.DARK_GRAY).append("Dificultat ").append(Ansi.RESET)
                    .append(terrainDifficultyStars(terrain.difficulty()))
                    .append('\n');
        }

        String description = terrain.shortDescription() == null || terrain.shortDescription().isBlank()
                ? terrain.description()
                : terrain.shortDescription();
        appendWrapped(out, description, "   ", Ansi.DARK_GRAY);
    }

    private void appendDivineAffinitySection(StringBuilder out, Character player) {
        DivineCharismaAffinity.Band band = DivineCharismaAffinity.classifyBand(player.getStatistics().getCharisma());
        DivineAffinity affinity = divineAffinity(band);

        appendSectionTitle(out, "Favor dels déus");
        out.append(" ")
                .append(affinity.color()).append(Ansi.BOLD).append(affinity.label()).append(Ansi.RESET)
                .append('\n');
        appendWrapped(out, affinity.description(), "   ", Ansi.DARK_GRAY);
    }

    private String buildWinnerSummary(Winner winner, Character player1, Character player2) {
        StringBuilder out = new StringBuilder(4096);

        appendSectionTitle(out, "Resultat");
        switch (winner) {
            case PLAYER1 -> appendVictory(out, player1, player2);
            case PLAYER2 -> appendVictory(out, player2, player1);
            case TIE -> {
                out.append(" ").append(Ansi.YELLOW).append(Ansi.BOLD).append("EMPAT").append(Ansi.RESET).append("\n\n");
                out.append(" Tots dos combatents han caigut al mateix temps.\n");
                out.append(" No hi ha vencedor en aquesta batalla.\n\n");
            }
            default -> out.append(" El combat ha finalitzat.\n\n");
        }

        appendSectionTitle(out, "Estat final");
        appendFinalState(out, player1);
        appendFinalState(out, player2);
        return out.toString();
    }

    private void appendVictory(StringBuilder out, Character winner, Character defeated) {
        out.append(" ").append(Ansi.GREEN).append(Ansi.BOLD).append("VICTÒRIA").append(Ansi.RESET).append("\n\n");
        out.append(" ")
                .append(Ansi.WHITE).append(Ansi.BOLD).append(winner.getName()).append(Ansi.RESET)
                .append(" ha guanyat el combat.\n");
        out.append(" ")
                .append(Ansi.DARK_GRAY).append(defeated.getName()).append(" ha caigut derrotat.").append(Ansi.RESET)
                .append("\n\n");
    }

    private void appendFinalState(StringBuilder out, Character player) {
        if (player == null) {
            return;
        }

        out.append(" ")
                .append(Ansi.WHITE).append(Ansi.BOLD).append(player.getName()).append(Ansi.RESET)
                .append('\n');
        appendStatusBars(out, player);
        out.append('\n');
    }

    private void appendSectionTitle(StringBuilder out, String title) {
        if (!out.isEmpty()) {
            out.append('\n');
        }
        out.append(" ")
                .append(Ansi.WHITE).append(Ansi.BOLD).append(title).append(Ansi.RESET)
                .append('\n');
        out.append(SECTION_DIV);
    }

    private void appendWrapped(StringBuilder out, String text, String indent, String color) {
        String safe = text == null ? "" : text.trim();
        if (safe.isEmpty()) {
            return;
        }
        for (String line : wrapCache.get(safe, WRAP_WIDTH - indent.length())) {
            out.append(indent).append(color).append(line).append(Ansi.RESET).append('\n');
        }
    }

    private String statChip(String label, int value) {
        return Ansi.DARK_GRAY + label + ":" + Ansi.RESET + " " + Ansi.BOLD + value + Ansi.RESET;
    }

    private void appendStatusBars(StringBuilder out, Character player) {
        List<String> lines = combatRenderer.statusLines(CombatantStatus.from(player));
        for (int i = 1; i < lines.size(); i++) {
            out.append("   ").append(lines.get(i)).append('\n');
        }
    }

    private String terrainDifficultyStars(int difficulty) {
        int stars = Math.clamp(difficulty, 0, 5);
        String color = switch (stars) {
            case 0 -> Ansi.DARK_GRAY;
            case 1 -> Ansi.GREEN;
            case 2 -> Ansi.CYAN;
            case 3 -> Ansi.YELLOW;
            case 4 -> Ansi.ORANGE;
            default -> Ansi.RED;
        };
        return color + "★".repeat(stars) + Ansi.DARK_GRAY + "☆".repeat(5 - stars) + Ansi.RESET;
    }

    private String colorByWeaponType(WeaponType type) {
        if (type == null) {
            return Ansi.WHITE;
        }

        return switch (type) {
            case PHYSICAL -> Ansi.MAGENTA;
            case RANGE -> Ansi.BRIGHT_BLUE;
            case MAGICAL -> Ansi.ORANGE;
            default -> Ansi.WHITE;
        };
    }

    private DivineAffinity divineAffinity(DivineCharismaAffinity.Band band) {
        return switch (band) {
            case DISLIKED_LOW -> new DivineAffinity("Molt desfavorable", band.toString(), Ansi.RED);
            case NEUTRAL_LOW -> new DivineAffinity("Freda", band.toString(), Ansi.YELLOW);
            case FAVORED -> new DivineAffinity("Propícia", band.toString(), Ansi.GREEN);
            case NEUTRAL_HIGH -> new DivineAffinity("Acceptable", band.toString(), Ansi.CYAN);
            case DISLIKED_HIGH -> new DivineAffinity("Incòmoda", band.toString(), Ansi.ORANGE);
            default -> new DivineAffinity(
                    "Desconeguda",
                    "La voluntat dels déus és impossible d'interpretar.",
                    Ansi.DARK_GRAY);
        };
    }

    private static double roundTwo(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static int roundPercent(double value) {
        return (int) Math.round(value * 100.0);
    }

    private record DivineAffinity(String label, String description, String color) {
    }
}
