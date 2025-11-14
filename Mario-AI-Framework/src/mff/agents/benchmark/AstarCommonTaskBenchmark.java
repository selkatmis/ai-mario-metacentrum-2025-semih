package mff.agents.benchmark;

import mff.LevelLoader;
import mff.agents.astar.Agent;

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

public class AstarCommonTaskBenchmark {

    private static final int[] SEARCH_STEPS = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 20};
    private static final float[] TIME_TO_FINISH_WEIGHTS =
            {0.2f, 0.4f, 0.6f, 0.8f, 1.0f, 1.2f, 1.4f, 1.6f, 1.8f, 2.0f};

    private static final LevelSpec[] LEVEL_SPECS = {
            new LevelSpec("original", 15, 200),
            new LevelSpec("krys", 15, 30),
            new LevelSpec("patternCount", 15, 30)
    };

    private static final DecimalFormat PERCENTAGE_FORMAT;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.getDefault());
        symbols.setDecimalSeparator('.');
        PERCENTAGE_FORMAT = new DecimalFormat("0.00", symbols);
    }

    private static final Path OUTPUT_DIR = Paths.get("agent-benchmark");
    private static final Path DETAIL_CSV = OUTPUT_DIR.resolve("astar-common-task.csv");
    private static final Path SUMMARY_CSV = OUTPUT_DIR.resolve("astar-common-task-summary.csv");

    public static void main(String[] args) throws IOException {
        Files.createDirectories(OUTPUT_DIR);
        deleteIfExists(DETAIL_CSV);
        deleteIfExists(SUMMARY_CSV);

        Map<ParameterKey, AggregatedStats> aggregates = new LinkedHashMap<>();

        try (FileWriter detailWriter = new FileWriter(DETAIL_CSV.toFile());
             FileWriter summaryWriter = new FileWriter(SUMMARY_CSV.toFile())) {

            writeDetailHeader(detailWriter);

            for (int searchSteps : SEARCH_STEPS) {
                Agent.setSearchSteps(searchSteps);
                for (float timeToFinishWeight : TIME_TO_FINISH_WEIGHTS) {
                    Agent.setTimeToFinishWeight(timeToFinishWeight);
                    ParameterKey key = new ParameterKey(searchSteps, timeToFinishWeight);
                    AggregatedStats aggregatedStats = aggregates.computeIfAbsent(key, AggregatedStats::new);

                    for (LevelSpec levelSpec : LEVEL_SPECS) {
                        for (int levelIndex = 1; levelIndex <= levelSpec.levelCount; levelIndex++) {
                            runBenchmarkCase(detailWriter, aggregatedStats, levelSpec, levelIndex);
                        }
                    }
                }
            }

            writeSummary(summaryWriter, aggregates);
        } finally {
            Agent.resetParameters();
        }
    }

    private static void runBenchmarkCase(FileWriter detailWriter,
                                         AggregatedStats aggregatedStats,
                                         LevelSpec levelSpec,
                                         int levelIndex) throws IOException {
        String levelPath = "./levels/" + levelSpec.levelPack + "/lvl-" + levelIndex + ".txt";
        String levelContent = LevelLoader.getLevel(levelPath);

        AgentBenchmarkGame game = new AgentBenchmarkGame();
        Agent agent = new Agent();
        AgentStats stats = game.runGame(agent, levelContent, levelSpec.timerSeconds, 0, false);
        stats.level = levelSpec.levelPack + "-" + levelIndex;

        writeDetailRow(detailWriter, aggregatedStats.key.searchSteps, aggregatedStats.key.timeToFinishWeight,
                levelSpec.levelPack, levelIndex, stats);
        aggregatedStats.add(stats);
    }

    private static void writeDetailHeader(FileWriter detailWriter) throws IOException {
        detailWriter.write("searchSteps,timeToFinishWeight,levelPack,levelIndex,win,% travelled,run time,game ticks,"
                + "planning time,total plannings,nodes evaluated,most backtracked nodes\n");
    }

    private static void writeDetailRow(FileWriter detailWriter,
                                       int searchSteps,
                                       float timeToFinishWeight,
                                       String levelPack,
                                       int levelIndex,
                                       AgentStats stats) throws IOException {
        detailWriter.write(searchSteps + ","
                + timeToFinishWeight + ","
                + levelPack + ","
                + levelIndex + ","
                + stats.win + ","
                + PERCENTAGE_FORMAT.format(stats.percentageTravelled) + ","
                + stats.runTime + ","
                + stats.totalGameTicks + ","
                + stats.totalPlanningTime + ","
                + stats.searchCalls + ","
                + stats.nodesEvaluated + ","
                + stats.mostBacktrackedNodes + "\n");
    }

    private static void writeSummary(FileWriter summaryWriter,
                                     Map<ParameterKey, AggregatedStats> aggregates) throws IOException {
        summaryWriter.write("searchSteps,timeToFinishWeight,gamesPlayed,wins,winRate,avgRunTime,avgPlanningTime,"
                + "avgGameTicks,avgNodesEvaluated,avgMostBacktrackedNodes,avgPercentageTravelled\n");
        for (AggregatedStats aggregatedStats : aggregates.values()) {
            summaryWriter.write(aggregatedStats.toCsvRow() + "\n");
        }
    }

    private static void deleteIfExists(Path path) throws IOException {
        if (Files.exists(path)) {
            Files.delete(path);
        }
    }

    private static class ParameterKey {
        final int searchSteps;
        final float timeToFinishWeight;

        ParameterKey(int searchSteps, float timeToFinishWeight) {
            this.searchSteps = searchSteps;
            this.timeToFinishWeight = timeToFinishWeight;
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
                    + key.timeToFinishWeight + ","
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

