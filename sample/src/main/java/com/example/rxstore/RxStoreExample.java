/*
 * Copyright (C) GRIDSTONE 2017
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.rxstore;

import au.com.gridstone.rxstore.Converter;
import au.com.gridstone.rxstore.ListStore;
import au.com.gridstone.rxstore.RxStore;
import au.com.gridstone.rxstore.ValueStore;
import au.com.gridstone.rxstore.converters.MoshiConverter;
import io.reactivex.schedulers.Schedulers;
import java.io.File;
import java.util.Arrays;
import java.util.List;

public final class RxStoreExample {
  // Simple model class for the purpose of demonstration.
  static class Person {
    final String name;
    final int age;

    Person(String name, int age) {
      this.name = name;
      this.age = age;
    }
  }

  public static void main(String[] args) {
    // For this example we'll use Moshi to persist our model objects to disk as JSON.
    Converter converter = new MoshiConverter();
    ValueStore<Person> personStore = RxStore.value(new File("person"), converter, Person.class);
    ListStore<Person> peopleStore = RxStore.list(new File("people"), converter, Person.class);

    // We can subscribe for updates every time our personStore changes.
    personStore.observe()
        .filter(update -> !update.empty) // We might only want non-empty updates.
        .map(update -> update.value) // If updates are non-empty we can get a non-null Person.
        .subscribe(person -> {
          // Here we receive a new person every time the store updates.
        });

    // We can do the same for our ListStore of many people.
    peopleStore.observe().subscribe(people -> {
      // Now we'll be informed every time our list of people updates.
    });

    // Make some model objects to persist.
    Person sally = new Person("Sally Sample", 30);
    List<Person> manyPeople = Arrays.asList(
        new Person("Trevor Tester", 24),
        new Person("Edward Example", 61),
        new Person("Daphne Demonstration", 9)
    );

    // Here we'll write one person to disk and observe the operation.
    personStore.observePut(sally).subscribe(person -> {
      // In the onSuccess callback we now have a persisted model object to work with.
    });

    // Write many people to a ListStore. Rather than observe the operation this time we'll just fire
    // and forget. We specify the Scheduler as trampoline to keep it simple and synchronous.
    peopleStore.put(manyPeople, Schedulers.trampoline());

    // We can add one person at a time to our ListStore if we like.
    peopleStore.add(sally, Schedulers.trampoline());

    // Let's remove Edward from our ListStore and observe the operation.
    peopleStore.observeRemove(person -> person.name.contains("Edward")).subscribe(people -> {
      // Now we have a list of people without Edward.
    });

    // If we don't need our data persisted anymore we can clear the files from disk.
    personStore.clear(Schedulers.trampoline());
    peopleStore.clear(Schedulers.trampoline());
  }
}
