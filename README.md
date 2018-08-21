[![Build Status](https://travis-ci.org/vpro/vpro-shared.svg?)](https://travis-ci.org/vpro/vpro-shared)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/nl.vpro/vpro-shared/badge.svg?style=plastic)](https://maven-badges.herokuapp.com/maven-central/nl.vpro/vpro-shared)

# VPRO Shared modules



These are various shared utility modules which are used on several
places in VPRO artifacts.


## highlights

- vpro-shared-util: Classes in `nl.vpro.util`.  Related to collections and other low level java utilities.
- vpro-shared-logging: Utilities related to logging. Mostly related to SLF4j. Also contains a more simple framework 'SimpleLogger', which is easy to implement on the fly
- vpro-shared-eleasticsearch5: Related to elasticsearch


## Builds

SNAPSHOT builds can be found at https://oss.sonatype.org/content/repositories/snapshots/nl/vpro/shared/

Release builds will be provided via maven central.

## TODO

- Some of these modules are old, contain very little usefull or are very specific for VPRO. We should remove them.
- A bit more of documentation here and there would be welcome. Though we provide this mainly to be able to provide _other_ artifact too, which themselves depend on this

