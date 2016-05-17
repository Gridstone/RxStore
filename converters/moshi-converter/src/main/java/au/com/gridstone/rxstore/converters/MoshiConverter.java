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

package au.com.gridstone.rxstore.converters;

import au.com.gridstone.rxstore.Converter;
import au.com.gridstone.rxstore.ConverterException;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;

public class MoshiConverter implements Converter {
  private final Moshi moshi;

  public MoshiConverter() {
    this(new Moshi.Builder().build());
  }

  public MoshiConverter(Moshi moshi) {
    this.moshi = moshi;
  }

  @Override public <T> void write(T data, Type type, File file) throws ConverterException {
    try {
      JsonAdapter<T> adapter = moshi.adapter(type);
      BufferedSink sink = Okio.buffer(Okio.sink(file));
      adapter.toJson(sink, data);
      sink.close();
    } catch (IOException e) {
      throw new ConverterException(e);
    }
  }

  @Override public <T> T read(File file, Type type) {
    try {
      JsonAdapter<T> adapter = moshi.adapter(type);
      BufferedSource source = Okio.buffer(Okio.source(file));
      T value;

      if (source.exhausted()) {
        value = null;
      } else {
        value = adapter.nullSafe().fromJson(source);
      }

      source.close();
      return value;
    } catch (Exception e) {
      throw new ConverterException(e);
    }
  }
}
