#!/usr/bin/env python3
import argparse
import csv
import math
from collections import defaultdict
from pathlib import Path
from typing import Dict, Iterable, List, Tuple

import matplotlib.pyplot as plt


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Aggregate and visualize dynamic-weighting experiment results."
    )
    parser.add_argument(
        "--detail",
        required=True,
        type=Path,
        help="Path to the merged detail CSV.",
    )
    parser.add_argument(
        "--summary",
        type=Path,
        help="path for the summary CSV output.",
    )
    parser.add_argument(
        "--output-dir",
        type=Path,
        default=Path("Experiment-Results/MFF A-Star/dynamic-weighting-plots"),
        help="Directory where heatmaps will be stored.",
    )
    return parser.parse_args()


def aggregate_detail(detail_path: Path) -> List[Dict[str, float]]:
    aggregations: Dict[Tuple[int, float, float, float, bool], Dict[str, float]] = defaultdict(
        lambda: {
            "gamesPlayed": 0,
            "wins": 0,
            "totalRunTime": 0.0,
            "totalPlanningTime": 0.0,
            "totalGameTicks": 0.0,
            "totalNodesEvaluated": 0.0,
            "totalMostBacktracked": 0.0,
            "totalPercentageTravelled": 0.0,
        }
    )

    with detail_path.open("r", newline="") as csvfile:
        reader = csv.DictReader(csvfile)
        for row in reader:
            try:
                search_steps = int(row["searchSteps"])
                start_weight = float(row["startWeight"])
                end_weight = float(row["endWeight"])
                exponent = float(row["exponent"])
                dynamic_enabled = row["dynamicEnabled"].strip().lower() == "true"

                win = row["win"].strip().lower() == "true"
                run_time = float(row["run time"])
                planning_time = float(row["planning time"])
                game_ticks = float(row["game ticks"])
                nodes_evaluated = float(row["nodes evaluated"])
                most_backtracked = float(row.get("most backtracked nodes", 0.0))
                percentage_travelled = float(row["% travelled"])
            except (KeyError, ValueError) as exc:
                raise ValueError(f"Malformed row in detail CSV: {row}") from exc

            key = (search_steps, start_weight, end_weight, exponent, dynamic_enabled)
            agg = aggregations[key]
            agg["gamesPlayed"] += 1
            agg["wins"] += 1 if win else 0
            agg["totalRunTime"] += run_time
            agg["totalPlanningTime"] += planning_time
            agg["totalGameTicks"] += game_ticks
            agg["totalNodesEvaluated"] += nodes_evaluated
            agg["totalMostBacktracked"] += most_backtracked
            agg["totalPercentageTravelled"] += percentage_travelled

    records: List[Dict[str, float]] = []
    for (search_steps, start_weight, end_weight, exponent, dynamic_enabled), agg in aggregations.items():
        games = agg["gamesPlayed"]
        wins = agg["wins"]
        win_rate = wins / games if games else 0.0

        def average(total: float) -> float:
            return total / games if games else 0.0

        records.append({
            "searchSteps": search_steps,
            "startWeight": start_weight,
            "endWeight": end_weight,
            "exponent": exponent,
            "dynamicEnabled": dynamic_enabled,
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
    return records


def write_summary(summary_path: Path, records: Iterable[Dict[str, float]]) -> None:
    fieldnames = [
        "searchSteps",
        "startWeight",
        "endWeight",
        "exponent",
        "dynamicEnabled",
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
        for record in sorted(
                records,
                key=lambda r: (
                        r["searchSteps"],
                        r["exponent"],
                        r["startWeight"],
                        r["endWeight"],
                        not r["dynamicEnabled"],
                ),
        ):
            writer.writerow(record)


def aggregate_by_weight_pairs(records: List[Dict[str, float]]):
    pair_stats: Dict[Tuple[float, float], Dict[str, float]] = defaultdict(
        lambda: {
            "count": 0,
            "sumWinRate": 0.0,
            "sumRunTime": 0.0,
            "sumNodes": 0.0,
        }
    )
    start_weights = set()
    end_weights = set()

    for record in records:
        if not record["dynamicEnabled"]:
            continue
        key = (record["startWeight"], record["endWeight"])
        stats = pair_stats[key]
        stats["count"] += 1
        stats["sumWinRate"] += record["winRate"]
        stats["sumRunTime"] += record["avgRunTime"]
        stats["sumNodes"] += record["avgNodesEvaluated"]
        start_weights.add(record["startWeight"])
        end_weights.add(record["endWeight"])

    return pair_stats, sorted(start_weights), sorted(end_weights)


def build_heatmap_matrix(pair_stats: Dict[Tuple[float, float], Dict[str, float]],
                         start_weights: List[float],
                         end_weights: List[float],
                         metric: str) -> List[List[float]]:
    matrix: List[List[float]] = []
    for end_weight in end_weights:
        row = []
        for start_weight in start_weights:
            stats = pair_stats.get((start_weight, end_weight))
            if not stats or stats["count"] == 0:
                row.append(float("nan"))
                continue
            if metric == "winRate":
                value = stats["sumWinRate"] / stats["count"]
            elif metric == "avgRunTime":
                value = stats["sumRunTime"] / stats["count"]
            elif metric == "avgNodesEvaluated":
                value = stats["sumNodes"] / stats["count"]
            else:
                value = float("nan")
            row.append(value)
        matrix.append(row)
    return matrix


def plot_metric_heatmap(output_dir: Path,
                        start_weights: List[float],
                        end_weights: List[float],
                        metric: str,
                        label: str,
                        formatter,
                        matrix: List[List[float]]) -> None:
    fig, ax = plt.subplots(figsize=(12, 6))
    heatmap = ax.imshow(matrix, aspect="auto", origin="lower", cmap="viridis")
    cbar = fig.colorbar(heatmap, ax=ax)
    cbar.set_label(label)

    ax.set_xticks(range(len(start_weights)))
    ax.set_xticklabels([f"{w:.2f}" for w in start_weights])
    ax.set_yticks(range(len(end_weights)))
    ax.set_yticklabels([f"{w:.2f}" for w in end_weights])
    ax.set_xlabel("startWeight")
    ax.set_ylabel("endWeight")
    ax.set_title(f"{label} (averaged across exponents)")

    for y, end_weight in enumerate(end_weights):
        for x, start_weight in enumerate(start_weights):
            value = matrix[y][x]
            if math.isnan(value):
                continue
            ax.text(x, y, formatter(value), ha="center", va="center", color="white", fontsize=8)

    fig.tight_layout()
    output_dir.mkdir(parents=True, exist_ok=True)
    fig.savefig(output_dir / f"dynamic-weight-{metric}.png", dpi=300)
    plt.close(fig)


def main() -> None:
    args = parse_args()

    records = aggregate_detail(args.detail)

    summary_path = args.summary
    if summary_path is None:
        summary_path = args.detail.with_name(args.detail.stem + "-summary.csv")
    write_summary(summary_path, records)

    pair_stats, start_weights, end_weights = aggregate_by_weight_pairs(records)
    if start_weights and end_weights:
        metric_configs = {
            "winRate": ("Win Rate", lambda v: f"{v * 100:.0f}%"),
            "avgRunTime": ("Average Run Time (ms)", lambda v: f"{v:.0f}"),
            "avgNodesEvaluated": ("Average Nodes Evaluated", lambda v: f"{v:.0f}"),
        }
        for metric, (label, formatter) in metric_configs.items():
            matrix = build_heatmap_matrix(pair_stats, start_weights, end_weights, metric)
            plot_metric_heatmap(args.output_dir, start_weights, end_weights, metric, label, formatter, matrix)
    else:
        print("No dynamic-weighting data found to plot.")

    print(f"Summary CSV: {summary_path.resolve()}")
    print(f"Plots written to: {args.output_dir.resolve()}")


if __name__ == "__main__":
    main()

