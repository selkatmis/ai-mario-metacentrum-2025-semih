package mff.agents.benchmark;

import mff.LevelLoader;
import mff.agents.astarGrid.Agent;
import mff.agents.common.IGridHeuristic;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import static mff.agents.common.AStarGridHelper.giveLevelTilesWithPath;

public class AstarGridDynamicWeightBenchmark {

    private static final int[] DEFAULT_SEARCH_STEPS = {2, 3, 4};
    private static final float[] DEFAULT_START_WEIGHTS = {0.8f, 1.0f, 1.2f, 1.5f, 1.8f};
    private static final float[] DEFAULT_END_WEIGHTS = {0.5f, 0.8f, 1.0f, 1.5f, 2.0f};
    private static final float[] DEFAULT_EXPONENTS = {0.5f, 1.0f, 2.0f, 3.0f};

    private static final LevelSpec[] LEVEL_SPECS = {
            new LevelSpec("original", 15, 200),
            new LevelSpec("krys", 100, 30),
            new LevelSpec("patternCount", 100, 30)
    };

    private static final Path OUTPUT_DIR = Paths.get("agent-benchmark");
    private static final Path DETAIL_CSV = OUTPUT_DIR.resolve("astar-grid-dynamic-detail.csv");
    private static final Path SUMMARY_CSV = OUTPUT_DIR.resolve("astar-grid-dynamic-summary.csv");

    private static final DecimalFormat PERCENT_FORMAT;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.getDefault());
        symbols.setDecimalSeparator('.');
        PERCENT_FORMAT = new DecimalFormat("0.00", symbols);
    }

    public static void main(String[] args) throws IOException {
        Files.createDirectories(OUTPUT_DIR);
        deleteIfExists(DETAIL_CSV);
        deleteIfExists(SUMMARY_CSV);

        Map<ParameterKey, AggregatedStats> aggregates = new LinkedHashMap<>();

        try (FileWriter detailWriter = new FileWriter(DETAIL_CSV.toFile());
             FileWriter summaryWriter = new FileWriter(SUMMARY_CSV.toFile())) {

            writeDetailHeader(detailWriter);

            int[] searchStepsList = parseSearchSteps();
            float[] startWeights = parseFloatList("START_WEIGHTS", DEFAULT_START_WEIGHTS);
            float[] endWeights = parseFloatList("END_WEIGHTS", DEFAULT_END_WEIGHTS);
            float[] exponents = parseFloatList("EXPONENTS", DEFAULT_EXPONENTS);

            System.out.println("Configured searchSteps: " + arrayToString(searchStepsList));
            System.out.println("Configured startWeights: " + arrayToString(startWeights));
            System.out.println("Configured endWeights: " + arrayToString(endWeights));
            System.out.println("Configured exponents: " + arrayToString(exponents));

            for (int searchSteps : searchStepsList) {
                Agent.setSearchSteps(searchSteps);

                for (float startWeight : startWeights) {
                    for (float endWeight : endWeights) {
                        for (float exponent : exponents) {
                            boolean dynamicEnabled = shouldEnableDynamic(startWeight, endWeight, exponent);
                            configureAgent(searchSteps, startWeight, endWeight, exponent, dynamicEnabled);

                            ParameterKey key = new ParameterKey(
                                    searchSteps,
                                    startWeight,
                                    endWeight,
                                    exponent,
                                    dynamicEnabled
                            );

                            AggregatedStats aggregatedStats = aggregates.computeIfAbsent(key, AggregatedStats::new);

                            for (LevelSpec levelSpec : LEVEL_SPECS) {
                                for (int levelIndex = 1; levelIndex <= levelSpec.levelCount; levelIndex++) {
                                    runBenchmarkCase(detailWriter, aggregatedStats, levelSpec, levelIndex);
                                }
                            }
                        }
                    }
                }
            }

            writeSummary(summaryWriter, aggregates);
        } finally {
            Agent.resetParameters();
            resetGridStatics();
        }
    }

    private static void configureAgent(int searchSteps,
                                       float startWeight,
                                       float endWeight,
                                       float exponent,
                                       boolean dynamicEnabled) {
        Agent.setSearchSteps(searchSteps);
        Agent.setTimeToFinishWeight(startWeight);
        Agent.setDynamicTimeWeightEndpoints(startWeight, endWeight);
        Agent.setDynamicTimeWeightExponent(exponent);
        Agent.enableDynamicTimeWeighting(dynamicEnabled);
    }

    private static boolean shouldEnableDynamic(float startWeight, float endWeight, float exponent) {
        final float epsilon = 1e-4f;
        if (Math.abs(startWeight - endWeight) > epsilon) {
            return true;
        }
        return Math.abs(exponent - 1f) > epsilon;
    }

    private static void runBenchmarkCase(FileWriter detailWriter,
                                         AggregatedStats aggregatedStats,
                                         LevelSpec levelSpec,
                                         int levelIndex) throws IOException {
        String levelPath = "./levels/" + levelSpec.levelPack + "/lvl-" + levelIndex + ".txt";
        String levelContent = LevelLoader.getLevel(levelPath);

        AgentBenchmarkGame game = new AgentBenchmarkGame();
        Agent agent = new Agent();
        if (agent instanceof IGridHeuristic) {
            giveLevelTilesWithPath(agent, levelPath);
        }

        AgentStats stats = game.runGame(agent, levelContent, levelSpec.timerSeconds, 0, false);
        stats.level = levelSpec.levelPack + "-" + levelIndex;

        writeDetailRow(detailWriter, aggregatedStats.key, levelSpec.levelPack, levelIndex, stats);
        aggregatedStats.add(stats);
    }

    private static void writeDetailHeader(FileWriter detailWriter) throws IOException {
        detailWriter.write(
                "searchSteps,startWeight,endWeight,exponent,dynamicEnabled,levelPack,levelIndex,win,% travelled,"
                        + "run time,game ticks,planning time,total plannings,nodes evaluated,most backtracked nodes\n"
        );
    }

    private static void writeDetailRow(FileWriter detailWriter,
                                       ParameterKey key,
                                       String levelPack,
                                       int levelIndex,
                                       AgentStats stats) throws IOException {
        detailWriter.write(
                key.searchSteps + ","
                        + key.startWeight + ","
                        + key.endWeight + ","
                        + key.exponent + ","
                        + key.dynamicEnabled + ","
                        + levelPack + ","
                        + levelIndex + ","
                        + stats.win + ","
                        + PERCENT_FORMAT.format(stats.percentageTravelled) + ","
                        + stats.runTime + ","
                        + stats.totalGameTicks + ","
                        + stats.totalPlanningTime + ","
                        + stats.searchCalls + ","
                        + stats.nodesEvaluated + ","
                        + stats.mostBacktrackedNodes + "\n"
        );
    }

    private static void writeSummary(FileWriter summaryWriter,
                                     Map<ParameterKey, AggregatedStats> aggregates) throws IOException {
        summaryWriter.write(
                "searchSteps,startWeight,endWeight,exponent,dynamicEnabled,gamesPlayed,wins,winRate,avgRunTime,"
                        + "avgPlanningTime,avgGameTicks,avgNodesEvaluated,avgMostBacktrackedNodes,avgPercentageTravelled\n"
        );

        for (AggregatedStats aggregatedStats : aggregates.values()) {
            summaryWriter.write(aggregatedStats.toCsvRow() + "\n");
        }
    }

    private static void deleteIfExists(Path path) throws IOException {
        if (Files.exists(path)) {
            Files.delete(path);
        }
    }

    private static int[] parseSearchSteps() {
        String value = System.getenv("SEARCH_STEPS");
        if (value == null || value.isBlank()) {
            return DEFAULT_SEARCH_STEPS;
        }
        return parseIntArray(value);
    }

    private static int[] parseIntArray(String raw) {
        return java.util.Arrays.stream(raw.split("[,\\s]+"))
                .filter(token -> !token.isBlank())
                .mapToInt(token -> {
                    try {
                        return Integer.parseInt(token);
                    } catch (NumberFormatException ex) {
                        throw new IllegalArgumentException("Invalid integer value: " + token, ex);
                    }
                })
                .distinct()
                .sorted()
                .toArray();
    }

    private static float[] parseFloatList(String envName, float[] defaults) {
        String value = System.getenv(envName);
        if (value == null || value.isBlank()) {
            return defaults;
        }
        return parseFloatArray(value);
    }

    private static float[] parseFloatArray(String raw) {
        java.util.Set<Float> values = new java.util.TreeSet<>();
        for (String token : raw.split("[,\\s]+")) {
            if (token.isBlank()) {
                continue;
            }
            try {
                values.add(Float.parseFloat(token));
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Invalid float value: " + token, ex);
            }
        }
        float[] result = new float[values.size()];
        int index = 0;
        for (Float value : values) {
            result[index++] = value;
        }
        return result;
    }

    private static String arrayToString(int[] values) {
        return java.util.Arrays.toString(values);
    }

    private static String arrayToString(float[] values) {
        return java.util.Arrays.toString(values);
    }

    private static void resetGridStatics() {
        mff.agents.astarGrid.AStarTree.NODE_DEPTH_WEIGHT = 1f;
        mff.agents.astarGrid.AStarTree.TIME_TO_FINISH_WEIGHT = 2f;
        mff.agents.astarGrid.AStarTree.DISTANCE_FROM_PATH_TOLERANCE = 1f;
        mff.agents.astarGrid.AStarTree.DISTANCE_FROM_PATH_ADDITIVE_PENALTY = 5f;
        mff.agents.astarGrid.AStarTree.DISTANCE_FROM_PATH_MULTIPLICATIVE_PENALTY = 5f;
        mff.agents.astarGrid.AStarTree.USE_DYNAMIC_TIME_WEIGHT = false;
        mff.agents.astarGrid.AStarTree.TIME_TO_FINISH_WEIGHT_START = 2f;
        mff.agents.astarGrid.AStarTree.TIME_TO_FINISH_WEIGHT_END = 2f;
        mff.agents.astarGrid.AStarTree.TIME_TO_FINISH_WEIGHT_EXPONENT = 1f;
    }

    private static class ParameterKey {
        final int searchSteps;
        final float startWeight;
        final float endWeight;
        final float exponent;
        final boolean dynamicEnabled;

        ParameterKey(int searchSteps, float startWeight, float endWeight, float exponent, boolean dynamicEnabled) {
            this.searchSteps = searchSteps;
            this.startWeight = startWeight;
            this.endWeight = endWeight;
            this.exponent = exponent;
            this.dynamicEnabled = dynamicEnabled;
        }

        @Override
        public int hashCode() {
            int result = Integer.hashCode(searchSteps);
            result = 31 * result + Float.hashCode(startWeight);
            result = 31 * result + Float.hashCode(endWeight);
            result = 31 * result + Float.hashCode(exponent);
            result = 31 * result + Boolean.hashCode(dynamicEnabled);
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof ParameterKey)) return false;
            ParameterKey other = (ParameterKey) obj;
            return searchSteps == other.searchSteps
                    && Float.compare(startWeight, other.startWeight) == 0
                    && Float.compare(endWeight, other.endWeight) == 0
                    && Float.compare(exponent, other.exponent) == 0
                    && dynamicEnabled == other.dynamicEnabled;
        }
    }

    private static class AggregatedStats {
        final ParameterKey key;
        int gamesPlayed = 0;
        int wins = 0;
        double totalRunTime = 0;
        double totalPlanningTime = 0;
        double totalGameTicks = 0;
        double totalNodesEvaluated = 0;
        double totalMostBacktracked = 0;
        double totalPercentageTravelled = 0;

        AggregatedStats(ParameterKey key) {
            this.key = key;
        }

        void add(AgentStats stats) {
            gamesPlayed++;
            if (stats.win) {
                wins++;
            }
            totalRunTime += stats.runTime;
            totalPlanningTime += stats.totalPlanningTime;
            totalGameTicks += stats.totalGameTicks;
            totalNodesEvaluated += stats.nodesEvaluated;
            totalMostBacktracked += stats.mostBacktrackedNodes;
            totalPercentageTravelled += stats.percentageTravelled;
        }

        String toCsvRow() {
            double winRate = gamesPlayed == 0 ? 0 : (double) wins / gamesPlayed;
            double avgRunTime = gamesPlayed == 0 ? 0 : totalRunTime / gamesPlayed;
            double avgPlanningTime = gamesPlayed == 0 ? 0 : totalPlanningTime / gamesPlayed;
            double avgGameTicks = gamesPlayed == 0 ? 0 : totalGameTicks / gamesPlayed;
            double avgNodesEvaluated = gamesPlayed == 0 ? 0 : totalNodesEvaluated / gamesPlayed;
            double avgMostBacktracked = gamesPlayed == 0 ? 0 : totalMostBacktracked / gamesPlayed;
            double avgPercentageTravelled = gamesPlayed == 0 ? 0 : totalPercentageTravelled / gamesPlayed;

            return key.searchSteps + ","
                    + key.startWeight + ","
                    + key.endWeight + ","
                    + key.exponent + ","
                    + key.dynamicEnabled + ","
                    + gamesPlayed + ","
                    + wins + ","
                    + formatDouble(winRate) + ","
                    + formatDouble(avgRunTime) + ","
                    + formatDouble(avgPlanningTime) + ","
                    + formatDouble(avgGameTicks) + ","
                    + formatDouble(avgNodesEvaluated) + ","
                    + formatDouble(avgMostBacktracked) + ","
                    + formatDouble(avgPercentageTravelled);
        }

        private String formatDouble(double value) {
            return String.format(Locale.ROOT, "%.4f", value);
        }
    }

    private static class LevelSpec {
        final String levelPack;
        final int levelCount;
        final int timerSeconds;

        LevelSpec(String levelPack, int levelCount, int timerSeconds) {
            this.levelPack = levelPack;
            this.levelCount = levelCount;
            this.timerSeconds = timerSeconds;
        }
    }
}

