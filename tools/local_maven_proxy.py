#!/usr/bin/env python3
"""Expose Maven repositories locally when only curl has outbound access.

This helper is intentionally opt-in. Normal developer and CI builds keep using the
official HTTPS repositories configured in settings.gradle.kts.
"""

from __future__ import annotations

import argparse
import hashlib
import mimetypes
import os
from pathlib import Path
import subprocess
import threading
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import unquote, urlsplit


ORIGINS = {
    "google": "https://dl.google.com/dl/android/maven2/",
    "central": "https://repo.maven.apache.org/maven2/",
    "plugins": "https://plugins.gradle.org/m2/",
}
ORIGIN_PRIORITY = tuple(ORIGINS)

GOOGLE_REPOSITORY_PREFIXES = (
    "androidx/",
    "com/android/",
    "com/google/android/",
    "com/google/firebase/",
    "com/google/mlkit/",
)


def origin_priority(relative: str) -> tuple[str, ...]:
    """Try the repository most likely to own an artifact before fallbacks."""
    if ".gradle.plugin/" in relative:
        preferred = "plugins"
    elif relative.startswith(GOOGLE_REPOSITORY_PREFIXES):
        preferred = "google"
    else:
        preferred = "central"
    return (preferred, *(origin for origin in ORIGIN_PRIORITY if origin != preferred))


class MavenProxy(BaseHTTPRequestHandler):
    cache_root: Path
    slots = threading.BoundedSemaphore(16)
    locks_guard = threading.Lock()
    locks: dict[str, threading.Lock] = {}

    def do_HEAD(self) -> None:  # noqa: N802
        self._serve(send_body=False)

    def do_GET(self) -> None:  # noqa: N802
        self._serve(send_body=True)

    def _serve(self, *, send_body: bool) -> None:
        parsed = urlsplit(self.path)
        parts = unquote(parsed.path).lstrip("/").split("/", 1)
        if len(parts) != 2 or parts[0] != "all" or ".." in parts[1].split("/"):
            self.send_error(404)
            return

        _, relative = parts
        cache_file = self.cache_root / "all" / relative
        miss_file = cache_file.with_suffix(cache_file.suffix + ".missing")
        lock_key = str(cache_file)
        with self.locks_guard:
            lock = self.locks.setdefault(lock_key, threading.Lock())

        with lock:
            if miss_file.exists():
                self.send_error(404)
                return
            self._materialize_checksum(relative, cache_file)
            if self._is_pom_only_jar(relative, cache_file):
                miss_file.touch()
                self.send_error(404)
                return
            if not cache_file.exists() and not self._fetch(relative, cache_file, miss_file):
                self.send_error(404)
                return

        size = cache_file.stat().st_size
        content_type = mimetypes.guess_type(cache_file.name)[0] or "application/octet-stream"
        self.send_response(200)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(size))
        self.send_header("ETag", f'"{size:x}-{cache_file.stat().st_mtime_ns:x}"')
        self.end_headers()
        if send_body:
            with cache_file.open("rb") as source:
                while chunk := source.read(1024 * 1024):
                    self.wfile.write(chunk)

    @staticmethod
    def _materialize_checksum(relative: str, destination: Path) -> None:
        algorithms = {
            ".md5": hashlib.md5,
            ".sha1": hashlib.sha1,
            ".sha256": hashlib.sha256,
            ".sha512": hashlib.sha512,
        }
        for suffix, algorithm in algorithms.items():
            if not relative.endswith(suffix):
                continue
            source = destination.with_suffix("")
            if source.exists() and not destination.exists():
                destination.write_text(algorithm(source.read_bytes()).hexdigest(), encoding="ascii")
            return

    @staticmethod
    def _is_pom_only_jar(relative: str, destination: Path) -> bool:
        if not relative.endswith(".jar") or destination.exists():
            return False
        pom = destination.with_suffix(".pom")
        return pom.exists() and "<packaging>pom</packaging>" in pom.read_text(
            encoding="utf-8", errors="ignore"
        )

    def _fetch(self, relative: str, destination: Path, miss_file: Path) -> bool:
        destination.parent.mkdir(parents=True, exist_ok=True)
        request_id = threading.get_ident()

        for origin_name in origin_priority(relative):
            temporary = destination.with_name(
                f".{destination.name}.{request_id}.{origin_name}.part"
            )
            with self.slots:
                result = subprocess.run(
                    [
                        "curl",
                        "--fail",
                        "--location",
                        "--silent",
                        "--show-error",
                        "--output",
                        str(temporary),
                        ORIGINS[origin_name] + relative,
                    ],
                    check=False,
                )
            if result.returncode == 0:
                os.replace(temporary, destination)
                return True
            if temporary.exists():
                temporary.unlink()
        miss_file.touch()
        return False

    def log_message(self, _format: str, *_args: object) -> None:
        return


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--port", type=int, default=4873)
    parser.add_argument("--cache", type=Path, required=True)
    args = parser.parse_args()
    MavenProxy.cache_root = args.cache.resolve()
    MavenProxy.cache_root.mkdir(parents=True, exist_ok=True)
    server = ThreadingHTTPServer(("127.0.0.1", args.port), MavenProxy)
    print(f"Pangmao Maven proxy listening on http://127.0.0.1:{args.port}", flush=True)
    server.serve_forever()


if __name__ == "__main__":
    main()
