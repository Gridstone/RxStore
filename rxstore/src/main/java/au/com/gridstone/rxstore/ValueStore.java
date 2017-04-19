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

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;
import io.reactivex.schedulers.Schedulers;

/**
 * Store a single object on disk.
 */
public interface ValueStore<T> {
  /**
   * Retrieve the current value from this store using Rx. If this store has not had a value written
   * then the returned {@link Maybe} completes without a value.
   */
  @NonNull Maybe<T> get();

  /**
   * Retrieve the current value from this store in a blocking manner. This may take time. If the
   * store has not yet had a value written then this method returns null.
   */
  @Nullable T blockingGet();

  /**
   * Write an object to this store and observe the operation. The value returned in the {@link
   * Single} is the value written to this store, making it useful for chaining.
   */
  @NonNull Single<T> observePut(@NonNull final T value);

  /**
   * Asynchronously write a value to this store. The write operation occurs on {@link
   * Schedulers#io()}. If you wish to specify the {@link Scheduler} then use {@link #put(Object,
   * Scheduler)}.
   */
  void put(@NonNull T value);

  /**
   * Write a value to this store on a specified {@link Scheduler}.
   */
  void put(@NonNull T value, @NonNull Scheduler scheduler);

  /**
   * Observe changes to the value in this store. {@code onNext(valueUpdate)} will be invoked
   * immediately with the current value upon subscription and subsequent changes thereafter.
   * <p>
   * Values are wrapped inside of {@link ValueUpdate ValueUpdate} objects, as it's possible for a
   * store to hold no current value. If that's the case {@link ValueUpdate#empty update.empty} will
   * be true and {@link ValueUpdate#value update.value} will be null.
   */
  @NonNull Observable<ValueUpdate<T>> observe();

  /**
   * Clear the value in this store and observe the operation. (Useful for chaining).
   */
  @NonNull Completable observeClear();

  /**
   * Asynchronously clear the value in this store. The clear operation occurs on {@link
   * Schedulers#io()}. If you wish to specify the {@link Scheduler} then use {@link
   * #clear(Scheduler)}.
   */
  void clear();

  /**
   * Clear the value from this store on a specified {@link Scheduler}.
   */
  void clear(@NonNull Scheduler scheduler);

  /**
   * Wraps the current value in a {@link ValueStore}. This is useful as {@link ValueStore#observe()}
   * is unable to deliver null objects in {@code onNext()} to represent an empty state. To that end,
   * you may query {@link ValueUpdate#empty update.empty} to determine if the current value is empty
   * and whether or not {@link ValueUpdate#value update.value} is null.
   */
  final class ValueUpdate<T> {
    @Nullable public final T value;
    public final boolean empty;

    ValueUpdate(@Nullable T value) {
      this.value = value;
      this.empty = value == null;
    }

    @Override public boolean equals(Object obj) {
      if (this == obj) return true;
      if (!(obj instanceof ValueUpdate)) return false;

      ValueUpdate other = (ValueUpdate) obj;
      if (this.empty) return other.empty;
      return this.value.equals(other.value);
    }

    @Override public int hashCode() {
      int valueHash = value == null ? 0 : value.hashCode();
      int emptyHash = empty ? 1 : 0;
      return 37 * (valueHash + emptyHash);
    }

    @SuppressWarnings("unchecked") // Empty instance needs no type as value is always null.
    private static final ValueUpdate EMPTY = new ValueUpdate(null);

    @SuppressWarnings("unchecked")
    static <T> ValueUpdate<T> empty() {
      return (ValueUpdate<T>) EMPTY;
    }
  }
}
