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

package au.com.gridstone.rxstore;

import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.annotations.NonNull;
import java.util.List;

public interface ListStore<T> {
  @NonNull Single<List<T>> get();

  @NonNull List<T> blockingGet();

  @NonNull Single<List<T>> observePut(@NonNull final List<T> list);

  void put(@NonNull List<T> list);

  void put(@NonNull List<T> list, @NonNull Scheduler scheduler);

  @NonNull Observable<List<T>> observe();

  @NonNull Single<List<T>> observeClear();

  void clear();

  void clear(@NonNull Scheduler scheduler);

  @NonNull Single<List<T>> observeAdd(@NonNull final T value);

  void add(@NonNull T value);

  void add(@NonNull T value, @NonNull Scheduler scheduler);

  @NonNull Single<List<T>> observeRemove(@NonNull final PredicateFunc<T> predicateFunc);

  void remove(@NonNull PredicateFunc<T> predicateFunc);

  void remove(@NonNull Scheduler scheduler, @NonNull PredicateFunc<T> predicateFunc);

  @NonNull Single<List<T>> observeRemove(@NonNull final T value);

  void remove(@NonNull final T value);

  void remove(@NonNull final T value, @NonNull Scheduler scheduler);

  @NonNull Single<List<T>> observeRemove(final int position);

  void remove(int position);

  void remove(int position, @NonNull Scheduler scheduler);

  @NonNull Single<List<T>> observeReplace(@NonNull final T value,
      @NonNull final PredicateFunc<T> predicateFunc);

  void replace(@NonNull T value, @NonNull PredicateFunc<T> predicateFunc);

  void replace(@NonNull T value, @NonNull Scheduler scheduler,
      @NonNull PredicateFunc<T> predicateFunc);

  @NonNull Single<List<T>> observeAddOrReplace(@NonNull final T value,
      @NonNull final PredicateFunc<T> predicateFunc);

  void addOrReplace(@NonNull T value, @NonNull PredicateFunc<T> predicateFunc);

  void addOrReplace(@NonNull T value, @NonNull Scheduler scheduler,
      @NonNull PredicateFunc<T> predicateFunc);

  interface PredicateFunc<T> {
    boolean test(@NonNull T value);
  }
}
