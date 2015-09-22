RxStore
=====

A tiny library that assists in saving and restoring objects to and from disk using [RxJava][1].

Details
-------

RxStore is a simple persistence framework for those already making use of RxJava in their projects. There are many occasions where you don't need the complexity introduced by a database; you just require a simple put/get API.

RxStore is nothing fancy. It's probably code you written many times yourself. Its only goal is to make simple persistence code even simpler.

We have found this particularly useful on Android, where there are [many options][2], but none of them quite right...

* Simple key/value pair? [SharedPreferences][3] makes that simple.
* Elaborate and enormous data sets? [SQLite][4] can help you, if that's your kind of thing...
* Everything else? We just want to put and get objects from disk with minimal overhead.

By design, RxStore lets you use whatever serialization format you prefer, so long as you provide a valid [`Converter`][5]. Converters for [Gson][6] and [Jackson][7] are provided out of the box, and pull requests for more are always welcome!

The other advantage of RxStore is that RxJava helps alleviate some threading concerns when reading and writing to disk, and allows for some pretty nifty method chaining once the operation completes.

Usage
-----

There are two ways to initialise an RxStore. The first involves taking in a `File` that specifies the directory to write files into.
```java
RxStore myStore = RxStore.with(someDirectory).using(new GsonConverter());
```

The second is just for Android that makes the process a little smoother.
```java
RxStore myStore = RxStore.withContext(context)
    .in("someDir")
    .using(new GsonConverter());
```

The special Android method will use your app's private directory. Calling `in()` allows you to specify a subdirectory.

----

RxStore supports simple put/get operations for individual objects as well as append/remove operations for `List`s of objects. All objects/lists are stored against a String key. That key is used to create a file in your previously specified directory.

Say we have a class called `Dino`
```java
public class Dino {
	public String name;
	public int armLength;
}
```

You can use `RxStore` to persist a single instance.
```java
RxStore rxStore = RxStore.withContext(context)
    .in("dinoDir")
    .using(new GsonConverter());

Dino dino = new Dino("Gregory", 37);

rxStore.put("dino", dino)
	  .subscribeOn(Schedulers.io())
	  .observeOn(AndroidSchedulers.mainThread())
	  .subscribe((persistedDino) -> {
	    // Do something with your persisted dino.
	  });
```

If you're an [rxandroid][8] user, this code will look very familiar to you. The write operation will occur *off* the main thread, and we get informed of the completion back *on* the main thread.

You could also store a `List` of toothy friends.
```java
List<Dino> dinos = getDinoList(); //Some method that returns an ArrayList of Dinos.

rxStore.putList("dinoList", dinos, Dino.class)
	  .subscribeOn(Schedulers.io())
	  .observeOn(AndroidSchedulers.mainThead())
	  .subscribe((persistedDinos) -> {
	    // Do something with your persisted dino army.
	  });
```

Are you using RxJava in conjunction with Square's [Retrofit][9]? You could download and persist data in one go.
```java
webServices.getDino()
    .flatMap((dino) -> rxStore.put("dino", dino))
    .subscribeOn(Schedulers.io())
    .observeOn(AndroidSchedulers.mainThread())
    .subscribe((dino) -> {
      // Behold the downloaded and persisted dino
    });
```

Example
-------

Would you like to see this library in action on Android? Check out the `rxstore-android-example` directory.

![](images/example.png)

Download
--------

All artifacts are up on Maven Central.

For the base library
```groovy
compile 'au.com.gridstone.rxstore:rxstore:4.0.0'
```
For the Gson converter
```groovy
compile 'au.com.gridstone.rxstore:converter-gson:4.0.0'
```
For the Jackson converter
```groovy
compile 'au.com.gridstone.rxstore:converter-jackson:4.0.0'
```

License
--------

    Copyright 2014 GRIDSTONE

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

 [1]: https://github.com/ReactiveX/RxJava
 [2]: http://developer.android.com/guide/topics/data/data-storage.html
 [3]: http://developer.android.com/reference/android/content/SharedPreferences.html
 [4]: http://developer.android.com/reference/android/database/sqlite/SQLiteOpenHelper.html
 [5]: https://github.com/Gridstone/RxStore/blob/master/rxstore/src/main/java/au/com/gridstone/rxstore/Converter.java
 [6]: https://code.google.com/p/google-gson/
 [7]: http://jackson.codehaus.org/
 [8]: https://github.com/ReactiveX/RxAndroid
 [9]: http://square.github.io/retrofit/
