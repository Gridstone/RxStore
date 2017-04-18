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
import io.reactivex.annotations.Nullable;
import java.io.File;
import java.lang.reflect.Type;

/**
 * Convert objects to and from serializable formats and read/write them from disk.
 */
public interface Converter {
  /**
   * Convert data into a serializable format and write to writer.
   */
  <T> void write(@Nullable T data, @NonNull Type type, @NonNull File file)
      throws ConverterException;

  /**
   * Pull typed data out of reader.
   */
  @Nullable <T> T read(@NonNull File file, @NonNull Type type) throws ConverterException;
}
