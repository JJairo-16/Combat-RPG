package rpgcombat.utils.ui;

public class ColorGradient {
    private ColorGradient() {
    }

    public static String getColor(double percentage) {
        percentage = Math.clamp(percentage, 0.0, 1.0);
        int r = 0;
        int g = 0;
        int b = 0;

        if (percentage < 0.5) {
            // Vermell → Taronja
            double t = percentage / 0.5; // normalitzar 0-1
            r = 255;
            g = (int) (165 * t);
        } else {
            // Taronja → Verd
            double t = (percentage - 0.5) / 0.5;
            r = (int) (255 * (1 - t));
            g = (int) (165 + (255 - 165) * t);
        }

        return String.format("\u001B[38;2;%d;%d;%dm", r, g, b);
    }

    public static final String RESET = "\u001B[0m";

    // public static void main(String[] args) throws InterruptedException {

    //     int width = 30;

    //     while (true) {
    //         for (int i = 0; i <= width; i++) {
    //             double percentage = i / (double) width;

    //             String color = ColorGradient.getColor(percentage);

    //             String bar = "[" +
    //                     "#".repeat(i) +
    //                     " ".repeat(width - i) +
    //                     "]";

    //             System.out.print("\r" + color + bar + ColorGradient.RESET +
    //                     String.format(" %.2f%%", percentage * 100));

    //             Thread.sleep(50);
    //         }

    //         for (int i = width; i >= 0; i--) {
    //             double percentage = i / (double) width;

    //             String color = ColorGradient.getColor(percentage);

    //             String bar = "[" +
    //                     "#".repeat(i) +
    //                     " ".repeat(width - i) +
    //                     "]";

    //             System.out.print("\r" + color + bar + ColorGradient.RESET +
    //                     String.format(" %.2f%%", percentage * 100));

    //             Thread.sleep(50);
    //         }
    //     }
    // }

}