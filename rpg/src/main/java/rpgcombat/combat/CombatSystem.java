package rpgcombat.combat;

import java.util.Random;

import rpgcombat.models.characters.Character;
import rpgcombat.models.characters.Statistics;
import rpgcombat.combat.turnservice.DefaultTurnPriorityPolicy;
import rpgcombat.combat.turnservice.TurnPriorityPolicy;
import rpgcombat.combat.turnservice.TurnResolver;
import rpgcombat.combat.turnservice.TurnResult;
import rpgcombat.combat.ui.CombatRenderer;

public class CombatSystem {
    private final Character player1;
    private final Character player2;

    private final Random combatRng = new Random();
    private final TurnPriorityPolicy priorityPolicy;

    private final CombatRenderer renderer = new CombatRenderer();
    private final AttackResolver attackResolver = new AttackResolver();
    private final EffectPipeline effectPipeline = new EffectPipeline();
    private final RoundRecoveryService recoveryService = new RoundRecoveryService();
    private final TurnResolver turnResolver = new TurnResolver(attackResolver, effectPipeline, recoveryService);

    public CombatSystem(Character p1, Character p2) {
        this(p1, p2, new DefaultTurnPriorityPolicy());
    }

    public CombatSystem(Character p1, Character p2, TurnPriorityPolicy policy) {
        this.player1 = p1;
        this.player2 = p2;
        this.priorityPolicy = policy;
    }

    public Winner play(Action a1, Action a2) {
        CombatRoundResult round = playRound(a1, a2);

        renderer.printRoundHeader();
        renderer.printTurnResult(round.firstTurn());
        System.out.println();
        renderer.printTurnResult(round.secondTurn());

        System.out.println();
        renderer.printRoundSummary(player1, round.p1DamageTaken());
        renderer.printRoundSummary(player2, round.p2DamageTaken());

        if (round.winner() != Winner.NONE) {
            return round.winner();
        }

        System.out.println();
        renderer.printRegenHeader();
        renderer.printRegenSummary(player1, round.p1Regen().healthRecovered(), round.p1Regen().manaRecovered());
        renderer.printRegenSummary(player2, round.p2Regen().healthRecovered(), round.p2Regen().manaRecovered());

        return Winner.NONE;
    }

    public CombatRoundResult playRound(Action a1, Action a2) {
        Statistics p1Stats = player1.getStatistics();
        Statistics p2Stats = player2.getStatistics();

        EndRoundRegenBonus p1Bonus = new EndRoundRegenBonus();
        EndRoundRegenBonus p2Bonus = new EndRoundRegenBonus();

        double p1HealthBefore = p1Stats.getHealth();
        double p2HealthBefore = p2Stats.getHealth();

        boolean p1First = priorityPolicy.player1First(player1, a1, player2, a2, combatRng);

        TurnResult firstTurn;
        TurnResult secondTurn;

        if (p1First) {
            firstTurn = turnResolver.resolveTurn(player1, player2, a1, a2, p2Bonus);
            secondTurn = turnResolver.resolveTurn(player2, player1, a2, a1, p1Bonus);
        } else {
            firstTurn = turnResolver.resolveTurn(player2, player1, a2, a1, p1Bonus);
            secondTurn = turnResolver.resolveTurn(player1, player2, a1, a2, p2Bonus);
        }

        double p1HealthAfterAttacks = p1Stats.getHealth();
        double p2HealthAfterAttacks = p2Stats.getHealth();

        double p1DamageTaken = p1HealthBefore - p1HealthAfterAttacks;
        double p2DamageTaken = p2HealthBefore - p2HealthAfterAttacks;

        Winner winner = resolveWinner(player1, player2);
        if (winner != Winner.NONE) {
            return new CombatRoundResult(
                    firstTurn,
                    secondTurn,
                    p1DamageTaken,
                    p2DamageTaken,
                    RegenResult.ZERO,
                    RegenResult.ZERO,
                    winner);
        }

        double p1HealthPreRegen = p1Stats.getHealth();
        double p1ManaPreRegen = p1Stats.getMana();

        double p2HealthPreRegen = p2Stats.getHealth();
        double p2ManaPreRegen = p2Stats.getMana();

        player1.regen();
        player2.regen();

        recoveryService.applyEndRoundBonus(player1, p1Bonus);
        recoveryService.applyEndRoundBonus(player2, p2Bonus);

        RegenResult p1Regen = new RegenResult(
                p1Stats.getHealth() - p1HealthPreRegen,
                p1Stats.getMana() - p1ManaPreRegen);

        RegenResult p2Regen = new RegenResult(
                p2Stats.getHealth() - p2HealthPreRegen,
                p2Stats.getMana() - p2ManaPreRegen);

        return new CombatRoundResult(
                firstTurn,
                secondTurn,
                p1DamageTaken,
                p2DamageTaken,
                p1Regen,
                p2Regen,
                Winner.NONE);
    }

    private Winner resolveWinner(Character p1, Character p2) {
        boolean player1Alive = p1.isAlive();
        boolean player2Alive = p2.isAlive();

        if (player1Alive && player2Alive) {
            return Winner.NONE;
        }
        if (player1Alive) {
            return Winner.PLAYER1;
        }
        if (player2Alive) {
            return Winner.PLAYER2;
        }
        return Winner.TIE;
    }

    // Compatibilidad con código existente como GameLoop
    public static void printStatusBars(Character character) {
        new CombatRenderer().printStatusBars(character);
    }

    public static void appendStatusBars(StringBuilder sb, Character character) {
        new CombatRenderer().appendStatusBars(sb, character);
    }
}