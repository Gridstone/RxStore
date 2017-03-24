RxStore
=====

A tiny library that assists in saving and restoring objects to and from disk using [RxJava2](https://github.com/ReactiveX/RxJava), and observing changes over time.

Details
-------

RxStore is a simple persistence framework for those already making use of RxJava in their projects. There are many occasions where you don't need the complexity introduced by a database; you just require a simple put/get API.

We have found this particularly useful on Android, where there are [many options](http://developer.android.com/guide/topics/data/data-storage.html), but none of them quite right...

* Simple key/value pair? [SharedPreferences](http://developer.android.com/reference/android/content/SharedPreferences.html) makes that simple.
* Elaborate interconnected data sets? [SQLite](http://developer.android.com/reference/android/database/sqlite/SQLiteOpenHelper.html) can help you.
* Everything else? We just want to put and get objects from disk with minimal overhead.

By design, RxStore lets you use whatever serialization format you prefer, so long as you provide a valid [`Converter`](https://github.com/Gridstone/RxStore/blob/master/rxstore/src/main/java/au/com/gridstone/rxstore/Converter.java). Converters for [Moshi](https://github.com/square/moshi), [Gson](https://code.google.com/p/google-gson/) and [Jackson](https://github.com/ReactiveX/RxAndroid) are provided out of the box, and pull requests for more are always welcome!

Leaning on RxJava, RxStore can help alleviate some threading concerns when reading and writing to disk, and allows for some pretty nifty method chaining once the operation completes. It also lets you observe changes as you write new values into stores.

Usage
-----

####Setting up stores

In RxStore, there are two kinds of stores: `ValueStores` and `ListStores`. A `ValueStore` lets you write, read, and observe changes to a single value you want to persist. A `ListStore` does the same but for many values, and has convenience methods for adding and removing individual items in the list.

Stores represent a `File` on disk where your data will be persisted. To get a store, you must first create a `StoreProvider`. A `StoreProvider` represents a directory on disk that can house many store files. You can create an instance using the builder:

```java
StoreProvider storeProvider = StoreProvider.with(directory).using(new MoshiConverter());
```

Or if you're on Android and you would like a convenient way to initialise a `StoreProvider` in a subdirectory of your app's private directory, you can use:

```java
StoreProvider storeProvider = StoreProvider.withContext(context).inDir("mySubDir").using(new MoshiConverter());
```

Both builders also allow you to specify a `Scheduler` you would like async read/writes to occur on. By default they will create a new `Scheduler` running on a single thread to ensure that events are delivered in chronological sequence.

####Storing data

RxStore supports simple put/get operations for individual objects as well as append/remove operations for `Lists` of objects. All objects/lists are stored against a String key. That key is used to create a file in your previously specified directory.

Say we have a model class called `Person`
```java
public final class Person {
	public final String name;
	public final int age;
}
```

To persist a single `Person`, we must first create `ValueStore`.

```java
ValueStore<Person> store = storeProvider.valueStore("person", Person.class);
```

There are two ways we can add a `Person` to our store: `store.put(person)` or `store.observePut(person)`. `put()` is a fire-and-forget method that will asynchronously write the value to disk. `observePut()` returns an RxJava `Single` that must be subscribed to in order for the write operation to begin. This is useful when incorporating the write operation into a chain, or would like to know when a write operation has completed.

Perhaps you're making use of Square's [Retrofit](http://square.github.io/retrofit/). You could download and persist data in a single rx chain.

```java
webServices.getPerson()
    .flatMap((person) -> store.observePut("person", person).toObservable())
    .subscribe((person) -> {
      // Do something with newly downloaded and persisted person.
    });
```

`ListStore` also has some handy methods such as `addToList(person)` or `removeFromList(person)` if you wanted to store a collection of people.

####Retrieving data

When retrieving a value, we can use `store.get()` or `store.getBlocking()`. The former returning a `Single`, the latter blocking until the disk read and deserialisation is complete.

It's worth noting that a `ValueStore` that has been cleared or not yet given a value will return `null` on `getBlocking()` and `null` for the value delivered in the `Single` from `get()`.

A `ListStore` that has been cleared or not yet given a value will return an immutable empty `List`.

####Observing data

Another handy trick is to observe a store change over time. Calling `store.asObservable()` will give you an rx `Observable`. This `Observable` will immediately deliver deliver updated values if changes occur in `onNext()`. `onCompleted()` will only be called if the store is deleted.

Kotlin
------

If you're working in Kotlin, there are also two convenience extension functions provided in the `rxstore-kotlin` artifact that make use of reified type parameters. This removes the need to pass the `Type` in the store initialisation methods.

```kotlin
val personStore = storeProvider.valueStore<Person>("singlePerson")
val peopleStore = storeProvider.listStore<Person>("manyPeople")
```

Download
--------

All artifacts are up on Maven Central.

For the base library
```groovy
compile 'au.com.gridstone.rxstore:rxstore:5.1.1'
```
For the kotlin convenience functions
```groovy
compile 'au.com.gridstone.rxstore:rxstore-kotlin:5.1.1'
```
For the Moshi converter
```groovy
compile 'au.com.gridstone.rxstore:converter-moshi:5.1.1'
```
For the Gson converter
```groovy
compile 'au.com.gridstone.rxstore:converter-gson:5.1.1'
```
For the Jackson converter
```groovy
compile 'au.com.gridstone.rxstore:converter-jackson:5.1.1'
```

License
--------

    Copyright 2016 GRIDSTONE

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
