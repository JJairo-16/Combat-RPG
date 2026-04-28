package rpgcombat.debug.test;

import java.nio.file.Path;
import java.util.List;
import java.util.Random;

import rpgcombat.perks.PerkChoiceMenu;
import rpgcombat.perks.PerkDefinition;
import rpgcombat.perks.PerkLoader;
import rpgcombat.perks.PerkRegistry;

/** Prova manual del menú de selecció de perks. */
public final class PerkChoiceMenuTest {
    private PerkChoiceMenuTest() {}

    /** Executa el menú sense iniciar cap combat. */
    public static void main(String[] args) throws Exception {
        Path perksPath = args.length > 0 ? Path.of(args[0]) : Path.of("rpg/data/perks.json");
        boolean corruptedOnly = true;

        PerkRegistry.initialize(PerkLoader.load(perksPath));
        List<PerkDefinition> options = PerkRegistry.rollOptions(corruptedOnly, 3, new Random());
        PerkDefinition chosen = PerkChoiceMenu.choose("Prova", options);

        if (chosen != null) {
            System.out.println("Perk seleccionada: " + chosen.name() + " (" + chosen.family() + ")");
        }
    }
}
