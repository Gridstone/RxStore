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
import com.google.gson.Gson;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;

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

  @Override public <T> void write(T data, Type type, File file) throws ConverterException {
    try {
      Writer writer = new FileWriter(file);
      gson.toJson(data, type, writer);
      writer.close();
    } catch (Exception e) {
      throw new ConverterException(e);
    }
  }

  @Override public <T> T read(File file, Type type) throws ConverterException {
    try {
      Reader reader = new FileReader(file);
      T value = gson.fromJson(reader, type);
      reader.close();
      return value;
    } catch (Exception e) {
      throw new ConverterException(e);
    }
  }
}
