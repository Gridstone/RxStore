package au.com.gridstone.grex.converter;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;

public interface Converter {
    public <T> void write(T data, Writer writer) throws ConverterException;

    public <T> T read(Reader reader, Type type) throws ConverterException;
}
