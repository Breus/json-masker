# Message masker

High-performance message masker that can be used to mask the value corresponding to some target key(s). Currently, the only supported message format is JSON.

## Upcoming features
- [x] JSON: adding length obfuscation option
- [x] JSON: targeting multiple (naive, time complexity n * target key set length)
- [ ] JSON: masking number values
- [ ] JSON: targeting multiple keys (time complexity n)
- [ ] JSON: add support for input String containing JSON array instead of JSON object

## Bug fixes
- [ ] Besides skipping UTF_8 SPACE (dec 32), also skip LINE FEED (dec 10) and CARIAGE RETURN (dec 13)
