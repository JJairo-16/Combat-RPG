package rpgcombat.combat;

public record RegenResult(double healthRecovered, double manaRecovered) {
    public static final RegenResult ZERO = new RegenResult(0, 0);
}