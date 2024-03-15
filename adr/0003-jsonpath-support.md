# 3. JSONPath support

Date: 15-03-2024

## Status

Draft

## Context

Support for JsonPATH was requested by some potential library clients.  
However, JsonPATH RFC specifies way too many features which are tedious to implement without having runtime dependencies.  
Therefore, we have to decide which features are necessary and which are not. 

## Decisions

The decisions are premised on the following expectations:
1. Most of the clients will not use JsonPATH keys.  
2. The correctness of the library takes precedent over the amount of features.

Any implemented jsonpath feature adds some performance overhead both on configuration and on masking time, even if it is not used. In general, it also increases the likelihood of incorrect behaviour as the amount of code increases.
Therefore, we decided to keep support for JsonPATH as limited as possible and to extend it only when someone specifically requests it.

The following features are deemed necessary:
1. Support for bracket, dot and mixed notations.  
2. Support for object segments.  
3. Support for array segments with wildcard indexing only.  
4. Wildcard segments and selectors.   

The rest of the features are deemed unnecessary.

## Consequences

### Performance considerations.

Supporting JsonPATH makes the masking 25% slower according to the latest benchmarks.  
We decided to disable JsonPATH in case no JsonPATH keys are supplied.

### Potential bugs

Mixing keys and JsonPATH keys in the same trie opens up some bugs:
1. https://github.com/Breus/json-masker/issues/94  
We do not expect this issue to be reproduced naturally.