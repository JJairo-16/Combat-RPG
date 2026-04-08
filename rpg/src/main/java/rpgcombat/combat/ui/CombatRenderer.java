package rpgcombat.combat.ui;

import java.util.List;

import rpgcombat.models.characters.Character;
import rpgcombat.models.characters.Statistics;
import rpgcombat.combat.TurnResult;
import rpgcombat.utils.ui.Ansi;

public class CombatRenderer {
    private static final int BAR_SIZE = 20;
    private static final int DIV_WIDTH = 44;

    private static final String DIV = Ansi.DARK_GRAY + "─".repeat(DIV_WIDTH) + Ansi.RESET;
    private static final String BIG_DIV = Ansi.DARK_GRAY + "═".repeat(DIV_WIDTH) + Ansi.RESET;

    public void printRoundHeader() {
        System.out.println(BIG_DIV);
        System.out.println(Ansi.BOLD + "          COMBAT ROUND" + Ansi.RESET);
        System.out.println(BIG_DIV);
        System.out.println();
    }

    public void printRegenHeader() {
        System.out.println(BIG_DIV);
        System.out.println(Ansi.CYAN + Ansi.BOLD + "           REGENERACIÓ" + Ansi.RESET);
        System.out.println(BIG_DIV);
    }

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

    public void printRoundSummary(Character character, double damageTaken) {
        System.out.println(DIV);

        System.out.printf("%s%s%s ha rebut %s%.2f%s de dany.%n",
                Ansi.BOLD, character.getName(), Ansi.RESET,
                Ansi.BRIGHT_RED, damageTaken, Ansi.RESET);

        printStatusBars(character);
        System.out.println();
    }

    public void printRegenSummary(Character character, double hpRegen, double manaRegen) {
        System.out.printf("%s%s%s recupera %s+%.2f%s vida i %s+%.2f%s mana.%n",
                Ansi.BOLD, character.getName(), Ansi.RESET,
                Ansi.GREEN, Math.max(0, hpRegen), Ansi.RESET,
                Ansi.BRIGHT_BLUE, Math.max(0, manaRegen), Ansi.RESET);

        printStatusBars(character);
        System.out.println();
    }

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

    private void printRawLine(String line) {
        if (line == null || line.isBlank()) {
            return;
        }
        System.out.println(Ansi.BOLD + line + Ansi.RESET);
    }

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

    private void printPassiveMessages(List<String> msgs) {
        if (msgs == null || msgs.isEmpty()) {
            return;
        }

        StringBuilder sb = new StringBuilder();

        for (String msg : msgs) {
            if (msg == null || msg.isBlank()) {
                continue;
            }

            if (msg.charAt(0) == '-') {
                sb.append("  ").append(Ansi.RED).append("-").append(Ansi.RESET);
                sb.append(" ").append(msg.substring(1).trim()).append("\n");
            } else {
                sb.append("  ").append(Ansi.GREEN).append("+").append(Ansi.RESET);
                sb.append(" ").append(msg).append("\n");
            }
        }

        if (!sb.isEmpty()) {
            System.out.print(sb);
        }
    }

    private String healthColor(double current, double max) {
        if (max <= 0) {
            return Ansi.BRIGHT_RED;
        }

        double ratio = Math.clamp(current / max, 0.0, 1.0);

        if (ratio > 0.60) {
            return Ansi.GREEN;
        }
        if (ratio > 0.30) {
            return Ansi.YELLOW;
        }
        return Ansi.BRIGHT_RED;
    }

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

    private double round2(double n) {
        return Math.round(n * 100.0) / 100.0;
    }
}