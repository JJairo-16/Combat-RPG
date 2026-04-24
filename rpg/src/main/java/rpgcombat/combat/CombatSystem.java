package rpgcombat.combat;

import java.util.Random;

import rpgcombat.models.characters.Character;
import rpgcombat.models.characters.Statistics;
import rpgcombat.balance.CombatBalanceRegistry;
import rpgcombat.balance.config.AntiStallConfig;
import rpgcombat.balance.config.CombatBalanceConfig;
import rpgcombat.balance.config.character.BloodPactConfig;
import rpgcombat.combat.models.Action;
import rpgcombat.combat.models.CombatRoundResult;
import rpgcombat.combat.models.EffectPipeline;
import rpgcombat.combat.models.RegenResult;
import rpgcombat.combat.models.Winner;
import rpgcombat.combat.services.EndRoundRegenBonus;
import rpgcombat.combat.services.RoundRecoveryService;
import rpgcombat.combat.turnservice.DefaultTurnPriorityPolicy;
import rpgcombat.combat.turnservice.TurnPriorityPolicy;
import rpgcombat.combat.turnservice.TurnResolver;
import rpgcombat.combat.turnservice.TurnResult;
import rpgcombat.combat.ui.CombatRenderer;
import rpgcombat.models.effects.impl.MagicalTiredness;
import rpgcombat.models.effects.impl.SuddenDeathPoisonEffect;

/**
 * Coordina una ronda de combat entre dos personatges.
 */
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

    private CombatBalanceConfig balance = CombatBalanceRegistry.get();
    private AntiStallConfig antiStall = balance.antiStall();
    private BloodPactConfig bloodPactConfig = balance.bloodPact();

    private int roundNumber = 0;

    /**
     * Crea el sistema amb la política de prioritat per defecte.
     *
     * @param p1 primer personatge
     * @param p2 segon personatge
     */
    public CombatSystem(Character p1, Character p2) {
        this(p1, p2, new DefaultTurnPriorityPolicy());
    }

    /**
     * Crea el sistema amb una política de prioritat concreta.
     *
     * @param p1     primer personatge
     * @param p2     segon personatge
     * @param policy política de prioritat
     */
    public CombatSystem(Character p1, Character p2, TurnPriorityPolicy policy) {
        this.player1 = p1;
        this.player2 = p2;
        this.priorityPolicy = policy;
    }

    /**
     * Executa una ronda, la mostra i retorna el guanyador.
     *
     * @param a1 acció del primer personatge
     * @param a2 acció del segon personatge
     * @return guanyador de la ronda o cap
     */
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

    /**
     * Resol una ronda completa de combat.
     *
     * @param a1 acció del primer personatge
     * @param a2 acció del segon personatge
     * @return resultat de la ronda
     */
    public CombatRoundResult playRound(Action a1, Action a2) {
        roundNumber++;
        applySuddenDeathPoisonIfNeeded();

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

        player1.applyInvulnerability();
        player2.applyInvulnerability();

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

        player1.tickSpiritualCallingCooldown();
        player2.tickSpiritualCallingCooldown();

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

        applyOrRemoveMagicalTiredness(player1);
        applyOrRemoveMagicalTiredness(player2);

        return new CombatRoundResult(
                firstTurn,
                secondTurn,
                p1DamageTaken,
                p2DamageTaken,
                p1Regen,
                p2Regen,
                Winner.NONE);
    }

    /**
     * Aplica o elimina l'efecte de {@link MagicalTiredness} en funció del percentatge de mana actual.
     * <p>
     * Si el mana del personatge és igual o inferior al 90% del màxim, s'aplica l'efecte de fatiga màgica. En cas contrari, l'efecte s'elimina.
     *
     * @param player el personatge sobre el qual s'avalua i s'aplica l'efecte
     */
    private void applyOrRemoveMagicalTiredness(Character player) {
        Statistics stats = player.getStatistics();

        boolean belowThreshold = (stats.getMana() / stats.getMaxMana()) <= bloodPactConfig.manaThreshold();

        if (belowThreshold) {
            if (!player.hasEffect(MagicalTiredness.INTERNAL_EFFECT_KEY)) {
                player.addEffect(new MagicalTiredness());
            }
        } else {
            player.removeEffect(MagicalTiredness.INTERNAL_EFFECT_KEY);
        }
    }

    /**
     * Aplica o elimina l'efecte de {@link MagicalTiredness} en funció del percentatge de mana actual.
     * <p>
     * Si el mana del personatge és igual o inferior al 90% del màxim, s'aplica l'efecte de fatiga màgica. En cas contrari, l'efecte s'elimina.
     *
     * @param player el personatge sobre el qual s'avalua i s'aplica l'efecte
     */
    public void syncEffectsOnly() {
        applyOrRemoveMagicalTiredness(player1);
        applyOrRemoveMagicalTiredness(player2);
    }

    /**
     * Determina el guanyador segons qui continua viu.
     */
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

    /**
     * Imprimeix les barres d'estat d'un personatge.
     *
     * @param character personatge a mostrar
     */
    public static void printStatusBars(Character character) {
        new CombatRenderer().printStatusBars(character);
    }

    /**
     * Afegeix les barres d'estat a un text.
     *
     * @param sb        destí del text
     * @param character personatge a mostrar
     */
    public static void appendStatusBars(StringBuilder sb, Character character) {
        new CombatRenderer().appendStatusBars(sb, character);
    }

    /**
     * Aplica el verí de mort sobtada si correspon.
     */
    private void applySuddenDeathPoisonIfNeeded() {
        if (roundNumber < antiStall.startTurn()) {
            return;
        }

        double poisonDamage = resolveSuddenDeathDamage(roundNumber);
        replaceSuddenDeathPoison(player1, poisonDamage);
        replaceSuddenDeathPoison(player2, poisonDamage);
    }

    /**
     * Calcula el dany del verí de mort sobtada.
     */
    private double resolveSuddenDeathDamage(int roundNumber) {
        int increments = (roundNumber - antiStall.startTurn()) / antiStall.increaseEveryTurns();
        return antiStall.initialDamage() + increments * antiStall.damageIncreasePerStep();
    }

    /**
     * Substitueix el verí de mort sobtada del personatge.
     */
    private void replaceSuddenDeathPoison(Character character, double damagePerTurn) {
        character.removeEffect(SuddenDeathPoisonEffect.INTERNAL_EFFECT_KEY);
        character.addEffect(new SuddenDeathPoisonEffect(damagePerTurn));
    }
}