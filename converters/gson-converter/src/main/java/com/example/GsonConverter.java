package com.example;

import com.google.gson.Gson;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;

import au.com.gridstone.grex.converter.Converter;

public class GsonConverter implements Converter {
    private Gson gson;

    public GsonConverter() {
        this(new Gson());
    }

    public GsonConverter(Gson gson) {
        this.gson = gson;
    }

    @Override
    public <T> void write(T data, Writer writer) {
        gson.toJson(data, writer);
    }

    @Override
    public <T> T read(Reader reader, Type type) {
        return gson.fromJson(reader, type);
    }
}
