package springframework.beans.factory;

import springframework.beans.BeansException;
import springframework.beans.PropertyValue;
import springframework.beans.PropertyValues;
import springframework.beans.factory.config.BeanDefinition;
import springframework.beans.factory.config.BeanFactoryPostProcessor;
import springframework.core.io.DefaultResourceLoader;
import springframework.core.io.Resource;
import springframework.utils.StringValueResolver;

import java.io.IOException;
import java.util.Properties;

public class PropertyPlaceholderConfigurer implements BeanFactoryPostProcessor {
    
    public static final String DEFAULT_PLACEHOLDER_PREFIX = "${";
    
    public static final String DEFAULT_PLACEHOLDER_SUFFIX = "}";
    
    private String location;
    
    public void setLocation(String location) {
        this.location = location;
    }
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        try {
            DefaultResourceLoader resourceLoader = new DefaultResourceLoader();
            Resource resource = resourceLoader.getResource(location);
            Properties properties = new Properties();
            properties.load(resource.getInputStream());
            
            String[] beanDefinitionNames = beanFactory.getBeanDefinitionNames();
            for (String beanName : beanDefinitionNames) {
                BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);

                PropertyValues propertyValues = beanDefinition.getPropertyValues();
                for (PropertyValue propertyValue : propertyValues.getPropertyValues()) {
                    Object value = propertyValue.getValue();
                    if (!(value instanceof String)) continue;
                    // 解析占位符
                    value = resolvePlaceholder((String) value, properties);
                    // 更新属性值
                    propertyValues.addPropertyValue(new PropertyValue(propertyValue.getName(), value));
                }
            }

            StringValueResolver valueResolver = new PlaceholderResolvingStringValueResolver(properties);
            // 向容器中添加字符串解析器，供解析@Value注解使用
            beanFactory.addEmbeddedValueResolver(valueResolver);
        } catch (IOException e) {
            throw new BeansException("Could not load properties from location: " + location, e);
        }
    }
    
    private String resolvePlaceholder(String value, Properties properties) {
        String strVal = value;
        StringBuilder buffer = new StringBuilder(strVal);
        int startIdx = strVal.indexOf(DEFAULT_PLACEHOLDER_PREFIX);
        int stopIdx = strVal.indexOf(DEFAULT_PLACEHOLDER_SUFFIX);
        while (startIdx != -1 && stopIdx != -1 && startIdx < stopIdx) {
            String propKey = strVal.substring(startIdx + 2, stopIdx);
            String propVal = properties.getProperty(propKey);
            // 如果属性值不存在，则跳过当前占位符
            if (propVal == null) {
                startIdx = strVal.indexOf(DEFAULT_PLACEHOLDER_PREFIX, stopIdx + 1);
                stopIdx = strVal.indexOf(DEFAULT_PLACEHOLDER_SUFFIX, startIdx + 1);
                continue;
            }
            // 替换占位符
            buffer.replace(startIdx, stopIdx + 1, propVal);
            strVal = buffer.toString();
            startIdx = strVal.indexOf(DEFAULT_PLACEHOLDER_PREFIX);
            stopIdx = strVal.indexOf(DEFAULT_PLACEHOLDER_SUFFIX);
        }
        return buffer.toString();
    }
    
    private class PlaceholderResolvingStringValueResolver implements StringValueResolver {
        
        private final Properties properties;
        
        public PlaceholderResolvingStringValueResolver(Properties properties) {
            this.properties = properties;
        }
        @Override
        public String resolveStringValue(String strVal) {
            // 引用外部类实例
            return PropertyPlaceholderConfigurer.this.resolvePlaceholder(strVal, properties);
        }
    }
}
