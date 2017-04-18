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

import io.reactivex.annotations.NonNull;
import java.io.File;
import java.lang.reflect.Type;

/**
 * Facilitates the read and write of objects to and from disk using RxJava and observing changes
 * over time.
 * <p>
 * To create a store for a single object use {@link #value(File, Converter, Type)}.
 * <p>
 * For {@code Lists} of objects use {@link #list(File, Converter, Type)}.
 */
public class RxStore {
  private RxStore() {
    throw new AssertionError("No instances.");
  }

  /**
   * Create a new {@link ValueStore} that is capable of persisting a single object to disk.
   */
  public static <T> ValueStore<T> value(@NonNull File file, @NonNull Converter converter, @NonNull
      Type type) {
    return new RealValueStore<T>(file, converter, type);
  }

  /**
   * Create a new {@link ListStore} that is capable of persisting many objects to disk.
   */
  public static <T> ListStore<T> list(@NonNull File file, @NonNull Converter converter,
      @NonNull Type type) {
    return new RealListStore<T>(file, converter, type);
  }
}
