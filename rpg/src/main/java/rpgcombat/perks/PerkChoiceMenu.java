package rpgcombat.perks;

import java.util.List;

import rpgcombat.models.characters.Character;
import rpgcombat.utils.input.Menu;

/** Menú de selecció de perk. */
public final class PerkChoiceMenu {
    private PerkChoiceMenu() {}

    public static PerkDefinition choose(Character player, List<PerkDefinition> options) {
        if (options == null || options.isEmpty()) return null;
        int option = Menu.getOption(
                options.stream().map(PerkDefinition::menuLine).toList(),
                "Recompensa de missió — " + player.getName());
        return options.get(option - 1);
    }
}
