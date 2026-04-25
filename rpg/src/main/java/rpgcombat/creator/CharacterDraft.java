package rpgcombat.creator;

import java.util.Arrays;

import rpgcombat.models.breeds.Breed;

/** Esborrany editable abans de crear el personatge real. */
final class CharacterDraft {
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
    static CharacterDraft from(String name, int age, CharacterCreator.Generation generation) {
        return new CharacterDraft(name, age, generation.breed(), generation.stats());
    }

    /** Retorna el nom. */
    String name() {
        return name;
    }

    /** Actualitza el nom. */
    void setName(String name) {
        this.name = name;
    }

    /** Retorna l'edat. */
    int age() {
        return age;
    }

    /** Actualitza l'edat. */
    void setAge(int age) {
        this.age = age;
    }

    /** Retorna la raça. */
    Breed breed() {
        return breed;
    }

    /** Actualitza la raça. */
    void setBreed(Breed breed) {
        this.breed = breed;
    }

    /** Retorna una estadística per índex. */
    int stat(int index) {
        return stats[index];
    }

    /** Actualitza una estadística. */
    void setStat(int index, int value) {
        stats[index] = value;
    }

    /** Substitueix raça i estadístiques per una nova generació. */
    void replaceGeneration(CharacterCreator.Generation generation) {
        breed = generation.breed();
        stats = Arrays.copyOf(generation.stats(), generation.stats().length);
    }

    /** Retorna la suma de les estadístiques. */
    int totalStats() {
        return Arrays.stream(stats).sum();
    }

    /** Retorna els punts encara disponibles. */
    int remainingPoints() {
        return CharacterCreator.TOTAL_POINTS - totalStats();
    }

    /** Retorna una còpia de les estadístiques. */
    int[] statsCopy() {
        return Arrays.copyOf(stats, stats.length);
    }
}