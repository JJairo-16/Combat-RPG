package rpgcombat.creator.editor;

import rpgcombat.creator.CharacterCreator;

import java.util.Arrays;

import rpgcombat.models.breeds.Breed;

/** Esborrany editable abans de crear el personatge real. */
public final class CharacterDraft {
    private String name;
    private int age;
    private Breed breed;
    private int[] stats;

    /** Crea un esborrany amb dades inicials. */
    private CharacterDraft(String name, int age, Breed breed, int[] stats) {
        this.name = name;
        this.age = age;
        this.breed = breed;
        this.stats = Arrays.copyOf(stats, stats.length);
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