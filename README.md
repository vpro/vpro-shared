[![Build Status](https://travis-ci.org/vpro/vpro-shared.svg?)](https://travis-ci.org/vpro/vpro-shared)
[![Maven Central](https://img.shields.io/maven-central/v/nl.vpro.shared/vpro-shared-parent.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22nl.vpro.shared%22)


# VPRO Shared modules

These are various shared utility modules which are used on several
places in VPRO artifacts.

[Javadoc can be found here](https://vpro.github.io/vpro-shared/)


## highlights

- [vpro-shared-util](vpro-shared-util): Classes in `nl.vpro.util`.  Related to collections and other low level java utilities.
- [vpro-shared-logging](vpro-shared-logging): Utilities related to logging. Mostly related to SLF4j. Also contains a more simple framework 'SimpleLogger', which is easy to implement on the fly
- [vpro-shared-eleasticsearch5](vpro-shared-elasticsearch5): Related to elasticsearch5
- [vpro-shared-eleasticsearch-client](vpro-shared-elasticsearch-client): Related to elasticsearch
- [vpro-shared-couchdb-util](vpro-shared-couchdb-util): Lightweight and streaming couchdb clients
- [vpro-shared-swagger](vpro-shared-swagger): War overlay containing swagger frontend

- ..


## Builds

SNAPSHOT builds can be found at https://oss.sonatype.org/content/repositories/snapshots/nl/vpro/shared/

Release builds will be provided via [maven central](https://search.maven.org/search?q=g:nl.vpro.shared).

## Checkout

The maven build will ensure that this is checked out with git clone --recurse-submodules. It is essential for a correct build of vpro-shared-swagger.war


## TODO

- Some of these modules are old, contain very little usefull or are very specific for VPRO. We should remove them.
- A bit more of documentation here and there would be welcome. Though we provide this mainly to be able to provide _other_ artifact too, which themselves depend on this
- Some, or perhaps even most or all, of the submodules should be migrated to git repositories of their own. They don't change often. This will make the builds faster and leaner.


