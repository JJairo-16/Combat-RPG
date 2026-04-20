package rpgcombat.debug.test;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import rpgcombat.creator.CharacterCreator;
import rpgcombat.models.characters.Character;
import rpgcombat.utils.rng.DivineCharismaAffinity;
import rpgcombat.utils.rng.SpiritualCallingDie;

public class SpiritualCallingDieTest {
    public static void main(String[] args) {
        Random rng = new Random(42);
        Character dummy = CharacterCreator.dummy();
        DivineCharismaAffinity.rollForRun(rng);
        
        test1(rng);

        System.out.println();
        test2(rng);

        System.out.println("\nDivine Affinity: " + DivineCharismaAffinity.classifyBand(dummy.getStatistics().getCharisma()));
    }

    private static void test1(Random rng) {
        Map<Integer, Integer> distribution = new HashMap<>();
        Character dummy = CharacterCreator.dummy();

        int count = 100000;
        for (int i = 0; i < count; i++) {
            int face = SpiritualCallingDie.rollFace(rng, dummy.getStatistics());
            distribution.put(face, distribution.getOrDefault(face, 0) + 1);
        }

        System.out.println("Face\tCount\tPercentage");
        for (int face = 1; face <= 20; face++) {
            int faceCount = distribution.getOrDefault(face, 0);
            double percentage = (faceCount / (double) count) * 100;
            System.out.printf("%d\t%d\t%.2f%%%n", face, faceCount, percentage);
        }
    }

    private static void test2(Random rng) {
        Map<NumberType, Integer> distribution = new EnumMap<>(NumberType.class);
        Character dummy = CharacterCreator.dummy();

        int count = 100000;
        for (int i = 0; i < count; i++) {
            int face = SpiritualCallingDie.rollFace(rng, dummy.getStatistics());
            NumberType type = classify(face);
            distribution.put(type, distribution.getOrDefault(type, 0) + 1);
        }

        System.out.println("Type\t\tCount\tPercentage");
        for (NumberType type : NumberType.values()) {
            int typeCount = distribution.getOrDefault(type, 0);
            double percentage = (typeCount / (double) count) * 100;
            System.out.printf("%-10s\t%d\t%.2f%%%n", type, typeCount, percentage);
        }
    }

    private static NumberType classify(int face) {
        if (face == 1) return NumberType.UNFORTUNATE;
        if (face <= 5) return NumberType.VERY_LOW;
        if (face <= 10) return NumberType.LOW;
        if (face <= 15) return NumberType.HIGH;
        if (face <= 19) return NumberType.VERY_HIGH;
        return NumberType.EXCEEDED;
    }

    private enum NumberType {
        UNFORTUNATE,
        VERY_LOW,
        LOW,
        HIGH,
        VERY_HIGH,
        EXCEEDED
    }
}