package rpgcombat.game.modifier.ultimate;

import rpgcombat.models.breeds.Breed;
import rpgcombat.models.characters.Character;
import rpgcombat.models.characters.Statistics;
import rpgcombat.models.effects.Effect;
import rpgcombat.models.effects.EffectState;
import rpgcombat.models.effects.MenuTurnEffect;
import rpgcombat.weapons.Weapon;

/**
 * Efecte intern persistent que controla l'aparició, el cooldown i l'ús únic
 * d'una habilitat definitiva durant un combat.
 *
 * <p>Aquest efecte actua com una marca interna associada a un personatge. No
 * expira mai per si mateix i s'encarrega de validar si una definitiva es pot
 * activar, aplicar-ne els costos i impedir que s'utilitzi més d'una vegada per
 * combat.</p>
 */
public final class UltimateActionEffect implements Effect, MenuTurnEffect {

    /**
     * Nombre inicial de torns de cooldown abans que la definitiva estigui
     * disponible.
     */
    public static final int INITIAL_COOLDOWN_TURNS = 6;

    /**
     * Cooldown artificialment alt aplicat després d'utilitzar la definitiva per
     * evitar que es pugui tornar a activar durant el mateix combat.
     */
    public static final int USED_COOLDOWN_TURNS = 999;

    private final UltimateActionType type;
    private final EffectState state = new EffectState(0, 1, 1, INITIAL_COOLDOWN_TURNS);
    private boolean usedThisCombat;
    private boolean usedThisTurn;

    /**
     * Crea un efecte intern per controlar una definitiva concreta.
     *
     * @param type tipus de definitiva associada a aquest efecte
     */
    public UltimateActionEffect(UltimateActionType type) {
        this.type = type;
    }

    /**
     * Retorna el tipus de definitiva associada a aquest efecte.
     *
     * @return tipus de definitiva controlada
     */
    public UltimateActionType type() {
        return type;
    }

    /**
     * Retorna la clau única de l'efecte, derivada del tipus de definitiva.
     *
     * @return clau identificadora de l'efecte
     */
    @Override
    public String key() {
        return type.effectKey();
    }

    /**
     * Retorna l'estat intern de l'efecte, incloent-hi el cooldown actual.
     *
     * @return estat de l'efecte
     */
    @Override
    public EffectState state() {
        return state;
    }

    /**
     * Indica si l'efecte ha expirat.
     *
     * <p>Aquest efecte és persistent durant el combat, de manera que mai no
     * expira automàticament.</p>
     *
     * @return {@code false} sempre
     */
    @Override
    public boolean isExpired() {
        return false;
    }

    /**
     * Actualitza l'efecte al final d'un torn de menú.
     *
     * <p>Redueix el cooldown i reinicia la marca d'ús durant el torn actual.</p>
     *
     * @param owner personatge propietari de l'efecte
     */
    @Override
    public void onMenuTurnEnd(Character owner) {
        state.tickCooldown();
        usedThisTurn = false;
    }

    /**
     * Indica si aquesta definitiva ja s'ha utilitzat durant el combat actual.
     *
     * @return {@code true} si ja s'ha utilitzat en aquest combat;
     *         {@code false} en cas contrari
     */
    public boolean usedThisCombat() {
        return usedThisCombat;
    }

    /**
     * Indica si aquesta definitiva ja s'ha utilitzat durant el torn actual.
     *
     * @return {@code true} si ja s'ha utilitzat aquest torn;
     *         {@code false} en cas contrari
     */
    public boolean usedThisTurn() {
        return usedThisTurn;
    }

    /**
     * Comprova si el propietari pot activar la definitiva associada a aquest
     * efecte.
     *
     * @param owner personatge que intenta activar la definitiva
     * @return {@code true} si es pot activar; {@code false} en cas contrari
     */
    public boolean canActivate(Character owner) {
        return canActivate(owner, type);
    }

    /**
     * Comprova si un personatge pot activar una definitiva concreta.
     *
     * <p>La validació comprova, entre altres condicions, que existeixi l'efecte
     * intern corresponent, que la definitiva no s'hagi utilitzat encara, que no
     * estigui en cooldown, que el personatge tingui un atac carregat disponible,
     * que l'acció estigui desbloquejada, que porti l'arma adequada i que compleixi
     * els requisits específics del tipus de definitiva.</p>
     *
     * @param owner personatge que intenta activar la definitiva
     * @param type tipus de definitiva que es vol activar
     * @return {@code true} si la definitiva es pot activar; {@code false} en cas contrari
     */
    public static boolean canActivate(Character owner, UltimateActionType type) {
        UltimateActionEffect flag = find(owner, type);
        if (owner == null || type == null || flag == null) {
            return false;
        }
        if (flag.usedThisCombat || flag.usedThisTurn || flag.state.onCooldown()) {
            return false;
        }
        if (owner.hasSpecialMenuActionUsedThisTurn() || anyUltimateUsedThisCombat(owner)) {
            return false;
        }
        if (!owner.hasChargedAttack() || UltimateChargeBoost.isArmed(owner)) {
            return false;
        }
        if (!UltimateActionUnlocks.isUnlocked(type)) {
            return false;
        }

        Weapon weapon = owner.getWeapon();
        if (weapon == null || weapon.getType() != type.weaponType()) {
            return false;
        }

        Statistics stats = owner.getStatistics();
        return switch (type) {
            case ARCANE_OVERLOAD -> manaRatio(stats) >= 0.75;
            case COLOSSAL_BREAK -> true;
            case ELVEN_OPENING_SHOT -> owner.getBreed() == Breed.ELF && stats.getDexterity() >= 25;
        };
    }

    /**
     * Activa una definitiva concreta per a un personatge.
     *
     * <p>Si l'activació és vàlida, consumeix l'atac carregat, aplica els costos
     * corresponents, afegeix l'efecte {@link UltimateChargeBoost}, marca l'acció
     * especial de menú com a utilitzada i bloqueja futures activacions durant el
     * mateix combat.</p>
     *
     * @param owner personatge que activa la definitiva
     * @param type tipus de definitiva que es vol activar
     * @return {@code true} si l'activació s'ha completat correctament;
     *         {@code false} si no es pot activar o no es pot consumir l'atac carregat
     */
    public static boolean activate(Character owner, UltimateActionType type) {
        UltimateActionEffect flag = find(owner, type);
        if (flag == null || !flag.canActivate(owner)) {
            return false;
        }
        if (!owner.consumeChargedAttack()) {
            return false;
        }

        applyActivationCosts(owner, type);
        owner.addInternalEffect(new UltimateChargeBoost(type));
        owner.markSpecialMenuActionUsedThisTurn();

        flag.usedThisCombat = true;
        flag.usedThisTurn = true;
        flag.state.setCooldown(USED_COOLDOWN_TURNS);
        return true;
    }

    /**
     * Cerca l'efecte intern d'una definitiva concreta entre els efectes d'un
     * personatge.
     *
     * @param owner personatge on es busca l'efecte
     * @param type tipus de definitiva associada a l'efecte buscat
     * @return l'efecte trobat, o {@code null} si no existeix
     */
    public static UltimateActionEffect find(Character owner, UltimateActionType type) {
        if (owner == null || type == null) {
            return null;
        }
        for (Effect effect : owner.getEffects()) {
            if (effect instanceof UltimateActionEffect ultimate && ultimate.type == type) {
                return ultimate;
            }
        }
        return null;
    }

    /**
     * Indica si qualsevol definitiva del personatge ja s'ha utilitzat durant el
     * combat actual.
     *
     * @param owner personatge a comprovar
     * @return {@code true} si alguna definitiva ja s'ha utilitzat en aquest combat;
     *         {@code false} en cas contrari
     */
    public static boolean anyUltimateUsedThisCombat(Character owner) {
        if (owner == null) {
            return false;
        }
        for (Effect effect : owner.getEffects()) {
            if (effect instanceof UltimateActionEffect ultimate && ultimate.usedThisCombat()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Indica si qualsevol definitiva del personatge ja s'ha utilitzat durant el
     * torn actual.
     *
     * @param owner personatge a comprovar
     * @return {@code true} si alguna definitiva ja s'ha utilitzat aquest torn;
     *         {@code false} en cas contrari
     */
    public static boolean anyUltimateUsedThisTurn(Character owner) {
        if (owner == null) {
            return false;
        }
        for (Effect effect : owner.getEffects()) {
            if (effect instanceof UltimateActionEffect ultimate && ultimate.usedThisTurn()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Aplica els costos d'activació d'una definitiva sobre les estadístiques del
     * personatge.
     *
     * <p>Els costos es calculen com una proporció dels valors màxims de mana,
     * stamina i resistència definits pel tipus de definitiva.</p>
     *
     * @param owner personatge que paga els costos d'activació
     * @param type tipus de definitiva activada
     */
    private static void applyActivationCosts(Character owner, UltimateActionType type) {
        Statistics stats = owner.getStatistics();
        if (type.manaCostRatio() > 0) {
            stats.consumeMana(stats.getMaxMana() * type.manaCostRatio());
        }
        if (type.staminaCostRatio() > 0) {
            stats.consumeStamina(stats.getMaxStamina() * type.staminaCostRatio());
        }
        if (type.resistanceCostRatio() > 0) {
            stats.consumeResistance(stats.getMaxResistance() * type.resistanceCostRatio());
        }
    }

    /**
     * Calcula la proporció de mana actual respecte al mana màxim.
     *
     * @param stats estadístiques del personatge
     * @return valor entre {@code 0.0} i {@code 1.0} que representa la proporció
     *         de mana disponible, o {@code 0.0} si les estadístiques són nul·les
     *         o el mana màxim no és positiu
     */
    private static double manaRatio(Statistics stats) {
        if (stats == null || stats.getMaxMana() <= 0) {
            return 0.0;
        }
        return Math.clamp(stats.getMana() / stats.getMaxMana(), 0.0, 1.0);
    }
}