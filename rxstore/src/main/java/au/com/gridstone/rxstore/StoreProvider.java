/*
 * Copyright (C) GRIDSTONE 2016
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

import android.content.Context;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.Single;
import rx.SingleSubscriber;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

/**
 * Facilitates the read and write of objects to and from disk using RxJava, and observing
 * changes over time.
 * <p>
 * A StoreProvider essentially represents a directory on disk, and each Store a file in that
 * directory. Pushing data into a store writes it to its backing file. Entities observing a
 * store will receive update events as the value stored on disk changes over time.
 * <p>
 * Initialise with...
 * <pre>
 * {@code
 * StoreProvider storeProvider = StoreProvider.with(directory).using(converter);
 * }
 * </pre>
 * Android users can also invoke...
 * <pre>
 * {@code
 * StoreProvider storeProvider = StoreProvider.with(context).inDir("subDir").using(converter);
 * }
 * </pre>
 * A StoreProvider can then be used to get a {@link ValueStore ValueStore} or a
 * {@link ListStore ListStore} depending on your needs. ValueStores hold a single object,
 * whereas ListStores hold many (and have convenience methods for adding and removing single
 * objects).
 * <p>
 * To get a {@link ValueStore ValueStore}...
 * <pre>
 * {@code
 * storeProvider.valueStore("someKey", Data.class);
 * }
 * </pre>
 * Or a {@link ListStore ListStore}...
 * <pre>
 * {@code
 * storeProvider.listStore("someKey", Data.class);
 * }
 * </pre>
 */
public final class StoreProvider {
  private final File directory;
  private final Converter converter;
  private final Scheduler scheduler;

  private StoreProvider(File directory, Converter converter, Scheduler scheduler) {
    this.directory = directory;
    this.converter = converter;
    this.scheduler = scheduler;
  }

  private File getFileForStore(String key) {
    if (!directory.exists()) {
      if (!directory.mkdir()) throw new RuntimeException("Could not create directory for store.");
    }

    File file = new File(directory, key);

    if (!file.exists()) {
      try {
        if (!file.createNewFile()) throw new IOException("Could not create file for store.");
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    return file;
  }

  public @NotNull <T> ValueStore<T> valueStore(@NotNull String key, @NotNull Type type) {
    return new ValueStore<T>(getFileForStore(key), converter, type, scheduler);
  }

  @NotNull public <T> ListStore<T> listStore(@NotNull String key, @NotNull Type type) {
    return new ListStore<T>(getFileForStore(key), converter, new ListType(type), scheduler);
  }

  /**
   * Start creating a {@link StoreProvider} that will utilise an Android application's
   * private directory.
   * <p>
   * To specify a subdirectory in your application's private directory, call
   * {@link AndroidBuilder#inDir(String) inDir()} before {@link AndroidBuilder#using(Converter)
   * using()}.
   */
  @NotNull public static AndroidBuilder withContext(@NotNull Context context) {
    return new AndroidBuilder(context);
  }

  /**
   * Start creating a {@link StoreProvider} that will utilise the specified directory
   * for storage.
   */
  @NotNull public static Builder with(@NotNull File directory) {
    return new Builder(directory);
  }

  /**
   * Fluent API for creating {@link StoreProvider} instances.
   */
  public static final class Builder {
    private final File directory;

    private Scheduler scheduler;

    private Builder(File directory) {
      assertNotNull(directory, "directory");
      this.directory = directory;
    }

    /**
     * By default, a StoreProvider will instantiate all stores with a single thread executor
     * {@link Scheduler} to ensure items get written and read in order. If you would like to
     * override this behaviour (such as for testing with {@link Schedulers#immediate()}), then
     * you can specify a custom Scheduler here.
     */
    @NotNull public Builder schedulingWith(@NotNull Scheduler scheduler) {
      assertNotNull(scheduler, "scheduler");
      this.scheduler = scheduler;
      return this;
    }

    /**
     * Specify the {@link Converter} that this {@link StoreProvider} will use to serialise
     * and deserialize objects.
     * <p>
     * This will also finish initializing this StoreProvider instance.
     */
    public @NotNull StoreProvider using(@NotNull Converter converter) {
      assertNotNull(converter, "converter");
      if (scheduler == null) scheduler = Schedulers.from(Executors.newSingleThreadExecutor());
      return new StoreProvider(directory, converter, scheduler);
    }
  }

  /**
   * Fluent API for creating {@link StoreProvider} instances on Android.
   */
  public static final class AndroidBuilder {
    private final Context context;

    private String directoryName = "";
    private Scheduler scheduler;

    private AndroidBuilder(@NotNull Context context) {
      assertNotNull(context, "context");
      this.context = context.getApplicationContext();
    }

    /**
     * By default, a StoreProvider will instantiate all stores with a single thread executor
     * {@link Scheduler} to ensure items get written and read in order. If you would like to
     * override this behaviour (such as for testing with {@link Schedulers#immediate()}), then
     * you can specify a custom Scheduler here.
     */
    @NotNull public AndroidBuilder schedulingWith(@NotNull Scheduler scheduler) {
      assertNotNull(scheduler, "scheduler");
      this.scheduler = scheduler;
      return this;
    }

    /**
     * Specify a subdirectory that this {@link StoreProvider} will use to save and
     * restore objects.
     */
    @NotNull public AndroidBuilder inDir(@NotNull String directory) {
      assertNotNull(directory, "directory");
      this.directoryName = directory;
      return this;
    }

    /**
     * Specify the {@link Converter} that this {@link StoreProvider} will use to serialise
     * and deserialize objects.
     * <p>
     * This will also finish initializing this StoreProvider instance.
     */
    public @NotNull StoreProvider using(@NotNull Converter converter) {
      assertNotNull(converter, "converter");
      File directory = context.getDir(directoryName, Context.MODE_PRIVATE);
      if (scheduler == null) scheduler = Schedulers.from(Executors.newSingleThreadExecutor());
      return new StoreProvider(directory, converter, scheduler);
    }
  }

  /**
   * Store a single object on disk.
   */
  public static final class ValueStore<T> {
    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final PublishSubject<T> updateSubject = PublishSubject.create();

    private final File file;
    private final Converter converter;
    private final Type type;
    private final Scheduler scheduler;

    ValueStore(File file, Converter converter, Type type, final Scheduler scheduler) {
      this.file = file;
      this.converter = converter;
      this.type = type;
      this.scheduler = scheduler;
    }

    /**
     * Write a value to this store and observe the operation. The value returned in the
     * {@link Single} is the value written to disk. (Useful for chaining).
     * <p>
     * The {@link Scheduler} used for this operation will be the one specified in
     * {@link Builder#schedulingWith(Scheduler) schedulingWith()}.
     */
    @NotNull public Single<T> observePut(@NotNull final T value) {
      return Single.create(new Single.OnSubscribe<T>() {
        @Override public void call(final SingleSubscriber<? super T> subscriber) {
          try {
            if (!file.exists()) throw new IOException("This store has been deleted!");

            runInWriteLock(readWriteLock, new Runnable() {
              @Override public void run() {
                converter.write(value, type, file);
                subscriber.onSuccess(value);
                updateSubject.onNext(value);
              }
            });
          } catch (Exception e) {
            subscriber.onError(e);
          }
        }
      }).subscribeOn(scheduler);
    }

    /**
     * Write a value to this store. An exception will be thrown if an error occurs.
     * <p>
     * The {@link Scheduler} used for this operation will be the one specified in
     * {@link Builder#schedulingWith(Scheduler) schedulingWith()}.
     */
    public void put(@NotNull T value) {
      observePut(value).subscribe(errorThrowingObserver);
    }

    /**
     * Get a value from disk as a {@link Single}, making it easier to retrieve
     * asynchronously.
     * <p>
     * If no value has been written to this store, then the value in the Single will be
     * null.
     * <p>
     * The {@link Scheduler} used for this operation will be the one specified in
     * {@link Builder#schedulingWith(Scheduler) schedulingWith()}.
     */
    @NotNull public Single<T> get() {
      return Single.create(new Single.OnSubscribe<T>() {
        @Override public void call(final SingleSubscriber<? super T> subscriber) {
          try {
            if (!file.exists()) throw new IOException("This store has been deleted!");

            runInReadLock(readWriteLock, new Runnable() {
              @Override public void run() {
                T value = converter.read(file, type);
                subscriber.onSuccess(value);
              }
            });
          } catch (Exception e) {
            subscriber.onError(e);
          }
        }
      }).subscribeOn(scheduler);
    }

    /**
     * Synchronously get the value associated with this store, or null if this
     * store has no current value.
     */
    @Nullable public T getBlocking() {
      return get().toBlocking().value();
    }

    /**
     * Observe changes to the value in this store. {@code onNext(value)} will be called immediately
     * with the current value upon subscription.
     * <p>
     * {@code onCompleted()} will only ever be called if this store has {@link #delete()} called
     * upon it.
     * <p>
     * The {@link Scheduler} used for this observable will be the one specified in
     * {@link Builder#schedulingWith(Scheduler) schedulingWith()}.
     */
    @NotNull public Observable<T> asObservable() {
      return updateSubject.asObservable()
          .startWith(get().toObservable())
          .onBackpressureLatest()
          .subscribeOn(scheduler);
    }

    /**
     * Clear the value in this store, setting it to null, and observe the operation.
     * <p>
     * The {@link Scheduler} used for this operation will be the one specified in
     * {@link Builder#schedulingWith(Scheduler) schedulingWith()}.
     */
    @NotNull public Single<T> observeClear() {
      return Single.create(new Single.OnSubscribe<T>() {
        @Override public void call(final SingleSubscriber<? super T> subscriber) {
          try {
            if (!file.exists()) throw new IOException("This store has been deleted!");

            runInWriteLock(readWriteLock, new Runnable() {
              @Override public void run() {
                converter.write(null, type, file);
                subscriber.onSuccess(null);
                updateSubject.onNext(null);
              }
            });
          } catch (Exception e) {
            subscriber.onError(e);
          }
        }
      }).subscribeOn(scheduler);
    }

    /**
     * Clear the value in this store, setting it to null. An exception will be thrown if
     * an error occurs.
     */
    public void clear() {
      observeClear().subscribe(errorThrowingObserver);
    }

    /**
     * Delete the file associated with this store, render it completely unusable, and
     * observe the operation.
     * <p>
     * Once this operation completes, this store object will no longer be usable. Any
     * observers subscribed to {@link #asObservable()} will receive {@code onCompleted()}.
     * <p>
     * Any subsequent calls to {@link #get()}, {@link #put(Object) put()}, or
     * {@link #clear()} will throw an {@link IOException}.
     * <p>
     * If you wish to use a ValueStore again with this store's key, then you must create
     * a new instance through your StoreProvider.
     * <p>
     * The {@link Scheduler} used for this operation will be the one specified in
     * {@link Builder#schedulingWith(Scheduler) schedulingWith()}.
     */
    @NotNull public Single<T> observeDelete() {
      return Single.create(new Single.OnSubscribe<T>() {
        @Override public void call(final SingleSubscriber<? super T> subscriber) {
          try {
            if (!file.exists()) throw new IOException("This store has already been deleted!");

            runInWriteLock(readWriteLock, new Runnable() {
              @Override public void run() {
                if (file.delete()) {
                  subscriber.onSuccess(null);
                  updateSubject.onNext(null);
                  updateSubject.onCompleted();
                } else {
                  subscriber.onError(new IOException("Delete operation on store failed!"));
                }
              }
            });
          } catch (Exception e) {
            subscriber.onError(e);
          }
        }
      }).subscribeOn(scheduler);
    }

    /**
     * Delete the file associated with this store and render it completely unusable.
     * <p>
     * Once this operation completes, this store object will no longer be usable. Any
     * observers subscribed to {@link #asObservable()} will receive {@code onCompleted()}.
     * <p>
     * Any subsequent calls to {@link #get()}, {@link #put(Object) put()}, or
     * {@link #clear()} will throw an {@link IOException}.
     * <p>
     * If you wish to use a ValueStore again with this store's key, then you must create
     * a new instance through your StoreProvider.
     */
    public void delete() {
      observeDelete().subscribe(errorThrowingObserver);
    }
  }

  /**
   * Store a list of homogeneous objects on disk.
   */
  public final class ListStore<T> {
    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final PublishSubject<List<T>> updateSubject = PublishSubject.create();

    private final File file;
    private final Converter converter;
    private final Type type;
    private final Scheduler scheduler;

    private ListStore(File file, Converter converter, Type type, final Scheduler scheduler) {
      this.file = file;
      this.converter = converter;
      this.type = type;
      this.scheduler = scheduler;
    }

    /**
     * Write a List to this store and observe the operation. The value returned in the
     * {@link Single} is the value written to disk. (Useful for chaining).
     * <p>
     * The {@link Scheduler} used for this operation will be the one specified in
     * {@link Builder#schedulingWith(Scheduler) schedulingWith()}.
     */
    @NotNull public Single<List<T>> observePut(@NotNull final List<T> value) {
      return Single.create(new Single.OnSubscribe<List<T>>() {
        @Override public void call(final SingleSubscriber<? super List<T>> subscriber) {
          try {
            if (!file.exists()) throw new IOException("This store has been deleted!");

            runInWriteLock(readWriteLock, new Runnable() {
              @Override public void run() {
                converter.write(value, type, file);
                subscriber.onSuccess(value);
                updateSubject.onNext(value);
              }
            });
          } catch (Exception e) {
            subscriber.onError(e);
          }
        }
      }).subscribeOn(scheduler);
    }

    /**
     * Write a List to this store. An exception will be thrown if an error occurs.
     * <p>
     * The {@link Scheduler} used for this operation will be the one specified in
     * {@link Builder#schedulingWith(Scheduler) schedulingWith()}.
     */
    public void put(@NotNull List<T> value) {
      observePut(value).subscribe(errorThrowingObserver);
    }

    /**
     * Get a list from disk as a {@link Single}, making it easier to retrieve
     * asynchronously.
     * <p>
     * If no list has been written to this store, then the value in the Single will be
     * a non-null empty immutable {@code List}.
     * <p>
     * The {@link Scheduler} used for this operation will be the one specified in
     * {@link Builder#schedulingWith(Scheduler) schedulingWith()}.
     */
    @NotNull public Single<List<T>> get() {
      return Single.create(new Single.OnSubscribe<List<T>>() {
        @Override public void call(final SingleSubscriber<? super List<T>> subscriber) {
          try {
            if (!file.exists()) throw new IOException("This store has been deleted!");

            runInReadLock(readWriteLock, new Runnable() {
              @Override public void run() {
                List<T> value = converter.read(file, type);
                if (value == null) value = Collections.emptyList();
                subscriber.onSuccess(value);
              }
            });
          } catch (Exception e) {
            subscriber.onError(e);
          }
        }
      }).subscribeOn(scheduler);
    }

    /**
     * Synchronously get the list associated with this store, or an immutable empty
     * {@code List} if this store has no items.
     */
    @NotNull public List<T> getBlocking() {
      return get().toBlocking().value();
    }

    /**
     * Observe changes to the list in this store. {@code onNext(value)} will be called immediately
     * with the current list upon subscription.
     * <p>
     * {@code onCompleted()} will only ever be called if this store has {@link #delete()} called
     * upon it.
     * <p>
     * The {@link Scheduler} used for this observable will be the one specified in
     * {@link Builder#schedulingWith(Scheduler) schedulingWith()}.
     */
    @NotNull public Observable<List<T>> asObservable() {
      return updateSubject.asObservable()
          .startWith(get().toObservable())
          .onBackpressureLatest()
          .subscribeOn(scheduler);
    }

    /**
     * Clear the list in this store, remove all its entries, and observe the operation.
     * <p>
     * The {@link Scheduler} used for this operation will be the one specified in
     * {@link Builder#schedulingWith(Scheduler) schedulingWith()}.
     */
    @NotNull public Single<List<T>> observeClear() {
      return Single.create(new Single.OnSubscribe<List<T>>() {
        @Override public void call(final SingleSubscriber<? super List<T>> subscriber) {
          try {
            if (!file.exists()) throw new IOException("This store has been deleted!");

            runInWriteLock(readWriteLock, new Runnable() {
              @Override public void run() {
                List<T> emptyList = Collections.emptyList();
                converter.write(emptyList, type, file);
                subscriber.onSuccess(emptyList);
                updateSubject.onNext(emptyList);
              }
            });
          } catch (Exception e) {
            subscriber.onError(e);
          }
        }
      }).subscribeOn(scheduler);
    }

    /**
     * Clear the list in this store and remove all its entries. An exception will be thrown if
     * an error occurs.
     */
    public void clear() {
      observeClear().subscribe(errorThrowingObserver);
    }

    /**
     * Add an item to the stored list and observe the operation.
     * <p>
     * This operation will also create a list if it currently does not exist.
     * <p>
     * The {@link Scheduler} used for this operation will be the one specified in
     * {@link Builder#schedulingWith(Scheduler) schedulingWith()}.
     */
    @NotNull public Single<List<T>> observeAddToList(@NotNull final T value) {
      return Single.create(new Single.OnSubscribe<List<T>>() {
        @Override public void call(final SingleSubscriber<? super List<T>> subscriber) {
          try {
            if (!file.exists()) throw new IOException("This store has already been deleted!");

            runInWriteLock(readWriteLock, new Runnable() {
              @Override public void run() {
                List<T> originalList = converter.read(file, type);
                if (originalList == null) originalList = Collections.emptyList();

                List<T> result = new ArrayList<T>(originalList.size() + 1);
                result.addAll(originalList);
                result.add(value);

                converter.write(result, type, file);
                subscriber.onSuccess(result);
                updateSubject.onNext(result);
              }
            });
          } catch (Exception e) {
            subscriber.onError(e);
          }
        }
      }).subscribeOn(scheduler);
    }

    /**
     * Add an item to the stored list.
     * <p>
     * This operation will also create a list if it currently does not exist.
     */
    public void addToList(@NotNull T value) {
      observeAddToList(value).subscribe(errorThrowingObserver);
    }

    /**
     * Remove an item from the stored list and observe the operation.
     * <p>
     * The {@link Scheduler} used for this operation will be the one specified in
     * {@link Builder#schedulingWith(Scheduler) schedulingWith()}.
     */
    @NotNull public Single<List<T>> observeRemoveFromList(@NotNull final T value) {
      return observeRemoveFromList(new RemovePredicateFunc<T>() {
        @Override public boolean shouldRemove(T valueToRemove) {
          return value.equals(valueToRemove);
        }
      });
    }

    /**
     * Remove an item from the list if it exists.
     */
    public void removeFromList(@NotNull T value) {
      observeRemoveFromList(value).subscribe(errorThrowingObserver);
    }

    /**
     * Remove the first item in the list that the provided predicate returns true for and observe
     * the operation.
     * <p>
     * The {@link Scheduler} used for this operation will be the one specified in
     * {@link Builder#schedulingWith(Scheduler) schedulingWith()}.
     */
    @NotNull public Single<List<T>> observeRemoveFromList(@NotNull final RemovePredicateFunc<T> predicateFunc) {
      return Single.create(new Single.OnSubscribe<List<T>>() {
        @Override public void call(final SingleSubscriber<? super List<T>> subscriber) {
          try {
            if (!file.exists()) throw new IOException("This store has already been deleted!");

            runInWriteLock(readWriteLock, new Runnable() {
              @Override public void run() {
                List<T> originalList = converter.read(file, type);
                if (originalList == null) originalList = Collections.emptyList();

                int indexOfItemToRemove = -1;

                for (int i = 0; i < originalList.size(); i++) {
                  if (predicateFunc.shouldRemove(originalList.get(i))) {
                    indexOfItemToRemove = i;
                    break;
                  }
                }

                List<T> modifiedList = new ArrayList<T>(originalList);

                if (indexOfItemToRemove != -1) {
                  modifiedList.remove(indexOfItemToRemove);
                }

                converter.write(modifiedList, type, file);
                subscriber.onSuccess(modifiedList);
                updateSubject.onNext(modifiedList);
              }
            });
          } catch (Exception e) {
            subscriber.onError(e);
          }
        }
      }).subscribeOn(scheduler);
    }

    /**
     * Remove an item from the list that the provided predicate returns true for.
     */
    public void removeFromList(@NotNull final RemovePredicateFunc<T> predicateFunc) {
      observeRemoveFromList(predicateFunc).subscribe(errorThrowingObserver);
    }

    /**
     * Remove an item from the stored list by index and observe the operation.
     * <p>
     * The {@link Scheduler} used for this operation will be the one specified in
     * {@link Builder#schedulingWith(Scheduler) schedulingWith()}.
     */
    @NotNull public Single<List<T>> observeRemoveFromList(final int position) {
      return Single.create(new Single.OnSubscribe<List<T>>() {
        @Override public void call(SingleSubscriber<? super List<T>> subscriber) {
          try {
            if (!file.exists()) throw new IOException("This store has already been deleted!");

            runInWriteLock(readWriteLock, new Runnable() {
              @Override public void run() {
                List<T> originalList = converter.read(file, type);
                if (originalList == null) originalList = Collections.emptyList();

                List<T> modifiedList = new ArrayList<T>(originalList);
                modifiedList.remove(position);

                converter.write(modifiedList, type, file);
                updateSubject.onNext(modifiedList);
              }
            });
          } catch (Exception e) {
            subscriber.onError(e);
          }
        }
      }).subscribeOn(scheduler);
    }

    public void removeFromList(int position) {
      observeRemoveFromList(position).subscribe(errorThrowingObserver);
    }

    /**
     * Replace the first item in the list that the provided predicate returns true for.
     * <p>
     * The {@link Scheduler} used for this operation will be the one specified in
     * {@link Builder#schedulingWith(Scheduler) schedulingWith()}.
     */
    @NotNull public Single<List<T>> observeReplace(@NotNull final T value,
        @NotNull final ReplacePredicateFunc<T> predicateFunc) {
      return Single.create(new Single.OnSubscribe<List<T>>() {
        @Override public void call(SingleSubscriber<? super List<T>> subscriber) {
          try {
            if (!file.exists()) throw new IOException("This store has already been deleted!");

            runInWriteLock(readWriteLock, new Runnable() {
              @Override public void run() {
                List<T> originalList = converter.read(file, type);
                if (originalList == null) originalList = Collections.emptyList();

                int indexOfItemToReplace = -1;

                for (int i = 0; i < originalList.size(); i++) {
                  if (predicateFunc.shouldReplace(originalList.get(i))) {
                    indexOfItemToReplace = i;
                    break;
                  }
                }

                List<T> modifiedList = new ArrayList<T>(originalList);

                if (indexOfItemToReplace != -1) {
                  modifiedList.remove(indexOfItemToReplace);
                  modifiedList.add(indexOfItemToReplace, value);
                }

                converter.write(modifiedList, type, file);
                subscriber.onSuccess(modifiedList);
                updateSubject.onNext(modifiedList);
              }
            });
          } catch (Exception e) {
            subscriber.onError(e);
          }
        }
      }).subscribeOn(scheduler);
    }

    /**
     * Replace the first item in the list that the provided predicate returns true for.
     */
    public void replace(@NotNull T value, @NotNull ReplacePredicateFunc<T> predicateFunc) {
      observeReplace(value, predicateFunc).subscribe(errorThrowingObserver);
    }

    /**
     * Replace the first item in the list that the provided predicate returns true for with the
     * provided item. If no matching item is found, add the provided item into the list.
     * <p>
     * The {@link Scheduler} used for this operation will be the one specified in
     * {@link Builder#schedulingWith(Scheduler) schedulingWith()}.
     */
    @NotNull public Single<List<T>> observeAddOrReplace(@NotNull final T value,
        @NotNull final ReplacePredicateFunc<T> predicateFunc) {
      return Single.create(new Single.OnSubscribe<List<T>>() {
        @Override public void call(SingleSubscriber<? super List<T>> subscriber) {
          try {
            if (!file.exists()) throw new IOException("This store has already been deleted!");

            runInWriteLock(readWriteLock, new Runnable() {
              @Override public void run() {
                List<T> originalList = converter.read(file, type);
                if (originalList == null) originalList = Collections.emptyList();

                int indexOfItemToReplace = -1;

                for (int i = 0; i < originalList.size(); i++) {
                  if (predicateFunc.shouldReplace(originalList.get(i))) {
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
                subscriber.onSuccess(modifiedList);
                updateSubject.onNext(modifiedList);
              }
            });
          } catch (Exception e) {
            subscriber.onError(e);
          }
        }
      }).subscribeOn(scheduler);
    }

    /**
     * Replace the first item in the list that the provided predicate returns true for with the
     * provided item. If no matching item is found, add the provided item into the list.
     */
    public void addOrReplace(@NotNull T value, @NotNull ReplacePredicateFunc<T> predicateFunc) {
      observeAddOrReplace(value, predicateFunc).subscribe(errorThrowingObserver);
    }

    /**
     * Delete the file associated with this store, render it completely unusable, and
     * observe the operation.
     * <p>
     * Once this operation completes, this store object will no longer be usable. Any
     * observers subscribed to {@link #asObservable()} will receive {@code onCompleted()}.
     * <p>
     * Any subsequent calls to {@link #get()}, {@link #put(List) put()}, or
     * {@link #clear()} will throw an {@link IOException}.
     * <p>
     * If you wish to use a ListStore again with this store's key, then you must create
     * a new instance through your StoreProvider.
     * <p>
     * The {@link Scheduler} used for this operation will be the one specified in
     * {@link Builder#schedulingWith(Scheduler) schedulingWith()}.
     */
    @NotNull public Single<List<T>> observeDelete() {
      return Single.create(new Single.OnSubscribe<List<T>>() {
        @Override public void call(final SingleSubscriber<? super List<T>> subscriber) {
          try {
            if (!file.exists()) throw new IOException("This store has already been deleted!");

            runInWriteLock(readWriteLock, new Runnable() {
              @Override public void run() {
                if (file.delete()) {
                  List<T> emptyList = Collections.emptyList();
                  subscriber.onSuccess(emptyList);
                  updateSubject.onNext(emptyList);
                  updateSubject.onCompleted();
                } else {
                  subscriber.onError(new IOException("Delete operation on list store failed!"));
                }
              }
            });
          } catch (Exception e) {
            subscriber.onError(e);
          }
        }
      }).subscribeOn(scheduler);
    }

    /**
     * Delete the file associated with this store, render it completely unusable, and
     * observe the operation.
     * <p>
     * Once this operation completes, this store object will no longer be usable. Any
     * observers subscribed to {@link #asObservable()} will receive {@code onCompleted()}.
     * <p>
     * Any subsequent calls to {@link #get()}, {@link #put(List) put()}, or
     * {@link #clear()} will throw an {@link IOException}.
     * <p>
     * If you wish to use a ListStore again with this store's key, then you must create
     * a new instance through your StoreProvider.
     */
    public void delete() {
      observeDelete().subscribe(errorThrowingObserver);
    }
  }

  public interface ReplacePredicateFunc<T> {
    boolean shouldReplace(T value);
  }

  public interface RemovePredicateFunc<T> {
    boolean shouldRemove(T value);
  }

  private static Observer<Object> errorThrowingObserver = new Observer<Object>() {
    @Override public void onCompleted() {
    }

    @Override public void onError(Throwable e) {
      throw new RuntimeException(e);
    }

    @Override public void onNext(Object o) {
    }
  };

  private static void assertNotNull(Object object, String name) {
    if (object == null) {
      throw new NullPointerException(name + " must not be null.");
    }
  }

  static final class ListType implements ParameterizedType {
    private final Type wrappedType;

    public ListType(Type wrappedType) {
      this.wrappedType = wrappedType;
    }

    @Override public Type[] getActualTypeArguments() {
      return new Type[] { wrappedType };
    }

    @Override public Type getOwnerType() {
      return null;
    }

    @Override public Type getRawType() {
      return List.class;
    }
  }

  private static void runInReadLock(ReentrantReadWriteLock readWriteLock, Runnable runnable) {
    Lock readLock = readWriteLock.readLock();
    readLock.lock();

    try {
      runnable.run();
    } finally {
      readLock.unlock();
    }
  }

  private static void runInWriteLock(ReentrantReadWriteLock readWriteLock, Runnable runnable) {
    Lock readLock = readWriteLock.readLock();
    int readCount = readWriteLock.getWriteHoldCount() == 0 ? readWriteLock.getReadHoldCount() : 0;

    for (int i = 0; i < readCount; i++) {
      readLock.unlock();
    }

    Lock writeLock = readWriteLock.writeLock();
    writeLock.lock();

    try {
      runnable.run();
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      for (int i = 0; i < readCount; i++) {
        readLock.lock();
      }
      writeLock.unlock();
    }
  }
}
