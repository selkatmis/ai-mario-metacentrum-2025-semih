#!/bin/bash
#PBS -l select=1:ncpus=1:mem=4gb:scratch_local=1gb:cluster=eltu2
#PBS -l walltime=10:00:00
#PBS -e /storage/vestec1-elixir/home/semih/logs/astar_common_error.log
#PBS -o /storage/vestec1-elixir/home/semih/logs/astar_common_output.log

ROOT=/storage/vestec1-elixir/home/semih
DATADIR=$ROOT/repo/super-mario-astar
RESULTDIR=$ROOT/results/astar-common

module add openjdk-17

mkdir -p "$ROOT/logs"
echo "$PBS_JOBID is running on $(hostname -f) with scratch $SCRATCHDIR" >> "$ROOT/logs/jobs_info.txt"

test -n "$SCRATCHDIR" || { echo >&2 "SCRATCHDIR is not set"; exit 1; }
cd "$SCRATCHDIR" || { echo >&2 "Cannot cd to SCRATCHDIR"; exit 1; }

cp -R "$DATADIR/Mario-AI-Framework" . || { echo >&2 "Cannot copy Mario-AI-Framework"; exit 1; }

cd Mario-AI-Framework || { echo >&2 "Cannot cd to Mario-AI-Framework"; exit 1; }

# Compile and run the common-task benchmark
find src -name "*.java" > sources.list
javac -cp src @sources.list || { echo >&2 "javac failed"; exit 1; }
mkdir -p agent-benchmark
java -cp src mff.agents.benchmark.AstarCommonTaskBenchmark || { echo >&2 "java failed"; exit 1; }

mkdir -p "$RESULTDIR" || { echo >&2 "Cannot create RESULTDIR"; exit 1; }
cp -a agent-benchmark/. "$RESULTDIR"/ || { echo >&2 "Cannot copy benchmark results"; exit 1; }

rm -f sources.list

clean_scratch


