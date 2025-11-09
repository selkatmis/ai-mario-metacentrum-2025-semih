package mff.agents.common;

import engine.core.MarioLevelGenerator;
import engine.core.MarioLevelModel;
import engine.core.MarioTimer;
import mff.LevelLoader;
import mff.agents.astarGrid.AStarTree;
import mff.agents.benchmark.AgentBenchmarkGame;
import mff.agents.benchmark.IAgentBenchmarkBacktrack;

import java.util.ArrayList;

public class AgentMain {
    public static void main(String[] args) {
        //testLevel();
        //testLevelGrid();
        //testLevelBenchmark();
        //testLevelWaypoints();

        testAllOriginalLevels();
        //testAllOriginalLevelsGrid();
        //testAllKrysLevelsGrid();

        //testGeneratedLevels();
        //testAllAgents();
    }

    private static void testLevel() {
        AgentMarioGame game = new AgentMarioGame();
        game.runGame(new mff.agents.astar.Agent(), LevelLoader.getLevel("./levels/original/lvl-1.txt"),
                200,0,true);
    }

    private static void testLevelGrid() {
        AgentMarioGame game = new AgentMarioGame();
        String levelPath = "./levels/original/lvl-1.txt";
        IMarioAgentMFF astarGridAgent = new mff.agents.astarGrid.Agent();
        AStarTree.NODE_DEPTH_WEIGHT = 1f;
        AStarTree.TIME_TO_FINISH_WEIGHT = 2f;
        AStarTree.DISTANCE_FROM_PATH_TOLERANCE = 1f;
        AStarTree.DISTANCE_FROM_PATH_ADDITIVE_PENALTY = 5f;
        AStarTree.DISTANCE_FROM_PATH_MULTIPLICATIVE_PENALTY = 5f;
        AStarGridHelper.giveLevelTilesWithPath(astarGridAgent, levelPath);
        game.runGame(astarGridAgent, LevelLoader.getLevel(levelPath),
                200, 0, true);
    }

    private static void testLevelBenchmark() {
        String level = LevelLoader.getLevel("./levels/original/lvl-1.txt");
        AgentBenchmarkGame game = new AgentBenchmarkGame();
        IMarioAgentMFF astarGridAgent = new mff.agents.astarGrid.Agent();
        AStarTree.NODE_DEPTH_WEIGHT = 2f;
        AStarTree.TIME_TO_FINISH_WEIGHT = 2f;
        AStarTree.DISTANCE_FROM_PATH_TOLERANCE = 1f;
        AStarTree.DISTANCE_FROM_PATH_ADDITIVE_PENALTY = 20f;
        AStarTree.DISTANCE_FROM_PATH_MULTIPLICATIVE_PENALTY = 7f;
        AStarGridHelper.giveLevelTilesWithPath(astarGridAgent, "./levels/original/lvl-1.txt");
        var agentStats = game.runGame(astarGridAgent, level, 30, 0, false);
        agentStats.level = "original-1";
        var mostBacktrackedNodes = ((IAgentBenchmarkBacktrack)astarGridAgent).getMostBacktrackedNodes();

        System.out.println("level: " + agentStats.level);
        System.out.println("win: " + agentStats.win);
        System.out.println("percentageTravelled: " + agentStats.percentageTravelled);
        System.out.println("runTime: " + agentStats.runTime);
        System.out.println("totalGameTicks: " + agentStats.totalGameTicks);
        System.out.println("totalPlanningTime: " + agentStats.totalPlanningTime);
        System.out.println("searchCalls: " + agentStats.searchCalls);
        System.out.println("nodesEvaluated: " + agentStats.nodesEvaluated);
        System.out.println("mostBacktrackedNodes: " + mostBacktrackedNodes);
    }

    private static void testLevelWaypoints() {
        AgentMarioGame game = new AgentMarioGame();
        String levelPath = "./levels/showcase/lvl-1.txt";
        IMarioAgentMFF astarWaypointsAgent = new mff.agents.astarWaypoints.Agent();
        AStarGridHelper.giveLevelTilesWithPath(astarWaypointsAgent, levelPath);
        AStarGridHelper.giveGridPath(astarWaypointsAgent, levelPath);
        game.runGame(astarWaypointsAgent, LevelLoader.getLevel(levelPath),
                200, 0, true);
    }

    private static void testAllOriginalLevels() {
        for (int i = 1; i < 16; i++) {
            AgentMarioGame game = new AgentMarioGame();
            game.runGame(new mff.agents.astar.Agent(), LevelLoader.getLevel("./levels/original/lvl-" + i + ".txt"),
                    200, 0, true);
        }
    }

    private static void testAllOriginalLevelsGrid() {
        for (int i = 1; i < 16; i++) {
            AgentMarioGame game = new AgentMarioGame();
            String levelPath = "./levels/original/lvl-" + i + ".txt";
            IMarioAgentMFF astarGridAgent = new mff.agents.astarGrid.Agent();
            AStarTree.NODE_DEPTH_WEIGHT = 1f;
            AStarTree.TIME_TO_FINISH_WEIGHT = 2f;
            AStarTree.DISTANCE_FROM_PATH_TOLERANCE = 2f;
            AStarTree.DISTANCE_FROM_PATH_ADDITIVE_PENALTY = 5f;
            AStarTree.DISTANCE_FROM_PATH_MULTIPLICATIVE_PENALTY = 7f;
            AStarGridHelper.giveLevelTilesWithPath(astarGridAgent, levelPath);
            game.runGame(astarGridAgent, LevelLoader.getLevel(levelPath),
                    200, 0, true);
        }
    }

    private static void testAllKrysLevelsGrid() {
        for (int i = 1; i <= 100; i++) {
            AgentMarioGame game = new AgentMarioGame();
            String levelPath = "./levels/krys/lvl-" + i + ".txt";
            IMarioAgentMFF astarGridAgent = new mff.agents.astarGrid.Agent();
            AStarTree.NODE_DEPTH_WEIGHT = 1f;
            AStarTree.TIME_TO_FINISH_WEIGHT = 2f;
            AStarTree.DISTANCE_FROM_PATH_TOLERANCE = 2f;
            AStarTree.DISTANCE_FROM_PATH_ADDITIVE_PENALTY = 5f;
            AStarTree.DISTANCE_FROM_PATH_MULTIPLICATIVE_PENALTY = 7f;
            AStarGridHelper.giveLevelTilesWithPath(astarGridAgent, levelPath);
            game.runGame(astarGridAgent, LevelLoader.getLevel(levelPath),
                    200, 0, true);
        }
    }


    private static void testGeneratedLevels() {
        for (int i = 1; i <= 100; i++) {
            MarioLevelGenerator generator = new levelGenerators.krys.LevelGenerator(i);
            String level = generator.getGeneratedLevel(new MarioLevelModel(150, 16),
                    new MarioTimer(5 * 60 * 60 * 1000));
            AgentMarioGame game = new AgentMarioGame();
            game.runGame(new mff.agents.astar.Agent(), level, 30, 0, true);
        }
    }

    private static void testAllAgents() {
        ArrayList<IMarioAgentMFF> agents = new ArrayList<>() {{
            add(new mff.agents.astar.Agent());
            add(new mff.agents.astarDistanceMetric.Agent());
            add(new mff.agents.astarFast.Agent());
            add(new mff.agents.astarJump.Agent());
            add(new mff.agents.astarPlanning.Agent());
            add(new mff.agents.astarPlanningDynamic.Agent());
            add(new mff.agents.astarWindow.Agent());
            add(new mff.agents.robinBaumgartenSlim.Agent());
            add(new mff.agents.robinBaumgartenSlimImproved.Agent());
        }};

        for (var agent : agents) {
            AgentMarioGame game = new AgentMarioGame();
            System.out.println("Testing " + agent.getAgentName());
            game.runGame(agent, LevelLoader.getLevel("./levels/original/lvl-1.txt"), 200, 0, true);
        }
    }
}
