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
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.annotations.NonNull;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static au.com.gridstone.rxstore.Locks.runInReadLock;
import static au.com.gridstone.rxstore.Locks.runInWriteLock;
import static au.com.gridstone.rxstore.Preconditions.assertNotNull;

final class RealListStore<T> implements ListStore<T> {
  private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
  private final PublishSubject<List<T>> updateSubject = PublishSubject.create();

  private final File file;
  private final Converter converter;
  private final Type type;

  RealListStore(@NonNull File file, @NonNull Converter converter, @NonNull Type type) {
    assertNotNull(file, "file");
    assertNotNull(converter, "converter");
    assertNotNull(type, "type");
    this.file = file;
    this.converter = converter;
    this.type = new ListType(type);
  }

  @Override @NonNull public Single<List<T>> get() {
    return Single.create(new SingleOnSubscribe<List<T>>() {
      @Override public void subscribe(final SingleEmitter<List<T>> emitter) throws Exception {
        runInReadLock(readWriteLock, new ThrowingRunnable() {
          @Override public void run() throws Exception {
            if (!file.exists()) {
              emitter.onSuccess(Collections.<T>emptyList());
              return;
            }

            List<T> list = converter.read(file, type);
            if (list == null) list = Collections.emptyList();
            emitter.onSuccess(list);
          }
        });
      }
    });
  }

  @Override @NonNull public List<T> blockingGet() {
    return get().blockingGet();
  }

  @Override @NonNull public Single<List<T>> observePut(@NonNull final List<T> list) {
    assertNotNull(list, "list");

    return Single.create(new SingleOnSubscribe<List<T>>() {
      @Override public void subscribe(final SingleEmitter<List<T>> emitter) throws Exception {
        runInWriteLock(readWriteLock, new ThrowingRunnable() {
          @Override public void run() throws Exception {
            if (!file.exists() && !file.createNewFile()) {
              emitter.onError(new IOException("Could not create file for store."));
              return;
            }

            converter.write(list, type, file);
            emitter.onSuccess(list);
            updateSubject.onNext(list);
          }
        });
      }
    });
  }

  @Override public void put(@NonNull List<T> list) {
    put(list, Schedulers.io());
  }

  @Override public void put(@NonNull List<T> list, @NonNull Scheduler scheduler) {
    assertNotNull(scheduler, "scheduler");
    observePut(list).subscribeOn(scheduler).subscribe();
  }

  @Override @NonNull public Observable<List<T>> observe() {
    return updateSubject.startWith(get().toObservable());
  }

  @Override @NonNull public Single<List<T>> observeClear() {
    return Single.create(new SingleOnSubscribe<List<T>>() {
      @Override public void subscribe(final SingleEmitter<List<T>> emitter) throws Exception {
        runInWriteLock(readWriteLock, new ThrowingRunnable() {
          @Override public void run() throws Exception {
            if (file.exists() && !file.delete()) {
              emitter.onError(new IOException("Clear operation on store failed."));
            } else {
              emitter.onSuccess(Collections.<T>emptyList());
              updateSubject.onNext(Collections.<T>emptyList());
            }
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

  @Override @NonNull public Single<List<T>> observeAdd(@NonNull final T value) {
    assertNotNull(value, "value");

    return Single.create(new SingleOnSubscribe<List<T>>() {
      @Override public void subscribe(final SingleEmitter<List<T>> emitter) throws Exception {
        runInWriteLock(readWriteLock, new ThrowingRunnable() {
          @Override public void run() throws Exception {
            if (!file.exists() && !file.createNewFile()) {
              emitter.onError(new IOException("Could not create file for store."));
            }

            List<T> originalList = converter.read(file, type);
            if (originalList == null) originalList = Collections.emptyList();

            List<T> result = new ArrayList<T>(originalList.size() + 1);
            result.addAll(originalList);
            result.add(value);

            converter.write(result, type, file);
            emitter.onSuccess(result);
            updateSubject.onNext(result);
          }
        });
      }
    });
  }

  @Override public void add(@NonNull T value) {
    add(value, Schedulers.io());
  }

  @Override public void add(@NonNull T value, @NonNull Scheduler scheduler) {
    assertNotNull(scheduler, "scheduler");
    observeAdd(value).subscribeOn(scheduler).subscribe();
  }

  @Override @NonNull public Single<List<T>> observeRemove(
      @NonNull final PredicateFunc<T> predicateFunc) {
    assertNotNull(predicateFunc, "predicateFunc");

    return Single.create(new SingleOnSubscribe<List<T>>() {
      @Override public void subscribe(final SingleEmitter<List<T>> emitter) throws Exception {
        runInWriteLock(readWriteLock, new ThrowingRunnable() {
          @Override public void run() throws Exception {
            if (!file.exists()) {
              emitter.onSuccess(Collections.<T>emptyList());
              return;
            }

            List<T> originalList = converter.read(file, type);
            if (originalList == null) originalList = Collections.emptyList();

            int indexOfItemToRemove = -1;

            for (int i = 0; i < originalList.size(); i++) {
              if (predicateFunc.test(originalList.get(i))) {
                indexOfItemToRemove = i;
                break;
              }
            }

            List<T> modifiedList = new ArrayList<T>(originalList);

            if (indexOfItemToRemove != -1) {
              modifiedList.remove(indexOfItemToRemove);
              converter.write(modifiedList, type, file);
            }

            emitter.onSuccess(modifiedList);
            updateSubject.onNext(modifiedList);
          }
        });
      }
    });
  }

  @Override public void remove(@NonNull PredicateFunc<T> predicateFunc) {
    remove(Schedulers.io(), predicateFunc);
  }

  @Override public void remove(@NonNull Scheduler scheduler,
      @NonNull PredicateFunc<T> predicateFunc) {
    assertNotNull(scheduler, "scheduler");
    observeRemove(predicateFunc).subscribeOn(scheduler).subscribe();
  }

  @Override @NonNull public Single<List<T>> observeRemove(@NonNull final T value) {
    assertNotNull(value, "value");
    return observeRemove(new PredicateFunc<T>() {
      @Override public boolean test(T valueToRemove) {
        return value.equals(valueToRemove);
      }
    });
  }

  @Override public void remove(@NonNull final T value) {
    remove(value, Schedulers.io());
  }

  @Override public void remove(@NonNull final T value, @NonNull Scheduler scheduler) {
    assertNotNull(scheduler, "scheduler");
    observeRemove(value).subscribeOn(scheduler).subscribe();
  }

  @Override @NonNull public Single<List<T>> observeRemove(final int position) {
    return Single.create(new SingleOnSubscribe<List<T>>() {
      @Override public void subscribe(final SingleEmitter<List<T>> emitter) throws Exception {
        runInWriteLock(readWriteLock, new ThrowingRunnable() {
          @Override public void run() throws Exception {
            List<T> originalList = converter.read(file, type);
            if (originalList == null) originalList = Collections.emptyList();

            List<T> modifiedList = new ArrayList<T>(originalList);
            modifiedList.remove(position);

            converter.write(modifiedList, type, file);
            emitter.onSuccess(modifiedList);
            updateSubject.onNext(modifiedList);
          }
        });
      }
    });
  }

  @Override public void remove(int position) {
    remove(position, Schedulers.io());
  }

  @Override public void remove(int position, @NonNull Scheduler scheduler) {
    assertNotNull(scheduler, "scheduler");
    observeRemove(position).subscribeOn(scheduler).subscribe();
  }

  @Override @NonNull public Single<List<T>> observeReplace(@NonNull final T value,
      @NonNull final PredicateFunc<T> predicateFunc) {
    assertNotNull(value, "value");
    assertNotNull(predicateFunc, "predicateFunc");

    return Single.create(new SingleOnSubscribe<List<T>>() {
      @Override public void subscribe(final SingleEmitter<List<T>> emitter) throws Exception {
        runInWriteLock(readWriteLock, new ThrowingRunnable() {
          @Override public void run() throws Exception {
            if (!file.exists()) {
              emitter.onSuccess(Collections.<T>emptyList());
              return;
            }

            List<T> originalList = converter.read(file, type);
            if (originalList == null) originalList = Collections.emptyList();

            int indexOfItemToReplace = -1;

            for (int i = 0; i < originalList.size(); i++) {
              if (predicateFunc.test(originalList.get(i))) {
                indexOfItemToReplace = i;
                break;
              }
            }

            List<T> modifiedList = new ArrayList<T>(originalList);

            if (indexOfItemToReplace != -1) {
              modifiedList.remove(indexOfItemToReplace);
              modifiedList.add(indexOfItemToReplace, value);
              converter.write(modifiedList, type, file);
            }

            emitter.onSuccess(modifiedList);
            updateSubject.onNext(modifiedList);
          }
        });
      }
    });
  }

  @Override public void replace(@NonNull T value, @NonNull PredicateFunc<T> predicateFunc) {
    replace(value, Schedulers.io(), predicateFunc);
  }

  @Override public void replace(@NonNull T value, @NonNull Scheduler scheduler,
      @NonNull PredicateFunc<T> predicateFunc) {
    assertNotNull(scheduler, "scheduler");
    observeReplace(value, predicateFunc).subscribeOn(scheduler).subscribe();
  }

  @Override @NonNull public Single<List<T>> observeAddOrReplace(@NonNull final T value,
      @NonNull final PredicateFunc<T> predicateFunc) {
    assertNotNull(value, "value");
    assertNotNull(predicateFunc, "predicateFunc");

    return Single.create(new SingleOnSubscribe<List<T>>() {
      @Override public void subscribe(final SingleEmitter<List<T>> emitter) throws Exception {
        runInWriteLock(readWriteLock, new ThrowingRunnable() {
          @Override public void run() throws Exception {
            if (!file.exists() && !file.createNewFile()) {
              emitter.onError(new IOException("Could not create store."));
              return;
            }

            List<T> originalList = converter.read(file, type);
            if (originalList == null) originalList = Collections.emptyList();

            int indexOfItemToReplace = -1;

            for (int i = 0; i < originalList.size(); i++) {
              if (predicateFunc.test(originalList.get(i))) {
                indexOfItemToReplace = i;
                break;
              }
            }

            int modifiedListSize = indexOfItemToReplace == -1 ? originalList.size() + 1 :
                originalList.size();

            List<T> modifiedList = new ArrayList<T>(modifiedListSize);
            modifiedList.addAll(originalList);

            if (indexOfItemToReplace == -1) {
              modifiedList.add(value);
            } else {
              modifiedList.remove(indexOfItemToReplace);
              modifiedList.add(indexOfItemToReplace, value);
            }

            converter.write(modifiedList, type, file);
            emitter.onSuccess(modifiedList);
            updateSubject.onNext(modifiedList);
          }
        });
      }
    });
  }

  @Override public void addOrReplace(@NonNull T value, @NonNull PredicateFunc<T> predicateFunc) {
    addOrReplace(value, Schedulers.io(), predicateFunc);
  }

  @Override public void addOrReplace(@NonNull T value, @NonNull Scheduler scheduler,
      @NonNull PredicateFunc<T> predicateFunc) {
    assertNotNull(scheduler, "scheduler");
    observeAddOrReplace(value, predicateFunc).subscribeOn(scheduler).subscribe();
  }

  static final class ListType implements ParameterizedType {
    private final Type wrappedType;

    ListType(Type wrappedType) {
      this.wrappedType = wrappedType;
    }

    @Override public Type[] getActualTypeArguments() {
      return new Type[] {wrappedType};
    }

    @Override public Type getOwnerType() {
      return null;
    }

    @Override public Type getRawType() {
      return List.class;
    }
  }
}
