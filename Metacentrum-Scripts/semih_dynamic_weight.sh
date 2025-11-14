#!/bin/bash
#PBS -l select=1:ncpus=1:mem=4gb:scratch_local=1gb:cluster=eltu2
#PBS -l walltime=10:00:00
#PBS -e /storage/vestec1-elixir/home/semih/logs/astar_grid_dynamic_error.log
#PBS -o /storage/vestec1-elixir/home/semih/logs/astar_grid_dynamic_output.log

ROOT=/storage/vestec1-elixir/home/semih
DATADIR=$ROOT/repo/super-mario-astar
RESULTDIR=$ROOT/results/astar-grid-dynamic

module add openjdk-17

mkdir -p "$ROOT/logs"
echo "$PBS_JOBID is running on $(hostname -f) with scratch $SCRATCHDIR" >> "$ROOT/logs/jobs_info.txt"

test -n "$SCRATCHDIR" || { echo >&2 "SCRATCHDIR is not set"; exit 1; }
cd "$SCRATCHDIR" || { echo >&2 "Cannot cd to SCRATCHDIR"; exit 1; }

cp -R "$DATADIR/Mario-AI-Framework" . || { echo >&2 "Cannot copy Mario-AI-Framework"; exit 1; }

cd Mario-AI-Framework || { echo >&2 "Cannot cd to Mario-AI-Framework"; exit 1; }

# parameter list
SEARCH_STEPS=${SEARCH_STEPS:-"2 3 4"}
START_WEIGHTS=${START_WEIGHTS:-"0.8 1.0 1.2 1.5 1.8"}
END_WEIGHTS=${END_WEIGHTS:-"0.5 0.8 1.0 1.5 2.0"}
EXPONENTS=${EXPONENTS:-"0.5 1.0 2.0 3.0"}
BATCH_NAME=${BATCH_NAME:-"batch1"}

export SEARCH_STEPS START_WEIGHTS END_WEIGHTS EXPONENTS BATCH_NAME

echo "Using SEARCH_STEPS=$SEARCH_STEPS"
echo "Using START_WEIGHTS=$START_WEIGHTS"
echo "Using END_WEIGHTS=$END_WEIGHTS"
echo "Using EXPONENTS=$EXPONENTS"
echo "Using BATCH_NAME=$BATCH_NAME"

# Compile and run the dynamic weighting benchmark
find src -name "*.java" > sources.list
javac -cp src @sources.list || { echo >&2 "javac failed"; exit 1; }
mkdir -p agent-benchmark
java -cp src mff.agents.benchmark.AstarGridDynamicWeightBenchmark "$@" || { echo >&2 "java failed"; exit 1; }

mkdir -p "$RESULTDIR/$BATCH_NAME" || { echo >&2 "Cannot create batch folder"; exit 1; }
cp -a agent-benchmark/. "$RESULTDIR/$BATCH_NAME"/ || { echo >&2 "Cannot copy benchmark results"; exit 1; }

rm -f sources.list

clean_scratch


