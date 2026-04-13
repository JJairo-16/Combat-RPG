package rpgcombat.game.modifier.ui;

import rpgcombat.utils.cache.TextWrapCache;

public class Messages {
    private Messages() {}

    private static final int WIDTH = 78;

    private static String getWrapped(String msg) {
        StringBuilder sb = new StringBuilder();
        for (String line : TextWrapCache.wrap(msg, WIDTH)) {
            sb.append(line).append("\n");
        }

        return sb.toString();
    }

    public enum CALL_SPIRITS {
        CALL_IN_COOLDOWN("Els espirits t'ajudaram quan considerin que és el moment adequat; no intentis forçar la mà dels déus."),
        CALL_INIT("Els espirits et donen una oportunitat a l'atzar; sigui quin sigui el resultat, sigues-ne agraït."),
        UNFORTUNATE("Els esperits et concedeixen una ajuda gairebé simbòlica."),
        VERY_LOW("Una mica d'ajuda dels esperits per mantenir-te dret."),
        LOW("Generós suport dels espirits per continuar la lluita en el seu nom."),
        HIGH("Un gran impuls dels esperits per ajudar a assegurar la teva victòria."),
        VERY_HIGH("La més gran ajuda dels esperits, només per a tu, el favorit dels déus."),
        EXCEDED("Sense dubte, el preferit dels esperits.");

        private final String msg;

        private CALL_SPIRITS(String msg) {
            this.msg = getWrapped(msg);
        }

        public void print() {
            System.out.println(msg);
        }
    }
}
