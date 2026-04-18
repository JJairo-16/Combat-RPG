package rpgcombat;

public class App {
    public static void main(String[] args) {
        App app = new App();
        app.run();
    }

    public void run() {
        GameBootstrap bootstrap = new GameBootstrap();
        bootstrap.createGame().init();
    }
}