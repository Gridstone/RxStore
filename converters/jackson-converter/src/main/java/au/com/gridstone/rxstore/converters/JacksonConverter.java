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

import au.com.gridstone.rxstore.ConverterException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;

import au.com.gridstone.rxstore.Converter;

/**
 * A {@link Converter} that uses a Jackson {@link ObjectMapper} to get the
 * job done.
 */
public class JacksonConverter implements Converter {
  private ObjectMapper objectMapper;

  public JacksonConverter() {
    this(new ObjectMapper());
  }

  public JacksonConverter(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override public <T> void write(T data, Writer writer) throws ConverterException {
    try {
      objectMapper.writeValue(writer, data);
    } catch (IOException e) {
      throw new ConverterException(e);
    }
  }

  @Override public <T> T read(Reader reader, Type type) throws ConverterException {
    JavaType javaType = objectMapper.getTypeFactory().constructType(type);
    try {
      return objectMapper.readValue(reader, javaType);
    } catch (Exception e) {
      throw new ConverterException(e);
    }
  }
}
