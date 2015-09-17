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

package au.com.gridstone.rxstore.converters;

import com.google.gson.Gson;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;

import au.com.gridstone.rxstore.Converter;

/**
 * A {@link Converter} that uses {@link Gson} to get the job done.
 */
public class GsonConverter implements Converter {
  private Gson gson;

  public GsonConverter() {
    this(new Gson());
  }

  public GsonConverter(Gson gson) {
    this.gson = gson;
  }

  @Override public <T> void write(T data, Writer writer) {
    gson.toJson(data, writer);
  }

  @Override public <T> T read(Reader reader, Type type) {
    return gson.fromJson(reader, type);
  }
}
