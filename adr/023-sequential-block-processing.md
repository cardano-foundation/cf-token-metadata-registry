# ADR-023: Blocks Are Indexed One at a Time, in Chain Order

## Status

Accepted. Records how 2.0.0 already works; no code changes.

## Date

2026-11-04

## Context

The embedded yaci-store (ADR-007) can process blocks in two ways, set by `store.executor.enable-parallel-processing`:

- **Off (yaci-store's default):** blocks are processed one at a time, in chain order. Each block's events are handled,
  then the cursor moves.
- **On:** while the sync is behind the tip, blocks are processed in batches (`store.executor.blocks-batch-size`), and
  each batch is split into partitions (`store.executor.blocks-partition-size`) that run at the same time. A later block
  can be indexed before an earlier block of the same batch. At the tip, blocks are processed one at a time either way.

The CIP-68 listener (`Cip68EventListener.indexDatum`) makes two decisions that depend on what earlier blocks stored. A
datum is skipped unless its reference NFT is already stored (`existsByPolicyIdAndAssetName`), when:

- its transaction has no user token and the datum has none of the CIP-68 token fields: a dApp's own state under the
  `000643b0` prefix, or an empty map (#117);
- its transaction has the 222 NFT and no 333 or 444 token (#121).

Both rules exist so that once a reference NFT is indexed, every later datum of it is: an owner who clears the metadata
is not served the old one. They are only correct if every earlier block has been indexed when the check runs.

yaci-store's `assets-ext` module implements the same rules and runs with parallel processing on, as in the
`config/application.properties` shipped with yaci-store. There, the check depended on which partition finished first,
in both directions: an update was missed, or an NFT's datum was stored. It was found on mainnet by comparing it with
this registry (House of Titans #1001, #3377 and #4102: their metadata was cleared about 20 blocks after the mint;
`assets-ext` still served the old metadata, this registry served the cleared one). `assets-ext` fixed it by keeping
the CIP-68 outputs of a batch and indexing them in chain order on yaci-store's `CommitEvent`, at the end of the batch
(bloxbean/yaci-store#1310, `assets-ext` ADR 0010).

## Decision

**The registry keeps parallel processing off.** It does not set `store.executor.enable-parallel-processing`, so
yaci-store's default (`false`) applies: neither `application.properties` nor the shipped `.env` files set it.

Because blocks arrive in chain order, the listeners index each block as its `AddressUtxoEvent` arrives
(`Cip68EventListener.processTransaction`, `Cip113EventListener.processTransaction`). They need no end-of-batch step, so
they do not listen to yaci-store's `PreCommitEvent` or `CommitEvent`.

CIP-113 does not depend on the order: registry nodes are only saved, and deleted on rollback, with no lookup of earlier
rows.

## Consequences

### Positive

- **The two CIP-68 skips are exact.** When a block is indexed, every earlier block has been.
- **Simple listeners.** No state is kept between blocks, and nothing is held in memory until the end of a batch.
- **Same rows at the tip and during a sync from scratch.**

### Negative

- **A sync from scratch is slower** than it could be with parallel processing. On mainnet, from slot 65,836,843 to the
  tip, it takes about 9 hours (measured 2026-11-03, on a shared host with a hard disk).

### Warning

**Do not turn on `store.executor.enable-parallel-processing` on its own.** The sync would still run and the API would
answer, but the two CIP-68 skips would depend on timing, and the rows would differ from run to run. Turning it on needs
the CIP-68 listener to keep the outputs of a batch and index them in chain order on `CommitEvent`, as `assets-ext` does
(ADR 0010 there). yaci-store publishes `CommitEvent` once per batch, after every partition has finished, and before the
cursor moves. `PreCommitEvent`, published just before it, is internal to yaci-store, used to prepare data for its own
calculations (adapot), so `CommitEvent` is the one to use.

## Alternatives Considered

- **Parallel processing with indexing in chain order on `CommitEvent`** (what `assets-ext` does). It gives the same rows
  and a faster sync from scratch, but adds batch state, rollback handling for it, and a second code path to test. Not
  needed while a sync of about 9 hours is acceptable.
- **Parallel processing as it is.** Rejected: the CIP-68 skips would be wrong, as seen in `assets-ext`.

## Corrections

ADR-019 and the javadoc of `Cip68EventListener` said that yaci-store processes the blocks of a batch in parallel during
sync, as a reason why only the transaction can be relied on. That reasoning came from `assets-ext`. The conclusion still
holds here: a 333 or 444 token minted in a later block is not known when the datum is indexed. ADR-019 is amended; the
javadoc is to be corrected with the next code change.

## References

- ADR-007 (yaci-store for on-chain indexing), ADR-018 and ADR-019 (what a CIP-68 datum is), #117 and #121 (the two skips)
- `Cip68EventListener.indexDatum`, `Cip68EventListener.processTransaction`, `Cip113EventListener.processTransaction`
- yaci-store: `StoreProperties.enableParallelProcessing`, `ShelleyBlockEventPublisher.processBlocksInParallel`
- bloxbean/yaci-store#1310 and `extensions/assets-ext/docs/adr/0010-cip-68-outputs-of-a-batch-are-indexed-in-chain-order.md`
