package com.example;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;

import au.com.gridstone.grex.converter.Converter;
import au.com.gridstone.grex.converter.ConverterException;

public class JacksonConverter implements Converter {

    private ObjectMapper objectMapper;

    public JacksonConverter() {
        this(new ObjectMapper());
    }

    public JacksonConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public <T> void write(T data, Writer writer) throws ConverterException {
        try {
            objectMapper.writeValue(writer, data);
        } catch (IOException e) {
            throw new ConverterException(e);
        }
    }

    @Override
    public <T> T read(Reader reader, Type type) throws ConverterException {
        JavaType javaType = objectMapper.getTypeFactory().constructType(type);
        try {
            return objectMapper.readValue(reader, javaType);
        } catch (Exception e) {
            throw new ConverterException(e);
        }
    }
}
