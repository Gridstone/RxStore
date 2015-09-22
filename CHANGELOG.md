Change Log
==========
Version 4.0.0 *(2015-22-09)*
* Project renamed to G-Rex to RxStore
* No more separate artifact for Android (it's now an optional dependency of RxStore)
* Builders instead of public constructors for `RxStore` (previously known as `GRexPersister`)

Version 3.0.0 *(2015-05-01)*
----------------------------
* G-Rex is no longer tied to Android, allowing you to unleash dino persistence on desktop Java
* API break: `grex-android` artifact has been introduced. `grex` artifact is now used for plain Java

Version 2.0.0 *(2015-02-23)*
----------------------------
* API break: `GRexPersister.get()` and `getList()` now return empty `Observables` if nothing is found at the key

Version 1.1.0 *(2014-12-05)*
----------------------------
* Support for `Converter`, allowing for custom serialization formats
* New grex-gson-converter artifact
* New grex-jackson-converter artifact
* API break: `GRexPersister` now must have a `Converter` provided in its constructor