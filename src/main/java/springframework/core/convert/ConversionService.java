package springframework.core.convert;

import org.jetbrains.annotations.Nullable;

public interface ConversionService {
    boolean canConvert(@Nullable Class<?> sourceType, Class<?> targetType);
    
    <T> T convert(Object source, Class<T> targetType);
}
