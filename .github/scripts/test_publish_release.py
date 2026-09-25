import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest


SCRIPT = Path(__file__).with_name("publish-release.sh").resolve()
PUBLICATIONS = {
    "lib": [":lib:publishToMavenCentral", ":lib-extra:publishToMavenCentral"],
    "plugin-gradle": [":plugin-gradle:publishPlugins", ":plugin-gradle:publishToMavenCentral"],
    "plugin-maven": [":plugin-maven:publishToMavenCentral"],
}


class ReleasePublicationTest(unittest.TestCase):
    def run_release(self, component, publish_exit):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            wrapper = root / "gradlew"
            wrapper.write_text(
                "#!/usr/bin/env python3\n"
                "import json, os, sys\n"
                "with open('calls.jsonl', 'a') as calls:\n"
                "    calls.write(json.dumps(sys.argv[1:]) + '\\n')\n"
                "if any(arg.endswith(':changelogPush') for arg in sys.argv):\n"
                "    open('tag-created', 'w').close()\n"
                "else:\n"
                "    # Model a failure at build shutdown, after publication tasks finish.\n"
                "    sys.exit(int(os.environ['PUBLISH_EXIT']))\n"
            )
            wrapper.chmod(0o755)
            result = subprocess.run(
                ["bash", str(SCRIPT), component],
                cwd=root,
                env=dict(os.environ, PUBLISH_EXIT=str(publish_exit),
                         GRADLE_KEY="test-key", GRADLE_SECRET="test-secret"),
                capture_output=True,
                text=True,
            )
            calls = [json.loads(line) for line in (root / "calls.jsonl").read_text().splitlines()]
            return result, calls, (root / "tag-created").exists()

    def test_failed_publication_never_updates_changelog_or_pushes_tag(self):
        for component in PUBLICATIONS:
            with self.subTest(component=component):
                result, calls, tagged = self.run_release(component, 42)
                self.assertEqual(result.returncode, 42)
                self.assertEqual(len(calls), 1)
                self.assertFalse(tagged)
                self.assertNotIn("-PpublicationConfirmed=true", calls[0])

    def test_confirmed_publication_precedes_changelog_without_republishing(self):
        for component, publications in PUBLICATIONS.items():
            with self.subTest(component=component):
                result, calls, tagged = self.run_release(component, 0)
                self.assertEqual(result.returncode, 0, result.stderr)
                self.assertEqual(len(calls), 2)
                self.assertTrue(tagged)
                self.assertIn("-PmavenCentralDeploymentValidation=PUBLISHED", calls[0])
                self.assertNotIn("-PpublicationConfirmed=true", calls[0])
                self.assertIn("-PpublicationConfirmed=true", calls[1])
                for task in publications:
                    self.assertIn(task, calls[0])
                    self.assertEqual(calls[1][calls[1].index(task) - 1], "-x")


if __name__ == "__main__":
    unittest.main()
