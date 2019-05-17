RxStore
=======

A tiny library that assists in saving and restoring objects to and from disk using [RxJava](https://github.com/ReactiveX/RxJava), and observing changes over time.

This library now targets RxJava2. If you're using RxJava1 then take a look at a [version 5.1.1](https://github.com/Gridstone/RxStore/tree/v5.1.1).

Details
-------

RxStore is a simple persistence framework for those already making use of RxJava2 in their projects. There are many occasions where you don't need the complexity introduced by a database; you just require a simple put/get API.

We have found this particularly useful on Android, where there are [many options](http://developer.android.com/guide/topics/data/data-storage.html), but none of them quite right...

* Simple key/value pair? [SharedPreferences](http://developer.android.com/reference/android/content/SharedPreferences.html) makes that simple.
* Elaborate interconnected data sets? [SQLite](http://developer.android.com/reference/android/database/sqlite/SQLiteOpenHelper.html) can help you.
* Everything else? We just want to put and get objects from disk with minimal overhead.

By design, RxStore lets you use whatever serialization format you prefer, so long as you provide a valid [`Converter`](https://github.com/Gridstone/RxStore/blob/master/rxstore/src/main/java/au/com/gridstone/rxstore/Converter.java). Converters for [Moshi](https://github.com/square/moshi), [Gson](https://code.google.com/p/google-gson/) and [Jackson](https://github.com/FasterXML/jackson) are provided out of the box, and pull requests for more are always welcome!

Leaning on RxJava, RxStore can help alleviate some threading concerns when reading and writing to disk, and allows for some pretty nifty method chaining once the operation completes. It also lets you observe changes as you write new values into stores.

Usage
-----

### Creating Stores

There are two kinds of stores:
 - `ValueStore` lets you write, read, and observe changes to a single value you want to persist.
 - `ListStore` does the same but for many values, and has convenience methods for adding and removing individual items in the list.

Say we have a model class called `Person`
```java
public final class Person {
  public final String name;
  public final int age;
}
```

To persist a single `Person`, we must first create a `ValueStore`.

```java
ValueStore<Person> store = RxStore.value(file, converter, Person.class);
```

In addition to the type we must also provide a `File` and a `Converter`. The `File` gives the object a place to live on disk, and the `Converter` dictates how it's saved and restored. You can make your own `Converter` or use [one we prepared earlier](https://github.com/Gridstone/RxStore/tree/master/converters).

### Storing Data

There are two ways we can add a `Person` to our store: `store.put(person)` or `store.observePut(person)`. `put()` is a fire-and-forget method that will asynchronously write the value to disk. `observePut()` returns an RxJava `Single` that must be subscribed to in order for the write operation to begin. This is useful when incorporating the write operation into a chain, or would like to know when a write operation has completed.

Perhaps you're making use of Square's [Retrofit](http://square.github.io/retrofit/). You could download and persist data in a single Rx chain.

```java
webServices.getPerson()
    .flatMap((person) -> store.observePut(person))
    .subscribe((person) -> {
      // Do something with newly downloaded and persisted person.
    });
```

`ListStore` is useful if you wanted to store many people. In addition to `put(people)` it also has some handy methods such as `add(person)` and `remove(person)`.

### Retrieving Data

When retrieving from a `ValueStore` we can use `store.get()` or `store.blockingGet()`. The former returns a `Maybe`, as there may not be a current value. The latter blocks until the disk read and deserialization is complete, and returns a nullable value.

`ListStore` behaves slightly differently. `get()` returns a `Single`, as empty stores can be represented by an immutable empty `List`. `blockingGet()` will always return a non-null `List`.


### Observing Data

Another handy trick is to observe a store change over time. Calling `store.observe()` will give you an Rx `Observable`. This `Observable` will immediately deliver the current value of the store upon subscription, and will then deliver updated values if changes occur in `onNext()`.

It's worth noting that `valueStore.observe()` does not return `Observable<T>`, but rather `Observable<ValueUpdate<T>>`. This is because the store cannot use null to represent the absence of a value, and must wrap the update in a non-null object.

`listStore.observe()` however does return `Observable<List<T>>`, as an empty `ListStore` can be represented by an immutable empty `List`.

Kotlin
------

If you're working in Kotlin, there are also two convenient functions provided in the `rxstore-kotlin` artifact that make use of reified type parameters. This removes the need to pass the `Type` in the store initialisation methods.

```kotlin
val personStore = storeProvider.valueStore<Person>(file, converter)
val peopleStore = storeProvider.listStore<Person>(file, converter)
```

Download
--------

All artifacts are up on Maven Central.

For the base library
```groovy
compile 'au.com.gridstone.rxstore:rxstore:6.0.2'
```
For the kotlin convenience functions
```groovy
compile 'au.com.gridstone.rxstore:rxstore-kotlin:6.0.2'
```
For the Moshi converter
```groovy
compile 'au.com.gridstone.rxstore:converter-moshi:6.0.2'
```
For the Gson converter
```groovy
compile 'au.com.gridstone.rxstore:converter-gson:6.0.2'
```
For the Jackson converter
```groovy
compile 'au.com.gridstone.rxstore:converter-jackson:6.0.2'
```

License
--------

    Copyright 2017 GRIDSTONE

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
