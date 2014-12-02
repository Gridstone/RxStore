package au.com.gridstone.grex.converter;

public class ConverterException extends Exception {
    public ConverterException() {}

    public ConverterException(String detailMessage) {
        super(detailMessage);
    }

    public ConverterException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public ConverterException(Throwable throwable) {
        super(throwable);
    }
}
