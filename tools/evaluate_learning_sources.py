#!/usr/bin/env python3
"""Measure direct French/English-to-Chinese FreeDict source candidates."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import unicodedata
import xml.etree.ElementTree as ET
from collections import Counter, defaultdict
from pathlib import Path


TEI = "http://www.tei-c.org/ns/1.0"
XML = "http://www.w3.org/XML/1998/namespace"
NS = {"tei": TEI}
DEFAULT_SAMPLES = {
    "fr": ("bonjour", "être", "chat", "manger", "aimer", "travail", "apprendre", "aller"),
    "en": ("hello", "be", "cat", "eat", "love", "work", "learn", "go"),
}
HAN_RANGES = (
    (0x3400, 0x4DBF),
    (0x4E00, 0x9FFF),
    (0xF900, 0xFAFF),
    (0x20000, 0x323AF),
)


def normalized_text(value: str) -> str:
    return re.sub(r"\s+", " ", unicodedata.normalize("NFC", value).strip())


def element_text(element: ET.Element) -> str:
    return normalized_text("".join(element.itertext()))


def unique(values: list[str]) -> list[str]:
    result: list[str] = []
    seen: set[str] = set()
    for value in values:
        value = normalized_text(value)
        marker = value.casefold()
        if value and marker not in seen:
            seen.add(marker)
            result.append(value)
    return result


def contains_han(value: str) -> bool:
    return any(
        any(start <= ord(character) <= end for start, end in HAN_RANGES)
        for character in value
    )


def file_sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for block in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def percentage(value: int, total: int) -> float:
    return round(100 * value / total, 2) if total else 0.0


def entry_values(entry: ET.Element, path: str) -> list[str]:
    return unique([element_text(element) for element in entry.findall(path, NS)])


def chinese_translations(entry: ET.Element) -> tuple[list[str], int]:
    raw: list[str] = []
    empty_nodes = 0
    for citation in entry.findall(".//tei:cit[@type='trans']", NS):
        if citation.get(f"{{{XML}}}lang") != "zh":
            continue
        for quote in citation.findall("./tei:quote", NS):
            value = element_text(quote)
            if value:
                raw.append(value)
            else:
                empty_nodes += 1
    return unique(raw), empty_nodes


def source_header(root: ET.Element) -> dict:
    edition = root.find(".//tei:editionStmt/tei:edition", NS)
    extent = root.find(".//tei:fileDesc/tei:extent", NS)
    availability = root.find(".//tei:publicationStmt/tei:availability", NS)
    license_link = root.find(".//tei:publicationStmt/tei:availability//tei:ref", NS)
    source_description = root.find(".//tei:sourceDesc", NS)
    return {
        "edition": element_text(edition) if edition is not None else "",
        "declared_extent": element_text(extent) if extent is not None else "",
        "availability": element_text(availability) if availability is not None else "",
        "license_url": license_link.get("target", "") if license_link is not None else "",
        "source_description": element_text(source_description) if source_description is not None else "",
    }


def evaluate_source(
    path: Path,
    expected_language: str,
    sample_words: tuple[str, ...] | None = None,
) -> dict:
    root = ET.parse(path).getroot()
    body = root.find("./tei:text/tei:body", NS)
    if body is None:
        raise ValueError(f"No TEI body in {path}")
    body_language = body.get(f"{{{XML}}}lang", "")
    if body_language != expected_language:
        raise ValueError(
            f"Expected source language {expected_language!r}, found {body_language!r} in {path}"
        )

    entries = body.findall("./tei:entry", NS)
    headword_counts: Counter[str] = Counter()
    part_of_speech: Counter[str] = Counter()
    samples: dict[str, list[dict]] = defaultdict(list)
    anomaly_samples: dict[str, list[dict]] = defaultdict(list)
    counts: Counter[str] = Counter(entries=len(entries))

    for entry in entries:
        headwords = entry_values(entry, "./tei:form/tei:orth")
        pronunciations = entry_values(entry, "./tei:form/tei:pron")
        parts = entry_values(entry, "./tei:gramGrp/tei:pos")
        genders = entry_values(entry, "./tei:gramGrp/tei:gen")
        definitions = entry_values(entry, ".//tei:def")
        translations, empty_translation_nodes = chinese_translations(entry)
        is_noun = any(part.casefold() in {"n", "noun", "pn", "propernoun"} for part in parts)
        suspicious_pronunciations = [
            value for value in pronunciations if "<" in value or ">" in value
        ]
        non_han_translations = [value for value in translations if not contains_han(value)]

        for headword in headwords:
            headword_counts[headword.casefold()] += 1
        part_of_speech.update(parts)
        counts["headword_forms"] += len(headwords)
        counts["translation_values"] += len(translations)
        counts["empty_translation_nodes"] += empty_translation_nodes
        counts["entries_with_headword"] += bool(headwords)
        counts["entries_with_pronunciation"] += bool(pronunciations)
        counts["entries_with_part_of_speech"] += bool(parts)
        counts["entries_with_gender"] += bool(genders)
        counts["entries_with_definition"] += bool(definitions)
        counts["entries_with_chinese_translation"] += bool(translations)
        counts["noun_entries"] += is_noun
        counts["noun_entries_with_gender"] += is_noun and bool(genders)
        counts["entries_with_many_pronunciations"] += len(pronunciations) > 8
        counts["entries_with_suspicious_pronunciation"] += bool(suspicious_pronunciations)
        counts["non_han_translation_values"] += len(non_han_translations)

        candidate = {"headwords": headwords}
        if suspicious_pronunciations and len(anomaly_samples["suspicious_pronunciation"]) < 10:
            anomaly_samples["suspicious_pronunciation"].append(
                {**candidate, "pronunciations": suspicious_pronunciations}
            )
        if non_han_translations and len(anomaly_samples["non_han_translation"]) < 10:
            anomaly_samples["non_han_translation"].append(
                {**candidate, "translations": non_han_translations}
            )
        if empty_translation_nodes and len(anomaly_samples["empty_translation"]) < 10:
            anomaly_samples["empty_translation"].append(candidate)

        for sample in sample_words or DEFAULT_SAMPLES.get(expected_language, ()):
            if any(headword.casefold() == sample.casefold() for headword in headwords):
                samples[sample].append(
                    {
                        "headwords": headwords,
                        "pronunciations": pronunciations,
                        "parts_of_speech": parts,
                        "genders": genders,
                        "chinese": translations,
                        "definitions": definitions[:3],
                    }
                )

    total = counts["entries"]
    header = source_header(root)
    license_is_compatible = "creativecommons.org/licenses/by-sa/" in header["license_url"]
    coverage = {
        field: {
            "count": counts[f"entries_with_{field}"],
            "percentage": percentage(counts[f"entries_with_{field}"], total),
        }
        for field in (
            "headword",
            "chinese_translation",
            "pronunciation",
            "part_of_speech",
            "gender",
            "definition",
        )
    }
    coverage["noun_gender"] = {
        "count": counts["noun_entries_with_gender"],
        "percentage": percentage(counts["noun_entries_with_gender"], counts["noun_entries"]),
    }
    eligible = (
        license_is_compatible
        and coverage["headword"]["percentage"] >= 99
        and coverage["chinese_translation"]["percentage"] >= 95
        and coverage["part_of_speech"]["percentage"] >= 90
    )
    return {
        "path": str(path),
        "bytes": path.stat().st_size,
        "sha256": file_sha256(path),
        "source_language": body_language,
        "header": header,
        "counts": {
            **dict(sorted(counts.items())),
            "unique_headwords": len(headword_counts),
            "headwords_with_multiple_entries": sum(value > 1 for value in headword_counts.values()),
        },
        "coverage": coverage,
        "parts_of_speech": dict(part_of_speech.most_common()),
        "samples": dict(samples),
        "anomaly_samples": dict(anomaly_samples),
        "gate": {
            "eligible_for_staged_import": eligible,
            "license_requires_attribution_and_share_alike": license_is_compatible,
            "required_filters": [
                "merge duplicate headwords without merging distinct parts of speech",
                "deduplicate simplified and traditional Chinese equivalents",
                "rank and cap regional pronunciation variants",
                "reject empty or non-Han Chinese translations",
                "retain source and per-sense attribution",
            ],
        },
    }


def human_summary(report: dict) -> str:
    lines = ["Pangmao v0.6 learning-source evaluation"]
    for name, source in report["sources"].items():
        coverage = source["coverage"]
        lines.append(
            f"{name}: {source['counts']['entries']} entries; "
            f"Chinese={coverage['chinese_translation']['percentage']}%; "
            f"pronunciation={coverage['pronunciation']['percentage']}%; "
            f"POS={coverage['part_of_speech']['percentage']}%; "
            f"staged_import={source['gate']['eligible_for_staged_import']}"
        )
    return "\n".join(lines)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--french", type=Path, required=True)
    parser.add_argument("--english", type=Path, required=True)
    parser.add_argument("--json-out", type=Path)
    arguments = parser.parse_args()
    report = {
        "methodology": {
            "unit": "TEI entry",
            "policy": "measurement only; no automatic production import",
        },
        "sources": {
            "french_to_chinese": evaluate_source(arguments.french, "fr"),
            "english_to_chinese": evaluate_source(arguments.english, "en"),
        },
    }
    if arguments.json_out:
        arguments.json_out.parent.mkdir(parents=True, exist_ok=True)
        arguments.json_out.write_text(
            json.dumps(report, ensure_ascii=False, indent=2, sort_keys=True) + "\n",
            encoding="utf-8",
        )
    print(human_summary(report))


if __name__ == "__main__":
    main()
