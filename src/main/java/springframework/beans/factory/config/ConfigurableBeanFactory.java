package springframework.beans.factory.config;

import org.jetbrains.annotations.Nullable;
import springframework.beans.factory.HierarchicalBeanFactory;
import springframework.core.convert.ConversionService;
import springframework.utils.StringValueResolver;

public interface ConfigurableBeanFactory extends HierarchicalBeanFactory, SingletonBeanRegistry {
    String SCOPE_SINGLETON = "singleton";

    String SCOPE_PROTOTYPE = "prototype";

    void addBeanPostProcessor(BeanPostProcessor beanPostProcessor);

    // 销毁单例对象
    void destroySingletons();
    
    // 添加一个字符串解析器，用于处理注解标记的配置注入
    void addEmbeddedValueResolver(StringValueResolver valueResolver);
    
    // 解析${}嵌入的值
    String resolveEmbeddedValue(String value);


    /**
     * Specify a Spring 3.0 ConversionService to use for converting
     * property values, as an alternative to JavaBeans PropertyEditors.
     * @since 3.0
     */
    void setConversionService(ConversionService conversionService);

    /**
     * Return the associated ConversionService, if any.
     * @since 3.0
     */
    @Nullable
    ConversionService getConversionService();
}
