# Changelog

## [1.5.0](https://github.com/cardano-foundation/cf-token-metadata-registry/compare/v1.4.4...v1.5.0) (2026-04-09)


### Features

* add Kubernetes liveness and readiness probes with Yaci Store health integration ([#58](https://github.com/cardano-foundation/cf-token-metadata-registry/issues/58)) ([46521d4](https://github.com/cardano-foundation/cf-token-metadata-registry/commit/46521d499ff7588a80a8a0fbc1f8a33cde548576))
* add startup probe, onchain sync progress health check, and separate health group concerns ([#63](https://github.com/cardano-foundation/cf-token-metadata-registry/issues/63)) ([43e1374](https://github.com/cardano-foundation/cf-token-metadata-registry/commit/43e137462e3b651993239f99fe19fc9c2c249d7c))
* add yaci-store admin UI dependency and configuration ([#66](https://github.com/cardano-foundation/cf-token-metadata-registry/issues/66)) ([3acdacb](https://github.com/cardano-foundation/cf-token-metadata-registry/commit/3acdacbf57de259a7326d3d7d1781dd974c2359b))


### Bug Fixes

* add mainClass and process-aot to native profile for GraalVM build ([17b5a27](https://github.com/cardano-foundation/cf-token-metadata-registry/commit/17b5a2742e56a7e4def8ee334f0fe2f078b8baaf))
* liveness cannot monitor sync progress ([d2cacac](https://github.com/cardano-foundation/cf-token-metadata-registry/commit/d2cacac1ecb04019d5bd18c561d99d199c11ee0d))
* pin third-party GitHub Actions to full SHA and add --ignore-scripts ([552cb9e](https://github.com/cardano-foundation/cf-token-metadata-registry/commit/552cb9e385d10422a756e96f63c59dd001adb122))
* remove literal quotes from admin UI header text ([494d716](https://github.com/cardano-foundation/cf-token-metadata-registry/commit/494d71669f0f323aaad801b8c530d60b00eb5efa))
* remove unused method parameters in MetadataCliApplication ([8ea757d](https://github.com/cardano-foundation/cf-token-metadata-registry/commit/8ea757d542e9fafcf488428079b3e94918f42e10))
* replace @ConditionalOnProperty with runtime check for native image compatibility ([5676ece](https://github.com/cardano-foundation/cf-token-metadata-registry/commit/5676ecead880b0f30c3a045a571e0069f2a59ff6))
* resolve 29 SonarCloud code smells ([dc41f4b](https://github.com/cardano-foundation/cf-token-metadata-registry/commit/dc41f4bd2acbb8cda083a528219ce30f867cb43e))
* resolve GraalVM native image DNS failure in Docker and improve CI reliability ([842eab0](https://github.com/cardano-foundation/cf-token-metadata-registry/commit/842eab05c110b854cde386cb114ba757ef73a7cf))
* resolve read-only mode startup crash and Dockerfile build failure ([#72](https://github.com/cardano-foundation/cf-token-metadata-registry/issues/72)) ([bb3888e](https://github.com/cardano-foundation/cf-token-metadata-registry/commit/bb3888ec3e66dca784398677a261f465fc28e117))
* resolve read-only mode startup crash by disabling admin-ui ([#73](https://github.com/cardano-foundation/cf-token-metadata-registry/issues/73)) ([62737f9](https://github.com/cardano-foundation/cf-token-metadata-registry/commit/62737f95cf2ab716cdd05ef760d1d8341319f514))
* resolve remaining Sonar blockers and minor issues ([b937446](https://github.com/cardano-foundation/cf-token-metadata-registry/commit/b937446a2a5e337d256b58fe450ca8814e4b53bf))
* resolve remaining SonarCloud issues on develop branch ([413e3cc](https://github.com/cardano-foundation/cf-token-metadata-registry/commit/413e3cc4f198ddd830e3ef902738b21ab9b8ed66))
* resolve Sonar code smells and improve test coverage ([3056b1d](https://github.com/cardano-foundation/cf-token-metadata-registry/commit/3056b1d39e63ce611ec6f4ccbc9f4dcd55cb5ac3))
* resolve SonarCloud issues across codebase ([0ecb998](https://github.com/cardano-foundation/cf-token-metadata-registry/commit/0ecb998913759ee82f6ab894ae958ace023a7e26))
* split onchain health into separate liveness and readiness indicators ([abd4df4](https://github.com/cardano-foundation/cf-token-metadata-registry/commit/abd4df4dcefc07cbfeb30cadf5fa585a0b4007bd))
* update integration tests for renamed health indicators ([a75abaa](https://github.com/cardano-foundation/cf-token-metadata-registry/commit/a75abaa15cf883c1ec46aa6c02735dde24cd6c0c))
* use CIP_26 priority in V2 CIP-26 test and update fixtures ([40b8490](https://github.com/cardano-foundation/cf-token-metadata-registry/commit/40b84907215bc59167e4239f379693b14645af40))
* use recursive TreeWalk for root commit in batch mapping resolution ([1cbd787](https://github.com/cardano-foundation/cf-token-metadata-registry/commit/1cbd7878bcc1d4477ce1dee1bfc384769d6f4566))
* use regex patterns for Flyway resource hints in native image ([a61fd98](https://github.com/cardano-foundation/cf-token-metadata-registry/commit/a61fd98c2facfc62e3664184788d908d48ea2ab0))
* use relative imports for conftest in e2e tests ([056d8a8](https://github.com/cardano-foundation/cf-token-metadata-registry/commit/056d8a88ac9f6bbf9d1c8bba808ab4c79bd90bf5))
* use relative imports for conftest in e2e tests ([97c55d2](https://github.com/cardano-foundation/cf-token-metadata-registry/commit/97c55d2a33a4ed04bf22bd4933216b6f8b0598fe))
* V2 API required properties validation and AssetType crash on short units ([#62](https://github.com/cardano-foundation/cf-token-metadata-registry/issues/62)) ([c481778](https://github.com/cardano-foundation/cf-token-metadata-registry/commit/c4817789d7d88518bd5e935644feab22b7d30726))


### Performance Improvements

* add GraalVM native image optimization flags ([1dcc014](https://github.com/cardano-foundation/cf-token-metadata-registry/commit/1dcc01449a1f8c4383adc6d79635907d75021aea))
* batch JGit history resolution for CIP-26 offchain sync ([2605524](https://github.com/cardano-foundation/cf-token-metadata-registry/commit/2605524a962ddb676c9f7eb7fd0fdeeebcad3ee3))


### Documentation

* add Architecture Decision Records for key design choices ([#64](https://github.com/cardano-foundation/cf-token-metadata-registry/issues/64)) ([7a0ba7b](https://github.com/cardano-foundation/cf-token-metadata-registry/commit/7a0ba7b023647998795b248607699b4165c2c975))
* add Docker build and deployment guide to README ([23df9eb](https://github.com/cardano-foundation/cf-token-metadata-registry/commit/23df9eb1fbb69ffd8e8cdb9d0dab4550fe6f45a0))
* add operational endpoints table to README ([8daea64](https://github.com/cardano-foundation/cf-token-metadata-registry/commit/8daea64ad2e54acc231bc49a71e12baeddd397c2))
* comprehensive CLAUDE.md rewrite with CIP-68, ops endpoints, and conventions ([724d200](https://github.com/cardano-foundation/cf-token-metadata-registry/commit/724d2003d9950c98c7a380289858be605a481f19))
* document API_DOCKERFILE env var for native image builds ([f8dc8db](https://github.com/cardano-foundation/cf-token-metadata-registry/commit/f8dc8db615833f8d9268fa9da675c69e482c398b))
* improve README and add Scalar API reference ([#57](https://github.com/cardano-foundation/cf-token-metadata-registry/issues/57)) ([199c557](https://github.com/cardano-foundation/cf-token-metadata-registry/commit/199c5570f74cc58c0796a217c819ea7cb8ccb1e7))
* mark JVM as production variant, native as experimental ([e9f944b](https://github.com/cardano-foundation/cf-token-metadata-registry/commit/e9f944bf951b42b9233625078a83a7daa313392a))

## [1.4.4](https://github.com/cardano-foundation/cf-token-metadata-registry/compare/v1.4.3...v1.4.4) (2026-03-19)


### Bug Fixes

* Build fail ([d06c9df](https://github.com/cardano-foundation/cf-token-metadata-registry/commit/d06c9df4b86f8380c47ba0d6dd99892e03de80a7))
* Build fail ([d06c9df](https://github.com/cardano-foundation/cf-token-metadata-registry/commit/d06c9df4b86f8380c47ba0d6dd99892e03de80a7))

## [1.4.3](https://github.com/cardano-foundation/cf-token-metadata-registry/compare/v1.4.2...v1.4.3) (2026-01-21)


### Bug Fixes

* return only the latest injested token metadata (CIP-68)) ([c14a1fb](https://github.com/cardano-foundation/cf-token-metadata-registry/commit/c14a1fb23a1b7d0aa3f091ea2f30e5cc1daf3972))

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
