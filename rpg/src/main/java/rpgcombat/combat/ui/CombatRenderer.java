package rpgcombat.combat.ui;

import java.util.List;

import rpgcombat.combat.turnservice.TurnResult;
import rpgcombat.models.characters.Character;
import rpgcombat.models.characters.Statistics;
import rpgcombat.utils.ui.Ansi;
import rpgcombat.utils.ui.ColorGradient;

/**
 * Mostra per consola la informació visual del combat.
 */
public class CombatRenderer {
    private static final int BAR_SIZE = 20;
    private static final int DIV_WIDTH = 44;

    private static final String DIV = Ansi.DARK_GRAY + "─".repeat(DIV_WIDTH) + Ansi.RESET;
    private static final String BIG_DIV = Ansi.DARK_GRAY + "═".repeat(DIV_WIDTH) + Ansi.RESET;

    /**
     * Imprimeix la capçalera d'una ronda.
     */
    public void printRoundHeader() {
        System.out.println(BIG_DIV);
        System.out.println(Ansi.BOLD + "          COMBAT ROUND" + Ansi.RESET);
        System.out.println(BIG_DIV);
        System.out.println();
    }

    /**
     * Imprimeix la capçalera de regeneració.
     */
    public void printRegenHeader() {
        System.out.println(BIG_DIV);
        System.out.println(Ansi.CYAN + Ansi.BOLD + "           REGENERACIÓ" + Ansi.RESET);
        System.out.println(BIG_DIV);
    }

    /**
     * Imprimeix el resultat visual d'un torn.
     *
     * @param result resultat del torn
     */
    public void printTurnResult(TurnResult result) {
        if (result == null) {
            return;
        }

        printRawLine(result.attackerMessage());
        printSpecialLines(result.startMessages());
        printPassiveMessages(result.preDefenseMessages());

        if (result.defenseMessage() != null && !result.defenseMessage().isBlank()) {
            System.out.println(Ansi.DARK_GRAY + "  -> " + Ansi.RESET + result.defenseMessage());
        }

        printPassiveMessages(result.postDefenseMessages());
        printPassiveMessages(result.endTurnMessages());
    }

    /**
     * Imprimeix el resum del dany rebut al final de la ronda.
     *
     * @param character personatge afectat
     * @param damageTaken dany rebut
     */
    public void printRoundSummary(Character character, double damageTaken) {
        System.out.println(DIV);

        System.out.printf("%s%s%s ha rebut %s%.2f%s de dany.%n",
                Ansi.BOLD, character.getName(), Ansi.RESET,
                Ansi.BRIGHT_RED, Math.max(damageTaken, 0), Ansi.RESET);

        printStatusBars(character);
        System.out.println();
    }

    /**
     * Imprimeix el resum de la regeneració aplicada.
     *
     * @param character personatge afectat
     * @param hpRegen vida recuperada
     * @param manaRegen mana recuperat
     */
    public void printRegenSummary(Character character, double hpRegen, double manaRegen) {
        System.out.printf("%s%s%s recupera %s+%.2f%s vida i %s+%.2f%s mana.%n",
                Ansi.BOLD, character.getName(), Ansi.RESET,
                Ansi.GREEN, Math.max(0, hpRegen), Ansi.RESET,
                Ansi.BRIGHT_BLUE, Math.max(0, manaRegen), Ansi.RESET);

        printStatusBars(character);
        System.out.println();
    }

    /**
     * Imprimeix les barres de vida i mana del personatge.
     *
     * @param character personatge a mostrar
     */
    public void printStatusBars(Character character) {
        Statistics stats = character.getStatistics();

        double currentHealth = stats.getHealth();
        double maxHealth = stats.getMaxHealth();

        double currentMana = stats.getMana();
        double maxMana = stats.getMaxMana();

        System.out.println("   " + Ansi.DARK_GRAY + "─────────────" + Ansi.RESET);

        String hpColor = healthColor(currentHealth, maxHealth);

        System.out.printf("Vida: %s %.2f / %.2f%n",
                buildBar(currentHealth, maxHealth, BAR_SIZE, hpColor),
                currentHealth,
                maxHealth);

        System.out.printf("Mana: %s %.2f / %.2f%n",
                buildBar(currentMana, maxMana, BAR_SIZE, Ansi.BRIGHT_BLUE),
                currentMana,
                maxMana);
    }

    /**
     * Afegeix les barres d'estat a un text existent.
     *
     * @param sb text de destí
     * @param character personatge a mostrar
     */
    public void appendStatusBars(StringBuilder sb, Character character) {
        Statistics stats = character.getStatistics();

        double currentHealth = stats.getHealth();
        double maxHealth = stats.getMaxHealth();

        double currentMana = stats.getMana();
        double maxMana = stats.getMaxMana();

        sb.append("  ").append(Ansi.DARK_GRAY).append("─────────────").append(Ansi.RESET).append("\n");

        String hpColor = healthColor(currentHealth, maxHealth);

        sb.append("Vida: ").append(buildBar(currentHealth, maxHealth, BAR_SIZE, hpColor));
        sb.append(" ").append(round2(currentHealth)).append(" / ").append(round2(maxHealth));
        sb.append("\n");

        sb.append("Mana: ").append(buildBar(currentMana, maxMana, BAR_SIZE, Ansi.BRIGHT_BLUE));
        sb.append(" ").append(round2(currentMana)).append(" / ").append(round2(maxMana));
        sb.append("\n");
    }

    /**
     * Imprimeix una línia si conté text.
     *
     * @param line línia a mostrar
     */
    private void printRawLine(String line) {
        if (line == null || line.isBlank()) {
            return;
        }
        System.out.println(Ansi.BOLD + line + Ansi.RESET);
    }

    /**
     * Imprimeix línies secundàries.
     *
     * @param lines línies a mostrar
     */
    private void printSpecialLines(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return;
        }

        for (String line : lines) {
            if (line == null || line.isBlank()) {
                continue;
            }
            System.out.println(Ansi.DARK_GRAY + line + Ansi.RESET);
        }
    }

    /**
     * Imprimeix missatges passius amb estil.
     *
     * @param msgs missatges a mostrar
     */
    private void printPassiveMessages(List<String> msgs) {
        if (msgs == null || msgs.isEmpty()) {
            return;
        }

        StringBuilder sb = new StringBuilder();

        for (String msg : msgs) {
            if (msg == null || msg.isBlank()) {
                continue;
            }

            String content = msg.trim();
            String symbol = "+";
            String explicitColor = null;

            if (content.startsWith("[")) {
                int end = content.indexOf(']');
                if (end > 1) {
                    String tag = content.substring(1, end).trim();
                    content = content.substring(end + 1).trim();

                    ParsedStyle style = parseStyleTag(tag);
                    if (style.color() != null && !style.color().isBlank()) {
                        explicitColor = style.color();
                    }
                    if (style.symbol() != null && !style.symbol().isBlank()) {
                        symbol = style.symbol();
                    }
                }
            }

            if ("+".equals(symbol) && content.startsWith("-")) {
                symbol = "-";
                content = content.substring(1).trim();
            }

            String color = getMessageColor(symbol, explicitColor);

            sb.append("  ")
                    .append(color)
                    .append(symbol)
                    .append(Ansi.RESET)
                    .append(" ")
                    .append(content)
                    .append("\n");
        }

        if (!sb.isEmpty()) {
            System.out.print(sb);
        }
    }

    /**
     * Extreu color i símbol d'una etiqueta d'estil.
     *
     * @param tag etiqueta a interpretar
     * @return estil parsejat
     */
    private ParsedStyle parseStyleTag(String tag) {
        if (tag == null || tag.isBlank()) {
            return new ParsedStyle(null, null);
        }

        String color = null;
        String symbol = null;

        String[] parts = tag.split("\\|", -1);

        if (parts.length >= 1) {
            String first = parts[0].trim();
            if (!first.isBlank()) {
                color = first.toUpperCase();
            }
        }

        if (parts.length >= 2) {
            String second = parts[1].trim();
            if (!second.isBlank()) {
                symbol = second;
            }
        }

        return new ParsedStyle(color, symbol);
    }

    /**
     * Guarda l'estil interpretat d'un missatge.
     */
    private record ParsedStyle(String color, String symbol) {
    }

    /**
     * Retorna el color d'un missatge.
     *
     * @param symbol símbol del missatge
     * @param explicitColor color forçat
     * @return codi ANSI del color
     */
    private String getMessageColor(String symbol, String explicitColor) {
        if (explicitColor != null) {
            return switch (explicitColor) {
                case "RED" -> Ansi.RED;
                case "GREEN" -> Ansi.GREEN;
                case "YELLOW" -> Ansi.YELLOW;
                case "BLUE" -> Ansi.BLUE;
                case "MAGENTA" -> Ansi.MAGENTA;
                case "CYAN" -> Ansi.CYAN;
                case "WHITE" -> Ansi.WHITE;
                default -> "-".equals(symbol) ? Ansi.RED : Ansi.GREEN;
            };
        }

        return "-".equals(symbol) ? Ansi.RED : Ansi.GREEN;
    }

    /**
     * Retorna el color segons el percentatge de vida.
     *
     * @param current vida actual
     * @param max vida màxima
     * @return codi ANSI del color
     */
    public static String healthColor(double current, double max) {
        return ColorGradient.getColor(
                current / max,
                255, 0, 0,
                255, 220, 0,
                70, 190, 110
        );
    }

    /**
     * Construeix una barra visual proporcional.
     *
     * @param current valor actual
     * @param max valor màxim
     * @param size mida de la barra
     * @param color color de la part plena
     * @return barra formatejada
     */
    private String buildBar(double current, double max, int size, String color) {
        if (max <= 0) {
            return "[ERROR]";
        }

        double overload = 0;
        if (current > max) {
            overload = current - max;
        }

        double ratio = Math.clamp(current / max, 0.0, 1.0);
        int filled = (int) Math.round(ratio * size);

        StringBuilder bar = new StringBuilder("[");

        for (int i = 0; i < size; i++) {
            if (i < filled) {
                bar.append(color).append("█").append(Ansi.RESET);
            } else {
                bar.append(Ansi.DARK_GRAY).append("░").append(Ansi.RESET);
            }
        }

        int overloadBars = (int) Math.round((overload / max) * size);
        for (int i = 0; i < overloadBars; i++) {
            bar.append(Ansi.MAGENTA).append("█").append(Ansi.RESET);
        }

        bar.append("]");
        return bar.toString();
    }

    /**
     * Arrodoneix a dos decimals.
     *
     * @param n valor a arrodonir
     * @return valor arrodonit
     */
    private double round2(double n) {
        return Math.round(n * 100.0) / 100.0;
    }
}