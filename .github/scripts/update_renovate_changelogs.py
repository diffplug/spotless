#!/usr/bin/env python3
import argparse
import re
import subprocess
import sys
import tomllib
from pathlib import Path

KEY_MAPPING = {
    ("versions", "jackson"): "jackson",
    ("versions", "ktlint"): "ktlint",
    ("versions", "javaparser"): "javaparser",
    ("libraries", "palantir-java-format"): "palantir-java-format",
    ("libraries", "adocfmt"): "adocfmt",
    ("libraries", "antlr4-formatter"): "antlr4-formatter",
    ("libraries", "cleanthat-java"): "cleanthat",
    ("libraries", "cool-rdf-formatter"): "cool-rdf-formatter",
    ("libraries", "diktat-runner"): "diktat",
    ("libraries", "flexmark-all"): "flexmark",
    ("libraries", "freshmark"): "freshmark",
    ("libraries", "gherkin-utils"): "gherkin-utils",
    ("libraries", "google-java-format"): "google-java-format",
    ("libraries", "gson"): "gson",
    ("libraries", "json-simple"): "json-simple",
    ("libraries", "ktfmt"): "ktfmt",
    ("libraries", "prince-of-space-core"): "prince-of-space",
    ("libraries", "scalafmt-core"): "scalafmt",
    ("libraries", "sortpom-sorter"): "sortpom",
    ("libraries", "tabletest-formatter-core"): "tabletest-formatter",
    ("libraries", "zjsonpatch"): "zjsonpatch",
}

CHANGELOG_FILES = [
    Path("CHANGES.md"),
    Path("plugin-gradle/CHANGES.md"),
    Path("plugin-maven/CHANGES.md"),
]


def extract_formatter_versions(toml_str: str) -> dict[str, str]:
    data = tomllib.loads(toml_str)
    versions = data.get("versions", {})
    libraries = data.get("libraries", {})
    result = {}
    for (sec, key), tool_name in KEY_MAPPING.items():
        if sec == "versions":
            if key in versions:
                result[tool_name] = str(versions[key])
        elif sec == "libraries":
            if key in libraries:
                val = libraries[key]
                if isinstance(val, str):
                    parts = val.split(":")
                    if len(parts) >= 3:
                        result[tool_name] = parts[-1]
                elif isinstance(val, dict):
                    if "version" in val:
                        result[tool_name] = str(val["version"])
                    elif "version.ref" in val and val["version.ref"] in versions:
                        result[tool_name] = str(versions[val["version.ref"]])
    return result


PR_LINK = r"\[#\d+\]\([^\s)]+\)"
BUMP_ENTRY = re.compile(
    r"- Bump default `(?P<tool>[^`]+)` version `(?P<old>[^`]+)` -> `(?P<new>[^`]+)`\. "
    rf"\((?P<links>{PR_LINK}(?:, {PR_LINK})*)\)(?P<trailing>[ \t]*)"
)


def merge_changelog_entry(changes: str, entry: str) -> str:
    """Consolidate generated bumps, leaving handwritten entries and continuation lines intact."""
    incoming = BUMP_ENTRY.fullmatch(entry)
    lines = changes.splitlines(keepends=True)
    matches = []
    if incoming:
        for index, line in enumerate(lines):
            existing = BUMP_ENTRY.fullmatch(line.rstrip("\n"))
            if existing and existing["tool"] == incoming["tool"]:
                matches.append((index, existing))

    if not matches:
        if entry in changes.splitlines():
            return changes
        prefix = changes.rstrip()
        return prefix + ("\n" if prefix else "") + entry + "\n\n"

    # The first unreleased entry records the version users last received. The catalog's
    # current version is authoritative, even when Renovate updates the same PR again.
    first_index, first = matches[0]
    links = []
    for match in [existing for _, existing in matches] + [incoming]:
        for link in re.findall(PR_LINK, match["links"]):
            if link not in links:
                links.append(link)
    lines[first_index] = (
        f"- Bump default `{incoming['tool']}` version `{first['old']}` -> `{incoming['new']}`. "
        f"({', '.join(links)}){first['trailing']}\n"
    )
    for index, _ in reversed(matches[1:]):
        del lines[index]
    return "".join(lines)


def insert_changelog_entries(content: str, entries: list[str]) -> str:
    if not entries:
        return content
    unreleased_match = re.search(r"^##[ \t]*\[Unreleased\][ \t]*\n", content, re.MULTILINE)
    if not unreleased_match:
        return content

    unreleased_start = unreleased_match.end()
    next_release = re.search(r"^##[ \t]+", content[unreleased_start:], re.MULTILINE)
    unreleased_end = unreleased_start + next_release.start() if next_release else len(content)
    block = content[unreleased_start:unreleased_end]

    changes_header = re.search(r"^###[ \t]+Changes[ \t]*\n", block, re.MULTILINE)
    if changes_header:
        start = changes_header.end()
        next_section = re.search(r"^###[ \t]+", block[start:], re.MULTILINE)
        end = start + next_section.start() if next_section else len(block)
        changes = block[start:end]
        for entry in entries:
            changes = merge_changelog_entry(changes, entry)
        block = block[:start] + changes + block[end:]
    else:
        changes = ""
        for entry in entries:
            changes = merge_changelog_entry(changes, entry)
        block = "\n### Changes\n" + changes.lstrip("\n") + block.lstrip("\n")

    return content[:unreleased_start] + block + content[unreleased_end:]


def main() -> None:
    parser = argparse.ArgumentParser(description="Update changelog files for Renovate version bumps.")
    parser.add_argument("--base-ref", default="origin/main", help="Base ref or branch name to compare against")
    parser.add_argument("--pr-number", required=True, help="Pull Request number")
    parser.add_argument("--pr-url", required=True, help="Pull Request URL")
    args = parser.parse_args()

    toml_path = Path("gradle/libs.versions.toml")
    if not toml_path.exists():
        print(f"Error: {toml_path} does not exist.")
        sys.exit(1)

    head_content = toml_path.read_text(encoding="utf-8")

    # Find the merge base commit between base_ref and HEAD so that only changes in this PR are considered
    merge_base_proc = subprocess.run(
        ["git", "merge-base", args.base_ref, "HEAD"],
        capture_output=True,
        text=True,
    )
    if merge_base_proc.returncode == 0 and merge_base_proc.stdout.strip():
        compare_ref = merge_base_proc.stdout.strip()
    else:
        compare_ref = args.base_ref

    git_show_cmd = ["git", "show", f"{compare_ref}:gradle/libs.versions.toml"]
    proc = subprocess.run(git_show_cmd, capture_output=True, text=True)
    if proc.returncode != 0:
        print(f"Warning: Failed to fetch base toml from {compare_ref}: {proc.stderr}")
        sys.exit(0)

    base_content = proc.stdout

    base_versions = extract_formatter_versions(base_content)
    head_versions = extract_formatter_versions(head_content)

    entries = []
    for tool_name, new_ver in head_versions.items():
        old_ver = base_versions.get(tool_name)
        if old_ver and old_ver != new_ver:
            entry = f"- Bump default `{tool_name}` version `{old_ver}` -> `{new_ver}`. ([#{args.pr_number}]({args.pr_url}))"
            entries.append(entry)

    if not entries:
        print("No formatter default versions changed in this branch.")
        return

    print(f"Detected {len(entries)} formatter version update(s):")
    for entry in entries:
        print(f"  {entry}")

    for changelog_file in CHANGELOG_FILES:
        if not changelog_file.exists():
            continue
        original = changelog_file.read_text(encoding="utf-8")
        updated = insert_changelog_entries(original, entries)
        if updated != original:
            changelog_file.write_text(updated, encoding="utf-8")
            print(f"Updated {changelog_file}")
        else:
            print(f"No changes needed for {changelog_file}")


if __name__ == "__main__":
    main()
