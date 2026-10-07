#!/usr/bin/env python3
"""Validate Pangmao's static web application and shipped French dictionary pack."""

from __future__ import annotations

import argparse
import hashlib
import json
import sys
if __package__:
    from .verify_web_voice_trial import verify_trial
else:
    from verify_web_voice_trial import verify_trial
from html.parser import HTMLParser
from pathlib import Path
from typing import Any
from urllib.parse import unquote, urlsplit


EXPECTED_ENTRY_COUNT = 10_926
EXPECTED_SENSE_COUNT = 11_562
EXPECTED_EQUIVALENT_COUNT = 15_187
EXPECTED_SOURCE_CODE = "FreeDict-fra-zho"
EXPECTED_SOURCE_REVISION = "2025.11.23"
EXPECTED_SOURCE_LICENSE = "CC BY-SA 3.0"
EXPECTED_ENRICHED_ENTRY_COUNT = 2_393
EXPECTED_FALLBACK_KEY_COUNT = 92_475
EXPECTED_FALLBACK_RECORD_COUNT = 94_560
EXPECTED_FALLBACK_DIRECT_COUNT = 56_327
EXPECTED_FALLBACK_INFERRED_COUNT = 3_480
MAX_PACK_BYTES = 5 * 1024 * 1024
MAX_FALLBACK_SHARD_BYTES = 600 * 1024
HAN_RANGES = (
    (0x3400, 0x4DBF),
    (0x4E00, 0x9FFF),
    (0xF900, 0xFAFF),
    (0x20000, 0x323AF),
)
REQUIRED_FILES = (
    ".nojekyll",
    "brand.json",
    "index.html",
    "manifest.webmanifest",
    "package.json",
    "styles.css",
    "sw.js",
    "data/french-pack.json",
    "src/app.js",
    "src/chinese-fallback.js",
    "src/reader.js",
    "src/release.js",
    "src/search-engine.js",
    "src/storage.js",
    "src/tts.js",
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


def validate_pack(path: Path, release_version: str) -> dict[str, int]:
    require(path.stat().st_size <= MAX_PACK_BYTES, f"Web dictionary exceeds {MAX_PACK_BYTES} bytes")
    pack = read_json(path)
    require(pack.get("schemaVersion") == 2, "Unsupported web dictionary schema")
    require(
        pack.get("releaseVersion") == release_version,
        "Web dictionary release does not match the application release",
    )
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
    require(
        pack.get("enrichedEntryCount") == EXPECTED_ENRICHED_ENTRY_COUNT,
        f"Expected {EXPECTED_ENRICHED_ENTRY_COUNT} Chinese-enriched French entries",
    )
    enrichment_sources = pack.get("enrichmentSources")
    require(
        isinstance(enrichment_sources, list) and len(enrichment_sources) == 1,
        "Exactly one Chinese glossary source is required",
    )
    enrichment_source = enrichment_sources[0]
    require(
        enrichment_source.get("code") == "zhwiktionary-french",
        "Unexpected Chinese glossary source",
    )
    require(
        enrichment_source.get("license") == "CC BY-SA 4.0",
        "Unexpected Chinese glossary licence",
    )
    supplement_sources = pack.get("supplementSources")
    require(
        isinstance(supplement_sources, list) and len(supplement_sources) == 1,
        "Exactly one reviewed supplement source is required",
    )
    supplement_source = supplement_sources[0]
    require(supplement_source.get("code") == "CFDICT", "Unexpected supplement source")
    require(
        supplement_source.get("revision") == "2026-09-11",
        "Unexpected supplement source revision",
    )
    require(
        supplement_source.get("license") == "CC BY-SA 3.0",
        "Unexpected supplement source licence",
    )
    editorial_sources = pack.get("editorialSources")
    require(
        isinstance(editorial_sources, list) and len(editorial_sources) == 1,
        "Exactly one Pangmao editorial source is required",
    )
    editorial_source = editorial_sources[0]
    require(
        editorial_source.get("code") == "PANGMAO-EDITORIAL",
        "Unexpected editorial source",
    )
    require(
        editorial_source.get("revision") == "2026-09-24",
        "Unexpected editorial source revision",
    )

    identifiers: set[str] = set()
    by_headword: dict[str, list[dict[str, Any]]] = {}
    sense_count = 0
    equivalent_count = 0
    enriched_entry_count = 0
    expected_prefix = f"fr:{EXPECTED_SOURCE_CODE}:"
    supplement_prefix = "fr:CFDICT:reviewed-"
    editorial_prefix = "fr:PANGMAO-EDITORIAL:"

    for entry_index, entry in enumerate(entries):
        label = f"entries[{entry_index}]"
        require(isinstance(entry, dict), f"{label} must be an object")
        identifier = entry.get("id")
        require(
            isinstance(identifier, str)
            and (
                identifier.startswith(expected_prefix)
                or identifier.startswith(supplement_prefix)
                or identifier.startswith(editorial_prefix)
            ),
            f"Invalid {label}.id",
        )
        require(identifier not in identifiers, f"Duplicate dictionary id: {identifier}")
        identifiers.add(identifier)

        if identifier.startswith(expected_prefix):
            source_index = identifier.removeprefix(expected_prefix)
            require(
                source_index.isdigit() and int(source_index) > 0,
                f"Invalid source index in {identifier}",
            )
        headword = entry.get("headword")
        require(isinstance(headword, str) and bool(headword.strip()), f"Invalid {label}.headword")
        forms = string_list(entry.get("forms"), f"{label}.forms", allow_empty=False)
        require(headword in forms, f"Headword is absent from {label}.forms")
        string_list(entry.get("pronunciations"), f"{label}.pronunciations")
        string_list(entry.get("partsOfSpeech"), f"{label}.partsOfSpeech")
        string_list(entry.get("genders"), f"{label}.genders")

        if "chineseGlosses" in entry:
            groups = entry["chineseGlosses"]
            require(isinstance(groups, list) and groups, f"{label}.chineseGlosses must not be empty")
            for group_index, group in enumerate(groups):
                group_label = f"{label}.chineseGlosses[{group_index}]"
                require(isinstance(group, dict), f"{group_label} must be an object")
                require(
                    isinstance(group.get("partOfSpeech"), str) and group["partOfSpeech"].strip(),
                    f"{group_label}.partOfSpeech is missing",
                )
                glosses = string_list(group.get("glosses"), f"{group_label}.glosses", allow_empty=False)
                require(
                    all(contains_han(gloss) for gloss in glosses),
                    f"{group_label}.glosses contains no Chinese text",
                )
            enriched_entry_count += 1

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
    require(
        enriched_entry_count == EXPECTED_ENRICHED_ENTRY_COUNT,
        "enrichedEntryCount does not match the enriched entries",
    )
    validate_witnesses(by_headword)
    return {
        "bytes": path.stat().st_size,
        "entries": len(entries),
        "senses": sense_count,
        "chineseEquivalents": equivalent_count,
        "chineseExplanations": enriched_entry_count,
    }


def fallback_shard_index(value: str, shard_count: int) -> int:
    value_hash = 2_166_136_261
    for character in value:
        value_hash ^= ord(character)
        value_hash = (value_hash * 16_777_619) & 0xFFFFFFFF
    return value_hash % shard_count


def validate_fallback(web_root: Path) -> dict[str, int]:
    fallback_root = web_root / "data/chinese-fallback"
    manifest = read_json(fallback_root / "manifest.json")
    require(manifest.get("schemaVersion") == 1, "Unsupported fallback schema")
    require(manifest.get("keyCount") == EXPECTED_FALLBACK_KEY_COUNT, "Unexpected fallback key count")
    require(
        manifest.get("recordCount") == EXPECTED_FALLBACK_RECORD_COUNT,
        "Unexpected fallback record count",
    )
    require(
        manifest.get("directEntryCount") == EXPECTED_FALLBACK_DIRECT_COUNT,
        "Unexpected direct fallback entry count",
    )
    require(
        manifest.get("inferredEntryCount") == EXPECTED_FALLBACK_INFERRED_COUNT,
        "Unexpected inferred fallback entry count",
    )
    shards = manifest.get("shards")
    shard_count = manifest.get("shardCount")
    require(isinstance(shard_count, int) and shard_count == 32, "Fallback must use 32 shards")
    require(isinstance(shards, list) and len(shards) == shard_count, "Fallback shard list is incomplete")

    key_count = 0
    record_count = 0
    witness = None
    for index, metadata in enumerate(shards):
        require(isinstance(metadata, dict), f"Fallback shard metadata {index} is invalid")
        expected_name = f"{index:02x}.json"
        require(metadata.get("file") == expected_name, f"Unexpected fallback shard name: {metadata}")
        path = fallback_root / expected_name
        require(path.is_file(), f"Missing fallback shard: {expected_name}")
        encoded = path.read_bytes()
        require(len(encoded) <= MAX_FALLBACK_SHARD_BYTES, f"Fallback shard too large: {expected_name}")
        require(metadata.get("bytes") == len(encoded), f"Fallback shard byte mismatch: {expected_name}")
        require(
            metadata.get("sha256") == hashlib.sha256(encoded).hexdigest(),
            f"Fallback shard hash mismatch: {expected_name}",
        )
        payload = read_json(path)
        require(payload.get("schemaVersion") == 1, f"Unsupported shard schema: {expected_name}")
        entries = payload.get("entries")
        require(isinstance(entries, dict), f"Fallback entries missing: {expected_name}")
        local_records = sum(len(records) for records in entries.values())
        require(metadata.get("keyCount") == len(entries), f"Fallback key mismatch: {expected_name}")
        require(metadata.get("recordCount") == local_records, f"Fallback record mismatch: {expected_name}")
        for query, records in entries.items():
            require(
                fallback_shard_index(query, shard_count) == index,
                f"Fallback key in wrong shard: {query}",
            )
            require(isinstance(records, list) and records, f"Fallback result is empty: {query}")
            for record in records:
                require(record.get("kind") in {"direct", "inferred"}, f"Invalid fallback kind: {query}")
                if record.get("kind") == "inferred":
                    require(record.get("confidence") == "strong", f"Weak inference shipped: {query}")
                    string_list(record.get("possibleFrench"), f"fallback[{query}].possibleFrench", allow_empty=False)
                    require(record.get("inferredFrom"), f"Inference basis missing: {query}")
                else:
                    string_list(record.get("french"), f"fallback[{query}].french", allow_empty=False)
        if "臭屁" in entries:
            witness = entries["臭屁"]
        key_count += len(entries)
        record_count += local_records

    require(key_count == EXPECTED_FALLBACK_KEY_COUNT, "Fallback total key count mismatch")
    require(record_count == EXPECTED_FALLBACK_RECORD_COUNT, "Fallback total record count mismatch")
    require(isinstance(witness, list) and witness, "臭屁 fallback witness is missing")
    inferred = next((record for record in witness if record.get("kind") == "inferred"), None)
    require(inferred is not None, "臭屁 must be a labelled inference")
    require(inferred.get("inferredFrom") == "拿大", "臭屁 inference basis changed")
    require("arrogant" in inferred.get("possibleFrench", []), "臭屁 inference is incomplete")
    return {"fallbackKeys": key_count, "fallbackRecords": record_count}


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
    affiche_senses = sense_sets("affiche")
    require(any("海报" in values for values in affiche_senses), "affiche → 海报 is missing")
    require(any("告示" in values for values in affiche_senses), "affiche → 告示 is missing")

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

    banane_senses = sense_sets("banane")
    require(any("香蕉" in values for values in banane_senses), "banane → 香蕉 is missing")
    require(any("腰包" in values for values in banane_senses), "banane → 腰包 is missing")
    require(
        any(values.intersection({"傻瓜", "笨蛋", "呆瓜"}) for values in banane_senses),
        "banane's playful insult sense is missing",
    )
    require(
        all("香蕉人" not in values for values in banane_senses),
        "banane still exposes the unsuitable 香蕉人 sense",
    )
    require(any("放屁" in values for values in sense_sets("péter")), "péter → 放屁 is missing")
    require(
        any(values.intersection({"欺骗", "坑骗", "耍"}) for values in sense_sets("bananer")),
        "bananer's colloquial deception sense is missing",
    )


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


def validate_static_app(web_root: Path) -> str:
    for relative in REQUIRED_FILES:
        require((web_root / relative).is_file(), f"Missing web application file: {relative}")

    html = (web_root / "index.html").read_text(encoding="utf-8")
    package = read_json(web_root / "package.json")
    release_version = package.get("version")
    require(
        isinstance(release_version, str) and bool(release_version.strip()),
        "Web package version is missing",
    )
    parser = LocalAssetParser()
    parser.feed(html)
    for reference in parser.references:
        path = local_path(web_root, reference)
        if path is not None:
            require(path.is_file(), f"index.html references a missing asset: {reference}")
    for element_id in (
        "readerView",
        "readerInput",
        "readerFile",
        "readerAnalyzeButton",
        "readerSentences",
        "voiceSelect",
        "voiceTestButton",
        "missedSearchEnabled",
        "missedSearchList",
        "missedSearchStatus",
        "missedSearchCopy",
        "missedSearchExport",
        "missedSearchClear",
    ):
        require(f'id="{element_id}"' in html, f"Reader control is missing: {element_id}")
    for asset in ("styles.css", "manifest.webmanifest", "src/app.js"):
        require(
            f"{asset}?v={release_version}" in html,
            f"index.html does not version {asset} with release {release_version}",
        )

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

    brand = read_json(web_root / "brand.json")
    for key in (
        "id",
        "theme",
        "title",
        "subtitle",
        "icon",
        "iconAlt",
        "themeColor",
        "welcomeEyebrow",
        "welcomeTitle",
        "welcomeBody",
    ):
        require(isinstance(brand.get(key), str) and brand[key].strip(), f"Brand {key} is missing")
    require(
        brand["themeColor"].startswith("#") and len(brand["themeColor"]) == 7,
        "Brand themeColor must be a hex colour",
    )
    for key in ("icon", "mascot"):
        reference = brand.get(key)
        if not reference:
            continue
        path = local_path(web_root, reference)
        require(path is not None and path.is_file(), f"Brand asset is missing: {reference}")
    seasonal = brand.get("seasonal")
    if seasonal is not None:
        require(isinstance(seasonal, dict), "Brand seasonal configuration must be an object")
        require(
            isinstance(seasonal.get("id"), str) and seasonal["id"].strip(),
            "Brand seasonal id is missing",
        )
        for key in ("osmanthus", "mooncakes"):
            reference = seasonal.get(key)
            require(
                isinstance(reference, str) and reference.strip(),
                f"Brand seasonal asset is missing: {key}",
            )
            path = local_path(web_root, reference)
            require(path is not None and path.is_file(), f"Brand asset is missing: {reference}")

    service_worker = (web_root / "sw.js").read_text(encoding="utf-8")
    for asset in (
        "index.html",
        "brand.json",
        "styles.css",
        "src/app.js",
        "src/chinese-fallback.js",
        "src/reader.js",
        "src/missed-searches.js",
        "src/release.js",
        "src/tts.js",
        "data/french-pack.json",
        "data/chinese-fallback/manifest.json",
    ):
        require(asset in service_worker, f"Service worker does not mention required offline asset: {asset}")
    app_source = (web_root / "src/app.js").read_text(encoding="utf-8")
    release_source = (web_root / "src/release.js").read_text(encoding="utf-8")
    require(
        f'WEB_VERSION = "{release_version}"' in release_source,
        "release.js does not match package.json",
    )
    require(
        f'RELEASE_VERSION = "{release_version}"' in service_worker,
        "Service worker cache release does not match package.json",
    )
    for module in (
        "chinese-fallback.js",
        "reader.js",
        "missed-searches.js",
        "release.js",
        "search-engine.js",
        "storage.js",
        "tts.js",
    ):
        require(
            f'{module}?v={release_version}' in app_source,
            f"Application import is not versioned: {module}",
        )
    require("segmentFrenchText" in app_source, "Reader is not connected to the application")
    require("record.pinyin" not in app_source, "Chinese fallback results must not display pinyin")
    require(
        "speakWithFrenchVoice" in app_source,
        "French-only TTS selection is not connected to the application",
    )
    if brand.get("mascot"):
        mascot_asset = str(brand["mascot"]).removeprefix("./")
        require(
            mascot_asset in service_worker,
            f"Service worker does not precache the brand mascot: {mascot_asset}",
        )
    if seasonal is not None:
        for key in ("osmanthus", "mooncakes"):
            seasonal_asset = str(seasonal[key]).removeprefix("./")
            require(
                seasonal_asset in service_worker,
                f"Service worker does not precache the seasonal asset: {seasonal_asset}",
            )
    verify_trial(web_root)
    return release_version


def main() -> int:
    default_root = Path(__file__).resolve().parents[1] / "webApp"
    parser = argparse.ArgumentParser()
    parser.add_argument("web_root", nargs="?", type=Path, default=default_root)
    arguments = parser.parse_args()
    web_root = arguments.web_root.resolve()
    try:
        require(web_root.is_dir(), f"Web application directory not found: {web_root}")
        release_version = validate_static_app(web_root)
        metrics = validate_pack(web_root / "data/french-pack.json", release_version)
        metrics.update(validate_fallback(web_root))
    except (OSError, UnicodeError, ValidationError) as error:
        print(f"Web application validation failed: {error}", file=sys.stderr)
        return 1
    print(json.dumps({"status": "ok", **metrics}, ensure_ascii=False, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
