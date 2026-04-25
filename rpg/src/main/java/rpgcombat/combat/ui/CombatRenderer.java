package rpgcombat.combat.ui;

import java.util.ArrayList;
import java.util.List;

import rpgcombat.combat.models.CombatantStatus;
import rpgcombat.combat.turnservice.TurnResult;
import rpgcombat.combat.ui.messages.CombatMessageFormatter;
import rpgcombat.models.characters.Character;
import rpgcombat.utils.ui.Ansi;
import rpgcombat.utils.ui.ColorGradient;

/**
 * Mostra per consola la informació visual del combat.
 */
public class CombatRenderer {
    public static final int BAR_SIZE = 20;
    public static final int DIV_WIDTH = 54;

    private static final String DIV = Ansi.DARK_GRAY + "─".repeat(DIV_WIDTH) + Ansi.RESET;
    private static final String BIG_DIV = Ansi.DARK_GRAY + "═".repeat(DIV_WIDTH) + Ansi.RESET;

    private final CombatMessageFormatter messageFormatter = new CombatMessageFormatter();

    /**
     * Imprimeix la capçalera d'una ronda.
     */
    public void printRoundHeader() {
        System.out.println(BIG_DIV);
        System.out.println(Ansi.BOLD + "                  COMBAT ROUND" + Ansi.RESET);
        System.out.println(BIG_DIV);
        System.out.println();
    }

    /**
     * Imprimeix la capçalera de regeneració.
     */
    public void printRegenHeader() {
        System.out.println(BIG_DIV);
        System.out.println(Ansi.CYAN + Ansi.BOLD + "                   REGENERACIÓ" + Ansi.RESET);
        System.out.println(BIG_DIV);
    }

    /**
     * Imprimeix el resultat visual d'un torn.
     */
    public void printTurnResult(TurnResult result) {
        StringBuilder sb = new StringBuilder();
        appendTurnResult(sb, result);
        System.out.print(sb);
    }

    /**
     * Afegeix el resultat d'un torn a un text.
     */
    public void appendTurnResult(StringBuilder sb, TurnResult result) {
        if (result == null) {
            return;
        }

        appendLine(sb, messageFormatter.attacker(result.attackerMessage()));
        appendLines(sb, messageFormatter.effects(result.startMessages()));
        appendLines(sb, messageFormatter.effects(result.preDefenseMessages()));
        appendLine(sb, messageFormatter.defense(result.defenseMessage()));
        appendLines(sb, messageFormatter.effects(result.postDefenseMessages()));
        appendLines(sb, messageFormatter.effects(result.endTurnMessages()));
    }

    /**
     * Imprimeix el resum del dany rebut.
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
     * Imprimeix el resum de la regeneració.
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
     * Imprimeix les barres d'estat del personatge.
     */
    public void printStatusBars(Character character) {
        for (String line : statusLines(CombatantStatus.from(character))) {
            System.out.println(line);
        }
    }

    /**
     * Afegeix les barres d'estat a un text existent.
     */
    public void appendStatusBars(StringBuilder sb, Character character) {
        appendLines(sb, statusLines(CombatantStatus.from(character)));
    }

    /**
     * Retorna les línies d'estat d'una captura.
     */
    public List<String> statusLines(CombatantStatus status) {
        List<String> lines = new ArrayList<>();
        if (status == null) {
            return lines;
        }

        lines.add("  " + Ansi.DARK_GRAY + "─────────────" + Ansi.RESET);
        lines.add(String.format("Vida: %s %s / %s",
                buildBar(status.health(), status.maxHealth(), BAR_SIZE, healthColor(status.health(), status.maxHealth())),
                format(status.health()),
                format(status.maxHealth())));
        lines.add(String.format("Mana: %s %s / %s",
                buildBar(status.mana(), status.maxMana(), BAR_SIZE, Ansi.BRIGHT_BLUE),
                format(status.mana()),
                format(status.maxMana())));
        return lines;
    }

    /**
     * Retorna el color segons el percentatge de vida.
     */
    public static String healthColor(double current, double max) {
        if (max <= 0) {
            return Ansi.RED;
        }
        return ColorGradient.getColor(
                current / max,
                255, 0, 0,
                255, 220, 0,
                70, 190, 110
        );
    }

    /**
     * Construeix una barra visual proporcional.
     */
    public String buildBar(double current, double max, int size, String color) {
        if (max <= 0) {
            return "[ERROR]";
        }

        double overload = Math.max(0, current - max);
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
     */
    public String format(double value) {
        return String.format("%.2f", value);
    }

    private void appendLine(StringBuilder sb, String line) {
        if (line != null && !line.isBlank()) {
            sb.append(line).append("\n");
        }
    }

    private void appendLines(StringBuilder sb, List<String> lines) {
        for (String line : lines) {
            appendLine(sb, line);
        }
    }
}
