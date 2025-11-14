package mff.agents.astar;

import mff.agents.benchmark.IAgentBenchmark;
import mff.agents.benchmark.IAgentBenchmarkBacktrack;
import mff.agents.common.IMarioAgentMFF;
import mff.agents.astarHelper.MarioAction;
import mff.agents.common.MarioTimerSlim;
import mff.forwardmodel.slim.core.MarioForwardModelSlim;

import java.util.ArrayList;

public class Agent implements IMarioAgentMFF, IAgentBenchmark, IAgentBenchmarkBacktrack {

    private static final int DEFAULT_SEARCH_STEPS = 3;
    private static final float DEFAULT_TIME_TO_FINISH_WEIGHT = 1.1f;

    private static int configuredSearchSteps = DEFAULT_SEARCH_STEPS;
    private static float configuredTimeToFinishWeight = DEFAULT_TIME_TO_FINISH_WEIGHT;

    private ArrayList<boolean[]> actionsList = new ArrayList<>();
    private float furthestDistance = -1;
    private boolean finished = false;
    private int totalSearchCalls = 0;
    private int totalNodesEvaluated = 0;
    private int mostBacktrackedNodes = 0;

    @Override
    public void initialize(MarioForwardModelSlim model) {
        AStarTree.winFound = false;
        AStarTree.exitTileX = model.getWorld().level.exitTileX * 16;
    }

    @Override
    public boolean[] getActions(MarioForwardModelSlim model, MarioTimerSlim timer) {
        if (finished) {
            if (actionsList.size() == 0)
                return MarioAction.NO_ACTION.value;
            else
                return actionsList.remove(actionsList.size() - 1);
        }

        AStarTree tree = new AStarTree(model, configuredSearchSteps, configuredTimeToFinishWeight);
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
        return "MFF AStar Agent";
    }

    public static void setSearchSteps(int searchSteps) {
        if (searchSteps <= 0)
            throw new IllegalArgumentException("searchSteps must be positive.");
        configuredSearchSteps = searchSteps;
    }

    public static int getSearchSteps() {
        return configuredSearchSteps;
    }

    public static void setTimeToFinishWeight(float timeToFinishWeight) {
        if (timeToFinishWeight <= 0)
            throw new IllegalArgumentException("timeToFinishWeight must be positive.");
        configuredTimeToFinishWeight = timeToFinishWeight;
    }

    public static float getTimeToFinishWeight() {
        return configuredTimeToFinishWeight;
    }

    public static void resetParameters() {
        configuredSearchSteps = DEFAULT_SEARCH_STEPS;
        configuredTimeToFinishWeight = DEFAULT_TIME_TO_FINISH_WEIGHT;
    }
}
