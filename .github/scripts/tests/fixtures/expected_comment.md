> [!NOTE]
> These results are affected by shared workloads on GitHub runners. Use the results only to detect possible regressions, but always rerun on a more stable machine before drawing conclusions!
> Regressions/improvements are highlighted when the difference exceeds 3.0%.

### Benchmark results

### BaselineBenchmark

| Method | characters | jsonSize | maskedKeyProbability | master (ops/s) | PR (ops/s) | change | master alloc (B/op) | PR alloc (B/op) | alloc change |
| --- | --- | --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| `writeFile` | `unicode` | `1kb` | `0.1` | `8,293` | `7,003` | 🔴 -15.5% | `5,920.3` | `5,920.3` | ⚪ +0.0% |

<details>
<summary>Full results — BaselineBenchmark</summary>

| Method | characters | jsonSize | maskedKeyProbability | master (ops/s) | PR (ops/s) | change | master alloc (B/op) | PR alloc (B/op) | alloc change |
| --- | --- | --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| `countBytes` | `unicode` | `1kb` | `0.1` | `2,518,975` | `2,533,217` | ⚪ +0.6% | `0.0010` | `0.0010` | ⚪ +0.0% |
| `jacksonParseAndMask` | `unicode` | `1kb` | `0.1` | `40,769` | `40,460` | ⚪ -0.8% | `58,184.1` | `58,456.1` | ⚪ +0.5% |
| `jacksonParseOnly` | `unicode` | `1kb` | `0.1` | `89,035` | `90,192` | ⚪ +1.3% | `17,616.0` | `17,616.0` | ⚪ +0.0% |
| `regexReplace` | `unicode` | `1kb` | `0.1` | `5,979` | `5,978` | ⚪ -0.0% | `53,048.4` | `53,048.4` | ⚪ +0.0% |
| `writeFile` | `unicode` | `1kb` | `0.1` | `8,293` | `7,003` | 🔴 -15.5% | `5,920.3` | `5,920.3` | ⚪ +0.0% |

</details>

---

### InstanceCreationBenchmark

_No significant changes (all within 3.0%)._

<details>
<summary>Full results — InstanceCreationBenchmark</summary>

| Method | numberOfTargetKeys | master (ops/s) | PR (ops/s) | change | master alloc (B/op) | PR alloc (B/op) | alloc change |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| `jsonMasker` | `1000` | `1,564` | `1,587` | ⚪ +1.4% | `1,637,307` | `1,637,212` | ⚪ -0.0% |

</details>

---

### JsonMaskerBenchmark

| Method | characters | jsonPath | jsonSize | maskedKeyProbability | master (ops/s) | PR (ops/s) | change | master alloc (B/op) | PR alloc (B/op) | alloc change |
| --- | --- | --- | --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| `jsonMaskerByteArrayStreams` | `unicode` | `true` | `1kb` | `0.1` | `221,516` | `275,345` | 🟢 +24.3% | `12,184.0` | `12,184.0` | ⚪ -0.0% |
| `jsonMaskerString` | `unicode` | `false` | `1kb` | `0.1` | `243,638` | `232,464` | 🔴 -4.6% | `10,176.0` | `10,176.0` | ⚪ +0.0% |

<details>
<summary>Full results — JsonMaskerBenchmark</summary>

| Method | characters | jsonPath | jsonSize | maskedKeyProbability | master (ops/s) | PR (ops/s) | change | master alloc (B/op) | PR alloc (B/op) | alloc change |
| --- | --- | --- | --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| `jsonMaskerByteArrayStreams` | `unicode` | `false` | `1kb` | `0.1` | `241,303` | `234,946` | ⚪ -2.6% | `10,840.0` | `10,840.0` | ⚪ +0.0% |
| `jsonMaskerByteArrayStreams` | `unicode` | `true` | `1kb` | `0.1` | `221,516` | `275,345` | 🟢 +24.3% | `12,184.0` | `12,184.0` | ⚪ -0.0% |
| `jsonMaskerBytes` | `unicode` | `false` | `1kb` | `0.1` | `446,412` | `443,851` | ⚪ -0.6% | `2,272.0` | `2,272.0` | ⚪ +0.0% |
| `jsonMaskerBytes` | `unicode` | `true` | `1kb` | `0.1` | `437,403` | `425,883` | ⚪ -2.6% | `2,024.0` | `2,024.0` | ⚪ +0.0% |
| `jsonMaskerString` | `unicode` | `false` | `1kb` | `0.1` | `243,638` | `232,464` | 🔴 -4.6% | `10,176.0` | `10,176.0` | ⚪ +0.0% |
| `jsonMaskerString` | `unicode` | `true` | `1kb` | `0.1` | `218,375` | `222,449` | ⚪ +1.9% | `10,944.0` | `10,944.0` | ⚪ -0.0% |

</details>

---

### LargeKeySetInstanceCreationBenchmark

_No significant changes (all within 3.0%)._

<details>
<summary>Full results — LargeKeySetInstanceCreationBenchmark</summary>

| Method | keyLength | numberOfTargetKeys | master (ops/s) | PR (ops/s) | change | master alloc (B/op) | PR alloc (B/op) | alloc change |
| --- | --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| `jsonMasker` | `100` | `1000` | `117.37` | `115.85` | ⚪ -1.3% | `32,372,334` | `32,372,239` | ⚪ -0.0% |

</details>

---

### StreamTypeBenchmark

| Method | jsonSize | streamInputType | streamOutputType | master (ops/s) | PR (ops/s) | change | master alloc (B/op) | PR alloc (B/op) | alloc change |
| --- | --- | --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| `jsonMaskerStreams` | `1kb` | `ByteArrayStream` | `ByteArrayStream` | `220,008` | `227,667` | 🟢 +3.5% | `12,184.0` | `12,184.0` | ⚪ +0.0% |
| `jsonMaskerStreams` | `1kb` | `FileStream` | `FileStream` | `5,474` | `5,818` | 🟢 +6.3% | `9,392.4` | `9,392.4` | ⚪ -0.0% |

<details>
<summary>Full results — StreamTypeBenchmark</summary>

| Method | jsonSize | streamInputType | streamOutputType | master (ops/s) | PR (ops/s) | change | master alloc (B/op) | PR alloc (B/op) | alloc change |
| --- | --- | --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| `jsonMaskerStreams` | `1kb` | `ByteArrayStream` | `ByteArrayStream` | `220,008` | `227,667` | 🟢 +3.5% | `12,184.0` | `12,184.0` | ⚪ +0.0% |
| `jsonMaskerStreams` | `1kb` | `ByteArrayStream` | `FileStream` | `6,179` | `6,162` | ⚪ -0.3% | `9,208.4` | `9,208.4` | ⚪ +0.0% |
| `jsonMaskerStreams` | `1kb` | `FileStream` | `ByteArrayStream` | `68,387` | `67,444` | ⚪ -1.4% | `12,336.0` | `12,320.0` | ⚪ -0.1% |
| `jsonMaskerStreams` | `1kb` | `FileStream` | `FileStream` | `5,474` | `5,818` | 🟢 +6.3% | `9,392.4` | `9,392.4` | ⚪ -0.0% |

</details>

---

### ValueMaskerBenchmark

| Method | characters | jsonSize | maskedKeyProbability | master (ops/s) | PR (ops/s) | change | master alloc (B/op) | PR alloc (B/op) | alloc change |
| --- | --- | --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| `maskWithTextValueFunction` | `unicode` | `1kb` | `0.1` | `588,537` | `585,127` | ⚪ -0.6% | `1,848.0` | `1,776.0` | 🟢 -3.9% |

<details>
<summary>Full results — ValueMaskerBenchmark</summary>

| Method | characters | jsonSize | maskedKeyProbability | master (ops/s) | PR (ops/s) | change | master alloc (B/op) | PR alloc (B/op) | alloc change |
| --- | --- | --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| `maskWithRawValueFunction` | `unicode` | `1kb` | `0.1` | `624,328` | `617,057` | ⚪ -1.2% | `1,632.0` | `1,632.0` | ⚪ +0.0% |
| `maskWithStatic` | `unicode` | `1kb` | `0.1` | `666,894` | `671,402` | ⚪ +0.7% | `1,272.0` | `1,272.0` | ⚪ +0.0% |
| `maskWithTextValueFunction` | `unicode` | `1kb` | `0.1` | `588,537` | `585,127` | ⚪ -0.6% | `1,848.0` | `1,776.0` | 🟢 -3.9% |

</details>

<details>
<summary>Raw output (PR @ fa40aad)</summary>

```text
Benchmark                                                           (characters)  (jsonPath)  (jsonSize)  (keyLength)  (maskedKeyProbability)  (numberOfTargetKeys)  (streamInputType)  (streamOutputType)   Mode  Cnt         Score        Error   Units
BaselineBenchmark.countBytes                                             unicode         N/A         1kb          N/A                     0.1                   N/A                N/A                 N/A  thrpt    4   2533216.945 ±  22406.298   ops/s
BaselineBenchmark.jacksonParseAndMask                                    unicode         N/A         1kb          N/A                     0.1                   N/A                N/A                 N/A  thrpt    4     40459.573 ±    691.912   ops/s
BaselineBenchmark.jacksonParseOnly                                       unicode         N/A         1kb          N/A                     0.1                   N/A                N/A                 N/A  thrpt    4     90191.993 ±    613.149   ops/s
BaselineBenchmark.regexReplace                                           unicode         N/A         1kb          N/A                     0.1                   N/A                N/A                 N/A  thrpt    4      5978.116 ±     88.936   ops/s
BaselineBenchmark.writeFile                                              unicode         N/A         1kb          N/A                     0.1                   N/A                N/A                 N/A  thrpt    4      7003.495 ±    155.660   ops/s
InstanceCreationBenchmark.jsonMasker                                         N/A         N/A         N/A          N/A                     N/A                  1000                N/A                 N/A  thrpt    4      1586.713 ±     25.807   ops/s
JsonMaskerBenchmark.jsonMaskerByteArrayStreams                           unicode       false         1kb          N/A                     0.1                   N/A                N/A                 N/A  thrpt    4    234946.258 ±  14786.835   ops/s
JsonMaskerBenchmark.jsonMaskerByteArrayStreams                           unicode        true         1kb          N/A                     0.1                   N/A                N/A                 N/A  thrpt    4    275344.797 ±   8288.823   ops/s
JsonMaskerBenchmark.jsonMaskerBytes                                      unicode       false         1kb          N/A                     0.1                   N/A                N/A                 N/A  thrpt    4    443851.316 ±   5845.203   ops/s
JsonMaskerBenchmark.jsonMaskerBytes                                      unicode        true         1kb          N/A                     0.1                   N/A                N/A                 N/A  thrpt    4    425883.158 ± 154382.066   ops/s
JsonMaskerBenchmark.jsonMaskerString                                     unicode       false         1kb          N/A                     0.1                   N/A                N/A                 N/A  thrpt    4    232464.069 ±   4542.969   ops/s
JsonMaskerBenchmark.jsonMaskerString                                     unicode        true         1kb          N/A                     0.1                   N/A                N/A                 N/A  thrpt    4    222449.337 ±   1652.349   ops/s
LargeKeySetInstanceCreationBenchmark.jsonMasker                              N/A         N/A         N/A          100                     N/A                  1000                N/A                 N/A  thrpt    4       115.852 ±      5.273   ops/s
StreamTypeBenchmark.jsonMaskerStreams                                        N/A         N/A         1kb          N/A                     N/A                   N/A    ByteArrayStream     ByteArrayStream  thrpt    4    227667.298 ±   3800.277   ops/s
StreamTypeBenchmark.jsonMaskerStreams                                        N/A         N/A         1kb          N/A                     N/A                   N/A    ByteArrayStream          FileStream  thrpt    4      6162.006 ±    993.413   ops/s
StreamTypeBenchmark.jsonMaskerStreams                                        N/A         N/A         1kb          N/A                     N/A                   N/A         FileStream     ByteArrayStream  thrpt    4     67444.497 ±   1689.348   ops/s
StreamTypeBenchmark.jsonMaskerStreams                                        N/A         N/A         1kb          N/A                     N/A                   N/A         FileStream          FileStream  thrpt    4      5817.861 ±    642.408   ops/s
ValueMaskerBenchmark.maskWithRawValueFunction                            unicode         N/A         1kb          N/A                     0.1                   N/A                N/A                 N/A  thrpt    4    617056.912 ±  10862.097   ops/s
ValueMaskerBenchmark.maskWithStatic                                      unicode         N/A         1kb          N/A                     0.1                   N/A                N/A                 N/A  thrpt    4    671401.839 ±  10288.106   ops/s
ValueMaskerBenchmark.maskWithTextValueFunction                           unicode         N/A         1kb          N/A                     0.1                   N/A                N/A                 N/A  thrpt    4    585127.096 ±  10531.726   ops/s
```

</details>

<details>
<summary>Raw output (master @ e3a00f2)</summary>

```text
Benchmark                                                           (characters)  (jsonPath)  (jsonSize)  (keyLength)  (maskedKeyProbability)  (numberOfTargetKeys)  (streamInputType)  (streamOutputType)   Mode  Cnt         Score       Error   Units
BaselineBenchmark.countBytes                                             unicode         N/A         1kb          N/A                     0.1                   N/A                N/A                 N/A  thrpt    4   2518975.204 ± 16196.191   ops/s
BaselineBenchmark.jacksonParseAndMask                                    unicode         N/A         1kb          N/A                     0.1                   N/A                N/A                 N/A  thrpt    4     40769.235 ±   387.452   ops/s
BaselineBenchmark.jacksonParseOnly                                       unicode         N/A         1kb          N/A                     0.1                   N/A                N/A                 N/A  thrpt    4     89035.225 ±  1005.204   ops/s
BaselineBenchmark.regexReplace                                           unicode         N/A         1kb          N/A                     0.1                   N/A                N/A                 N/A  thrpt    4      5978.987 ±   154.749   ops/s
BaselineBenchmark.writeFile                                              unicode         N/A         1kb          N/A                     0.1                   N/A                N/A                 N/A  thrpt    4      8292.662 ±  2471.505   ops/s
InstanceCreationBenchmark.jsonMasker                                         N/A         N/A         N/A          N/A                     N/A                  1000                N/A                 N/A  thrpt    4      1564.397 ±    46.524   ops/s
JsonMaskerBenchmark.jsonMaskerByteArrayStreams                           unicode       false         1kb          N/A                     0.1                   N/A                N/A                 N/A  thrpt    4    241303.055 ±  2648.009   ops/s
JsonMaskerBenchmark.jsonMaskerByteArrayStreams                           unicode        true         1kb          N/A                     0.1                   N/A                N/A                 N/A  thrpt    4    221515.799 ±  8491.633   ops/s
JsonMaskerBenchmark.jsonMaskerBytes                                      unicode       false         1kb          N/A                     0.1                   N/A                N/A                 N/A  thrpt    4    446412.357 ±  5090.383   ops/s
JsonMaskerBenchmark.jsonMaskerBytes                                      unicode        true         1kb          N/A                     0.1                   N/A                N/A                 N/A  thrpt    4    437402.948 ± 25140.543   ops/s
JsonMaskerBenchmark.jsonMaskerString                                     unicode       false         1kb          N/A                     0.1                   N/A                N/A                 N/A  thrpt    4    243637.569 ±  4871.658   ops/s
JsonMaskerBenchmark.jsonMaskerString                                     unicode        true         1kb          N/A                     0.1                   N/A                N/A                 N/A  thrpt    4    218375.055 ±  8553.659   ops/s
LargeKeySetInstanceCreationBenchmark.jsonMasker                              N/A         N/A         N/A          100                     N/A                  1000                N/A                 N/A  thrpt    4       117.373 ±     5.279   ops/s
StreamTypeBenchmark.jsonMaskerStreams                                        N/A         N/A         1kb          N/A                     N/A                   N/A    ByteArrayStream     ByteArrayStream  thrpt    4    220007.709 ±  3507.375   ops/s
StreamTypeBenchmark.jsonMaskerStreams                                        N/A         N/A         1kb          N/A                     N/A                   N/A    ByteArrayStream          FileStream  thrpt    4      6178.978 ±   268.377   ops/s
StreamTypeBenchmark.jsonMaskerStreams                                        N/A         N/A         1kb          N/A                     N/A                   N/A         FileStream     ByteArrayStream  thrpt    4     68386.635 ±  2259.387   ops/s
StreamTypeBenchmark.jsonMaskerStreams                                        N/A         N/A         1kb          N/A                     N/A                   N/A         FileStream          FileStream  thrpt    4      5473.957 ±  1258.010   ops/s
ValueMaskerBenchmark.maskWithRawValueFunction                            unicode         N/A         1kb          N/A                     0.1                   N/A                N/A                 N/A  thrpt    4    624328.185 ± 14233.853   ops/s
ValueMaskerBenchmark.maskWithStatic                                      unicode         N/A         1kb          N/A                     0.1                   N/A                N/A                 N/A  thrpt    4    666894.359 ±  4303.927   ops/s
ValueMaskerBenchmark.maskWithTextValueFunction                           unicode         N/A         1kb          N/A                     0.1                   N/A                N/A                 N/A  thrpt    4    588536.839 ±  3172.390   ops/s
```

</details>
