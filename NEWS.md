## 1.12.0 2026-07-23
### Breaking changes
* Description ([ISSUE](https://folio-org.atlassian.net/browse/ISSUE))

### Features
* Description ([ISSUE](https://folio-org.atlassian.net/browse/ISSUE))

### Bug fixes
* Description ([ISSUE](https://folio-org.atlassian.net/browse/ISSUE))

### Tech Dept
* Migrate tests from JUnit 4 to JUnit 6 and replace `vertx-unit` with `vertx-junit5`
* Add `maven-checkstyle-plugin` with `folio-java-checkstyle` rules and fix resulting violations

### Dependencies
* Bump `vertx-core` from `5.0.5` to `5.0.10`
* Bump `liquibase-core` from `5.0.1` to `5.0.3`
* Bump `postgresql` from `42.7.8` to `42.7.13`
* Bump `junit` from `4.13.2` to `junit-jupiter 6.1.2`
* Bump `maven-enforcer-plugin` from `3.6.2` to `3.6.3`
* Bump `maven-compiler-plugin` from `3.14.1` to `3.15.0`
* Bump `maven-surefire-plugin` from `3.5.4` to `3.5.6`
* Bump `maven-source-plugin` from `3.3.1` to `3.4.0`
* Bump `maven-release-plugin` from `3.2.0` to `3.3.1`
* Add `maven-checkstyle-plugin 3.6.0`
* Add `folio-java-checkstyle 1.2.0`
* Add `checkstyle 13.7.0`
* Add `junit-platform-launcher 6.1.2`
* Remove `maven-shade-plugin`
* Remove `vertx-unit`

## 1.11.0 2026-04-10
* [LIQUTIL-49](https://folio-org.atlassian.net/browse/LIQUTIL-49) UUpgrade to Vert.x 5.0

## 1.10.0 2025-03-07
* [LIQUTIL-45](https://folio-org.atlassian.net/browse/LIQUTIL-45) Update to liquibase-util Java 21

## 1.9.0 2024-10-28
* [LIQUTIL-43](https://folio-org.atlassian.net/browse/LIQUTIL-43) folio-liquibase-util Ramsons 2024 R2 - RMB v35.3.x update

## 1.8.0 2024-03-19
* [LIQUTIL-41](https://issues.folio.org/browse/LIQUTIL-41) Upgrade RMB to v35.2.0

## 1.7.0 2023-10-11
* [LIQUTIL-37](https://issues.folio.org/browse/LIQUTIL-37) Upgrade folio-liquibase-util to Java 17

## 1.6.0 2023-02-06
* [MODDATAIMP-750](https://issues.folio.org/browse/MODDATAIMP-750) Update util's dependencies
* [MODDATAIMP-736](https://issues.folio.org/browse/MODDATAIMP-736) Adjust logging configuration to display datetime in a proper format

## 1.5.3 2022-10-19
* [LIQUTIL-32](https://issues.folio.org/browse/LIQUTIL-32) commons-text 1.10.0 fixing Arbitrary Code Execution
* [LIQUTIL-33](https://issues.folio.org/browse/LIQUTIL-33) Upgrade RMB 35.0.1, Vert.x 4.3.4

## 1.5.2 2022-09-30
* [LIQUTIL-30](https://issues.folio.org/browse/LIQUTIL-30) snakeyaml 1.33 fixing DoS CVE-2022-38752
* [LIQUTIL-28](https://issues.folio.org/browse/LIQUTIL-28) Liquibase 4.16.1 fixing snakeyaml vulnerabilities
* [LIQUTIL-25](https://issues.folio.org/browse/LIQUTIL-25) Set liquibase schema name explicitly

## 1.5.1 2022-08-25
* [LIQUTIL-26](https://issues.folio.org/browse/LIQUTIL-26) Vert.x 4.3.3, PostgreSQL 42.5.0 fixing vulns; bump RMB, Liquibase, JUnit

## 1.5.0 2022-06-14
* [LIQUTIL-23](https://issues.folio.org/browse/LIQUTIL-23) Upgrade to RAML Module Builder 34.0.0

## 1.4.0 2022-02-22
* [LIQUTIL-17](https://issues.folio.org/browse/LIQUTIL-17) Pass exceptions, don't catch and swallow them

## 1.3.0 2022-02-01
* [LIQUTIL-11](https://issues.folio.org/browse/LIQUTIL-11) Upgrade to RAML Module Builder 33.2.3
* Update liquibase to v4.7.1

## 1.2.0 2021-02-11
* [LIQUTIL-9](https://issues.folio.org/browse/LIQUTIL-9) Upgrade to RAML Module Builder 32.x
* [LIQUTIL-8](https://issues.folio.org/browse/LIQUTIL-8) Add personal data disclosure form

## 1.1.0 2020-09-28
* Upgrade to RAML Module Builder 31.0.2

## 1.0.0 2020-06-05
 * [LIQUTIL-3](https://issues.folio.org/browse/LIQUTIL-3) Initial setup
 * Add LiquibaseUtil and SingleConnectionProvider
 * Add integration tests for LiquibaseUtil using embedded PostgreSQL
 * [LIQUTIL-4](https://issues.folio.org/browse/LIQUTIL-4) Upgrade to RAML Module Builder 30.0.2
