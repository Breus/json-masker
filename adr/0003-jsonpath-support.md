# 3. JSONPath support

Date: 15-03-2024

## Status

Accepted by:
- [Breus](https://github.com/Breus)
- [gavlyukovskiy](https://github.com/gavlyukovskiy)
- [donavdey](https://github.com/donavdey)

## Context

A JSON message may contain multiple nested objects with key/value pairs that have the same key but require a different
masking strategy.

Consider the following JSON:

```json
{
  "name": "do not mask",
  "nestedObject": {
    "name": "mask",
    "public": "do not mask",
    "private": "mask"
  }
}
```

In case the outer `name` value must not be masked but the inner `name` value in `nestedObject` must be masked, this is
currently not
possible with regular key masking.

## Decisions

### JSONPath

The solution to the described problem is to provide a way to disambiguate key/value pairs by selecting the target pair
using its location in JSON.  
The industry standard for selecting values in JSON is JSONPath. Most developers are expected to know how to create basic
JSONPath queries using either bracket or dot notation.  
Therefore, we decided to solve the key ambiguity problem using JSONPath.

### Supported features

The [JSONPath RFC 9535](https://www.rfc-editor.org/rfc/rfc9535.html) specifies a wide variety of features. Not all of them are required to solve the described problem.  
Therefore, we have to decide which features are necessary and which are not.

The decision is premised on the following expectations:

1. Most of the clients will not face the key ambiguity problem, therefore they will not use JSONPath.
2. Correctness, performance and maintainability of the library takes precedent over the number of features.
3. JSONPath support is required only for solving the key ambiguity problem.

Any implemented JSONPath feature adds some performance overhead both on configuration and on masking time, even if it is
not used.
In general, it also increases the likelihood of incorrect behaviour as the amount of code increases. So having
unnecessary features violates the premises.   
Therefore, we decided to keep support for JSONPath as limited as possible and extend it only when someone specifically
requests it.

The following features are necessary to support:

1. **Support for bracket, dot and mixed notations**.   
   We expect developers to know either of these notations, but we cannot know which one exactly. Also, the JSONPath
   could originate from code which uses either notation.
2. **Support for name selectors**.  
   This is the main building block of JSONPath queries.
3. **Support for wildcard segments and selectors**.  
   Wildcard selectors are necessary for traversing array values.

## Unsupported features

1. **Index selectors, array slice selectors, filter selectors**.  
   We do not expect clients to mask only some elements of an array value.
2. **Function extensions.**  
   We expect the key/value pairs locations in JSON to be known apriori. Therefore, no computation is required.
3. **Descendant segments**.  
   The same reason as point 2.

## Consequences

### Performance considerations.

Supporting JSONPath makes the masking 25% slower according to the latest benchmarks.  
We decided to disable JSONPath in case no JSONPath keys are supplied.

### Potential issues

Mixing keys and JSONPath keys in the same trie opens up some (highly unlikely) issues:

1. https://github.com/Breus/json-masker/issues/94