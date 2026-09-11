#!/usr/bin/env python3
"""Recursively prefetch Maven POM metadata into local_maven_proxy's cache."""

from __future__ import annotations

import argparse
from pathlib import Path
import re
import subprocess
from xml.etree import ElementTree


ORIGINS = (
    "https://dl.google.com/dl/android/maven2/",
    "https://repo.maven.apache.org/maven2/",
    "https://plugins.gradle.org/m2/",
)
PROPERTY = re.compile(r"\$\{([^}]+)}")


def child_text(element: ElementTree.Element, name: str) -> str | None:
    child = next((item for item in element if item.tag.rsplit("}", 1)[-1] == name), None)
    return child.text.strip() if child is not None and child.text else None


def resolve(value: str | None, properties: dict[str, str]) -> str | None:
    if value is None:
        return None
    for _ in range(8):
        updated = PROPERTY.sub(lambda match: properties.get(match.group(1), match.group(0)), value)
        if updated == value:
            break
        value = updated
    return value if "${" not in value else None


def coordinates_from_pom(path: Path) -> set[tuple[str, str, str]]:
    try:
        root = ElementTree.parse(path).getroot()
    except (ElementTree.ParseError, OSError):
        return set()

    parent = next((item for item in root if item.tag.rsplit("}", 1)[-1] == "parent"), None)
    group = child_text(root, "groupId") or (child_text(parent, "groupId") if parent is not None else None)
    version = child_text(root, "version") or (child_text(parent, "version") if parent is not None else None)
    artifact = child_text(root, "artifactId")
    properties = {
        "project.groupId": group or "",
        "pom.groupId": group or "",
        "project.version": version or "",
        "pom.version": version or "",
        "project.artifactId": artifact or "",
        "pom.artifactId": artifact or "",
    }
    props = next((item for item in root if item.tag.rsplit("}", 1)[-1] == "properties"), None)
    if props is not None:
        for item in props:
            if item.text:
                properties[item.tag.rsplit("}", 1)[-1]] = item.text.strip()

    found: set[tuple[str, str, str]] = set()
    candidates = [item for item in root.iter() if item.tag.rsplit("}", 1)[-1] == "dependency"]
    if parent is not None:
        candidates.append(parent)
    for item in candidates:
        candidate = (
            resolve(child_text(item, "groupId"), properties),
            resolve(child_text(item, "artifactId"), properties),
            resolve(child_text(item, "version"), properties),
        )
        if all(candidate) and not any(char in candidate[2] for char in "[,]()"):
            found.add(candidate)  # type: ignore[arg-type]
    return found


def pom_relative(coordinate: tuple[str, str, str]) -> Path:
    group, artifact, version = coordinate
    return Path(group.replace(".", "/")) / artifact / version / f"{artifact}-{version}.pom"


def download_batch(cache: Path, relatives: list[Path], parallel: int) -> int:
    processes: list[tuple[int, subprocess.Popen[bytes]]] = []
    for origin_index, origin in enumerate(ORIGINS):
        command = [
            "curl",
            "--parallel",
            "--parallel-immediate",
            "--parallel-max",
            str(parallel),
            "--fail",
            "--location",
            "--silent",
        ]
        for relative in relatives:
            temporary = cache / "all" / relative.parent / f".{relative.name}.prefetch-{origin_index}"
            temporary.parent.mkdir(parents=True, exist_ok=True)
            command.extend(("--output", str(temporary), "--url", origin + relative.as_posix()))
        processes.append((origin_index, subprocess.Popen(command)))

    for _, process in processes:
        process.wait()

    downloaded = 0
    for relative in relatives:
        destination = cache / "all" / relative
        if destination.exists():
            continue
        winner: Path | None = None
        for origin_index, _ in processes:
            temporary = destination.parent / f".{destination.name}.prefetch-{origin_index}"
            if winner is None and temporary.exists() and temporary.stat().st_size:
                winner = temporary
            elif temporary.exists():
                temporary.unlink()
        if winner is not None:
            winner.replace(destination)
            downloaded += 1
    return downloaded


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--cache", type=Path, required=True)
    parser.add_argument("--rounds", type=int, default=12)
    parser.add_argument("--batch-size", type=int, default=180)
    parser.add_argument("--parallel", type=int, default=64)
    args = parser.parse_args()

    cache = args.cache.resolve()
    for round_number in range(1, args.rounds + 1):
        coordinates: set[tuple[str, str, str]] = set()
        for pom in (cache / "all").rglob("*.pom"):
            coordinates.update(coordinates_from_pom(pom))
        missing = [
            relative
            for relative in sorted(map(pom_relative, coordinates), key=str)
            if not (cache / "all" / relative).exists()
            and not (cache / "all" / relative).with_suffix(".pom.missing").exists()
        ]
        if not missing:
            print(f"Metadata prefetch complete after {round_number - 1} rounds", flush=True)
            return
        downloaded = 0
        for offset in range(0, len(missing), args.batch_size):
            downloaded += download_batch(cache, missing[offset : offset + args.batch_size], args.parallel)
        print(
            f"Round {round_number}: requested {len(missing)}, downloaded {downloaded}",
            flush=True,
        )
        if downloaded == 0:
            return


if __name__ == "__main__":
    main()
