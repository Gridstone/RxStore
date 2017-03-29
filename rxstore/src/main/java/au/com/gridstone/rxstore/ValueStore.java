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
import io.reactivex.CompletableEmitter;
import io.reactivex.CompletableOnSubscribe;
import io.reactivex.Maybe;
import io.reactivex.MaybeEmitter;
import io.reactivex.MaybeOnSubscribe;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static au.com.gridstone.rxstore.Locks.runInReadLock;
import static au.com.gridstone.rxstore.Locks.runInWriteLock;
import static au.com.gridstone.rxstore.Preconditions.assertNotNull;

public final class ValueStore<T> {
  private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
  private final PublishSubject<ValueUpdate<T>> updateSubject = PublishSubject.create();

  private final File file;
  private final Converter converter;
  private final Type type;

  public ValueStore(@NonNull File file, @NonNull Converter converter, @NonNull Type type) {
    assertNotNull(file, "file");
    assertNotNull(converter, "converter");
    assertNotNull(type, "type");
    this.file = file;
    this.converter = converter;
    this.type = type;
  }

  @NonNull public Maybe<T> get() {
    return Maybe.create(new MaybeOnSubscribe<T>() {
      @Override public void subscribe(final MaybeEmitter<T> emitter) throws Exception {
        runInReadLock(readWriteLock, new ThrowingRunnable() {
          @Override public void run() throws Exception {
            if (!file.exists()) {
              emitter.onComplete();
              return;
            }

            T value = converter.read(file, type);
            if (value == null) emitter.onComplete();
            emitter.onSuccess(value);
          }
        });
      }
    });
  }

  @Nullable public T blockingGet() {
    return get().blockingGet();
  }

  @NonNull public Single<T> observePut(@NonNull final T value) {
    assertNotNull(value, "value");

    return Single.create(new SingleOnSubscribe<T>() {
      @Override public void subscribe(final SingleEmitter<T> emitter) throws Exception {
        runInWriteLock(readWriteLock, new ThrowingRunnable() {
          @Override public void run() throws Exception {
            if (!file.exists() && !file.createNewFile()) {
              emitter.onError(new IOException("Could not create file for store."));
            }

            converter.write(value, type, file);
            emitter.onSuccess(value);
            updateSubject.onNext(new ValueUpdate<T>(value));
          }
        });
      }
    });
  }

  public void put(@NonNull T value) {
    put(value, Schedulers.io());
  }

  public void put(@NonNull T value, @NonNull Scheduler scheduler) {
    assertNotNull(scheduler, "scheduler");
    observePut(value).subscribeOn(scheduler).subscribe();
  }

  @NonNull public Observable<ValueUpdate<T>> observe() {
    Observable<ValueUpdate<T>> startingValue = get()
        .map(new Function<T, ValueUpdate<T>>() {
          @Override public ValueUpdate<T> apply(T value) throws Exception {
            return new ValueUpdate<T>(value);
          }
        })
        .defaultIfEmpty(ValueUpdate.<T>empty())
        .toObservable();

    return updateSubject.startWith(startingValue);
  }

  @NonNull public Completable observeClear() {
    return Completable.create(new CompletableOnSubscribe() {
      @Override public void subscribe(final CompletableEmitter emitter) throws Exception {
        runInWriteLock(readWriteLock, new ThrowingRunnable() {
          @Override public void run() throws Exception {
            if (file.exists() && !file.delete()) {
              emitter.onError(new IOException("Clear operation on store failed."));
            } else {
              emitter.onComplete();
            }

            updateSubject.onNext(ValueUpdate.<T>empty());
          }
        });
      }
    });
  }

  public void clear() {
    clear(Schedulers.io());
  }

  public void clear(@NonNull Scheduler scheduler) {
    assertNotNull(scheduler, "scheduler");
    observeClear().subscribeOn(scheduler).subscribe();
  }

  public static final class ValueUpdate<T> {
    @Nullable public final T value;
    public final boolean empty;

    ValueUpdate(@Nullable T value) {
      this.value = value;
      this.empty = value == null;
    }

    @Override public boolean equals(Object obj) {
      if (this == obj) return true;
      if (!(obj instanceof ValueStore.ValueUpdate)) return false;

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
