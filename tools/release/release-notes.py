#!/usr/bin/env python3
"""Extract a dated changelog section only when its version matches the release tag."""
from pathlib import Path
import re
import sys

root = Path(__file__).resolve().parents[2]
tag = sys.argv[1]
if not re.fullmatch(r'v\d+\.\d+\.\d+', tag): sys.exit('Expected a semantic version tag: vX.Y.Z')
version = tag[1:]
properties = (root / 'gradle.properties').read_text()
if re.search(r'^mod_version=(.+)$', properties, re.MULTILINE).group(1) != version:
    sys.exit('Tag does not match mod_version')
changelog = (root / 'CHANGELOG.md').read_text()
match = re.search(r'^## \[' + re.escape(version) + r'\] - (\d{4}-\d{2}-\d{2})\n(.*?)(?=^## |\Z)', changelog, re.MULTILINE | re.DOTALL)
if not match: sys.exit('Date the changelog section before tagging a public release')
print(match.group(2).split('\n[Unreleased]:')[0].strip())
