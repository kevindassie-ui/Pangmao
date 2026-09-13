#!/usr/bin/env python3
"""Evaluate direct Tatoeba Mandarin–French links for Pangmao.

This tool is intentionally read-only by default. It joins Pangmao's pinned
Mandarin–English corpus to an official French sentence export through Tatoeba's
direct link graph, then applies a conservative sentence-review gate. It never
matches translations by fuzzy text or through English.
"""

from __future__ import annotations

import argparse
import bz2
import csv
import hashlib
import json
import re
import sqlite3
import tarfile
import unicodedata
from collections import Counter, defaultdict
from contextlib import contextmanager
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import IO, Iterable, Iterator


@dataclass(frozen=True)
class ReviewTotals:
    positive: int = 0
    negative: int = 0
    unsure: int = 0


@dataclass(frozen=True)
class SelectedTranslation:
    chinese_id: int
    chinese: str
    french_id: int
    french: str
    positive_reviews: int


def normalized_text(value: str) -> str:
    return re.sub(r"\s+", " ", unicodedata.normalize("NFC", value).strip())


def file_sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for block in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


@contextmanager
def text_rows(path: Path) -> Iterator[IO[str]]:
    if path.suffix == ".bz2":
        with bz2.open(path, "rt", encoding="utf-8", newline="") as handle:
            yield handle
    else:
        with path.open(encoding="utf-8-sig", newline="") as handle:
            yield handle


@contextmanager
def link_rows(path: Path) -> Iterator[Iterable[str]]:
    if path.name.endswith((".tar.bz2", ".tbz2")):
        with tarfile.open(path, "r:bz2") as archive:
            members = [member for member in archive.getmembers() if member.isfile()]
            if len(members) != 1:
                raise ValueError(f"Expected one link file in {path}, found {len(members)}")
            raw = archive.extractfile(members[0])
            if raw is None:
                raise ValueError(f"Cannot read {members[0].name} from {path}")
            yield (line.decode("utf-8").rstrip("\r\n") for line in raw)
    else:
        with path.open(encoding="utf-8-sig") as handle:
            yield (line.rstrip("\r\n") for line in handle)


def load_pinned_chinese(path: Path) -> tuple[dict[int, str], dict[int, str], dict[str, int]]:
    chinese: dict[int, str] = {}
    english: dict[int, str] = {}
    counters: Counter[str] = Counter()
    with text_rows(path) as handle:
        for line_number, row in enumerate(csv.reader(handle, delimiter="\t"), start=1):
            counters["rows"] += 1
            if len(row) < 4:
                counters["malformed_rows"] += 1
                continue
            chinese_id = int(row[0])
            sentence = normalized_text(row[1])
            translation = normalized_text(row[3])
            if not sentence or not translation:
                counters["empty_rows"] += 1
                continue
            previous = chinese.setdefault(chinese_id, sentence)
            if previous != sentence:
                raise ValueError(
                    f"Chinese sentence id {chinese_id} changes text at {path}:{line_number}"
                )
            english.setdefault(chinese_id, translation)
    counters["distinct_chinese_ids"] = len(chinese)
    counters["duplicate_translation_rows"] = counters["rows"] - len(chinese)
    return chinese, english, dict(counters)


def load_language_sentences(
    path: Path,
    language: str,
) -> tuple[dict[int, str], dict[str, int]]:
    sentences: dict[int, str] = {}
    counters: Counter[str] = Counter()
    with text_rows(path) as handle:
        for line_number, row in enumerate(csv.reader(handle, delimiter="\t"), start=1):
            counters["rows"] += 1
            if len(row) < 3:
                counters["malformed_rows"] += 1
                continue
            sentence_id = int(row[0])
            if row[1] != language:
                counters["wrong_language_rows"] += 1
                continue
            sentence = normalized_text(row[2])
            if not sentence:
                counters["empty_rows"] += 1
                continue
            previous = sentences.setdefault(sentence_id, sentence)
            if previous != sentence:
                raise ValueError(
                    f"French sentence id {sentence_id} changes text at {path}:{line_number}"
                )
    counters["distinct_sentence_ids"] = len(sentences)
    return sentences, dict(counters)


def load_french_sentences(path: Path) -> tuple[dict[int, str], dict[str, int]]:
    return load_language_sentences(path, "fra")


def load_direct_links(
    path: Path,
    chinese_ids: set[int],
    french_ids: set[int],
) -> tuple[dict[int, set[int]], dict[str, int]]:
    links: dict[int, set[int]] = defaultdict(set)
    counters: Counter[str] = Counter()
    with link_rows(path) as rows:
        for line_number, line in enumerate(rows, start=1):
            counters["rows"] += 1
            parts = line.split("\t")
            if len(parts) != 2:
                counters["malformed_rows"] += 1
                continue
            source, target = map(int, parts)
            if source in chinese_ids and target in french_ids:
                before = len(links[source])
                links[source].add(target)
                counters["duplicate_direct_rows"] += int(len(links[source]) == before)
    counters["direct_pairs"] = sum(len(values) for values in links.values())
    counters["chinese_ids_with_direct_french"] = len(links)
    counters["distinct_linked_french_ids"] = len(
        {identifier for values in links.values() for identifier in values}
    )
    return dict(links), dict(counters)


def load_reviews(path: Path, sentence_ids: set[int]) -> tuple[dict[int, ReviewTotals], dict[str, int]]:
    latest: dict[tuple[str, int], tuple[str, int]] = {}
    counters: Counter[str] = Counter()
    with text_rows(path) as handle:
        for row in csv.reader(handle, delimiter="\t"):
            counters["rows"] += 1
            if len(row) < 3:
                counters["malformed_rows"] += 1
                continue
            sentence_id = int(row[1])
            if sentence_id not in sentence_ids:
                continue
            review = int(row[2])
            if review not in {-1, 0, 1}:
                counters["invalid_review_rows"] += 1
                continue
            modified = row[4] if len(row) > 4 else ""
            key = row[0], sentence_id
            previous = latest.get(key)
            if previous is None or modified >= previous[0]:
                latest[key] = modified, review

    by_sentence: dict[int, Counter[int]] = defaultdict(Counter)
    for (_, sentence_id), (_, review) in latest.items():
        by_sentence[sentence_id][review] += 1
    totals = {
        sentence_id: ReviewTotals(
            positive=values[1],
            negative=values[-1],
            unsure=values[0],
        )
        for sentence_id, values in by_sentence.items()
    }
    counters["relevant_latest_reviews"] = len(latest)
    counters["reviewed_sentence_ids"] = len(totals)
    return totals, dict(counters)


def select_translations(
    chinese: dict[int, str],
    french: dict[int, str],
    links: dict[int, set[int]],
    reviews: dict[int, ReviewTotals],
    minimum_positive: int = 1,
) -> tuple[list[SelectedTranslation], dict[str, int]]:
    selected: list[SelectedTranslation] = []
    counters: Counter[str] = Counter()
    for chinese_id, french_ids in sorted(links.items()):
        eligible: list[tuple[int, int, int]] = []
        for french_id in french_ids:
            totals = reviews.get(french_id, ReviewTotals())
            if totals.negative:
                counters["pairs_rejected_negative_review"] += 1
            elif totals.positive < minimum_positive:
                counters["pairs_without_required_positive_review"] += 1
            else:
                eligible.append((-totals.positive, totals.unsure, french_id))
        if not eligible:
            continue
        counters["chinese_ids_with_multiple_eligible_translations"] += int(len(eligible) > 1)
        _, _, french_id = min(eligible)
        totals = reviews[french_id]
        selected.append(
            SelectedTranslation(
                chinese_id=chinese_id,
                chinese=chinese[chinese_id],
                french_id=french_id,
                french=french[french_id],
                positive_reviews=totals.positive,
            )
        )
    counters["selected_translations"] = len(selected)
    counters["selected_with_two_or_more_positive_reviews"] = sum(
        item.positive_reviews >= 2 for item in selected
    )
    return selected, dict(counters)


def projection(path: Path | None, selected: list[SelectedTranslation]) -> dict[str, int]:
    if path is None:
        return {}
    connection = sqlite3.connect(f"file:{path}?mode=ro", uri=True)
    existing = {
        int(chinese_id): bool(french)
        for chinese_id, french in connection.execute(
            "SELECT tatoeba_chinese_id, french FROM examples WHERE tatoeba_chinese_id > 0"
        )
    }
    bilingual_before = connection.execute(
        "SELECT count(*) FROM examples WHERE french <> ''"
    ).fetchone()[0]
    total = connection.execute("SELECT count(*) FROM examples").fetchone()[0]
    connection.close()
    already_french = sum(existing.get(item.chinese_id, False) for item in selected)
    fillable = sum(item.chinese_id in existing and not existing[item.chinese_id] for item in selected)
    return {
        "examples_total": total,
        "bilingual_before": bilingual_before,
        "selected_already_french": already_french,
        "selected_missing_from_dictionary": len(selected) - already_french - fillable,
        "fillable_in_french": fillable,
        "projected_bilingual_after": bilingual_before + fillable,
    }


def evaluate(
    english_pairs: Path,
    french_sentences: Path,
    links_path: Path,
    reviews_path: Path,
    *,
    mandarin_sentences: Path | None = None,
    dictionary: Path | None = None,
    minimum_positive: int = 1,
    include_hashes: bool = False,
) -> tuple[dict, list[SelectedTranslation]]:
    chinese, english, english_counts = load_pinned_chinese(english_pairs)
    french, french_counts = load_french_sentences(french_sentences)
    links, link_counts = load_direct_links(
        links_path,
        set(chinese),
        set(french),
    )
    selection_links = links
    source_consistency: dict[str, int | str] = {
        "policy": "not checked; provide --mandarin-sentences before importing",
    }
    current_mandarin_counts: dict[str, int] = {}
    if mandarin_sentences:
        current_mandarin, current_mandarin_counts = load_language_sentences(
            mandarin_sentences,
            "cmn",
        )
        missing = {identifier for identifier in links if identifier not in current_mandarin}
        changed = {
            identifier
            for identifier in links
            if identifier in current_mandarin
            and chinese[identifier] != current_mandarin[identifier]
        }
        selection_links = {
            identifier: values
            for identifier, values in links.items()
            if identifier not in missing and identifier not in changed
        }
        source_consistency = {
            "policy": "require current Mandarin id and normalized text to match the pinned corpus",
            "linked_chinese_ids_missing_from_current_export": len(missing),
            "linked_chinese_ids_with_changed_text": len(changed),
            "linked_chinese_ids_validated": len(selection_links),
        }
    linked_french_ids = {identifier for values in links.values() for identifier in values}
    reviews, review_counts = load_reviews(reviews_path, linked_french_ids)
    selected, selection_counts = select_translations(
        chinese,
        french,
        selection_links,
        reviews,
        minimum_positive,
    )

    exact_english_matches = sum(
        normalized_text(item.french).casefold() == normalized_text(english[item.chinese_id]).casefold()
        for item in selected
    )
    suspicious_markup = sum(
        bool(re.search(r"https?://|<[^>]+>|\{\{|\}\}", item.french)) for item in selected
    )
    report = {
        "policy": {
            "join": "direct Tatoeba sentence links by stable numeric id",
            "minimum_positive_reviews": minimum_positive,
            "maximum_negative_reviews": 0,
            "tie_break": "most positive reviews, fewest unsure reviews, lowest French id",
            "warning": "Tatoeba sentence reviews are experimental and rate sentences, not links",
        },
        "pinned_mandarin_english": english_counts,
        "french_export": french_counts,
        "current_mandarin_export": current_mandarin_counts,
        "direct_alignment": link_counts,
        "source_consistency": source_consistency,
        "reviews": review_counts,
        "selection": selection_counts,
        "projection": projection(dictionary, selected),
        "quality_flags": {
            "selected_equal_to_pinned_english": exact_english_matches,
            "selected_with_markup_or_url": suspicious_markup,
        },
        "sample": [
            asdict(item)
            for item in sorted(
                selected,
                key=lambda item: hashlib.sha256(str(item.chinese_id).encode()).digest(),
            )[:20]
        ],
    }
    if include_hashes:
        report["source_sha256"] = {
            "english_pairs": file_sha256(english_pairs),
            "french_sentences": file_sha256(french_sentences),
            "links": file_sha256(links_path),
            "reviews": file_sha256(reviews_path),
        }
        if mandarin_sentences:
            report["source_sha256"]["mandarin_sentences"] = file_sha256(mandarin_sentences)
    return report, selected


def write_selected(path: Path, selected: list[SelectedTranslation]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.writer(handle, delimiter="\t", lineterminator="\n")
        writer.writerow(("chinese_id", "chinese", "french_id", "french", "positive_reviews"))
        for item in selected:
            writer.writerow(
                (
                    item.chinese_id,
                    item.chinese,
                    item.french_id,
                    item.french,
                    item.positive_reviews,
                )
            )


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--english-pairs", required=True, type=Path)
    parser.add_argument("--french-sentences", required=True, type=Path)
    parser.add_argument("--mandarin-sentences", type=Path)
    parser.add_argument("--links", required=True, type=Path)
    parser.add_argument("--reviews", required=True, type=Path)
    parser.add_argument("--dictionary", type=Path)
    parser.add_argument("--minimum-positive", type=int, default=1)
    parser.add_argument("--json-out", type=Path)
    parser.add_argument("--selected-out", type=Path)
    parser.add_argument("--include-hashes", action="store_true")
    args = parser.parse_args()
    if args.minimum_positive < 1:
        parser.error("--minimum-positive must be at least 1")

    report, selected = evaluate(
        args.english_pairs,
        args.french_sentences,
        args.links,
        args.reviews,
        mandarin_sentences=args.mandarin_sentences,
        dictionary=args.dictionary,
        minimum_positive=args.minimum_positive,
        include_hashes=args.include_hashes,
    )
    rendered = json.dumps(report, ensure_ascii=False, indent=2, sort_keys=True)
    print(rendered)
    if args.json_out:
        args.json_out.parent.mkdir(parents=True, exist_ok=True)
        args.json_out.write_text(f"{rendered}\n", encoding="utf-8")
    if args.selected_out:
        write_selected(args.selected_out, selected)


if __name__ == "__main__":
    main()
