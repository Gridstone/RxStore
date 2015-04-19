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


import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import au.com.gridstone.grex.converter.Converter;
import au.com.gridstone.grex.converter.ConverterException;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;

/**
 * Facilitates the read and write of objects to and from an application's
 * private directory.
 *
 * @author Christopher Horner
 * @author Joseph Cooper
 */
public class BaseGRexPersister implements Persister {
    private final Converter converter;

    private final IODelegate ioDelegate;

    /**
     * Create a new instance using a provided {@link au.com.gridstone.grex
     * .converter.Converter}
     * .converter.Converter} and a provided {@link IODelegate}.
     *
     * @param converter  Converter used to serialize/deserialize objects.
     * @param ioDelegate IODelegate to get {@link java.io.Reader} and {@link
     *                   java.io.Writer} to use in IO operations.
     */
    public BaseGRexPersister(final Converter converter,
                             final IODelegate ioDelegate) {
        this.ioDelegate = ioDelegate;
        this.converter = converter;
    }

    /**
     * Write a List of objects to disk.
     *
     * @param key  The key to store the List against.
     * @param list The List to store.
     * @param type The class of each item stored in the List.
     * @return An Observable that returns the written list in its onNext().
     */
    @Override
    public final <T> Observable<List<T>> putList(final String key,
                                                 final List<T>
                                                         list,
                                                 final Class<T> type) {
        return Observable.create(new Observable.OnSubscribe<List<T>>() {
            @Override
            public void call(Subscriber<? super List<T>> subscriber) {
                Writer writer = null;

                try {
                    writer = ioDelegate.getWriter(key);
                    converter.write(list, writer);

                    if (!subscriber.isUnsubscribed()) {
                        subscriber.onNext(list);
                        subscriber.onCompleted();
                    }
                } catch (IOException | ConverterException e) {
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
     * @return An Observable that returns the read list in its onNext(). If no list is found, then
     * onCompleted() will be called immediately.
     */
    @Override
    public final <T> Observable<List<T>> getList(final String key,
                                                 final Class<T> type) {
        return Observable.create(new Observable.OnSubscribe<List<T>>() {
            @Override
            public void call(Subscriber<? super List<T>> subscriber) {
                Type listType = new ListOfSomething<>(type);
                Reader reader = null;

                try {

                    reader = ioDelegate.getReader(key);

                    if (reader == null) {
                        if (!subscriber.isUnsubscribed()) {
                            subscriber.onCompleted();
                        }

                        return;
                    }

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
    @Override
    public final <T> Observable<List<T>> addToList(final String key,
                                                   final T object,
                                                   final Class<T> type) {
        return getList(key, type)
                .defaultIfEmpty(Collections.<T>emptyList())
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
    @Override
    public <T> Observable<List<T>> removeFromList(final String key,
                                                  final T object,
                                                  final Class<T> type) {
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
    @Override
    public final <T> Observable<List<T>> removeFromList(final String key,
                                                        final int position,
                                                        final Class<T> type) {
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
    @Override
    public <T> Observable<T> put(final String key, final T object) {
        return Observable.create(new Observable.OnSubscribe<T>() {
            @Override
            public void call(Subscriber<? super T> subscriber) {
                Writer writer = null;

                try {
                    writer = ioDelegate.getWriter(key);
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
    @Override
    public final <T> Observable<T> get(final String key, final Class<T> type) {
        return Observable.create(new Observable.OnSubscribe<T>() {
            @Override
            public void call(Subscriber<? super T> subscriber) {
                Reader reader = null;

                try {

                    reader = ioDelegate.getReader(key);

                    if (reader == null) {
                        if (!subscriber.isUnsubscribed()) {
                            subscriber.onCompleted();
                        }

                        return;
                    }

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
    @Override
    public final Observable<Boolean> clear(final String key) {
        return Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                boolean result = ioDelegate.clear(key);

                if (!subscriber.isUnsubscribed()) {
                    subscriber.onNext(result);
                    subscriber.onCompleted();
                }
            }
        });
    }

    static final class ListOfSomething<T> implements ParameterizedType {
        private final Class<?> wrappedType;

        public ListOfSomething(Class<T> wrappedType) {
            this.wrappedType = wrappedType;
        }

        public static <T> ListOfSomething<T> wrap(Class<T> wrappedType) {
            return new ListOfSomething<>(wrappedType);
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
