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

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;

/**
 * Convert objects to and from a format that allows them to be saved to disk.
 * <p>
 * Use a converter when initializing an {@link RxStore}
 * <pre>
 * {@code
 * RxStore.with(directory).using(converter);
 * }
 * </pre>
 */
public interface Converter {
  /**
   * Convert data into a serializable format and write to writer.
   */
  <T> void write(T data, Writer writer) throws ConverterException;

  /**
   * Pull typed data out of reader.
   */
  <T> T read(Reader reader, Type type) throws ConverterException;
}
