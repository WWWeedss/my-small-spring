package springframework.core.convert.converter;

public interface ConverterFactory<S, R> {
    /**
     * Return a converter that converts from the source type {@code S} to the target type {@code T}.
     *
     * @param targetType the target type to convert to
     * @param <T>        the target type
     * @return a converter that converts from the source type {@code S} to the target type {@code T}
     */
    <T extends R> Converter<S, T> getConverter(Class<T> targetType);
}
