package rpgcombat.utils.ui;

public class ColorGradient {
    private ColorGradient() {
    }

    public static final String RESET = "\u001B[0m";

    public static String getColor(double percentage) {
        return getColor(
                percentage,
                255, 0, 0,
                255, 165, 0,
                60, 200, 120);
    }

    public static String getColor(
            double percentage,
            int startR, int startG, int startB,
            int middleR, int middleG, int middleB,
            int endR, int endG, int endB) {

        percentage = Math.clamp(percentage, 0.0, 1.0);

        int r;
        int g;
        int b;

        if (percentage < 0.5) {
            double t = percentage / 0.5;
            r = interpolate(startR, middleR, t);
            g = interpolate(startG, middleG, t);
            b = interpolate(startB, middleB, t);
        } else {
            double t = (percentage - 0.5) / 0.5;
            r = interpolate(middleR, endR, t);
            g = interpolate(middleG, endG, t);
            b = interpolate(middleB, endB, t);
        }

        return ansiRgb(r, g, b);
    }

    public static String getColor(
            double percentage,
            int startR, int startG, int startB,
            int endR, int endG, int endB) {

        percentage = Math.clamp(percentage, 0.0, 1.0);

        int r = interpolate(startR, endR, percentage);
        int g = interpolate(startG, endG, percentage);
        int b = interpolate(startB, endB, percentage);

        return ansiRgb(r, g, b);
    }

    private static int interpolate(int from, int to, double t) {
        return (int) Math.round(from + (to - from) * t);
    }

    private static String ansiRgb(int r, int g, int b) {
        r = Math.clamp(r, 0, 255);
        g = Math.clamp(g, 0, 255);
        b = Math.clamp(b, 0, 255);
        return String.format("\u001B[38;2;%d;%d;%dm", r, g, b);
    }

}