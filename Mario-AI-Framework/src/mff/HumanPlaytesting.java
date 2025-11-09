package mff;

import engine.core.MarioGame;

public class HumanPlaytesting {
    public static void main(String[] args) {
        MarioGame game = new MarioGame();
        String level = LevelLoader.getLevel("./levels/original/lvl-1.txt");
        game.playGame(level, 10000);
    }
}
