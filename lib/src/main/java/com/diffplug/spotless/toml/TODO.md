# VersionCatalogStep — Known Limitations & Future Work

Tracked issues found during review against the TOML v1.1.0 spec (https://toml.io/en/v1.1.0).

## Fixed

- [x] Comments inside sections are silently dropped (data loss for annotated catalogs)
- [x] Inline comments after values break inline-table/array formatting
- [x] Blank lines in preamble are dropped
- [x] Sort uses raw string including quote characters instead of logical key name
- [x] Multi-line inline tables are not parsed correctly (split across lines and corrupted)
- [x] Long lines can be split into multi-line inline tables (configurable `maxLineLength`)
- [x] Short multi-line inline tables are joined into single lines when they fit

## TODO — TOML spec edge cases

These are valid TOML constructs that the current implementation does not handle correctly.
They are uncommon (or impossible) in typical Gradle version catalogs, but should be
addressed for full TOML spec compliance.

### Parsing

- [ ] `ENTRY_LINE` regex `^([^=]+)=(.+)$` is not quote-aware — breaks on quoted keys
      containing `=`, e.g. `"key=val" = "foo"`. Needs a state-machine parser to find
      the separator `=` outside quoting context.
- [ ] `TABLE_HEADER` regex only matches bare keys — rejects dotted table headers
      (`[section.subsection]`) and quoted table headers (`["quoted.key"]`).
      Their entries are silently dropped.

### String handling in `splitTopLevel`

- [ ] Single-quoted (literal) strings `'...'` are not recognized — commas or `=` inside
      them will incorrectly split or match.
- [ ] Multiline string delimiters (`"""`, `'''`) confuse the single-char quote toggle.
- [ ] Double-backslash before closing quote (`"value\\"`) is misidentified as an escaped
      quote. Needs odd/even backslash counting instead of single-char lookbehind.
