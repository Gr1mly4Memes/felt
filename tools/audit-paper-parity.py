"""Inventory upstream patch targets and Felt-added unsupported operations.

This report records review work; matching a mixin target does not prove parity.
Run from any directory: python tools/audit-paper-parity.py
"""
import argparse
from collections import defaultdict
import hashlib
import json
import re
from pathlib import Path


EVENT_NAME = re.compile(r"\b[A-Z]\w+Event\b")


def audit(root):
    paper = root / "upstream/Paper"
    pin = json.loads((root / "build-support/paper-sources.json").read_text(encoding="utf-8"))["commit"]
    upstream = []
    events_in_patches = defaultdict(set)
    for group in ("paper-api", "paper-server"):
        for patch in sorted((paper / group / "patches").rglob("*.patch")):
            data = patch.read_bytes()
            text = data.decode("utf-8")
            relative = patch.relative_to(paper).as_posix()
            if group == "paper-server":
                for line in text.splitlines():
                    if line.startswith("+") and not line.startswith("+++"):
                        for event in EVENT_NAME.findall(line):
                            events_in_patches[event].add(relative)
            targets = re.findall(r"^diff --git a/(.+?) b/(.+)$", text, re.MULTILINE)
            if not targets:
                targets = re.findall(r"^--- a/(.+)\n\+\+\+ b/(.+)$", text, re.MULTILINE)
            upstream.append({
                "patch": relative,
                "sha256": hashlib.sha256(data).hexdigest(),
                "targets": sorted({new for _, new in targets}),
                "status": "requires-behavioral-review",
            })
    gaps = []
    for patch in sorted((root / "patches").rglob("*.patch")):
        rel = patch.relative_to(root / "patches")
        # Felt patch group precedes the path within the Paper checkout.
        source = paper.joinpath(*rel.parts[1:]).with_suffix("")
        if not source.is_file():
            continue
        source_lines = source.read_text(encoding="utf-8").splitlines()
        additions = {
            line[1:].strip() for line in patch.read_text(encoding="utf-8").splitlines()
            if line.startswith("+") and "throw new UnsupportedOperationException" in line
        }
        for line_number, line in enumerate(source_lines, 1):
            if line.strip() in additions:
                gaps.append({
                    "source": source.relative_to(paper).as_posix(),
                    "line": line_number,
                    "operation": line.strip(),
                    "status": "requires-reachability-and-behavioral-review",
                })
    events_in_server = set()
    for directory in (root / "felt-fabric/src/main/java", paper / "paper-server/src/main/java"):
        for source in directory.rglob("*.java"):
            events_in_server.update(EVENT_NAME.findall(source.read_text(encoding="utf-8")))
    api_events = {source.stem for source in (paper / "paper-api/src/main/java").rglob("*Event.java")}
    events_to_review = [
        {"event": event, "upstream_patches": sorted(events_in_patches[event])}
        for event in sorted((api_events & events_in_patches.keys()) - events_in_server)
    ]
    return {
        "paper_commit": pin,
        "full_parity_verified": False,
        "interpretation": "Inventory only. Patch presence, successful compilation and a passing subset of tests do not establish runtime parity. Unsupported operations can include intentional internal APIs; review reachability before treating each as a missing feature.",
        "upstream_patch_count": len(upstream),
        "felt_added_unsupported_operation_count": len(gaps),
        "public_event_source_inventory": {
            "api_event_source_count": len(api_events),
            "events_without_a_server_reference": events_to_review,
            "interpretation": "A missing reference is a candidate for review. A source reference does not prove dispatch, timing, payload, or cancellation parity.",
        },
        "upstream_patches": upstream,
        "unsupported_operations": gaps,
    }


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", default="build/paper-parity-audit.json")
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    result = audit(root)
    output = (root / args.output).resolve()
    if not output.is_relative_to(root):
        parser.error("The report must be written inside the workspace")
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    print(f"Pinned Paper {result['paper_commit']}: {result['upstream_patch_count']} patches, "
          f"{result['felt_added_unsupported_operation_count']} unsupported-operation candidates; "
          f"full parity remains unverified. Report: {output}")
    print(f"Public event candidates without a server reference: "
          f"{len(result['public_event_source_inventory']['events_without_a_server_reference'])}")
