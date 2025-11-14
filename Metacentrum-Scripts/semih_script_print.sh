#!/bin/bash
#PBS -l select=1:ncpus=1:mem=4gb:scratch_local=1gb:cluster=eltu2
#PBS -l walltime=1:00:00
#PBS -e /storage/vestec1-elixir/home/semih/logs/job_error.log
#PBS -o /storage/vestec1-elixir/home/semih/logs/job_output.log

RESULTDIR=/storage/vestec1-elixir/home/semih/logs
ROOT=/storage/vestec1-elixir/home/semih

# Load the correct Java module for the compute node
module add openjdk-17

# Make sure SCRATCHDIR exists and move there
test -n "$SCRATCHDIR" || { echo >&2 "SCRATCHDIR is not set"; exit 1; }
cd "$SCRATCHDIR" || { echo >&2 "Cannot cd to SCRATCHDIR"; exit 1; }

# Copy your source file to scratch if it's not already there
cp "$ROOT/SimplePrint.java" . || { echo >&2 "Cannot copy SimplePrint.java"; exit 1; }

# Compile the Java program on the compute node
javac SimplePrint.java || { echo >&2 "javac failed"; exit 1; }

# Run the Java program
java SimplePrint || { echo >&2 "java failed"; exit 1; }

# Clean up scratch space
clean_scratch
