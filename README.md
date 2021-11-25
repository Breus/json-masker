# Message masker

High-performance message masker that can be used to mask the value corresponding to some target key(s). Currently, the only supported message format is JSON.

## Upcoming features
- [ ] JSON: masking number values
- [ ] JSON: adding length obfuscation option
- [ ] JSON: targeting multiple (X) keys (we should implement this differently than calling the current function X times, because that would mean our time complexity is scaling both in input length and target key set length. Instead, we should probably make some set and manually detect JSON keys and see if the target set contains this key name.

## Bug fixes
- [ ] Besides skipping UTF_8 SPACE (dec 32), also skip LINE FEED (dec 10) and CARIAGE RETURN (dec 13)
