#!/usr/bin/env python3
"""Build a deployable Pangmao Web brand variant without changing the global app."""

from __future__ import annotations

import argparse
import json
import shutil
import tempfile
from pathlib import Path


ROOT_FILES = (
    ".nojekyll",
    "brand.json",
    "index.html",
    "manifest.webmanifest",
    "package.json",
    "styles.css",
    "sw.js",
)
ROOT_DIRECTORIES = ("data", "icons", "src", "voice-trial")


def copy_tree(source: Path, destination: Path) -> None:
    shutil.copytree(source, destination, dirs_exist_ok=True)


def build_variant(source: Path, brand: str, output: Path) -> None:
    source = source.resolve()
    output = output.resolve()
    brand_root = source / "brands" / brand
    if not brand_root.is_dir():
        raise FileNotFoundError(f"Unknown web brand: {brand_root}")
    if output == source or source in output.parents:
        raise ValueError("Output must be outside webApp")

    output.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix="pangmao-web-", dir=output.parent) as temporary:
        staging = Path(temporary) / "site"
        staging.mkdir()
        for name in ROOT_FILES:
            shutil.copy2(source / name, staging / name)
        for name in ROOT_DIRECTORIES:
            copy_tree(source / name, staging / name)

        shutil.copy2(brand_root / "brand.json", staging / "brand.json")
        shutil.copy2(brand_root / "manifest.webmanifest", staging / "manifest.webmanifest")
        copy_tree(brand_root / "icons", staging / "icons")
        images = brand_root / "images"
        if images.is_dir():
            copy_tree(images, staging / "images")
            service_worker_path = staging / "sw.js"
            service_worker = service_worker_path.read_text(encoding="utf-8")
            image_cache_entries = "".join(
                f'  "./images/{path.name}",\n'
                for path in sorted(images.iterdir())
                if path.is_file()
            )
            service_worker = service_worker.replace(
                '  "./icons/icon-512.png",\n',
                f'  "./icons/icon-512.png",\n{image_cache_entries}',
                1,
            )
            service_worker_path.write_text(service_worker, encoding="utf-8")

        brand_data = json.loads((brand_root / "brand.json").read_text(encoding="utf-8"))
        html_path = staging / "index.html"
        html = html_path.read_text(encoding="utf-8")
        html = html.replace(
            '<html lang="zh-Hans">',
            f'<html lang="zh-Hans" data-brand="{brand_data["theme"]}">',
            1,
        )
        html = html.replace(
            '<meta name="theme-color" content="#0f6b4f" />',
            f'<meta name="theme-color" content="{brand_data["themeColor"]}" />',
            1,
        )
        html_path.write_text(html, encoding="utf-8")

        for trial_html_path in (staging / "voice-trial").rglob("index.html"):
            trial_html = trial_html_path.read_text(encoding="utf-8").replace(
                '<html lang="zh-Hans">',
                f'<html lang="zh-Hans" data-brand="{brand_data["theme"]}">', 1,
            ).replace(
                '<meta name="theme-color" content="#0f6b4f" />',
                f'<meta name="theme-color" content="{brand_data["themeColor"]}" />', 1,
            )
            trial_html_path.write_text(trial_html, encoding="utf-8")

        if output.exists():
            shutil.rmtree(output)
        staging.rename(output)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", type=Path, default=Path("webApp"))
    parser.add_argument("--brand", default="wife")
    parser.add_argument("--output", type=Path, default=Path("build/web-wife"))
    arguments = parser.parse_args()
    build_variant(arguments.source, arguments.brand, arguments.output)
    print(arguments.output)


if __name__ == "__main__":
    main()
