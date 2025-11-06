# Changelog

## [1.4.2](https://github.com/cardano-foundation/cf-token-metadata-registry/compare/v1.4.1...v1.4.2) (2025-11-06)


### Bug Fixes

* updating to non alping ([c103eda](https://github.com/cardano-foundation/cf-token-metadata-registry/commit/c103eda83ea851087976e9fdc727d52dc4786457))

## [1.4.1](https://github.com/cardano-foundation/cf-token-metadata-registry/compare/v1.4.0...v1.4.1) (2025-11-06)


### Bug Fixes

* fixed docker images FROM because openjdk is now deprecated ([f707393](https://github.com/cardano-foundation/cf-token-metadata-registry/commit/f707393a355dfdf3c1bf6ea5843c7faad0b849a2))

## [1.4.0](https://github.com/cardano-foundation/cf-token-metadata-registry/compare/v1.3.1...v1.4.0) (2025-11-06)


### Features

* attempt to trigger release ([6ce9ad4](https://github.com/cardano-foundation/cf-token-metadata-registry/commit/6ce9ad467a8e67c0dcfa0d316410e91a29a869ef))

## [1.3.1](https://github.com/cardano-foundation/cf-token-metadata-registry/compare/v1.3.0...v1.3.1) (2025-10-16)


### Bug Fixes

* **env varaible value:** use internal Postgres port (5432) for service connection ([bc8d964](https://github.com/cardano-foundation/cf-token-metadata-registry/commit/bc8d964be46654b149611a5a3fc3764d5afc7c75))
* prevent invalid tokens, which are not passing validation from preprod repository from being submitted to the registry. ([d332b88](https://github.com/cardano-foundation/cf-token-metadata-registry/commit/d332b8855e235174adda7c9a830ad8fe413acf36))

## [1.3.0](https://github.com/cardano-foundation/cf-token-metadata-registry/compare/v1.2.1...v1.3.0) (2025-09-24)


### Features

* attempt to trigger release ([5bd14c3](https://github.com/cardano-foundation/cf-token-metadata-registry/commit/5bd14c31416f6d20101f90da91403218d1f75af4))

## [1.2.1](https://github.com/cardano-foundation/cf-token-metadata-registry/compare/v1.2.0...v1.2.1) (2025-09-16)


### Bug Fixes

* externalise environment variables and provide support for preprod token registry. ([#28](https://github.com/cardano-foundation/cf-token-metadata-registry/issues/28)) ([814cfcd](https://github.com/cardano-foundation/cf-token-metadata-registry/commit/814cfcdd743b87aae8f7236cca5b7874c6c09b30))

## [1.2.0](https://github.com/cardano-foundation/cf-token-metadata-registry/compare/v1.1.0...v1.2.0) (2025-06-26)


### Features

* bump minor ([208c5fe](https://github.com/cardano-foundation/cf-token-metadata-registry/commit/208c5fe8072fe6a02cb6778c2bf987bb0cb37789))

## [1.1.0](https://github.com/cardano-foundation/cf-token-metadata-registry/compare/v1.0.2...v1.1.0) (2025-06-26)


### Features

* Add CIP-68 Support for the only Fungible Tokens ([#21](https://github.com/cardano-foundation/cf-token-metadata-registry/issues/21)) ([a457c79](https://github.com/cardano-foundation/cf-token-metadata-registry/commit/a457c798c2b024d01ae56f5ea4b5861bca30f4ab))
* added Health Endpoint returning sync status. ([#16](https://github.com/cardano-foundation/cf-token-metadata-registry/issues/16)) ([c94e91e](https://github.com/cardano-foundation/cf-token-metadata-registry/commit/c94e91ec66eecda17083082afa03a246b1ff6d85))

## [1.0.2](https://github.com/cardano-foundation/cf-token-metadata-registry/compare/v1.0.1...v1.0.2) (2024-02-21)


### Bug Fixes

* quick null check for nullable property ([e48a503](https://github.com/cardano-foundation/cf-token-metadata-registry/commit/e48a50369b7328ad6281a9e65abc8843f4dcac52))

## [1.0.1](https://github.com/cardano-foundation/cf-token-metadata-registry/compare/v1.0.0...v1.0.1) (2024-02-21)


### Bug Fixes

* immediately return empty response for batch request with empty list of subjects ([#12](https://github.com/cardano-foundation/cf-token-metadata-registry/issues/12)) ([1d25b23](https://github.com/cardano-foundation/cf-token-metadata-registry/commit/1d25b238a4671ca6630361b7d3cc1e02eb8eaf6d))

## 1.0.0 (2024-02-15)


### Features

* Implemented verification function ([54d19c9](https://github.com/cardano-foundation/cf-token-metadata-registry/commit/54d19c9719265b47141f325c88c4a3d9d6deb5e5))


### Bug Fixes

* fixed spring configuration ([a53b761](https://github.com/cardano-foundation/cf-token-metadata-registry/commit/a53b761a408eea7bc49da02a1f420dcdd42f785c))
* fixed tests configuration for entity manager ([85a286c](https://github.com/cardano-foundation/cf-token-metadata-registry/commit/85a286cc7034351d929956b049ed56323c49660f))
* removed spring boot plugin for submodule common ([7b8f153](https://github.com/cardano-foundation/cf-token-metadata-registry/commit/7b8f1531dec2eaf646c0075e9a65021a0dddc34a))
