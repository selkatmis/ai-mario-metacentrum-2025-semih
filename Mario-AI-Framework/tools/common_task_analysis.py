#!/usr/bin/env python3
import argparse
import csv
import math
from collections import defaultdict
from pathlib import Path
from typing import Dict, Iterable, List, Tuple

import matplotlib.pyplot as plt


def read_summary(summary_path: Path):
    with summary_path.open("r", newline="") as csvfile:
        reader = csv.DictReader(csvfile)
        records = []
        for row in reader:
            try:
                record = {
                    "searchSteps": int(row["searchSteps"]),
                    "timeToFinishWeight": float(row["timeToFinishWeight"]),
                    "winRate": float(row["winRate"]),
                    "avgRunTime": float(row["avgRunTime"]),
                    "avgPlanningTime": float(row.get("avgPlanningTime", 0.0)),
                    "avgNodesEvaluated": float(row.get("avgNodesEvaluated", 0.0)),
                }
            except (KeyError, ValueError) as exc:
                raise ValueError(f"Malformed row in summary CSV: {row}") from exc
            records.append(record)
    if not records:
        raise ValueError(f"No records found in {summary_path}")
    return records


def aggregate_detail_records(detail_path: Path) -> Tuple[List[Dict[str, float]], Path]:
    with detail_path.open("r", newline="") as csvfile:
        reader = csv.DictReader(csvfile)
        aggregations: Dict[Tuple[int, float], Dict[str, float]] = defaultdict(lambda: {
            "gamesPlayed": 0,
            "wins": 0,
            "totalRunTime": 0.0,
            "totalPlanningTime": 0.0,
            "totalGameTicks": 0.0,
            "totalNodesEvaluated": 0.0,
            "totalMostBacktracked": 0.0,
            "totalPercentageTravelled": 0.0,
        })

        for row in reader:
            try:
                search_steps = int(row["searchSteps"])
                weight = float(row["timeToFinishWeight"])
                win = row["win"].strip().lower() == "true"
                run_time = float(row["run time"])
                planning_time = float(row["planning time"])
                game_ticks = float(row["game ticks"])
                nodes_evaluated = float(row["nodes evaluated"])
                most_backtracked = float(row.get("most backtracked nodes", 0.0))
                percentage_travelled = float(row["% travelled"])
            except (KeyError, ValueError) as exc:
                raise ValueError(f"Malformed row in detail CSV: {row}") from exc

            key = (search_steps, weight)
            agg = aggregations[key]
            agg["gamesPlayed"] += 1
            agg["wins"] += 1 if win else 0
            agg["totalRunTime"] += run_time
            agg["totalPlanningTime"] += planning_time
            agg["totalGameTicks"] += game_ticks
            agg["totalNodesEvaluated"] += nodes_evaluated
            agg["totalMostBacktracked"] += most_backtracked
            agg["totalPercentageTravelled"] += percentage_travelled

    records = []
    for (search_steps, weight), agg in aggregations.items():
        games = agg["gamesPlayed"]
        wins = agg["wins"]
        win_rate = wins / games if games else 0.0

        def average(total: float) -> float:
            return total / games if games else 0.0

        records.append({
            "searchSteps": search_steps,
            "timeToFinishWeight": weight,
            "gamesPlayed": games,
            "wins": wins,
            "winRate": win_rate,
            "avgRunTime": average(agg["totalRunTime"]),
            "avgPlanningTime": average(agg["totalPlanningTime"]),
            "avgGameTicks": average(agg["totalGameTicks"]),
            "avgNodesEvaluated": average(agg["totalNodesEvaluated"]),
            "avgMostBacktrackedNodes": average(agg["totalMostBacktracked"]),
            "avgPercentageTravelled": average(agg["totalPercentageTravelled"]),
        })

    summary_path = detail_path.with_name("astar-common-task-summary.csv")
    write_summary_csv(summary_path, records)
    return records, summary_path


def write_summary_csv(summary_path: Path, records: Iterable[Dict[str, float]]) -> None:
    fieldnames = [
        "searchSteps",
        "timeToFinishWeight",
        "gamesPlayed",
        "wins",
        "winRate",
        "avgRunTime",
        "avgPlanningTime",
        "avgGameTicks",
        "avgNodesEvaluated",
        "avgMostBacktrackedNodes",
        "avgPercentageTravelled",
    ]
    with summary_path.open("w", newline="") as csvfile:
        writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
        writer.writeheader()
        for record in sorted(records, key=lambda r: (r["searchSteps"], r["timeToFinishWeight"])):
            writer.writerow(record)


def build_matrix(records: List[Dict[str, float]], metric: str) -> Tuple[List[float], List[int], List[List[float]]]:
    weights = sorted({record["timeToFinishWeight"] for record in records})
    steps = sorted({record["searchSteps"] for record in records})

    value_map: Dict[Tuple[float, int], float] = {
        (record["timeToFinishWeight"], record["searchSteps"]): record[metric]
        for record in records
    }

    matrix = []
    for weight in weights:
        row = []
        for step in steps:
            value = value_map.get((weight, step))
            if value is None or math.isnan(value):
                row.append(float("nan"))
            else:
                row.append(value)
        matrix.append(row)

    return weights, steps, matrix


def plot_heatmap(matrix: List[List[float]],
                 weights: List[float],
                 steps: List[int],
                 title: str,
                 colorbar_label: str,
                 output_path: Path,
                 value_formatter) -> None:
    fig, ax = plt.subplots(figsize=(12, 6))
    heatmap = ax.imshow(matrix, aspect="auto", origin="lower", cmap="viridis")
    cbar = fig.colorbar(heatmap, ax=ax)
    cbar.set_label(colorbar_label)

    ax.set_xticks(range(len(steps)))
    ax.set_xticklabels([str(step) for step in steps])
    ax.set_yticks(range(len(weights)))
    ax.set_yticklabels([f"{weight:.1f}" for weight in weights])
    ax.set_xlabel("searchSteps")
    ax.set_ylabel("timeToFinish weight")
    ax.set_title(title)

    for y, weight in enumerate(weights):
        for x, step in enumerate(steps):
            value = matrix[y][x]
            if math.isnan(value):
                continue
            ax.text(x, y, value_formatter(value), ha="center", va="center", color="white", fontsize=8)

    fig.tight_layout()
    output_path.parent.mkdir(parents=True, exist_ok=True)
    fig.savefig(output_path, dpi=300)
    plt.close(fig)


def main():
    parser = argparse.ArgumentParser(description="Generate heatmaps for the A* common task.")
    parser.add_argument("--detail", type=Path,
                        help="Path to the detailed CSV output from the common-task sweep.")
    parser.add_argument("--summary", type=Path,
                        default=Path("agent-benchmark/astar-common-task-summary.csv"),
                        help="Path to the summary CSV. If --detail is provided, this is used as the output path.")
    parser.add_argument("--output-dir", type=Path,
                        default=Path("Experiment-Results/MFF A-Star/common-task-plots"),
                        help="Directory where the heatmap images will be stored.")
    args = parser.parse_args()

    if args.detail:
        summary_records, default_summary_path = aggregate_detail_records(args.detail)
        summary_path = args.summary or default_summary_path
        if summary_path != default_summary_path:
            write_summary_csv(summary_path, summary_records)
    else:
        summary_path = args.summary
        summary_records = read_summary(summary_path)

    weights, steps, win_matrix = build_matrix(summary_records, "winRate")
    _, _, runtime_matrix = build_matrix(summary_records, "avgRunTime")

    plot_heatmap(
        win_matrix,
        weights,
        steps,
        "A* Win Rate",
        "Win Rate",
        args.output_dir / "astar-win-rate-heatmap.png",
        lambda value: f"{value * 100:.0f}%"
    )

    plot_heatmap(
        runtime_matrix,
        weights,
        steps,
        "A* Average Run Time",
        "Run Time (ms)",
        args.output_dir / "astar-runtime-heatmap.png",
        lambda value: f"{value:.0f}"
    )

    print(f"Summary CSV: {summary_path.resolve()}")
    print(f"Plots written to: {args.output_dir.resolve()}")


if __name__ == "__main__":
    main()
