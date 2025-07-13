package springframework.core.convert.converter;

public interface Converter<S,T> {
    // Convert the source object of type {@code S} to target type {@code T}.
    T convert(S source);
}
