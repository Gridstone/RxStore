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

import com.google.gson.Gson;

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

import static android.content.Context.MODE_PRIVATE;

/**
 * @author Christopher Horner
 */
public class GRexPersister {
    private final Context context;
    private final Gson gson;
    private final String dirName;

    public GRexPersister(Context context, String dirName) {
        this(context, dirName, new Gson());
    }

    public GRexPersister(Context context, String dirName, Gson gson) {
        this.context = context.getApplicationContext();
        this.gson = gson;
        this.dirName = dirName;
    }

    public <T> Observable<Boolean> putList(final String key, final List<T> list, final Class<T> type) {
        return Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                Type listType = new ListOfSomething<>(type);
                Writer writer = null;

                try {
                    File outFile = getFile(key);
                    writer = new FileWriter(outFile);
                    gson.toJson(list, listType, writer);

                    if (!subscriber.isUnsubscribed()) {
                        subscriber.onNext(true);
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
                    List<T> result = gson.fromJson(reader, listType);

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

    public <T> Observable<Boolean> addToList(final String key, final T object, final Class<T> type) {
        return getList(key, type)
                .map(new Func1<List<T>, List<T>>() {
                    @Override
                    public List<T> call(List<T> list) {
                        list = new ArrayList<>(list);
                        list.add(object);
                        return list;
                    }
                })
                .flatMap(new Func1<List<T>, Observable<Boolean>>() {
                    @Override
                    public Observable<Boolean> call(List<T> list) {
                        return putList(key, list, type);
                    }
                });
    }

    public <T> Observable<Boolean> removeFromList(final String key, final T object, final Class<T> type) {
        return getList(key, type)
                .map(new Func1<List<T>, List<T>>() {
                    @Override
                    public List<T> call(List<T> list) {
                        list = new ArrayList<T>(list);
                        list.remove(object);
                        return list;
                    }
                })
                .flatMap(new Func1<List<T>, Observable<Boolean>>() {
                    @Override
                    public Observable<Boolean> call(List<T> list) {
                        return putList(key, list, type);
                    }
                });
    }

    public <T> Observable<Boolean> put(final String key, final T object) {
        return Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                Writer writer = null;

                try {
                    File outFile = getFile(key);
                    writer = new FileWriter(outFile);
                    gson.toJson(object, writer);

                    if (!subscriber.isUnsubscribed()) {
                        subscriber.onNext(true);
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
                    T result = gson.fromJson(reader, type);

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
