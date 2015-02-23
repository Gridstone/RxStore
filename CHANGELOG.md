Change Log
==========

Version 2.0.0 *(2015-02-23)*
----------------------------
* API break: `GRexPersister.get()` and `getList()` now return empty `Observables` if nothing is found at the key

Version 1.1.0 *(2014-12-05)*
----------------------------
* Support for `Converter`, allowing for custom serialization formats
* New grex-gson-converter artifact
* New grex-jackson-converter artifact
* API break: `GRexPersister` now must have a `Converter` provided in its constructor