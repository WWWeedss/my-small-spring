package springframework.context.support;

import com.sun.istack.internal.Nullable;
import springframework.beans.factory.FactoryBean;
import springframework.beans.factory.InitializingBean;
import springframework.core.convert.ConversionService;
import springframework.core.convert.converter.Converter;
import springframework.core.convert.converter.ConverterFactory;
import springframework.core.convert.converter.ConverterRegistry;
import springframework.core.convert.converter.GenericConverter;
import springframework.core.convert.support.DefaultConversionService;
import springframework.core.convert.support.GenericConversionService;

import java.util.Set;

public class ConversionServiceFactoryBean implements FactoryBean<ConversionService>, InitializingBean {
    @Nullable
    private Set<?> converters;
    
    @Nullable
    private GenericConversionService conversionService;
    
    @Override
    public ConversionService getObject() throws Exception {
        return conversionService;
    }

    @Override
    public Class<?> getObjectType() {
        return conversionService.getClass();
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.conversionService = new DefaultConversionService();
        registerConverters(converters, conversionService);
    }
    
    private void registerConverters(Set<?> converters, ConverterRegistry registry) {
        if (converters != null) {
            for (Object converter : converters) {
                if (converter instanceof GenericConverter) {
                    registry.addConverter((GenericConverter) converter);
                } else if (converter instanceof Converter<?, ?>) {
                    registry.addConverter((Converter<?, ?>) converter);
                } else if (converter instanceof ConverterFactory<?, ?>) {
                    registry.addConverterFactory((ConverterFactory<?, ?>) converter);
                } else {
                    throw new IllegalArgumentException("Unknown converter type: " + converter.getClass().getName());
                }
            }
        }
    } 
    
    public void setConverters(Set<?> converters) {
        this.converters = converters;
    }
}
