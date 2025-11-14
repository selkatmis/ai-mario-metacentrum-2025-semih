#!/bin/bash
#PBS -l select=1:ncpus=1:mem=4gb:scratch_local=1gb:cluster=eltu2
#PBS -l walltime=04:00:00
#PBS -e /storage/vestec1-elixir/home/semih/logs/job_error.log
#PBS -o /storage/vestec1-elixir/home/semih/logs/job_output.log

# Update these paths to match your directory layout on the HPC cluster.
ROOT=/storage/vestec1-elixir/home/semih
DATADIR=$ROOT/repo/super-mario-astar
RESULTDIR=$ROOT/results/agent-benchmark

mkdir -p "$ROOT/logs"
echo "$PBS_JOBID is running on node $(hostname -f) in scratch directory $SCRATCHDIR" >> "$ROOT/logs/jobs_info.txt"

# Load Java
module add openjdk-17

# Ensure scratch directory exists and switch to it
test -n "$SCRATCHDIR" || { echo >&2 "SCRATCHDIR is not set"; exit 1; }
cd "$SCRATCHDIR" || { echo >&2 "Cannot cd to SCRATCHDIR"; exit 1; }

# Copy project sources to scratch
cp -R "$DATADIR/Mario-AI-Framework" . || { echo >&2 "Cannot copy Mario-AI-Framework"; exit 1; }

cd Mario-AI-Framework || { echo >&2 "Cannot cd to Mario-AI-Framework"; exit 1; }

# Compile and run the benchmark (A* only by default)
javac -cp src src/mff/agents/benchmark/AgentBenchmark.java || { echo >&2 "javac failed"; exit 1; }
mkdir -p agent-benchmark
java -cp src mff.agents.benchmark.AgentBenchmark || { echo >&2 "java failed"; exit 1; }

# Archive results
mkdir -p "$RESULTDIR" || { echo >&2 "Cannot create RESULTDIR"; exit 1; }
cp -a agent-benchmark/. "$RESULTDIR"/ || { echo >&2 "Cannot copy benchmark results"; exit 1; }

clean_scratch

