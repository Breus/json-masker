# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.1.2] - 2025-03-31

This release contains no functional changes. It fixes the Multi-release JAR (MR-JAR) artifact to correctly include the Java 17 source set.

### Fixed
- Reimplemented multi-release JAR support so the Java 17 compiled classes are correctly packaged in the artifact ([#231](https://github.com/Breus/json-masker/pull/231))

## [1.1.1] - 2025-03-07

This release fixes a critical concurrency bug introduced in `1.1.0` that could cause exceptions when multiple threads reused the same `JsonMasker` instance simultaneously.

### Fixed
- Fixed leaking of mutable state during concurrent masking ([#227](https://github.com/Breus/json-masker/pull/227))

## [1.1.0] - 2025-01-26

### Added
- **Streaming API**: `JsonMasker.mask(InputStream, OutputStream)` for memory-efficient masking of large JSONs without loading the entire input into memory ([#163](https://github.com/Breus/json-masker/pull/163))
- Support for masking multiple JSON values in a single byte array input ([#175](https://github.com/Breus/json-masker/pull/175))
- **Java 11 support**: minimum JDK requirement lowered from 17 to 11 using a Multi-release JAR ([#214](https://github.com/Breus/json-masker/pull/214))

### Improved
- **Radix Trie key-matching**: replaced the previous trie implementation with a radix trie, reducing the retained memory of a `JsonMasker` instance by up to **70%** ([#153](https://github.com/Breus/json-masker/pull/153))

## [1.0.2] - 2024-07-16

### Added
- Support for masking JSON keys containing Unicode escape sequences ([#141](https://github.com/Breus/json-masker/pull/141))
- JSONTestSuite compliance tests ([#140](https://github.com/Breus/json-masker/pull/140))

### Improved
- Memory-optimised trie structure, reducing the retained memory of a `JsonMasker` instance by up to an order of magnitude for large target key sets (>500 keys) ([#156](https://github.com/Breus/json-masker/pull/156))

## [1.0.1] - 2024-05-01

### Added
- `JsonMaskingConfig.Builder.maskKeys(String key, KeyMaskingConfig config)` and `maskKeys(Map<String, KeyMaskingConfig> keyConfigs)` convenience overloads ([#132](https://github.com/Breus/json-masker/pull/132))
- JPMS `module-info.java` descriptor ([#126](https://github.com/Breus/json-masker/pull/126))

### Fixed
- JSON keys longer than 256 bytes were not matched correctly ([#130](https://github.com/Breus/json-masker/pull/130))
- Control characters in string mask values were not escaped, producing invalid JSON output ([#127](https://github.com/Breus/json-masker/pull/127))
- Mask values were not JSON-encoded, producing invalid JSON output in edge cases ([#133](https://github.com/Breus/json-masker/pull/133))
- Misleading ambiguity check error message in `JsonPathParser` ([#136](https://github.com/Breus/json-masker/pull/136))

## [1.0.0] - 2024-04-08

First stable release. No breaking changes relative to `1.0.0-rc3`.

### Added
- `ValueMaskers.withTextFunction(Function<String, String>)`: a safe alternative to `withRawValueFunction` that decodes the JSON string value before passing it to the function and re-encodes the result, preventing invalid JSON output ([#104](https://github.com/Breus/json-masker/pull/104))
- `ValueMaskers.eachDigitWith(String value)` to replace each digit of a number with a repeating string (e.g. `*`) ([#108](https://github.com/Breus/json-masker/pull/108))
- JSONPath-specific `KeyMaskingConfig` can now override a more general path's config ([#119](https://github.com/Breus/json-masker/pull/119))

### Improved
- JSONPath look-up performance optimisation: avoids traversing the trie from the root on each key ([#116](https://github.com/Breus/json-masker/pull/116))

## [1.0.0-rc3] - 2024-03-27

### Changed
- `ValueMaskers.withTextFunction` renamed to `ValueMaskers.withRawValueFunction` to make clear that the value passed to the function is a raw JSON literal, not a decoded string ([#103](https://github.com/Breus/json-masker/pull/103))

## [1.0.0-rc2] - 2024-03-13

### Fixed
- `ArrayIndexOutOfBoundsException` when skipping an allowed string value containing an escaped backslash (`\\`) ([#92](https://github.com/Breus/json-masker/pull/92))

## [1.0.0-rc1] - 2024-02-22

First release candidate for `1.0.0`, introducing a significantly reworked API.

### Added
- `ValueMasker` interface and `ValueMaskers` factory for custom per-type masking strategies (string, number, boolean) ([#96](https://github.com/Breus/json-masker/pull/96))
- `ValueMaskers.withTextFunction(Function<String, String>)` to mask values using a custom function ([#96](https://github.com/Breus/json-masker/pull/96))
- `InvalidJsonException` replaces the previous generic `RuntimeException` for invalid JSON input ([#99](https://github.com/Breus/json-masker/pull/99))
- JSONPath wildcard (`*`) segment support ([#83](https://github.com/Breus/json-masker/pull/83))
- JSONPath support for arrays ([#62](https://github.com/Breus/json-masker/pull/62))
- `CONTRIBUTING.md` and `SECURITY.md` ([#101](https://github.com/Breus/json-masker/pull/101), [#100](https://github.com/Breus/json-masker/pull/100))

### Changed
- Major API refactor: unified all value masking through `ValueMasker` / `KeyMaskingConfig` ([#96](https://github.com/Breus/json-masker/pull/96))
- JSON parsing rewritten to always advance the index, improving correctness and simplifying the implementation ([#97](https://github.com/Breus/json-masker/pull/97))

## [0.2.1] - 2024-01-06

### Improved
- Replacement operations are now batched and applied in a single array resize pass, giving up to **20× speedup** for JSONs that require resizing (non-ASCII characters, length-obfuscated masking) ([#33](https://github.com/Breus/json-masker/pull/33))

## [0.2.0] - 2023-12-30

> **Note:** This release is backward incompatible with `0.1.0`. JSON arrays and objects whose key matches a target key are now fully masked (all nested values replaced), whereas previously only the direct string/number value was masked.

### Added
- Masking of JSON array and object values when the parent key matches a target key ([#18](https://github.com/Breus/json-masker/pull/18))
- Case-insensitive key matching (enabled by default) ([#30](https://github.com/Breus/json-masker/pull/30))

### Improved
- Replaced `HashSet` key lookup with a byte-level Trie for significantly faster key matching, also used in allow-mode ([#19](https://github.com/Breus/json-masker/pull/19), [#22](https://github.com/Breus/json-masker/pull/22), [#26](https://github.com/Breus/json-masker/pull/26))

### Fixed
- Multiple correctness bugs discovered via fuzzing against a Jackson reference implementation ([#27](https://github.com/Breus/json-masker/pull/27), [#28](https://github.com/Breus/json-masker/pull/28), [#29](https://github.com/Breus/json-masker/pull/29))

## [0.1.0] - 2023-10-08

Initial alpha release.

### Added
- Core JSON masking by target key (mask mode and allow mode)
- Non-recursive JSON parser for safety on deeply nested input
- Length-preserving masking (obfuscation length)
- Character escape sequence support in string values
- CI/CD pipeline for building and releasing to Maven Central

[Unreleased]: https://github.com/Breus/json-masker/compare/v1.1.2...HEAD
[1.1.2]: https://github.com/Breus/json-masker/compare/v1.1.1...v1.1.2
[1.1.1]: https://github.com/Breus/json-masker/compare/v1.1.0...v1.1.1
[1.1.0]: https://github.com/Breus/json-masker/compare/v1.0.2...v1.1.0
[1.0.2]: https://github.com/Breus/json-masker/compare/v1.0.1...v1.0.2
[1.0.1]: https://github.com/Breus/json-masker/compare/v1.0.0...v1.0.1
[1.0.0]: https://github.com/Breus/json-masker/compare/v1.0.0-rc3...v1.0.0
[1.0.0-rc3]: https://github.com/Breus/json-masker/compare/v1.0.0-rc2...v1.0.0-rc3
[1.0.0-rc2]: https://github.com/Breus/json-masker/compare/v1.0.0-rc1...v1.0.0-rc2
[1.0.0-rc1]: https://github.com/Breus/json-masker/compare/v0.2.1...v1.0.0-rc1
[0.2.1]: https://github.com/Breus/json-masker/compare/v0.2.0...v0.2.1
[0.2.0]: https://github.com/Breus/json-masker/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/Breus/json-masker/commits/v0.1.0
