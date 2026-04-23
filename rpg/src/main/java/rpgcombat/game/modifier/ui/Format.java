package rpgcombat.game.modifier.ui;

import rpgcombat.models.characters.Character;
import rpgcombat.utils.ui.Ansi;

/**
 * Classe utilitària per formatejar i mostrar informació visual per consola.
 * <p>
 * Inclou funcions per imprimir barres de vida i mana, així com utilitats
 * de format com arrodoniment de decimals.
 */
public class Format {

    /**
     * Constructor privat per evitar la instanciació de la classe.
     */
    private Format() {
    }

    /**
     * Mostra per consola les barres de vida i mana del personatge.
     * <p>
     * La vida es representa amb un color dinàmic segons el percentatge restant,
     * mentre que el mana es mostra en blau.
     *
     * @param player el personatge del qual es vol mostrar l'estat
     */
    public static void printBloodPactBars(Character player) {
        double currentHealth = player.getStatistics().getHealth();
        double maxHealth = player.getStatistics().getMaxHealth();
        double currentMana = player.getStatistics().getMana();
        double maxMana = player.getStatistics().getMaxMana();

        System.out.println("   " + Ansi.DARK_GRAY + "─────────────" + Ansi.RESET);

        System.out.printf("Vida: %s %.2f / %.2f%n",
                buildBar(currentHealth, maxHealth, 20, healthColor(currentHealth, maxHealth)),
                currentHealth,
                maxHealth);

        System.out.printf("Mana: %s %.2f / %.2f%n",
                buildBar(currentMana, maxMana, 20, Ansi.BRIGHT_BLUE),
                currentMana,
                maxMana);
    }

    /**
     * Determina el color de la barra de vida segons el percentatge actual.
     *
     * @param current vida actual
     * @param max vida màxima
     * @return codi ANSI del color corresponent (vermell, groc o verd)
     */
    private static String healthColor(double current, double max) {
        double ratio = max <= 0 ? 0 : current / max;

        if (ratio <= 0.25)
            return Ansi.BRIGHT_RED;
        if (ratio <= 0.60)
            return Ansi.YELLOW;
        return Ansi.GREEN;
    }

    /**
     * Construeix una barra visual proporcional a un valor actual respecte a un màxim.
     *
     * @param current valor actual
     * @param max valor màxim
     * @param size longitud total de la barra
     * @param color color ANSI per a la part omplerta
     * @return cadena que representa la barra visual
     */
    private static String buildBar(double current, double max, int size, String color) {
        if (max <= 0) {
            return "[ERROR]";
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
        bar.append("]");
        return bar.toString();
    }

    /**
     * Arrodoneix un nombre decimal a dues xifres.
     *
     * @param n el valor a arrodonir
     * @return el valor arrodonit a dues decimals
     */
    public static double round2(double n) {
        return Math.round(n * 100.0) / 100.0;
    }
}