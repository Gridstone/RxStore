Change Log
==========
Version 6.0.1 *(2019-05-10)*
----------------------------
* Update to Gradle 5.0.
* Bump dependency versions.
* Add instructions for using library with RxJava1 to README.

Version 6.0.0 *(2017-04-19)*
----------------------------
* Migrate to RxJava2. (Thanks @Altoyyr)
  - `ValueStore.observe()` now returns `Observable<ValueUpdate<T>>`, wrapping the fact that the store may contain `null`.
  - `ValueStore.get()` now returns a `Maybe`, as there may not be a value in the store.
* All non `observe***()` methods now have a variant that takes a `Scheduler`, allowing for fire and forget calls but still controlling `Scheduler` execution.
* Remove `StoreProvider`. `ListStore` and `ValueStore` are now created by calling `RxStore.list()` and `RxStore.value()`.
* Remove `StoreProvider.Builder` and enforcement of a single `Scheduler` for all store interactions.
* Remove `delete()` method from stores, as it was dumb and `clear()` is sufficient.
* Replace Jetbrains' nullability annotations with RxJava2's inbuilt annotations.
* Converters now write to temporary files to prevent corruption. (Thanks @corcoran)

Version 5.1.1 *(2017-02-06)*
----------------------------
* Fix `observeRemoveFromList()` missing `onSuccess` call.

Version 5.1.0 *(2017-01-16)*
----------------------------
* Add `removeFromList()` variant that takes a predicate function.
* Add `addOrReplace()` method.
* Fix `observeReplace` not producing an item.

Version 5.0.5 *(2017-01-13)*
----------------------------
* Fix write lock being unlocked when it shouldn't.

Version 5.0.4 *(2016-12-21)*
----------------------------
* Fix `observeAddToList()` and `observeRemoveFromList()` not actually emitting modified lists.

Version 5.0.3 *(2016-09-14)*
----------------------------
* Fix potential race conditions in `ListStore's` utility methods.

Version 5.0.2 *(2016-05-20)*
----------------------------
* Fix Android compatibility by switching to jetbrains annotations to `annotations-java5`.

Version 5.0.1 *(2016-05-19)*
----------------------------
* Fix artifacts being built against Java 8 instead of 1.6.

Version 5.0.0 *(2016-05-17)*
----------------------------
* Replace `RxStore` with `StoreProvider`
* Stores have convenience fire-and-forget methods
* Stores can be observed
* Converters now more flexible
* Add `MoshiConverter`

Version 4.0.0 *(2015-09-22)*
----------------------------
* Project renamed from G-Rex to RxStore
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
