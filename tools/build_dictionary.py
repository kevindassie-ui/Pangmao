#!/usr/bin/env python3
"""Build Pangmao's immutable offline dictionary database.

The script intentionally accepts local source paths. It never scrapes Pleco and it
does not automate MDBG downloads. See tools/SOURCES.md for provenance and licenses.
"""

from __future__ import annotations

import argparse
import csv
import json
import re
import sqlite3
import unicodedata
import xml.etree.ElementTree as ET
from collections import Counter, defaultdict
from dataclasses import dataclass, field
from pathlib import Path
from typing import Iterable, Sequence


CEDICT_LINE = re.compile(r"^(\S+)\s+(\S+)\s+\[([^]]+)]\s+/(.*)/\s*$")
HAN_RANGES = (
    (0x3400, 0x4DBF),
    (0x4E00, 0x9FFF),
    (0xF900, 0xFAFF),
    (0x20000, 0x323AF),
)
DEFAULT_DEFINITION_SOURCES = {"en": "CC-CEDICT", "fr": "CFDICT"}
TEI_NAMESPACE = "http://www.tei-c.org/ns/1.0"
XML_NAMESPACE = "http://www.w3.org/XML/1998/namespace"
TEI_NAMESPACES = {"tei": TEI_NAMESPACE}
LEARNING_SOURCE_DETAILS = {
    "FreeDict-fra-zho": (
        "fr",
        "FreeDict français-chinois",
        "https://download.freedict.org/dictionaries/fra-zho/",
        "CC BY-SA 3.0",
    ),
    "FreeDict-eng-zho": (
        "en",
        "FreeDict anglais-chinois",
        "https://download.freedict.org/dictionaries/eng-zho/",
        "CC BY-SA 3.0",
    ),
}
MAX_LEARNING_PRONUNCIATIONS = 4


def is_han(character: str) -> bool:
    if not character:
        return False
    codepoint = ord(character)
    return any(start <= codepoint <= end for start, end in HAN_RANGES)


def pinyin_plain(value: str) -> str:
    value = value.replace("u:", "v").replace("ü", "v").lower()
    value = "".join(
        char
        for char in unicodedata.normalize("NFD", value)
        if unicodedata.category(char) != "Mn" and not char.isdigit()
    )
    return re.sub(r"[^a-zv]+", "", value)


def unique(items: Iterable[str]) -> list[str]:
    seen: set[str] = set()
    result: list[str] = []
    for item in items:
        item = item.strip()
        if item and item not in seen:
            seen.add(item)
            result.append(item)
    return result


@dataclass
class MutableEntry:
    traditional: str
    simplified: str
    pinyin: str
    definitions_en: list[str] = field(default_factory=list)
    definitions_fr: list[str] = field(default_factory=list)
    definition_records: list["DefinitionRecord"] = field(default_factory=list)
    sources: set[str] = field(default_factory=set)
    frequency: int = 0


@dataclass(frozen=True)
class DefinitionRecord:
    language: str
    text: str
    source: str
    source_page: str = ""
    part_of_speech: str = ""
    reviewed: bool = False


@dataclass
class MutableExample:
    chinese_id: int
    chinese: str
    english_id: int = 0
    english: str = ""
    french_id: int = 0
    french: str = ""
    chinese_source: str = "Tatoeba"
    english_source: str = "Tatoeba"
    french_source: str = ""


@dataclass(frozen=True)
class LearningSense:
    definitions: tuple[str, ...]
    chinese_equivalents: tuple[str, ...]


@dataclass(frozen=True)
class LearningEntry:
    language: str
    forms: tuple[str, ...]
    pronunciations: tuple[str, ...]
    parts_of_speech: tuple[str, ...]
    genders: tuple[str, ...]
    senses: tuple[LearningSense, ...]
    source_code: str
    source_entry_index: int


def normalized_text(value: str) -> str:
    return re.sub(r"\s+", " ", unicodedata.normalize("NFC", value).strip())


def element_text(element: ET.Element) -> str:
    return normalized_text("".join(element.itertext()))


def search_plain(value: str) -> str:
    value = value.casefold().replace("’", "'")
    value = "".join(
        character
        for character in unicodedata.normalize("NFKD", value)
        if unicodedata.category(character) != "Mn"
    )
    return re.sub(r"[^\w'-]+", " ", value, flags=re.UNICODE).strip()


def contains_han(value: str) -> bool:
    return any(is_han(character) for character in value)


def tei_values(entry: ET.Element, path: str) -> list[str]:
    return unique(element_text(element) for element in entry.findall(path, TEI_NAMESPACES))


def clean_pronunciation(value: str) -> tuple[str, bool]:
    """Remove WikDict's residual angle-bracket labels from a pronunciation."""
    cleaned = normalized_text(re.sub(r"<[^<>]+>", " ", value))
    return cleaned, cleaned != normalized_text(value)


def load_learning_dictionary(
    path: Path,
    expected_language: str,
    source_code: str,
) -> tuple[list[LearningEntry], Counter[str]]:
    """Load a filtered FreeDict TEI snapshot without merging lexical entries."""
    if source_code not in LEARNING_SOURCE_DETAILS:
        raise ValueError(f"Unsupported learning source: {source_code}")
    if expected_language not in {"fr", "en"}:
        raise ValueError(f"Unsupported learning language: {expected_language}")

    root = ET.parse(path).getroot()
    body = root.find("./tei:text/tei:body", TEI_NAMESPACES)
    if body is None:
        raise ValueError(f"No TEI body in {path}")
    language = body.get(f"{{{XML_NAMESPACE}}}lang", "")
    if language != expected_language:
        raise ValueError(
            f"Expected source language {expected_language!r}, found {language!r} in {path}"
        )

    result: list[LearningEntry] = []
    metrics: Counter[str] = Counter()
    for source_index, element in enumerate(body.findall("./tei:entry", TEI_NAMESPACES), start=1):
        metrics["source_entries"] += 1
        forms = tuple(tei_values(element, "./tei:form/tei:orth"))
        if not forms:
            metrics["rejected_without_headword"] += 1
            continue

        raw_pronunciations = tei_values(element, "./tei:form/tei:pron")
        cleaned_pronunciations: list[str] = []
        for raw in raw_pronunciations:
            pronunciation, changed = clean_pronunciation(raw)
            metrics["pronunciations_cleaned"] += changed
            if pronunciation:
                cleaned_pronunciations.append(pronunciation)
        all_pronunciations = unique(cleaned_pronunciations)
        if len(all_pronunciations) > MAX_LEARNING_PRONUNCIATIONS:
            metrics["entries_with_truncated_pronunciations"] += 1
            metrics["pronunciations_discarded"] += (
                len(all_pronunciations) - MAX_LEARNING_PRONUNCIATIONS
            )
        pronunciations = tuple(all_pronunciations[:MAX_LEARNING_PRONUNCIATIONS])

        senses: list[LearningSense] = []
        for sense in element.findall("./tei:sense", TEI_NAMESPACES):
            definitions = tuple(tei_values(sense, ".//tei:def"))
            raw_equivalents: list[str] = []
            for citation in sense.findall(".//tei:cit[@type='trans']", TEI_NAMESPACES):
                if citation.get(f"{{{XML_NAMESPACE}}}lang") != "zh":
                    continue
                raw_equivalents.extend(tei_values(citation, "./tei:quote"))
            equivalents = tuple(value for value in unique(raw_equivalents) if contains_han(value))
            metrics["non_han_equivalents_rejected"] += sum(
                bool(value) and not contains_han(value) for value in unique(raw_equivalents)
            )
            if not equivalents:
                metrics["senses_rejected_without_chinese"] += 1
                continue
            senses.append(
                LearningSense(
                    definitions=definitions,
                    chinese_equivalents=equivalents,
                )
            )

        if not senses:
            metrics["rejected_without_valid_sense"] += 1
            continue
        result.append(
            LearningEntry(
                language=expected_language,
                forms=forms,
                pronunciations=pronunciations,
                parts_of_speech=tuple(tei_values(element, "./tei:gramGrp/tei:pos")),
                genders=tuple(tei_values(element, "./tei:gramGrp/tei:gen")),
                senses=tuple(senses),
                source_code=source_code,
                source_entry_index=source_index,
            )
        )
        metrics["imported_entries"] += 1
        metrics["imported_senses"] += len(senses)
        metrics["imported_equivalents"] += sum(
            len(sense.chinese_equivalents) for sense in senses
        )
    return result, metrics


def entry_key(traditional: str, simplified: str, pinyin: str) -> tuple[str, str, str]:
    return traditional, simplified, re.sub(r"\s+", " ", pinyin.strip()).lower()


def add_definitions(
    entry: MutableEntry,
    language: str,
    definitions: Iterable[str],
    source: str,
    *,
    source_page: str = "",
    part_of_speech: str = "",
    reviewed: bool = False,
) -> None:
    if language not in {"en", "fr"}:
        raise ValueError(f"Unsupported definition language: {language}")
    definitions = unique(definitions)
    if language == "en":
        entry.definitions_en = unique([*entry.definitions_en, *definitions])
    else:
        entry.definitions_fr = unique([*entry.definitions_fr, *definitions])
    existing = {
        (record.language, record.text, record.source, record.source_page)
        for record in entry.definition_records
    }
    for definition in definitions:
        marker = (language, definition, source, source_page)
        if marker in existing:
            continue
        entry.definition_records.append(
            DefinitionRecord(
                language=language,
                text=definition,
                source=source,
                source_page=source_page,
                part_of_speech=part_of_speech,
                reviewed=reviewed,
            )
        )
        existing.add(marker)
    if definitions:
        entry.sources.add(source)


def load_cc_cedict(path: Path, entries: dict[tuple[str, str, str], MutableEntry]) -> None:
    raw = path.read_text(encoding="utf-8-sig").strip()
    if not raw.startswith("export default "):
        raise ValueError(f"Unexpected cc-cedict data format: {path}")
    payload = raw[len("export default ") :].rstrip(";\ufeff")
    data = json.loads(payload)
    for row in data["all"]:
        traditional, simplified, pinyin, definitions = row[:4]
        if isinstance(definitions, str):
            definitions = [definitions]
        key = entry_key(traditional, simplified, pinyin)
        entry = entries.setdefault(key, MutableEntry(traditional, simplified, pinyin))
        add_definitions(entry, "en", definitions, "CC-CEDICT")


def load_cfdict(path: Path, entries: dict[tuple[str, str, str], MutableEntry]) -> None:
    with path.open(encoding="utf-8-sig", errors="replace") as handle:
        for line in handle:
            line = line.strip().lstrip("\ufeff")
            if not line or line.startswith("#"):
                continue
            match = CEDICT_LINE.match(line)
            if not match:
                continue
            traditional, simplified, pinyin, definitions_blob = match.groups()
            definitions = unique(definitions_blob.split("/"))
            key = entry_key(traditional, simplified, pinyin)
            entry = entries.setdefault(key, MutableEntry(traditional, simplified, pinyin))
            add_definitions(entry, "fr", definitions, "CFDICT")


def load_pangmao_entries(entries: dict[tuple[str, str, str], MutableEntry]) -> None:
    custom = [
        (
            "胖貓",
            "胖猫",
            "pang4 mao1",
            ["fat cat; chubby cat", "Pangmao, name of this independent dictionary application"],
            ["chat dodu; gros chat", "Pangmao, nom de cette application indépendante de dictionnaire"],
        ),
        (
            "奶茶婊",
            "奶茶婊",
            "nai3 cha2 biao3",
            ["woman who acts sweet, innocent and helpless to attract male attention (derogatory Internet slang)"],
            ["femme qui joue l’ingénue douce et fragile pour attirer l’attention masculine (argot Internet péjoratif)"],
        ),
        (
            "肉夾饃",
            "肉夹馍",
            "rou4 jia1 mo2",
            ["roujiamo; Chinese flatbread filled with chopped meat; so-called Chinese burger"],
            ["roujiamo ; petit pain chinois garni de viande ; parfois appelé « burger chinois »"],
        ),
        (
            "爸比",
            "爸比",
            "ba4 bi3",
            ["daddy (loanword)"],
            ["papa (emprunt affectueux à l’anglais « daddy »)"],
        ),
    ]
    for traditional, simplified, pinyin, english, french in custom:
        key = entry_key(traditional, simplified, pinyin)
        entry = entries.get(key)
        if entry is None:
            entry = next(
                (
                    candidate
                    for candidate in entries.values()
                    if candidate.traditional == traditional and candidate.simplified == simplified
                ),
                None,
            )
        if entry is None:
            entry = MutableEntry(traditional, simplified, pinyin)
            entries[key] = entry
        existing_en = list(entry.definitions_en)
        existing_fr = list(entry.definitions_fr)
        entry.definitions_en = []
        entry.definitions_fr = []
        add_definitions(entry, "en", english, "Pangmao", reviewed=True)
        add_definitions(entry, "en", existing_en, "CC-CEDICT")
        add_definitions(entry, "fr", french, "Pangmao", reviewed=True)
        add_definitions(entry, "fr", existing_fr, "CFDICT")


def load_reviewed_definitions(
    path: Path,
    entries: dict[tuple[str, str, str], MutableEntry],
) -> None:
    """Load a small, source-attributed supplement that was reviewed row by row."""
    with path.open(encoding="utf-8-sig", newline="") as handle:
        reader = csv.DictReader(handle, delimiter="\t")
        required = {
            "traditional",
            "simplified",
            "pinyin",
            "language",
            "definition",
            "source",
            "source_page",
            "part_of_speech",
        }
        if not reader.fieldnames or not required.issubset(reader.fieldnames):
            raise ValueError(f"Reviewed definitions must contain {sorted(required)}: {path}")
        for line_number, row in enumerate(reader, start=2):
            traditional = row["traditional"].strip()
            simplified = row["simplified"].strip()
            pinyin = row["pinyin"].strip()
            language = row["language"].strip()
            definition = row["definition"].strip()
            source = row["source"].strip()
            if not all((traditional, simplified, pinyin, language, definition, source)):
                raise ValueError(f"Incomplete reviewed definition at {path}:{line_number}")
            key = entry_key(traditional, simplified, pinyin)
            entry = entries.get(key)
            if entry is None:
                raise ValueError(
                    f"Reviewed definition does not match an exact entry at {path}:{line_number}: "
                    f"{traditional} {simplified} [{pinyin}]"
                )
            add_definitions(
                entry,
                language,
                [definition],
                source,
                source_page=row["source_page"].strip(),
                part_of_speech=row["part_of_speech"].strip(),
                reviewed=True,
            )


def normalized_sentence(value: str) -> str:
    return re.sub(r"\s+", " ", unicodedata.normalize("NFC", value).strip())


def load_examples(path: Path) -> list[MutableExample]:
    examples: list[MutableExample] = []
    seen_chinese: set[str] = set()
    with path.open(encoding="utf-8-sig", newline="") as handle:
        for row in csv.reader(handle, delimiter="\t"):
            if len(row) < 4:
                continue
            chinese_id, chinese, english_id, english = row[:4]
            chinese = normalized_sentence(chinese)
            english = normalized_sentence(english)
            key = chinese
            if not chinese or not english or key in seen_chinese:
                continue
            seen_chinese.add(key)
            examples.append(
                MutableExample(
                    chinese_id=int(chinese_id),
                    chinese=chinese,
                    english_id=int(english_id),
                    english=english,
                )
            )
    return examples


def load_reviewed_tatoeba_french(path: Path, examples: list[MutableExample]) -> None:
    """Attach a small relation-level reviewed French subset by stable Tatoeba id."""
    by_chinese_id = {
        example.chinese_id: example for example in examples if example.chinese_id > 0
    }
    seen: set[int] = set()
    with path.open(encoding="utf-8-sig", newline="") as handle:
        reader = csv.DictReader(handle, delimiter="\t")
        required = {
            "chinese_id",
            "chinese",
            "french_id",
            "french",
            "positive_reviews",
        }
        if not reader.fieldnames or not required.issubset(reader.fieldnames):
            raise ValueError(f"Reviewed Tatoeba French rows must contain {sorted(required)}: {path}")
        for line_number, row in enumerate(reader, start=2):
            chinese_id = int(row["chinese_id"])
            french_id = int(row["french_id"])
            positive_reviews = int(row["positive_reviews"])
            chinese = normalized_sentence(row["chinese"])
            french = normalized_sentence(row["french"])
            if chinese_id <= 0 or french_id <= 0 or not chinese or not french:
                raise ValueError(f"Invalid reviewed Tatoeba row at {path}:{line_number}")
            if positive_reviews < 2:
                raise ValueError(
                    f"Production Tatoeba row lacks two positive reviews at {path}:{line_number}"
                )
            if chinese_id in seen:
                raise ValueError(f"Duplicate reviewed Chinese id at {path}:{line_number}: {chinese_id}")
            seen.add(chinese_id)
            example = by_chinese_id.get(chinese_id)
            if example is None:
                raise ValueError(
                    f"Reviewed Tatoeba row does not exist in the pinned English corpus at "
                    f"{path}:{line_number}: {chinese_id}"
                )
            if example.chinese != chinese:
                raise ValueError(
                    f"Reviewed Tatoeba Chinese text mismatch at {path}:{line_number}: {chinese_id}"
                )
            if example.french:
                raise ValueError(
                    f"Reviewed Tatoeba row would overwrite French text at {path}:{line_number}"
                )
            example.french_id = french_id
            example.french = french
            example.french_source = "Tatoeba"


def load_pangmao_examples(path: Path, examples: list[MutableExample]) -> None:
    """Merge small, human-reviewed bilingual examples and prefer them on collisions."""
    by_chinese = {normalized_sentence(example.chinese): example for example in examples}
    with path.open(encoding="utf-8-sig", newline="") as handle:
        reader = csv.DictReader(handle, delimiter="\t")
        required = {"chinese", "english", "french"}
        if not reader.fieldnames or not required.issubset(reader.fieldnames):
            raise ValueError(f"Curated examples must contain {sorted(required)}: {path}")
        for row in reader:
            chinese = normalized_sentence(row["chinese"])
            english = normalized_sentence(row["english"])
            french = normalized_sentence(row["french"])
            english_source = normalized_sentence(row.get("english_source", "")) or "Pangmao"
            french_source = normalized_sentence(row.get("french_source", "")) or "Pangmao"
            if not chinese or not english or not french:
                raise ValueError(f"Incomplete curated example in {path}: {row}")
            if english_source not in {"Pangmao", "Tatoeba"}:
                raise ValueError(f"Unsupported English example source in {path}: {english_source}")
            if french_source not in {"Pangmao", "Tatoeba", "Wiktionnaire"}:
                raise ValueError(f"Unsupported French example source in {path}: {french_source}")
            example = by_chinese.get(chinese)
            if example is None:
                if english_source == "Tatoeba":
                    raise ValueError(
                        f"Tatoeba attribution requires an existing pinned pair in {path}: {chinese}"
                    )
                example = MutableExample(
                    chinese_id=0,
                    chinese=chinese,
                    chinese_source="Pangmao",
                )
                examples.append(example)
                by_chinese[chinese] = example
            if english_source != "Tatoeba" or example.english != english:
                example.english_id = 0
            example.english = english
            example.french_id = 0
            example.french = french
            example.english_source = english_source
            example.french_source = french_source


def greedy_tokens(text: str, headwords: set[str], maximum_length: int) -> list[str]:
    tokens: list[str] = []
    index = 0
    while index < len(text):
        best: str | None = None
        upper = min(maximum_length, len(text) - index)
        for length in range(upper, 0, -1):
            candidate = text[index : index + length]
            if candidate in headwords:
                best = candidate
                break
        if best is None:
            best = text[index]
        tokens.append(best)
        index += len(best)
    return tokens


def apply_frequency_and_pinyin(
    entries: dict[tuple[str, str, str], MutableEntry],
    examples: list[MutableExample],
) -> tuple[Counter[str], dict[str, str]]:
    headwords = {
        word
        for entry in entries.values()
        for word in (entry.simplified, entry.traditional)
        if word and len(word) <= 8
    }
    maximum_length = max(map(len, headwords), default=1)
    frequencies: Counter[str] = Counter()
    preferred_pinyin: dict[str, str] = {}
    for entry in entries.values():
        for word in (entry.simplified, entry.traditional):
            if word and word not in preferred_pinyin:
                preferred_pinyin[word] = entry.pinyin
    for example in examples:
        for token in greedy_tokens(example.chinese, headwords, maximum_length):
            if token in headwords:
                frequencies[token] += 1
    for entry in entries.values():
        entry.frequency = max(
            frequencies.get(entry.simplified, 0),
            frequencies.get(entry.traditional, 0),
        )
    return frequencies, preferred_pinyin


def sentence_pinyin(
    sentence: str,
    headwords: set[str],
    maximum_length: int,
    preferred_pinyin: dict[str, str],
) -> str:
    rendered: list[str] = []
    for token in greedy_tokens(sentence, headwords, maximum_length):
        pronunciation = preferred_pinyin.get(token)
        if pronunciation:
            rendered.append(pronunciation)
        elif rendered and not is_han(token) and token.strip() and token not in "，。！？；：、“”‘’（）…—":
            rendered.append(token)
    return " ".join(rendered)


def parse_unihan(unihan_dir: Path, wanted: set[str]) -> dict[str, dict[str, str]]:
    properties = {
        "kMandarin",
        "kDefinition",
        "kRSUnicode",
        "kTotalStrokes",
        "kSimplifiedVariant",
        "kTraditionalVariant",
    }
    records: dict[str, dict[str, str]] = defaultdict(dict)
    for path in sorted(unihan_dir.glob("Unihan_*.txt")):
        with path.open(encoding="utf-8") as handle:
            for line in handle:
                if not line.startswith("U+"):
                    continue
                codepoint, prop, value = line.rstrip("\n").split("\t", 2)
                if prop not in properties:
                    continue
                character = chr(int(codepoint[2:], 16))
                if character in wanted:
                    records[character][prop] = value
    return records


def variants_to_text(value: str | None) -> str:
    if not value:
        return ""
    result: list[str] = []
    for codepoint in re.findall(r"U\+([0-9A-F]{4,6})", value):
        result.append(chr(int(codepoint, 16)))
    return " ".join(result)


def create_database(
    output: Path,
    entries: dict[tuple[str, str, str], MutableEntry],
    examples: list[MutableExample],
    unihan: dict[str, dict[str, str]],
    preferred_pinyin: dict[str, str],
    metadata: dict[str, str],
    learning_entries: Sequence[LearningEntry] = (),
) -> None:
    output.parent.mkdir(parents=True, exist_ok=True)
    if output.exists():
        output.unlink()
    connection = sqlite3.connect(output)
    connection.executescript(
        """
        PRAGMA page_size = 4096;
        PRAGMA journal_mode = OFF;
        PRAGMA synchronous = OFF;
        PRAGMA temp_store = MEMORY;
        PRAGMA foreign_keys = ON;
        CREATE TABLE entries (
            id INTEGER PRIMARY KEY,
            traditional TEXT NOT NULL,
            simplified TEXT NOT NULL,
            pinyin TEXT NOT NULL,
            pinyin_plain TEXT NOT NULL,
            definitions_en TEXT NOT NULL DEFAULT '',
            definitions_fr TEXT NOT NULL DEFAULT '',
            sources TEXT NOT NULL,
            frequency INTEGER NOT NULL DEFAULT 0
        );
        CREATE INDEX entries_simplified_idx ON entries(simplified);
        CREATE INDEX entries_traditional_idx ON entries(traditional);
        CREATE INDEX entries_pinyin_plain_idx ON entries(pinyin_plain);
        CREATE INDEX entries_frequency_idx ON entries(frequency DESC);
        CREATE VIRTUAL TABLE entries_fts USING fts4(
            simplified,
            traditional,
            pinyin_plain,
            definitions_en,
            definitions_fr,
            tokenize=unicode61 "remove_diacritics=2"
        );
        CREATE TABLE definition_sources (
            code TEXT PRIMARY KEY,
            display_name TEXT NOT NULL,
            url TEXT NOT NULL,
            revision TEXT NOT NULL,
            license TEXT NOT NULL
        ) WITHOUT ROWID;
        CREATE TABLE definition_attributions (
            id INTEGER PRIMARY KEY,
            entry_id INTEGER NOT NULL,
            language TEXT NOT NULL CHECK(language IN ('en', 'fr')),
            definition_index INTEGER NOT NULL,
            source_code TEXT NOT NULL,
            source_page TEXT NOT NULL DEFAULT '',
            part_of_speech TEXT NOT NULL DEFAULT '',
            reviewed INTEGER NOT NULL DEFAULT 0 CHECK(reviewed IN (0, 1)),
            FOREIGN KEY(entry_id) REFERENCES entries(id),
            FOREIGN KEY(source_code) REFERENCES definition_sources(code),
            UNIQUE(entry_id, language, definition_index, source_code, source_page)
        );
        CREATE INDEX definition_attributions_entry_idx
            ON definition_attributions(entry_id, language, definition_index);
        CREATE INDEX definition_attributions_source_idx
            ON definition_attributions(source_code);
        CREATE TABLE headwords (
            word TEXT PRIMARY KEY,
            preferred_entry_id INTEGER NOT NULL,
            length INTEGER NOT NULL
        ) WITHOUT ROWID;
        CREATE INDEX headwords_length_idx ON headwords(length DESC);
        CREATE TABLE examples (
            id INTEGER PRIMARY KEY,
            tatoeba_chinese_id INTEGER NOT NULL,
            chinese TEXT NOT NULL,
            pinyin TEXT NOT NULL,
            tatoeba_english_id INTEGER NOT NULL,
            english TEXT NOT NULL,
            tatoeba_french_id INTEGER NOT NULL,
            french TEXT NOT NULL,
            chinese_source TEXT NOT NULL,
            english_source TEXT NOT NULL,
            french_source TEXT NOT NULL
        );
        CREATE UNIQUE INDEX examples_chinese_idx ON examples(chinese);
        CREATE TABLE characters (
            character TEXT PRIMARY KEY,
            codepoint TEXT NOT NULL,
            mandarin TEXT NOT NULL DEFAULT '',
            definition TEXT NOT NULL DEFAULT '',
            radical INTEGER,
            additional_strokes INTEGER,
            total_strokes TEXT NOT NULL DEFAULT '',
            simplified_variants TEXT NOT NULL DEFAULT '',
            traditional_variants TEXT NOT NULL DEFAULT ''
        ) WITHOUT ROWID;
        CREATE TABLE learning_sources (
            code TEXT PRIMARY KEY,
            language TEXT NOT NULL CHECK(language IN ('fr', 'en')),
            display_name TEXT NOT NULL,
            url TEXT NOT NULL,
            revision TEXT NOT NULL,
            license TEXT NOT NULL
        ) WITHOUT ROWID;
        CREATE TABLE learning_entries (
            id INTEGER PRIMARY KEY,
            language TEXT NOT NULL CHECK(language IN ('fr', 'en')),
            primary_form TEXT NOT NULL,
            primary_form_search TEXT NOT NULL,
            parts_of_speech TEXT NOT NULL DEFAULT '',
            genders TEXT NOT NULL DEFAULT '',
            source_code TEXT NOT NULL,
            source_entry_index INTEGER NOT NULL,
            FOREIGN KEY(source_code) REFERENCES learning_sources(code),
            UNIQUE(source_code, source_entry_index)
        );
        CREATE INDEX learning_entries_language_form_idx
            ON learning_entries(language, primary_form_search, id);
        CREATE TABLE learning_forms (
            entry_id INTEGER NOT NULL,
            position INTEGER NOT NULL,
            form TEXT NOT NULL,
            form_search TEXT NOT NULL,
            PRIMARY KEY(entry_id, position),
            FOREIGN KEY(entry_id) REFERENCES learning_entries(id)
        ) WITHOUT ROWID;
        CREATE INDEX learning_forms_search_idx ON learning_forms(form_search, entry_id);
        CREATE TABLE learning_pronunciations (
            entry_id INTEGER NOT NULL,
            position INTEGER NOT NULL,
            pronunciation TEXT NOT NULL,
            PRIMARY KEY(entry_id, position),
            FOREIGN KEY(entry_id) REFERENCES learning_entries(id)
        ) WITHOUT ROWID;
        CREATE TABLE learning_senses (
            id INTEGER PRIMARY KEY,
            entry_id INTEGER NOT NULL,
            position INTEGER NOT NULL,
            definitions TEXT NOT NULL DEFAULT '',
            source_code TEXT NOT NULL,
            FOREIGN KEY(entry_id) REFERENCES learning_entries(id),
            FOREIGN KEY(source_code) REFERENCES learning_sources(code),
            UNIQUE(entry_id, position)
        );
        CREATE INDEX learning_senses_entry_idx ON learning_senses(entry_id, position);
        CREATE TABLE learning_equivalents (
            sense_id INTEGER NOT NULL,
            position INTEGER NOT NULL,
            chinese TEXT NOT NULL,
            PRIMARY KEY(sense_id, position),
            FOREIGN KEY(sense_id) REFERENCES learning_senses(id)
        ) WITHOUT ROWID;
        CREATE INDEX learning_equivalents_chinese_idx
            ON learning_equivalents(chinese, sense_id);
        CREATE VIRTUAL TABLE learning_entries_fts USING fts4(
            forms,
            forms_search,
            chinese_equivalents,
            definitions,
            tokenize=unicode61 "remove_diacritics=2"
        );
        CREATE TABLE metadata (
            key TEXT PRIMARY KEY,
            value TEXT NOT NULL
        ) WITHOUT ROWID;
        """
    )

    ordered_entries = sorted(
        entries.values(),
        key=lambda item: (-item.frequency, item.simplified, item.pinyin.lower()),
    )
    rows: list[tuple] = []
    entry_ids: dict[tuple[str, str, str], int] = {}
    for index, entry in enumerate(ordered_entries, start=1):
        row = (
            index,
            entry.traditional,
            entry.simplified,
            entry.pinyin,
            pinyin_plain(entry.pinyin),
            "\n".join(entry.definitions_en),
            "\n".join(entry.definitions_fr),
            " · ".join(sorted(entry.sources)),
            entry.frequency,
        )
        rows.append(row)
        entry_ids[entry_key(entry.traditional, entry.simplified, entry.pinyin)] = index
    connection.executemany(
        "INSERT INTO entries VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
        rows,
    )
    connection.execute(
        """
        INSERT INTO entries_fts(docid, simplified, traditional, pinyin_plain, definitions_en, definitions_fr)
        SELECT id, simplified, traditional, pinyin_plain, definitions_en, definitions_fr FROM entries
        """
    )

    source_rows = (
        (
            "CC-CEDICT",
            "CC-CEDICT",
            "https://www.mdbg.net/chinese/dictionary?page=cc-cedict",
            metadata["cc_cedict_revision"],
            "CC BY-SA 4.0",
        ),
        (
            "CFDICT",
            "CFDICT",
            "https://chine.in/mandarin/dictionnaire/CFDICT/",
            metadata["cfdict_downloaded"],
            "CC BY-SA 3.0",
        ),
        (
            "Pangmao",
            "Pangmao editorial supplement",
            "https://github.com/kevindassie-ui/Pangmao",
            metadata["pangmao_supplement_version"],
            "Project content",
        ),
        (
            "Wiktionnaire",
            "Wiktionnaire français via Kaikki",
            "https://kaikki.org/frwiktionary/Chinois/index.html",
            metadata["frwiktionary_revision"],
            "CC BY-SA 4.0",
        ),
    )
    connection.executemany("INSERT INTO definition_sources VALUES (?, ?, ?, ?, ?)", source_rows)
    definition_rows = []
    definition_identifier = 1
    for entry in ordered_entries:
        entry_identifier = entry_ids[entry_key(entry.traditional, entry.simplified, entry.pinyin)]
        display_order = {
            "en": {text: index for index, text in enumerate(entry.definitions_en)},
            "fr": {text: index for index, text in enumerate(entry.definitions_fr)},
        }
        source_priority = {"Pangmao": 0, "CFDICT": 1, "CC-CEDICT": 1, "Wiktionnaire": 2}
        ordered_definitions = sorted(
            entry.definition_records,
            key=lambda item: (
                item.language,
                display_order[item.language].get(item.text, 1_000_000),
                source_priority.get(item.source, 99),
                item.source,
            ),
        )
        for definition in ordered_definitions:
            if (
                definition.source == DEFAULT_DEFINITION_SOURCES[definition.language]
                and not definition.source_page
                and not definition.part_of_speech
                and not definition.reviewed
            ):
                continue
            definition_rows.append(
                (
                    definition_identifier,
                    entry_identifier,
                    definition.language,
                    display_order[definition.language][definition.text],
                    definition.source,
                    definition.source_page,
                    definition.part_of_speech,
                    int(definition.reviewed),
                )
            )
            definition_identifier += 1
    connection.executemany(
        "INSERT INTO definition_attributions VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
        definition_rows,
    )

    preferred: dict[str, tuple[int, int]] = {}
    for entry in ordered_entries:
        identifier = entry_ids[entry_key(entry.traditional, entry.simplified, entry.pinyin)]
        for word in (entry.simplified, entry.traditional):
            if not word:
                continue
            score = entry.frequency * 100 + (10 if entry.definitions_fr else 0) + (1 if entry.definitions_en else 0)
            if word not in preferred or score > preferred[word][0]:
                preferred[word] = score, identifier
    connection.executemany(
        "INSERT INTO headwords(word, preferred_entry_id, length) VALUES (?, ?, ?)",
        ((word, value[1], len(word)) for word, value in preferred.items()),
    )

    headword_set = {word for word in preferred if len(word) <= 8}
    maximum_length = max(map(len, headword_set), default=1)
    example_rows = []
    for identifier, example in enumerate(examples, start=1):
        example_rows.append(
            (
                identifier,
                example.chinese_id,
                example.chinese,
                sentence_pinyin(example.chinese, headword_set, maximum_length, preferred_pinyin),
                example.english_id,
                example.english,
                example.french_id,
                example.french,
                example.chinese_source,
                example.english_source,
                example.french_source,
            )
        )
    connection.executemany("INSERT INTO examples VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", example_rows)

    character_rows = []
    for character, values in sorted(unihan.items(), key=lambda item: ord(item[0])):
        radical = None
        additional = None
        match = re.search(r"(\d+)'?\.(\d+)", values.get("kRSUnicode", ""))
        if match:
            radical = int(match.group(1))
            additional = int(match.group(2))
        character_rows.append(
            (
                character,
                f"U+{ord(character):04X}",
                values.get("kMandarin", ""),
                values.get("kDefinition", ""),
                radical,
                additional,
                values.get("kTotalStrokes", ""),
                variants_to_text(values.get("kSimplifiedVariant")),
                variants_to_text(values.get("kTraditionalVariant")),
            )
        )
    connection.executemany("INSERT INTO characters VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", character_rows)

    source_codes = sorted({entry.source_code for entry in learning_entries})
    source_rows = []
    for source_code in source_codes:
        language, display_name, url, license_name = LEARNING_SOURCE_DETAILS[source_code]
        source_rows.append(
            (
                source_code,
                language,
                display_name,
                url,
                metadata[f"{source_code}_revision"],
                license_name,
            )
        )
    connection.executemany(
        "INSERT INTO learning_sources VALUES (?, ?, ?, ?, ?, ?)",
        source_rows,
    )

    ordered_learning_entries = sorted(
        learning_entries,
        key=lambda item: (
            item.language,
            search_plain(item.forms[0]),
            item.forms[0].casefold(),
            item.parts_of_speech,
            item.source_entry_index,
        ),
    )
    learning_entry_rows = []
    form_rows = []
    pronunciation_rows = []
    sense_rows = []
    equivalent_rows = []
    fts_rows = []
    sense_identifier = 1
    for entry_identifier, entry in enumerate(ordered_learning_entries, start=1):
        learning_entry_rows.append(
            (
                entry_identifier,
                entry.language,
                entry.forms[0],
                search_plain(entry.forms[0]),
                "\n".join(entry.parts_of_speech),
                "\n".join(entry.genders),
                entry.source_code,
                entry.source_entry_index,
            )
        )
        for position, form in enumerate(entry.forms):
            form_rows.append((entry_identifier, position, form, search_plain(form)))
        for position, pronunciation in enumerate(entry.pronunciations):
            pronunciation_rows.append((entry_identifier, position, pronunciation))
        entry_equivalents: list[str] = []
        entry_definitions: list[str] = []
        for sense_position, sense in enumerate(entry.senses):
            sense_rows.append(
                (
                    sense_identifier,
                    entry_identifier,
                    sense_position,
                    "\n".join(sense.definitions),
                    entry.source_code,
                )
            )
            entry_definitions.extend(sense.definitions)
            for equivalent_position, equivalent in enumerate(sense.chinese_equivalents):
                equivalent_rows.append(
                    (sense_identifier, equivalent_position, equivalent)
                )
                entry_equivalents.append(equivalent)
            sense_identifier += 1
        fts_rows.append(
            (
                entry_identifier,
                "\n".join(entry.forms),
                "\n".join(search_plain(form) for form in entry.forms),
                "\n".join(unique(entry_equivalents)),
                "\n".join(unique(entry_definitions)),
            )
        )
    connection.executemany(
        "INSERT INTO learning_entries VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
        learning_entry_rows,
    )
    connection.executemany(
        "INSERT INTO learning_forms VALUES (?, ?, ?, ?)",
        form_rows,
    )
    connection.executemany(
        "INSERT INTO learning_pronunciations VALUES (?, ?, ?)",
        pronunciation_rows,
    )
    connection.executemany(
        "INSERT INTO learning_senses VALUES (?, ?, ?, ?, ?)",
        sense_rows,
    )
    connection.executemany(
        "INSERT INTO learning_equivalents VALUES (?, ?, ?)",
        equivalent_rows,
    )
    connection.executemany(
        """
        INSERT INTO learning_entries_fts(
            docid, forms, forms_search, chinese_equivalents, definitions
        ) VALUES (?, ?, ?, ?, ?)
        """,
        fts_rows,
    )
    connection.executemany("INSERT INTO metadata VALUES (?, ?)", sorted(metadata.items()))
    connection.execute("PRAGMA user_version = 4")
    connection.commit()
    connection.execute("ANALYZE")
    connection.execute("VACUUM")
    integrity = connection.execute("PRAGMA integrity_check").fetchone()[0]
    if integrity != "ok":
        raise RuntimeError(f"Database integrity check failed: {integrity}")
    connection.close()


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--cc-cedict", required=True, type=Path)
    parser.add_argument("--cfdict", required=True, type=Path)
    parser.add_argument("--tatoeba", required=True, type=Path)
    parser.add_argument("--tatoeba-french", required=True, type=Path)
    parser.add_argument("--pangmao-examples", required=True, type=Path)
    parser.add_argument("--reviewed-definitions", required=True, type=Path)
    parser.add_argument("--unihan-dir", required=True, type=Path)
    parser.add_argument("--freedict-french", required=True, type=Path)
    parser.add_argument("--freedict-english", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--cc-revision", default="unknown")
    parser.add_argument("--tatoeba-release", default="unknown")
    parser.add_argument("--freedict-release", default="unknown")
    args = parser.parse_args()

    entries: dict[tuple[str, str, str], MutableEntry] = {}
    load_cc_cedict(args.cc_cedict, entries)
    load_cfdict(args.cfdict, entries)
    load_pangmao_entries(entries)
    load_reviewed_definitions(args.reviewed_definitions, entries)
    examples = load_examples(args.tatoeba)
    load_reviewed_tatoeba_french(args.tatoeba_french, examples)
    load_pangmao_examples(args.pangmao_examples, examples)
    french_learning_entries, french_learning_metrics = load_learning_dictionary(
        args.freedict_french,
        "fr",
        "FreeDict-fra-zho",
    )
    english_learning_entries, english_learning_metrics = load_learning_dictionary(
        args.freedict_english,
        "en",
        "FreeDict-eng-zho",
    )
    learning_entries = [*french_learning_entries, *english_learning_entries]
    _, preferred_pinyin = apply_frequency_and_pinyin(entries, examples)
    wanted_characters = {
        character
        for entry in entries.values()
        for word in (entry.simplified, entry.traditional)
        for character in word
        if is_han(character)
    }
    unihan = parse_unihan(args.unihan_dir, wanted_characters)
    metadata = {
        "schema_version": "4",
        "cc_cedict_revision": args.cc_revision,
        "cfdict_downloaded": "2026-09-11",
        "tatoeba_release": args.tatoeba_release,
        "tatoeba_french_release": "2026-09-12; relation-reviewed subset",
        "unihan_version": "17.0.0",
        "pangmao_supplement_version": "0.5.0",
        "frwiktionary_revision": "dump 2026-09-01; Kaikki extract 2026-09-08",
        "entry_count": str(len(entries)),
        "example_count": str(len(examples)),
        "bilingual_example_count": str(sum(bool(example.french) for example in examples)),
        "definition_missing_english": str(sum(not entry.definitions_en for entry in entries.values())),
        "definition_missing_french": str(sum(not entry.definitions_fr for entry in entries.values())),
        "definition_record_count": str(
            sum(len(entry.definition_records) for entry in entries.values())
        ),
        "definition_attribution_count": str(
            sum(
                definition.source != DEFAULT_DEFINITION_SOURCES[definition.language]
                or bool(definition.source_page)
                or bool(definition.part_of_speech)
                or definition.reviewed
                for entry in entries.values()
                for definition in entry.definition_records
            )
        ),
        "default_definition_source_en": DEFAULT_DEFINITION_SOURCES["en"],
        "default_definition_source_fr": DEFAULT_DEFINITION_SOURCES["fr"],
        "reviewed_definition_count": str(
            sum(
                definition.reviewed
                for entry in entries.values()
                for definition in entry.definition_records
            )
        ),
        "character_count": str(len(unihan)),
        "FreeDict-fra-zho_revision": args.freedict_release,
        "FreeDict-eng-zho_revision": args.freedict_release,
        "learning_entry_count": str(len(learning_entries)),
        "learning_entry_count_fr": str(len(french_learning_entries)),
        "learning_entry_count_en": str(len(english_learning_entries)),
        **{
            f"learning_filter_fr_{key}": str(value)
            for key, value in sorted(french_learning_metrics.items())
        },
        **{
            f"learning_filter_en_{key}": str(value)
            for key, value in sorted(english_learning_metrics.items())
        },
    }
    create_database(
        args.output,
        entries,
        examples,
        unihan,
        preferred_pinyin,
        metadata,
        learning_entries,
    )
    size_mb = args.output.stat().st_size / (1024 * 1024)
    print(json.dumps({**metadata, "size_mb": round(size_mb, 2)}, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
