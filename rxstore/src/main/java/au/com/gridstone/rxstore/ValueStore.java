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

public interface ValueStore<T> {
  @NonNull Maybe<T> get();

  @Nullable T blockingGet();

  @NonNull Single<T> observePut(@NonNull final T value);

  void put(@NonNull T value);

  void put(@NonNull T value, @NonNull Scheduler scheduler);

  @NonNull Observable<ValueUpdate<T>> observe();

  @NonNull Completable observeClear();

  void clear();

  void clear(@NonNull Scheduler scheduler);

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
    public static <T> ValueUpdate<T> empty() {
      return (ValueUpdate<T>) EMPTY;
    }
  }
}
