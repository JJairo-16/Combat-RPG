package rpgcombat.combat.ui.messages;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Acumula missatges de combat amb estil explícit.
 */
public final class CombatMessageBuffer {
    private final List<CombatMessage> messages = new ArrayList<>();
    private final List<String> legacyView = new LegacyView();
    private final CombatMessagePhase defaultPhase;

    /**
     * Crea un buffer amb fase de creuament per defecte.
     */
    public CombatMessageBuffer() {
        this(CombatMessagePhase.DURING_CROSS);
    }

    /**
     * Crea un buffer amb una fase visual per defecte.
     */
    public CombatMessageBuffer(CombatMessagePhase defaultPhase) {
        this.defaultPhase = defaultPhase == null ? CombatMessagePhase.DURING_CROSS : defaultPhase;
    }

    /**
     * Afegeix un missatge estructurat.
     */
    public void add(CombatMessage message) {
        if (message != null && !message.text().isBlank()) {
            messages.add(message.withPhase(defaultPhase));
        }
    }

    /**
     * Afegeix un missatge positiu.
     */
    public void positive(String text) {
        add(CombatMessage.positive(text));
    }

    /**
     * Afegeix un missatge negatiu.
     */
    public void negative(String text) {
        add(CombatMessage.negative(text));
    }

    /**
     * Afegeix un avís.
     */
    public void warning(String text) {
        add(CombatMessage.warning(text));
    }

    /**
     * Afegeix un missatge de caos. Caos es classifica com a mode de joc
     * i de moment es renderitza com a normal.
     */
    public void chaos(String text) {
        add(CombatMessage.chaos(text));
    }

    /**
     * Afegeix un missatge d'impacte.
     */
    public void hit(String text) {
        add(CombatMessage.hit(text));
    }

    /**
     * Afegeix un missatge amb color i símbol concrets.
     */
    public void styled(MessageColor color, MessageSymbol symbol, String text) {
        add(CombatMessage.of(symbol, color, text));
    }

    /**
     * Afegeix un missatge d'efecte d'estat al panell lateral.
     */
    public void statusEffect(MessageColor color, MessageSymbol symbol, String text) {
        add(CombatMessage.statusEffect(symbol, color, text));
    }


    /**
     * Afegeix un missatge de verí al panell lateral amb verd fosc.
     */
    public void poisonStatusEffect(MessageSymbol symbol, String text) {
        statusEffect(MessageColor.DARK_GREEN, symbol, text);
    }

    /**
     * Afegeix un missatge de passiva divina. De moment es renderitza com a normal.
     */
    public void divinePerk(MessageColor color, MessageSymbol symbol, String text) {
        add(CombatMessage.divinePerk(symbol, color, text));
    }

    /**
     * Afegeix un missatge de mode de joc. De moment es renderitza com a normal.
     */
    public void gamemode(MessageColor color, MessageSymbol symbol, String text) {
        add(CombatMessage.gamemode(symbol, color, text));
    }

    /**
     * Afegeix un missatge de mode de joc al panell lateral d'efectes.
     */
    public void gamemodeEffect(MessageColor color, MessageSymbol symbol, String text) {
        add(CombatMessage.gamemodeEffect(symbol, color, text));
    }

    /**
     * Retorna una vista per a APIs antigues.
     */
    public List<String> legacyView() {
        return legacyView;
    }

    /**
     * Retorna els missatges acumulats.
     */
    public List<CombatMessage> messages() {
        return Collections.unmodifiableList(messages);
    }

    private final class LegacyView extends AbstractList<String> {
        @Override
        public String get(int index) {
            return messages.get(index).text();
        }

        @Override
        public int size() {
            return messages.size();
        }

        @Override
        public boolean add(String text) {
            CombatMessageBuffer.this.add(CombatMessage.legacy(text));
            return true;
        }
    }
}
