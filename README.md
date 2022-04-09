# Message masker

High-performance message masker that can be used to mask the value corresponding to some target key(s). Currently, the only supported message format is JSON.

## Upcoming features
- [x] JSON: adding length obfuscation option
- [x] JSON: targeting multiple (naive, time complexity n * target key set length)
- [x] JSON: targeting multiple keys (time complexity n)
- [ ] JSON: add support for input String containing JSON array instead of JSON object

- [ ] XML: add XML masking
- [ ] HTTP variables: add HTTP key/value pair masking

### Optional features
- [ ] JSON: add feature to enable/disable numeric values

## Bug fixes
- [x] JSON: Besides skipping UTF_8 SPACE (dec 32), also skip LINE FEED (dec 10), HORIZONTAL TAB (dec 9), and CARIAGE RETURN (dec 13)
- [x] JSON: Ignore escaped characters 

## Tests / Benchmarks
- [x] Benchmark masking in larger, deeply nested JSON objects
- [ ] Benchmark the two multitarget key algortihms to find the break-even point 
