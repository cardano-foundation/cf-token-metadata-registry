---
title: Merging the standards
description: How the registry combines CIP-26 and CIP-68 metadata into one answer.
---

A token can have metadata in both CIP-26 and CIP-68. The registry returns **one** `metadata` block, built property by
property:

1. Standards are tried in priority order, **CIP-68 first, then CIP-26** by default.
2. Each property is taken from the first standard that has it; the others fill the gaps.
3. Every property says where it came from: `"source": "CIP_68"` or `"source": "CIP_26"`.

Change the order for one request with `query_priority`:

```bash
curl "$REGISTRY/api/v2/subjects/$SUBJECT?query_priority=CIP_26&query_priority=CIP_68"
```

To see each standard's own metadata as well, add `show_cips_details=true`: the response then has `standards.cip26` and
`standards.cip68`, each with its own `validation_errors`.

The `validation_errors` on the subject are the errors of every standard that has the token, in priority order.

## Programmable tokens

[CIP-113](../../guides/cip113/) does not take part in the merge: it carries no name or logo. When a token's policy is
registered, the response gets `"type": "PROGRAMMABLE"` and `extensions.cip113`, next to the merged metadata.
