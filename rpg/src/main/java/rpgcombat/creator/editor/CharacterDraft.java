package rpgcombat.creator.editor;

import rpgcombat.creator.CharacterCreator;

import java.util.Arrays;

import rpgcombat.models.breeds.Breed;
import rpgcombat.perks.divine.DivinePerkDefinition;
import rpgcombat.perks.divine.DivinePerkRegistry;

/** Esborrany editable abans de crear el personatge real. */
public final class CharacterDraft {
    private String name;
    private int age;
    private Breed breed;
    private int[] stats;
    private String divinePerkId;

    /** Crea un esborrany amb dades inicials. */
    private CharacterDraft(String name, int age, Breed breed, int[] stats) {
        this.name = name;
        this.age = age;
        this.breed = breed;
        this.stats = Arrays.copyOf(stats, stats.length);
        syncDivinePerkWithBreed();
    }

    /** Crea un esborrany a partir d'una generació. */
    public static CharacterDraft from(String name, int age, CharacterCreator.Generation generation) {
        return new CharacterDraft(name, age, generation.breed(), generation.stats());
    }

    /** Retorna el nom. */
    public String name() {
        return name;
    }

    /** Actualitza el nom. */
    public void setName(String name) {
        this.name = name;
    }

    /** Retorna l'edat. */
    public int age() {
        return age;
    }

    /** Actualitza l'edat. */
    public void setAge(int age) {
        this.age = age;
    }

    /** Retorna la raça. */
    public Breed breed() {
        return breed;
    }

    /** Actualitza la raça. */
    public void setBreed(Breed breed) {
        this.breed = breed;
        syncDivinePerkWithBreed();
    }

    /** Retorna la perk divina seleccionada. */
    public DivinePerkDefinition divinePerk() {
        DivinePerkDefinition selected = DivinePerkRegistry.find(divinePerkId).orElse(null);
        if (selected != null && selected.isAllowedFor(breed)) return selected;
        return DivinePerkRegistry.defaultFor(breed);
    }

    /** Retorna l'identificador de la perk divina seleccionada. */
    public String divinePerkId() {
        DivinePerkDefinition selected = divinePerk();
        return selected == null ? null : selected.id();
    }

    /** Selecciona una perk divina si és vàlida per a la raça actual. */
    public void setDivinePerk(DivinePerkDefinition perk) {
        if (perk != null && perk.isAllowedFor(breed)) {
            divinePerkId = perk.id();
        }
    }

    /** Manté una perk divina compatible amb la raça seleccionada. */
    private void syncDivinePerkWithBreed() {
        DivinePerkDefinition selected = DivinePerkRegistry.find(divinePerkId).orElse(null);
        if (selected != null && selected.isAllowedFor(breed)) return;
        DivinePerkDefinition fallback = DivinePerkRegistry.defaultFor(breed);
        divinePerkId = fallback == null ? null : fallback.id();
    }

    /** Retorna una estadística per índex. */
    public int stat(int index) {
        return stats[index];
    }

    /** Actualitza una estadística. */
    public void setStat(int index, int value) {
        stats[index] = value;
    }

    /** Substitueix raça i estadístiques per una nova generació. */
    public void replaceGeneration(CharacterCreator.Generation generation) {
        breed = generation.breed();
        stats = Arrays.copyOf(generation.stats(), generation.stats().length);
        syncDivinePerkWithBreed();
    }

    /** Retorna la suma de les estadístiques. */
    public int totalStats() {
        return Arrays.stream(stats).sum();
    }

    /** Retorna els punts encara disponibles. */
    public int remainingPoints() {
        return CharacterCreator.TOTAL_POINTS - totalStats();
    }

    /** Retorna una còpia de les estadístiques. */
    public int[] statsCopy() {
        return Arrays.copyOf(stats, stats.length);
    }
}