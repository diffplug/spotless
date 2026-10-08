import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

from update_renovate_changelogs import insert_changelog_entries


def bump(old, new, *prs, tool="palantir-java-format"):
    links = ", ".join(f"[#{pr}](https://github.com/diffplug/spotless/pull/{pr})" for pr in prs)
    return f"- Bump default `{tool}` version `{old}` -> `{new}`. ({links})"


def changelog(*entries):
    return "# Releases\n\n## [Unreleased]\n\n### Changes\n" + "\n".join(entries) + "\n\n"


class ChangelogTest(unittest.TestCase):
    def test_consecutive_bumps_preserve_start_and_links(self):
        original = changelog(bump("2.98.0", "2.99.0", 3103), "- Another change.")
        expected = changelog(bump("2.98.0", "2.102.0", 3103, 3132), "- Another change.")
        self.assertEqual(expected, insert_changelog_entries(original, [bump("2.99.0", "2.102.0", 3132)]))

    def test_rerun_preserves_manual_consolidation(self):
        original = changelog(bump("2.98.0", "2.102.0", 3103, 3132))
        self.assertEqual(original, insert_changelog_entries(original, [bump("2.99.0", "2.102.0", 3132)]))

    def test_repairs_duplicate_reintroduced_after_manual_consolidation(self):
        incoming = bump("2.99.0", "2.102.0", 3132)
        original = changelog(bump("2.98.0", "2.102.0", 3103, 3132), incoming)
        expected = changelog(bump("2.98.0", "2.102.0", 3103, 3132))
        self.assertEqual(expected, insert_changelog_entries(original, [incoming]))

    def test_consolidates_multiple_existing_bumps(self):
        original = changelog(bump("1", "2", 1), "- Another change.", bump("2", "3", 2))
        expected = changelog(bump("1", "4", 1, 2, 3), "- Another change.")
        self.assertEqual(expected, insert_changelog_entries(original, [bump("3", "4", 3)]))

    def test_same_pr_can_change_target_version_without_duplicate_link(self):
        original = changelog(bump("2.98.0", "2.100.0", 3103, 3132))
        expected = changelog(bump("2.98.0", "2.102.0", 3103, 3132))
        self.assertEqual(expected, insert_changelog_entries(original, [bump("2.99.0", "2.102.0", 3132)]))

    def test_grouped_update_merges_and_inserts_independent_formatters(self):
        original = changelog(bump("1", "2", 1))
        entries = [bump("2", "3", 2), bump("0.64", "0.65", 2, tool="ktfmt")]
        expected = changelog(bump("1", "3", 1, 2), entries[1])
        result = insert_changelog_entries(original, entries)
        self.assertEqual(expected, result)
        self.assertEqual(result, insert_changelog_entries(result, entries))

    def test_preserves_continuation_notes_and_markdown_hard_break(self):
        original = changelog(bump("1", "2", 1) + "  ", "  Coordinate migration details.")
        expected = changelog(bump("1", "3", 1, 2) + "  ", "  Coordinate migration details.")
        self.assertEqual(expected, insert_changelog_entries(original, [bump("2", "3", 2)]))

    def test_only_changes_in_unreleased_are_consolidated(self):
        suffix = (
            "### Fixed\n" + bump("1", "2", 1) + "\n\n"
            "## [1.0] - 2026-01-01\n\n### Changes\n" + bump("0", "1", 0) + "\n"
        )
        original = changelog("- Another change.") + suffix
        incoming = bump("2", "3", 2)
        self.assertEqual(changelog("- Another change.", incoming) + suffix,
                         insert_changelog_entries(original, [incoming]))

    def test_empty_unreleased_does_not_consume_next_release(self):
        suffix = "## [1.0] - 2026-01-01\n\n### Changes\n" + bump("0", "1", 1) + "\n"
        original = "# Releases\n\n## [Unreleased]\n\n" + suffix
        incoming = bump("1", "2", 2)
        self.assertEqual(changelog(incoming) + suffix, insert_changelog_entries(original, [incoming]))

    def test_creates_changes_before_existing_section(self):
        original = "# Releases\n\n## [Unreleased]\n\n### Fixed\n- Bug fix.\n"
        incoming = bump("1", "2", 2)
        self.assertEqual(changelog(incoming) + "### Fixed\n- Bug fix.\n",
                         insert_changelog_entries(original, [incoming]))

    def test_missing_unreleased_and_empty_updates_are_unchanged(self):
        released = "## [1.0]\n\n### Changes\n" + bump("1", "2", 1) + "\n"
        self.assertEqual(released, insert_changelog_entries(released, [bump("2", "3", 2)]))
        original = changelog(bump("1", "2", 1))
        self.assertEqual(original, insert_changelog_entries(original, []))

    def test_handwritten_bump_with_inline_explanation_is_preserved(self):
        handwritten = bump("1", "2", 1) + " Requires Java 21."
        incoming = bump("2", "3", 2)
        self.assertEqual(changelog(handwritten, incoming),
                         insert_changelog_entries(changelog(handwritten), [incoming]))


class CommandLineTest(unittest.TestCase):
    def test_updates_all_changelogs_using_merge_base_and_is_idempotent(self):
        script = Path(__file__).with_name("update_renovate_changelogs.py").resolve()
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)

            def git(*args):
                return subprocess.run(["git", *args], cwd=root, check=True, capture_output=True, text=True)

            def catalog(version):
                (root / "gradle/libs.versions.toml").write_text(
                    '[libraries]\npalantir-java-format = '
                    f'"com.palantir.javaformat:palantir-java-format:{version}"\n', encoding="utf-8")

            git("init", "-b", "main")
            git("config", "user.name", "Test")
            git("config", "user.email", "test@example.com")
            (root / "gradle").mkdir()
            catalog("2.99.0")
            paths = [root / name for name in ("CHANGES.md", "plugin-gradle/CHANGES.md", "plugin-maven/CHANGES.md")]
            for path in paths:
                path.parent.mkdir(exist_ok=True)
                path.write_text(changelog(bump("2.98.0", "2.99.0", 3103)), encoding="utf-8")
            git("add", ".")
            git("commit", "-m", "Base release")
            git("checkout", "-b", "renovate/test")
            catalog("2.102.0")
            git("commit", "-am", "Bump formatter")
            git("checkout", "main")
            catalog("2.100.0")
            git("commit", "-am", "Advance base branch")
            git("checkout", "renovate/test")

            command = [sys.executable, str(script), "--base-ref", "main", "--pr-number", "3132",
                       "--pr-url", "https://github.com/diffplug/spotless/pull/3132"]
            for _ in range(2):
                result = subprocess.run(command, cwd=root, check=True, capture_output=True, text=True)
                self.assertIn("`2.99.0` -> `2.102.0`", result.stdout)
                for path in paths:
                    self.assertEqual(changelog(bump("2.98.0", "2.102.0", 3103, 3132)),
                                     path.read_text(encoding="utf-8"))


if __name__ == "__main__":
    unittest.main()
