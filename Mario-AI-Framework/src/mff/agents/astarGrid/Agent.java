package mff.agents.astarGrid;

import mff.agents.astarHelper.MarioAction;
import mff.agents.benchmark.IAgentBenchmark;
import mff.agents.benchmark.IAgentBenchmarkBacktrack;
import mff.agents.common.IGridHeuristic;
import mff.agents.common.IMarioAgentMFF;
import mff.agents.common.MarioTimerSlim;
import mff.forwardmodel.slim.core.MarioForwardModelSlim;

import java.util.ArrayList;

public class Agent implements IMarioAgentMFF, IAgentBenchmark, IGridHeuristic, IAgentBenchmarkBacktrack {

    private static final int DEFAULT_SEARCH_STEPS = 3;
    private static final float DEFAULT_TIME_TO_FINISH_WEIGHT = 2f;
    private static final boolean DEFAULT_USE_DYNAMIC_TIME_WEIGHT = false;
    private static final float DEFAULT_TIME_TO_FINISH_WEIGHT_START = DEFAULT_TIME_TO_FINISH_WEIGHT;
    private static final float DEFAULT_TIME_TO_FINISH_WEIGHT_END = DEFAULT_TIME_TO_FINISH_WEIGHT;
    private static final float DEFAULT_TIME_TO_FINISH_WEIGHT_EXPONENT = 1f;

    private static int configuredSearchSteps = DEFAULT_SEARCH_STEPS;
    private static float configuredTimeToFinishWeight = DEFAULT_TIME_TO_FINISH_WEIGHT;
    private static boolean configuredUseDynamicTimeWeight = DEFAULT_USE_DYNAMIC_TIME_WEIGHT;
    private static float configuredTimeToFinishWeightStart = DEFAULT_TIME_TO_FINISH_WEIGHT_START;
    private static float configuredTimeToFinishWeightEnd = DEFAULT_TIME_TO_FINISH_WEIGHT_END;
    private static float configuredTimeToFinishWeightExponent = DEFAULT_TIME_TO_FINISH_WEIGHT_EXPONENT;

    private ArrayList<boolean[]> actionsList = new ArrayList<>();
    private float furthestDistance = -1;
    private boolean finished = false;
    private int totalSearchCalls = 0;
    private int totalNodesEvaluated = 0;
    private int mostBacktrackedNodes = 0;
    private int[][] levelTilesWithPath;

    @Override
    public void initialize(MarioForwardModelSlim model) {
        AStarTree.winFound = false;
        AStarTree.exitTileX = model.getWorld().level.exitTileX * 16;
        AStarTree.TIME_TO_FINISH_WEIGHT = configuredTimeToFinishWeight;
        AStarTree.USE_DYNAMIC_TIME_WEIGHT = configuredUseDynamicTimeWeight;
        AStarTree.TIME_TO_FINISH_WEIGHT_START = configuredTimeToFinishWeightStart;
        AStarTree.TIME_TO_FINISH_WEIGHT_END = configuredTimeToFinishWeightEnd;
        AStarTree.TIME_TO_FINISH_WEIGHT_EXPONENT = configuredTimeToFinishWeightExponent;
    }

    @Override
    public void receiveLevelWithPath(int[][] levelTilesWithPath) {
        this.levelTilesWithPath = levelTilesWithPath;
    }

    @Override
    public boolean[] getActions(MarioForwardModelSlim model, MarioTimerSlim timer) {
        if (finished) {
            if (actionsList.size() == 0)
                return MarioAction.NO_ACTION.value;
            else
                return actionsList.remove(actionsList.size() - 1);
        }

        AStarTree tree = new AStarTree(model, configuredSearchSteps, levelTilesWithPath);
        ArrayList<boolean[]> newActionsList = tree.search(timer);
        totalSearchCalls++;
        this.totalNodesEvaluated += tree.nodesEvaluated;
        this.mostBacktrackedNodes = Math.max(tree.mostBacktrackedNodes, this.mostBacktrackedNodes);

        if (AStarTree.winFound) {
            actionsList = newActionsList;
            finished = true;
            return actionsList.remove(actionsList.size() - 1);
        }

        if (tree.furthestNodeDistance > furthestDistance) {
            furthestDistance = tree.furthestNodeDistance;
            actionsList = newActionsList;
        }

        if (actionsList.size() == 0) { // didn't find a way further yet, take new actions to prevent stopping
            actionsList = newActionsList;
        }

        if (actionsList.size() == 0) // agent failed
            return MarioAction.NO_ACTION.value;

        return actionsList.remove(actionsList.size() - 1);
    }

    @Override
    public int getSearchCalls() {
        return totalSearchCalls;
    }

    @Override
    public int getNodesEvaluated() {
        return totalNodesEvaluated;
    }

    @Override
    public int getMostBacktrackedNodes() {
        return mostBacktrackedNodes;
    }

    @Override
    public String getAgentName() {
        return "MFF A* Grid Agent";
    }

    public static void setSearchSteps(int searchSteps) {
        if (searchSteps <= 0)
            throw new IllegalArgumentException("searchSteps must be positive.");
        configuredSearchSteps = searchSteps;
    }

    public static int getSearchSteps() {
        return configuredSearchSteps;
    }

    public static void setTimeToFinishWeight(float weight) {
        if (weight <= 0)
            throw new IllegalArgumentException("timeToFinishWeight must be positive.");
        configuredTimeToFinishWeight = weight;
    }

    public static float getTimeToFinishWeight() {
        return configuredTimeToFinishWeight;
    }

    public static void enableDynamicTimeWeighting(boolean enabled) {
        configuredUseDynamicTimeWeight = enabled;
    }

    public static boolean isDynamicTimeWeightingEnabled() {
        return configuredUseDynamicTimeWeight;
    }

    public static void setDynamicTimeWeightEndpoints(float startWeight, float endWeight) {
        if (startWeight <= 0 || endWeight <= 0)
            throw new IllegalArgumentException("Dynamic time weights must be positive.");
        configuredTimeToFinishWeightStart = startWeight;
        configuredTimeToFinishWeightEnd = endWeight;
    }

    public static float getDynamicTimeWeightStart() {
        return configuredTimeToFinishWeightStart;
    }

    public static float getDynamicTimeWeightEnd() {
        return configuredTimeToFinishWeightEnd;
    }

    public static void setDynamicTimeWeightExponent(float exponent) {
        if (Float.isNaN(exponent) || Float.isInfinite(exponent))
            throw new IllegalArgumentException("Exponent must be a finite number.");
        configuredTimeToFinishWeightExponent = exponent;
    }

    public static float getDynamicTimeWeightExponent() {
        return configuredTimeToFinishWeightExponent;
    }

    public static void resetParameters() {
        configuredSearchSteps = DEFAULT_SEARCH_STEPS;
        configuredTimeToFinishWeight = DEFAULT_TIME_TO_FINISH_WEIGHT;
        configuredUseDynamicTimeWeight = DEFAULT_USE_DYNAMIC_TIME_WEIGHT;
        configuredTimeToFinishWeightStart = DEFAULT_TIME_TO_FINISH_WEIGHT_START;
        configuredTimeToFinishWeightEnd = DEFAULT_TIME_TO_FINISH_WEIGHT_END;
        configuredTimeToFinishWeightExponent = DEFAULT_TIME_TO_FINISH_WEIGHT_EXPONENT;
    }
}
