/*
 * Copyright (C) GRIDSTONE 2014
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

package au.com.gridstone.grex;

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

import au.com.gridstone.grex.converter.Converter;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;

import static android.content.Context.MODE_PRIVATE;

/**
 * Facilitates the read and write of objects to and from an application's private directory.
 *
 * @author Christopher Horner
 */
public class GRexPersister {
    private final Context context;
    private final Converter converter;
    private final String dirName;

    /**
     * Create a new instances using a provided converter.
     *
     * @param context   Context used to determine file directory.
     * @param dirName   The sub directory name to perform all read/write operations to.
     * @param converter Converter used to serialize/deserialize objects.
     */
    public GRexPersister(Context context, String dirName, Converter converter) {
        this.context = context.getApplicationContext();
        this.converter = converter;
        this.dirName = dirName;
    }

    /**
     * Write a List of objects to disk.
     *
     * @param key  The key to store the List against.
     * @param list The List to store.
     * @param type The class of each item stored in the List.
     * @return An Observable that returns the written list in its onNext().
     */
    public <T> Observable<List<T>> putList(final String key, final List<T> list, final Class<T> type) {
        return Observable.create(new Observable.OnSubscribe<List<T>>() {
            @Override
            public void call(Subscriber<? super List<T>> subscriber) {
                Writer writer = null;

                try {
                    File outFile = getFile(key);
                    writer = new FileWriter(outFile);
                    converter.write(list, writer);

                    if (!subscriber.isUnsubscribed()) {
                        subscriber.onNext(list);
                        subscriber.onCompleted();
                    }
                } catch (IOException e) {
                    if (!subscriber.isUnsubscribed()) {
                        subscriber.onError(e);
                    }
                } finally {
                    try {
                        if (writer != null) {
                            writer.close();
                        }
                    } catch (IOException e) {
                        if (!subscriber.isUnsubscribed()) {
                            subscriber.onError(e);
                        }
                    }
                }
            }
        });
    }

    /**
     * Reads a List of objects from disk.
     *
     * @param key  The key that the List is stored against.
     * @param type The type of each item stored in the List.
     * @return An Observable that returns the read list in its onNext(). If no stored List is found,
     * an immutable empty List will be returned.
     */
    public <T> Observable<List<T>> getList(final String key, final Class<T> type) {
        return Observable.create(new Observable.OnSubscribe<List<T>>() {
            @Override
            public void call(Subscriber<? super List<T>> subscriber) {
                Type listType = new ListOfSomething<>(type);
                Reader reader = null;

                try {
                    File inFile = getFile(key);

                    if (!inFile.exists()) {
                        if (!subscriber.isUnsubscribed()) {
                            subscriber.onNext(Collections.<T>emptyList());
                            subscriber.onCompleted();
                        }

                        return;
                    }

                    reader = new FileReader(inFile);
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
     * Adds an object to an existing List, or creates and stores a new List.
     *
     * @param key    The key that the List is stored against. (Or will be stored against if its
     *               currently empty).
     * @param object The object to add to the List.
     * @param type   The type of each item in the List.
     * @return An Observable of the new List written to disk.
     */
    public <T> Observable<List<T>> addToList(final String key, final T object, final Class<T> type) {
        return getList(key, type)
                .map(new Func1<List<T>, List<T>>() {
                    @Override
                    public List<T> call(List<T> list) {
                        list = new ArrayList<>(list);
                        list.add(object);
                        return list;
                    }
                })
                .flatMap(new Func1<List<T>, Observable<List<T>>>() {
                    @Override
                    public Observable<List<T>> call(List<T> list) {
                        return putList(key, list, type);
                    }
                });
    }

    /**
     * Remove an object from an existing List.
     *
     * @param key    The key that the List is stored against.
     * @param object The object to remove from the List.
     * @param type   The type of each item stored in the List.
     * @return An Observable of the new List written to disk after the remove operation has
     * occurred.
     */
    public <T> Observable<List<T>> removeFromList(final String key, final T object, final Class<T> type) {
        return getList(key, type)
                .map(new Func1<List<T>, List<T>>() {
                    @Override
                    public List<T> call(List<T> list) {
                        list = new ArrayList<>(list);
                        list.remove(object);
                        return list;
                    }
                })
                .flatMap(new Func1<List<T>, Observable<List<T>>>() {
                    @Override
                    public Observable<List<T>> call(List<T> list) {
                        return putList(key, list, type);
                    }
                });
    }

    /**
     * Remove an object from an existing List by its index.
     *
     * @param key      The key that the List is stored against.
     * @param position The index of the item to remove.
     * @param type     The type of each item stored in the List.
     * @return An Observable of the new List written to disk after the remove operation has
     * occurred.
     */
    public <T> Observable<List<T>> removeFromList(final String key, final int position, final Class<T> type) {
        return getList(key, type)
                .map(new Func1<List<T>, List<T>>() {
                    @Override
                    public List<T> call(List<T> list) {
                        list = new ArrayList<>(list);
                        list.remove(position);
                        return list;
                    }
                })
                .flatMap(new Func1<List<T>, Observable<List<T>>>() {
                    @Override
                    public Observable<List<T>> call(List<T> list) {
                        return putList(key, list, type);
                    }
                });
    }

    /**
     * Writes an object to disk.
     *
     * @param key    The key to store the object against.
     * @param object The object to write to disk.
     * @return An Observable of the object written to disk.
     */
    public <T> Observable<T> put(final String key, final T object) {
        return Observable.create(new Observable.OnSubscribe<T>() {
            @Override
            public void call(Subscriber<? super T> subscriber) {
                Writer writer = null;

                try {
                    File outFile = getFile(key);
                    writer = new FileWriter(outFile);
                    converter.write(object, writer);

                    if (!subscriber.isUnsubscribed()) {
                        subscriber.onNext(object);
                        subscriber.onCompleted();
                    }

                } catch (Exception e) {
                    if (!subscriber.isUnsubscribed())
                        subscriber.onError(e);
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
     * Retrieves an object from disk.
     *
     * @param key  The key that the object is stored against.
     * @param type The type of the object stored on disk.
     * @return An observable of the retrieved object.
     */
    public <T> Observable<T> get(final String key, final Class<T> type) {
        return Observable.create(new Observable.OnSubscribe<T>() {
            @Override
            public void call(Subscriber<? super T> subscriber) {
                Reader reader = null;

                try {
                    File inFile = getFile(key);

                    if (!inFile.exists()) {
                        if (!subscriber.isUnsubscribed()) {
                            subscriber.onNext(null);
                            subscriber.onCompleted();
                        }

                        return;
                    }

                    reader = new FileReader(inFile);
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
     * Clears any data stored at the specified key.
     *
     * @param key The key to clear data at.
     * @return An Observable that calls onNext(true) if data was cleared.
     */
    public Observable<Boolean> clear(final String key) {
        return Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                File file = getFile(key);
                boolean result = file.delete();

                if (!subscriber.isUnsubscribed()) {
                    subscriber.onNext(result);
                    subscriber.onCompleted();
                }
            }
        });
    }

    private File getFile(String key) {
        return new File(context.getDir(dirName, MODE_PRIVATE), key);
    }

    private static class ListOfSomething<T> implements ParameterizedType {
        private final Class<?> wrappedType;

        public ListOfSomething(Class<T> wrappedType) {
            this.wrappedType = wrappedType;
        }

        @Override
        public Type[] getActualTypeArguments() {
            return new Type[]{wrappedType};
        }

        @Override
        public Type getOwnerType() {
            return null;
        }

        @Override
        public Type getRawType() {
            return List.class;
        }
    }
}
