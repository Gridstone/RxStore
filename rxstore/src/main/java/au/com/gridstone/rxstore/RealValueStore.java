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

import static au.com.gridstone.rxstore.Utils.converterWrite;
import static au.com.gridstone.rxstore.Utils.runInReadLock;
import static au.com.gridstone.rxstore.Utils.runInWriteLock;
import static au.com.gridstone.rxstore.Utils.assertNotNull;

final class RealValueStore<T> implements ValueStore<T> {
  private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
  private final PublishSubject<ValueUpdate<T>> updateSubject = PublishSubject.create();

  private final File file;
  private final Converter converter;
  private final Type type;

  RealValueStore(@NonNull File file, @NonNull Converter converter, @NonNull Type type) {
    assertNotNull(file, "file");
    assertNotNull(converter, "converter");
    assertNotNull(type, "type");
    this.file = file;
    this.converter = converter;
    this.type = type;
  }

  @Override @NonNull public Maybe<T> get() {
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

  @Override @Nullable public T blockingGet() {
    return get().blockingGet();
  }

  @Override @NonNull public Single<T> observePut(@NonNull final T value) {
    assertNotNull(value, "value");

    return Single.create(new SingleOnSubscribe<T>() {
      @Override public void subscribe(final SingleEmitter<T> emitter) throws Exception {
        runInWriteLock(readWriteLock, new ThrowingRunnable() {
          @Override public void run() throws Exception {
            if (!file.exists() && !file.createNewFile()) {
              throw new IOException("Could not create file for store.");
            }

            converterWrite(value, converter, type, file);
            emitter.onSuccess(value);
            updateSubject.onNext(new ValueUpdate<T>(value));
          }
        });
      }
    });
  }

  @Override public void put(@NonNull T value) {
    put(value, Schedulers.io());
  }

  @Override public void put(@NonNull T value, @NonNull Scheduler scheduler) {
    assertNotNull(scheduler, "scheduler");
    observePut(value).subscribeOn(scheduler).subscribe();
  }

  @Override @NonNull public Observable<ValueUpdate<T>> observe() {
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

  @Override @NonNull public Completable observeClear() {
    return Completable.create(new CompletableOnSubscribe() {
      @Override public void subscribe(final CompletableEmitter emitter) throws Exception {
        runInWriteLock(readWriteLock, new ThrowingRunnable() {
          @Override public void run() throws Exception {
            if (file.exists() && !file.delete()) {
              throw new IOException("Clear operation on store failed.");
            } else {
              emitter.onComplete();
            }

            updateSubject.onNext(ValueUpdate.<T>empty());
          }
        });
      }
    });
  }

  @Override public void clear() {
    clear(Schedulers.io());
  }

  @Override public void clear(@NonNull Scheduler scheduler) {
    assertNotNull(scheduler, "scheduler");
    observeClear().subscribeOn(scheduler).subscribe();
  }
}
