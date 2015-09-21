/*
 * Copyright (C) GRIDSTONE 2015
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
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;

/**
 * Facilitates the read and write of objects to and from disk using RxJava.
 * <p>
 * Initialise with...
 * <pre>
 * {@code
 * RxStore myStore = RxStore.with(directory).using(converter);
 * }
 * </pre>
 * Android users can also use...
 * <pre>
 * {@code
 * RxStore myStore = RxStore.with(context).in("subDir").using(converter);
 * }
 * </pre>
 */
public class RxStore {
  private final File directory;
  private final Converter converter;

  private RxStore(File directory, Converter converter) {
    this.directory = directory;
    this.converter = converter;
  }

  /**
   * Start creating an {@link RxStore} that will utilise an Android application's
   * private directory.
   * <p>
   * To specify a subdirectory in your application's private directory, call
   * {@link AndroidBuilder#in(String)} before {@link AndroidBuilder#using(Converter)}.
   */
  public static AndroidBuilder withContext(Context context) {
    return new AndroidBuilder(context);
  }

  /**
   * Start creating an {@link RxStore} that will utilise the specified directory
   * to store and retrieve objects.
   */
  public static Builder with(File directory) {
    return new Builder(directory);
  }

  /**
   * Fluent API for creating {@link RxStore} instances.
   */
  public static class Builder {
    private final File directory;

    private Builder(File directory) {
      assertNotNull(directory, "directory");
      this.directory = directory;
    }

    /**
     * Specify the {@link Converter} that this {@link RxStore} will use to serialise
     * and deserialize objects.
     * <p>
     * This will also finish initializing this RxStore instance.
     */
    public RxStore using(Converter converter) {
      assertNotNull(converter, "converter");
      return new RxStore(directory, converter);
    }
  }

  /**
   * Fluent API for creating {@link RxStore} instances on Android.
   */
  public static class AndroidBuilder {
    private final Context context;

    private String directoryName = "";

    private AndroidBuilder(Context context) {
      assertNotNull(context, "context");
      this.context = context.getApplicationContext();
    }

    /**
     * Specify a subdirectory that this {@link RxStore} will use to save and
     * restore objects.
     */
    public AndroidBuilder in(String directory) {
      assertNotNull(directory, "directory");
      this.directoryName = directory;
      return this;
    }

    /**
     * Specify the {@link Converter} that this {@link RxStore} will use to serialise
     * and deserialize objects.
     * <p>
     * This will also finish initializing this RxStore instance.
     */
    public RxStore using(Converter converter) {
      assertNotNull(converter, "converter");
      File directory = context.getDir(directoryName, Context.MODE_PRIVATE);
      return new RxStore(directory, converter);
    }
  }

  /**
   * Writes an object to disk using the provided key. Returns an Observable of
   * the object written to disk. (Useful for chaining).
   */
  public final <T> Observable<T> put(final String key, final T object) {
    return Observable.create(new Observable.OnSubscribe<T>() {
      @Override public void call(Subscriber<? super T> subscriber) {
        Writer writer = null;

        try {
          writer = new FileWriter(new File(directory, key));
          converter.write(object, writer);

          if (!subscriber.isUnsubscribed()) {
            subscriber.onNext(object);
            subscriber.onCompleted();
          }
        } catch (Exception e) {
          if (!subscriber.isUnsubscribed()) {
            subscriber.onError(e);
          }
        } finally {
          if (writer != null) {
            try {
              writer.close();
            } catch (IOException ignored) {
            }
          }
        }
      }
    });
  }

  /**
   * Retrieves an object from disk using the provided key. If no object is
   * found, then this Observable completes without calling
   * {@link Subscriber#onNext(Object)}. i.e. an empty Observable.
   */
  public final <T> Observable<T> get(final String key, final Class<T> type) {
    return Observable.create(new Observable.OnSubscribe<T>() {
      @Override public void call(Subscriber<? super T> subscriber) {
        Reader reader = null;

        try {
          File storedFile = new File(directory, key);

          if (!storedFile.exists()) {
            if (!subscriber.isUnsubscribed()) {
              subscriber.onCompleted();
            }

            return;
          }

          reader = new FileReader(storedFile);
          T result = converter.read(reader, type);

          if (!subscriber.isUnsubscribed()) {
            subscriber.onNext(result);
            subscriber.onCompleted();
          }
        } catch (Exception e) {
          if (!subscriber.isUnsubscribed()) {
            subscriber.onError(e);
          }
        } finally {
          try {
            if (reader != null) {
              reader.close();
            }
          } catch (IOException ignored) {
          }
        }
      }
    });
  }

  /**
   * Writes a list to disk using the specified key. Returns an Observable of the
   * list written to disk. (Useful for chaining).
   */
  public final <T> Observable<List<T>> putList(final String key, final List<T> list,
      final Type type) {
    return Observable.create(new Observable.OnSubscribe<List<T>>() {
      @Override public void call(Subscriber<? super List<T>> subscriber) {
        Writer writer = null;

        try {
          writer = new FileWriter(new File(directory, key));
          converter.write(list, writer);

          if (!subscriber.isUnsubscribed()) {
            subscriber.onNext(list);
            subscriber.onCompleted();
          }
        } catch (Exception e) {
          if (!subscriber.isUnsubscribed()) {
            subscriber.onError(e);
          }
        } finally {
          try {
            if (writer != null) {
              writer.close();
            }
          } catch (IOException ignored) {
          }
        }
      }
    });
  }

  /**
   * Retrieves a list form disk using the provided key. If no list is found
   * then this Observable completes without calling
   * {@link Subscriber#onNext(Object)}. i.e. an empty Observable.
   */
  public final <T> Observable<List<T>> getList(final String key, final Class<T> type) {
    return Observable.create(new Observable.OnSubscribe<List<T>>() {
      @Override public void call(Subscriber<? super List<T>> subscriber) {
        Type listType = new ListOfSomething(type);
        Reader reader = null;

        try {
          File storedFile = new File(directory, key);

          if (!storedFile.exists()) {
            if (!subscriber.isUnsubscribed()) {
              subscriber.onCompleted();
            }

            return;
          }

          reader = new FileReader(storedFile);
          List<T> result = converter.read(reader, listType);

          if (!subscriber.isUnsubscribed()) {
            subscriber.onNext(result);
            subscriber.onCompleted();
          }
        } catch (Exception e) {
          if (!subscriber.isUnsubscribed()) {
            subscriber.onError(e);
          }
        } finally {
          try {
            if (reader != null) {
              reader.close();
            }
          } catch (IOException ignored) {
          }
        }
      }
    });
  }

  /**
   * Adds an object to an existing list, or creates a new list with the provided
   * object. Returns an Observable of the list written to disk.
   */
  public final <T> Observable<List<T>> addToList(final String key, final T object,
      final Class<T> type) {
    return getList(key, type).defaultIfEmpty(Collections.<T>emptyList())
        .map(new Func1<List<T>, List<T>>() {
          @Override public List<T> call(List<T> list) {
            List<T> newList = new ArrayList<>(list.size() + 1);
            newList.addAll(list);
            newList.add(object);
            return newList;
          }
        })
        .flatMap(new Func1<List<T>, Observable<List<T>>>() {
          @Override public Observable<List<T>> call(List<T> list) {
            return putList(key, list, type);
          }
        });
  }

  /**
   * Removes an object from an existing List. Returns an Observable of the list
   * written to disk after the remove operation has occurred.
   * <p>
   * This Observable will be empty if the existing list is not found.
   */
  public final <T> Observable<List<T>> removeFromList(final String key, final T object,
      final Class<T> type) {
    return getList(key, type).map(new Func1<List<T>, List<T>>() {
      @Override public List<T> call(List<T> list) {
        list = new ArrayList<>(list);
        list.remove(object);
        return list;
      }
    }).flatMap(new Func1<List<T>, Observable<List<T>>>() {
      @Override public Observable<List<T>> call(List<T> list) {
        return putList(key, list, type);
      }
    });
  }

  /**
   * Remove an object from an existing List by its index. Returns an Observable
   * of the list written to disk after the remove operation has occurred.
   * <p>
   * This Observable will be empty if the existing list is not found.
   */
  public final <T> Observable<List<T>> removeFromList(final String key, final int position,
      final Class<T> type) {
    return getList(key, type).map(new Func1<List<T>, List<T>>() {
      @Override public List<T> call(List<T> list) {
        list = new ArrayList<>(list);
        list.remove(position);
        return list;
      }
    }).flatMap(new Func1<List<T>, Observable<List<T>>>() {
      @Override public Observable<List<T>> call(List<T> list) {
        return putList(key, list, type);
      }
    });
  }

  /**
   * Clears any data stored at the specified key, returning an Observable of
   * true or false depending on the success of the delete operation.
   */
  public final Observable<Boolean> clear(final String key) {
    return Observable.create(new Observable.OnSubscribe<Boolean>() {
      @Override public void call(Subscriber<? super Boolean> subscriber) {
        boolean result = new File(directory, key).delete();

        if (!subscriber.isUnsubscribed()) {
          subscriber.onNext(result);
          subscriber.onCompleted();
        }
      }
    });
  }

  private static void assertNotNull(Object object, String name) {
    if (object == null) {
      throw new NullPointerException(name + " must not be null.");
    }
  }

  static final class ListOfSomething implements ParameterizedType {
    private final Type wrappedType;

    public ListOfSomething(Type wrappedType) {
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
}
