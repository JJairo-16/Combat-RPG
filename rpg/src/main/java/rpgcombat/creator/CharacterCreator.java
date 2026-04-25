package rpgcombat.creator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import rpgcombat.models.breeds.*;
import rpgcombat.models.characters.Character;
import rpgcombat.models.effects.impl.SpiritualCallingFlag;
import rpgcombat.models.effects.triggers.FractureTrigger;
import rpgcombat.utils.input.Menu;
import rpgcombat.utils.rng.StatsBudget;
import rpgcombat.utils.rng.StatsBudget.Result;
import rpgcombat.utils.rng.StatsBudget.ScaledLimits;
import rpgcombat.utils.ui.Ansi;

/**
 * Gestor de creació de personatges.
 */
public class CharacterCreator {

    static final int MIN_NAME_LEN = 3;
    static final int MAX_NAME_LEN = 20;
    static final int MIN_AGE = 12;
    static final int MAX_AGE = Integer.MAX_VALUE;
    static final int TOTAL_POINTS = 140;

    private static final ScaledLimits limits = StatsBudget.scaleLimits(TOTAL_POINTS);
    static final int MIN_STAT = limits.minValue();
    static final int MAX_STAT = limits.maxValue();
    static final int MIN_CONSTITUTION = MIN_STAT + 2;

    private static int id = 1;

    private CharacterCreator() {
    }

    /**
     * Crea un personatge amb un formulari interactiu de terminal.
     *
     * @return personatge creat
     */
    public static Character createNewCharacter() {
        CharacterDraft draft = CharacterDraft.from("Aventurer", MIN_AGE, autoGenerate());
        new CharacterCreationEditor().edit(draft);
        return convert(draft.name(), draft.age(), new Generation(draft.statsCopy(), draft.breed()));
    }

    /**
     * Crea un personatge de depuració amb generació automàtica.
     *
     * @return personatge generat per a proves
     */
    public static Character createDebugCharacter() {
        String name = "test" + id++;
        return convert(name, MIN_AGE, autoGenerate());
    }

    /** Resultat de la generació d'estadístiques i raça. */
    static record Generation(int[] stats, Breed breed) {
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (o instanceof Generation(int[] otherStats, Breed otherBreed)) {
                return Arrays.equals(stats, otherStats) && breed == otherBreed;
            }

            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(Arrays.hashCode(stats), breed);
        }

        @Override
        public String toString() {
            return "Generation{" +
                    "stats=" + Arrays.toString(stats) +
                    ", breed=" + breed +
                    '}';
        }
    }

    /** Genera estadístiques i raça automàticament. */
    static Generation autoGenerate() {
        Result res = StatsBudget.generate(TOTAL_POINTS);
        return new Generation(res.baseStats(), res.breed());
    }

    /** Mostra un resum visual de raça, bonus i estadístiques. */
    static void printSummary(Generation gen) {
        int[] stats = gen.stats();
        Breed breed = gen.breed();
        StringBuilder sb = new StringBuilder(512);

        sb.append('\n');
        sb.append(" ")
                .append(Ansi.WHITE).append(Ansi.BOLD).append("Raça: ").append(Ansi.RESET)
                .append(Ansi.CYAN).append(Ansi.BOLD).append(breed.getName()).append(Ansi.RESET)
                .append('\n');

        String desc = (breed.getDescription() == null) ? "" : breed.getDescription().trim();
        if (!desc.isEmpty()) {
            for (String line : wrap(desc, 78)) {
                sb.append("   ").append(Ansi.DARK_GRAY).append(line).append(Ansi.RESET).append('\n');
            }
        }

        int bonusPct = (int) Math.round(breed.bonus() * 100.0);
        sb.append("   ")
                .append(Ansi.GREEN).append("Bonus: ").append(Ansi.RESET)
                .append(Ansi.BOLD).append("+").append(bonusPct).append("%").append(Ansi.RESET)
                .append(Ansi.DARK_GRAY).append(" a ").append(Ansi.RESET)
                .append(Ansi.BOLD).append(breed.bonusStat().getName()).append(Ansi.RESET)
                .append('\n');

        sb.append(" ").append(Ansi.DARK_GRAY)
                .append("────────────────────────────────────────────────────────")
                .append(Ansi.RESET).append('\n');

        sb.append("   ")
                .append(statChip("Força", stats[0])).append("   ")
                .append(statChip("Destresa", stats[1])).append("   ")
                .append(statChip("Constitució", stats[2])).append("   ")
                .append(statChip("Intel·ligència", stats[3])).append('\n');

        sb.append("   ")
                .append(statChip("Saviesa", stats[4])).append("   ")
                .append(statChip("Carisma", stats[5])).append("   ")
                .append(statChip("Sort", stats[6])).append('\n');

        sb.append(" ").append(Ansi.DARK_GRAY)
                .append("────────────────────────────────────────────────────────")
                .append(Ansi.RESET).append('\n');

        System.out.print(sb.toString());
        Menu.pause();
    }

    /** Formata una estadística per al resum. */
    private static String statChip(String label, int value) {
        return Ansi.DARK_GRAY + label + ":" + Ansi.RESET + " " + Ansi.BOLD + value + Ansi.RESET;
    }

    private static final Pattern WRAP_PATTERN = Pattern.compile("\\s+");

    /** Divideix un text en línies de mida limitada. */
    private static List<String> wrap(String text, int maxWidth) {
        text = (text == null) ? "" : text.trim();

        if (text.isEmpty()) {
            return List.of();
        }

        if (text.length() <= maxWidth) {
            return List.of(text);
        }

        ArrayList<String> lines = new ArrayList<>();
        String[] words = WRAP_PATTERN.split(text);
        StringBuilder line = new StringBuilder();

        for (String word : words) {
            if (line.isEmpty()) {
                line.append(word);
            } else if (line.length() + 1 + word.length() <= maxWidth) {
                line.append(' ').append(word);
            } else {
                lines.add(line.toString());
                line.setLength(0);
                line.append(word);
            }
        }

        if (!line.isEmpty()) {
            lines.add(line.toString());
        }

        return lines;
    }

    /** Converteix les dades en la classe concreta de personatge. */
    private static Character convert(String name, int age, Generation g) {
        Breed b = g.breed();
        int[] stats = g.stats();

        Character character = switch (b) {
            case ORC -> new Orc(name, age, stats);
            case ELF -> new Elf(name, age, stats);
            case DWARF -> new Dwarf(name, age, stats);
            case GNOME -> new Gnome(name, age, stats);
            case TIEFLING -> new Tiefling(name, age, stats);
            case HALFLING -> new Halfling(name, age, stats);
            default -> new Character(name, age, stats, b);
        };

        addTriggers(character);
        return character;
    }

    /** Crea un personatge de prova equilibrat. */
    public static Character dummy() {
        return convert("Dummy", MIN_AGE, new Generation(getDummyStats(), Breed.ORC));
    }

    /** Genera estadístiques equilibrades per al personatge de prova. */
    public static int[] getDummyStats() {
        int base = TOTAL_POINTS / 7;
        int remainder = TOTAL_POINTS % 7;
        int[] stats = new int[7];
        Arrays.fill(stats, base);

        for (int i = 0; i < remainder; i++) {
            stats[i]++;
        }

        return stats;
    }

    /** Afegeix efectes passius inicials. */
    private static void addTriggers(Character character) {
        character.addEffect(new SpiritualCallingFlag());
        character.addEffect(new FractureTrigger());
    }
}
