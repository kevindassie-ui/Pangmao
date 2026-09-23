#!/usr/bin/env python3
"""Validate Pangmao's static web application and shipped French dictionary pack."""

from __future__ import annotations

import argparse
import hashlib
import json
import sys
from html.parser import HTMLParser
from pathlib import Path
from typing import Any
from urllib.parse import unquote, urlsplit


EXPECTED_ENTRY_COUNT = 10_923
EXPECTED_SENSE_COUNT = 11_556
EXPECTED_EQUIVALENT_COUNT = 15_168
EXPECTED_SOURCE_CODE = "FreeDict-fra-zho"
EXPECTED_SOURCE_REVISION = "2025.11.23"
EXPECTED_SOURCE_LICENSE = "CC BY-SA 3.0"
MAX_PACK_BYTES = 5 * 1024 * 1024
HAN_RANGES = (
    (0x3400, 0x4DBF),
    (0x4E00, 0x9FFF),
    (0xF900, 0xFAFF),
    (0x20000, 0x323AF),
)
REQUIRED_FILES = (
    ".nojekyll",
    "index.html",
    "manifest.webmanifest",
    "package.json",
    "styles.css",
    "sw.js",
    "data/french-pack.json",
    "src/app.js",
    "src/search-engine.js",
    "src/storage.js",
)


class ValidationError(AssertionError):
    """Raised when a deployable web artifact violates a required invariant."""


class LocalAssetParser(HTMLParser):
    def __init__(self) -> None:
        super().__init__()
        self.references: list[str] = []

    def handle_starttag(
        self,
        tag: str,
        attrs: list[tuple[str, str | None]],
    ) -> None:
        attribute = "href" if tag == "link" else "src" if tag in {"script", "img"} else None
        if attribute is None:
            return
        values = dict(attrs)
        value = values.get(attribute)
        if value:
            self.references.append(value)


def require(condition: bool, message: str) -> None:
    if not condition:
        raise ValidationError(message)


def read_json(path: Path) -> dict[str, Any]:
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, UnicodeError, json.JSONDecodeError) as error:
        raise ValidationError(f"Invalid JSON file {path}: {error}") from error
    require(isinstance(value, dict), f"Expected a JSON object in {path}")
    return value


def string_list(value: Any, label: str, *, allow_empty: bool = True) -> list[str]:
    require(isinstance(value, list), f"{label} must be an array")
    require(allow_empty or bool(value), f"{label} must not be empty")
    require(
        all(isinstance(item, str) and item.strip() for item in value),
        f"{label} must contain only non-empty strings",
    )
    require(len(value) == len(set(value)), f"{label} contains duplicate values")
    return value


def contains_han(value: str) -> bool:
    return any(
        any(start <= ord(character) <= end for start, end in HAN_RANGES)
        for character in value
    )


def canonical_entries_digest(entries: list[dict[str, Any]]) -> str:
    encoded = json.dumps(
        entries,
        ensure_ascii=False,
        separators=(",", ":"),
        sort_keys=True,
    ).encode("utf-8")
    return hashlib.sha256(encoded).hexdigest()


def validate_pack(path: Path) -> dict[str, int]:
    require(path.stat().st_size <= MAX_PACK_BYTES, f"Web dictionary exceeds {MAX_PACK_BYTES} bytes")
    pack = read_json(path)
    require(pack.get("schemaVersion") == 1, "Unsupported web dictionary schema")
    require(pack.get("language") == "fr", "Web dictionary language must be 'fr'")

    entries = pack.get("entries")
    require(isinstance(entries, list), "Dictionary entries must be an array")
    require(pack.get("entryCount") == len(entries), "entryCount does not match the entries array")
    require(len(entries) == EXPECTED_ENTRY_COUNT, f"Expected {EXPECTED_ENTRY_COUNT} French entries")
    require(
        pack.get("entriesSha256") == canonical_entries_digest(entries),
        "entriesSha256 does not match the canonical entries payload",
    )

    source = pack.get("source")
    require(isinstance(source, dict), "Dictionary source metadata is missing")
    require(source.get("code") == EXPECTED_SOURCE_CODE, "Unexpected French dictionary source")
    require(source.get("revision") == EXPECTED_SOURCE_REVISION, "Unexpected French source revision")
    require(source.get("license") == EXPECTED_SOURCE_LICENSE, "Unexpected French source licence")
    require(
        isinstance(source.get("name"), str) and bool(source["name"].strip()),
        "Dictionary source name is missing",
    )
    require(
        isinstance(source.get("url"), str) and source["url"].startswith("https://"),
        "Dictionary source URL must use HTTPS",
    )

    identifiers: set[str] = set()
    by_headword: dict[str, list[dict[str, Any]]] = {}
    sense_count = 0
    equivalent_count = 0
    expected_prefix = f"fr:{EXPECTED_SOURCE_CODE}:"

    for entry_index, entry in enumerate(entries):
        label = f"entries[{entry_index}]"
        require(isinstance(entry, dict), f"{label} must be an object")
        identifier = entry.get("id")
        require(isinstance(identifier, str) and identifier.startswith(expected_prefix), f"Invalid {label}.id")
        require(identifier not in identifiers, f"Duplicate dictionary id: {identifier}")
        identifiers.add(identifier)

        source_index = identifier.removeprefix(expected_prefix)
        require(source_index.isdigit() and int(source_index) > 0, f"Invalid source index in {identifier}")
        headword = entry.get("headword")
        require(isinstance(headword, str) and bool(headword.strip()), f"Invalid {label}.headword")
        forms = string_list(entry.get("forms"), f"{label}.forms", allow_empty=False)
        require(headword in forms, f"Headword is absent from {label}.forms")
        string_list(entry.get("pronunciations"), f"{label}.pronunciations")
        string_list(entry.get("partsOfSpeech"), f"{label}.partsOfSpeech")
        string_list(entry.get("genders"), f"{label}.genders")

        senses = entry.get("senses")
        require(isinstance(senses, list) and bool(senses), f"{label}.senses must not be empty")
        for sense_index, sense in enumerate(senses):
            sense_label = f"{label}.senses[{sense_index}]"
            require(isinstance(sense, dict), f"{sense_label} must be an object")
            string_list(sense.get("definitions"), f"{sense_label}.definitions")
            equivalents = string_list(
                sense.get("chinese"),
                f"{sense_label}.chinese",
                allow_empty=False,
            )
            require(
                all(contains_han(equivalent) for equivalent in equivalents),
                f"{sense_label}.chinese contains a non-Han value",
            )
            sense_count += 1
            equivalent_count += len(equivalents)
        by_headword.setdefault(headword.casefold(), []).append(entry)

    require(sense_count == EXPECTED_SENSE_COUNT, f"Expected {EXPECTED_SENSE_COUNT} French senses")
    require(
        equivalent_count == EXPECTED_EQUIVALENT_COUNT,
        f"Expected {EXPECTED_EQUIVALENT_COUNT} Chinese equivalents",
    )
    validate_witnesses(by_headword)
    return {
        "bytes": path.stat().st_size,
        "entries": len(entries),
        "senses": sense_count,
        "chineseEquivalents": equivalent_count,
    }


def validate_witnesses(by_headword: dict[str, list[dict[str, Any]]]) -> None:
    def entries_for(headword: str) -> list[dict[str, Any]]:
        values = by_headword.get(headword.casefold(), [])
        require(values, f"Missing dictionary witness: {headword}")
        return values

    def sense_sets(headword: str) -> list[set[str]]:
        return [
            set(sense["chinese"])
            for entry in entries_for(headword)
            for sense in entry["senses"]
        ]

    require(any("你好" in values for values in sense_sets("bonjour")), "bonjour → 你好 is missing")
    require(any("是" in values for values in sense_sets("être")), "être → 是 is missing")
    require(any("猫" in values for values in sense_sets("chat")), "chat → 猫 is missing")
    require(any("吃" in values for values in sense_sets("manger")), "manger → 吃 is missing")

    avocat_senses = sense_sets("avocat")
    lawyer_indexes = {
        index for index, values in enumerate(avocat_senses) if values.intersection({"律师", "律師"})
    }
    fruit_indexes = {
        index for index, values in enumerate(avocat_senses) if values.intersection({"牛油果", "鳄梨", "酪梨"})
    }
    require(lawyer_indexes, "avocat's legal sense is missing")
    require(fruit_indexes, "avocat's fruit sense is missing")
    require(lawyer_indexes.isdisjoint(fruit_indexes), "avocat's legal and fruit senses were merged")

    voler_senses = sense_sets("voler")
    flying_indexes = {
        index for index, values in enumerate(voler_senses) if values.intersection({"飞", "飛", "飞行", "飛行"})
    }
    stealing_indexes = {
        index for index, values in enumerate(voler_senses) if values.intersection({"偷", "偷窃", "偷竊"})
    }
    require(flying_indexes and stealing_indexes, "voler's principal meanings are incomplete")
    require(flying_indexes.isdisjoint(stealing_indexes), "voler's meanings were merged")


def local_path(web_root: Path, reference: str) -> Path | None:
    split = urlsplit(reference)
    if split.scheme or split.netloc or reference.startswith(("#", "data:", "mailto:", "tel:")):
        return None
    require(not split.path.startswith("/"), f"Root-relative asset breaks project Pages: {reference}")
    candidate = (web_root / unquote(split.path)).resolve()
    try:
        candidate.relative_to(web_root.resolve())
    except ValueError as error:
        raise ValidationError(f"Asset escapes webApp: {reference}") from error
    return candidate


def validate_static_app(web_root: Path) -> None:
    for relative in REQUIRED_FILES:
        require((web_root / relative).is_file(), f"Missing web application file: {relative}")

    parser = LocalAssetParser()
    parser.feed((web_root / "index.html").read_text(encoding="utf-8"))
    for reference in parser.references:
        path = local_path(web_root, reference)
        if path is not None:
            require(path.is_file(), f"index.html references a missing asset: {reference}")

    manifest = read_json(web_root / "manifest.webmanifest")
    for key in ("name", "short_name", "start_url", "display"):
        require(isinstance(manifest.get(key), str) and bool(manifest[key].strip()), f"Manifest {key} is missing")
    require(manifest["start_url"].startswith("./"), "Manifest start_url must be project-relative")
    if "scope" in manifest:
        require(
            isinstance(manifest["scope"], str) and manifest["scope"].startswith("./"),
            "Manifest scope must be project-relative",
        )
    require(manifest["display"] in {"standalone", "fullscreen", "minimal-ui"}, "Manifest is not installable")
    icons = manifest.get("icons")
    require(isinstance(icons, list) and bool(icons), "Manifest must declare at least one icon")
    for index, icon in enumerate(icons):
        require(isinstance(icon, dict), f"Manifest icon {index} must be an object")
        source = icon.get("src")
        require(isinstance(source, str) and bool(source), f"Manifest icon {index} has no src")
        path = local_path(web_root, source)
        require(path is not None and path.is_file(), f"Manifest icon is missing: {source}")

    service_worker = (web_root / "sw.js").read_text(encoding="utf-8")
    for asset in ("index.html", "styles.css", "src/app.js", "data/french-pack.json"):
        require(asset in service_worker, f"Service worker does not mention required offline asset: {asset}")


def main() -> int:
    default_root = Path(__file__).resolve().parents[1] / "webApp"
    parser = argparse.ArgumentParser()
    parser.add_argument("web_root", nargs="?", type=Path, default=default_root)
    arguments = parser.parse_args()
    web_root = arguments.web_root.resolve()
    try:
        require(web_root.is_dir(), f"Web application directory not found: {web_root}")
        validate_static_app(web_root)
        metrics = validate_pack(web_root / "data/french-pack.json")
    except (OSError, UnicodeError, ValidationError) as error:
        print(f"Web application validation failed: {error}", file=sys.stderr)
        return 1
    print(json.dumps({"status": "ok", **metrics}, ensure_ascii=False, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
