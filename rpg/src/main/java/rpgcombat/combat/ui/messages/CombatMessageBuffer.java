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

    /**
     * Afegeix un missatge estructurat.
     */
    public void add(CombatMessage message) {
        if (message != null && !message.text().isBlank()) {
            messages.add(message);
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
     * Afegeix un missatge de caos.
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
