package springframework.beans.factory;

import springframework.beans.BeansException;
import springframework.beans.PropertyValue;
import springframework.beans.PropertyValues;
import springframework.beans.factory.config.BeanDefinition;
import springframework.beans.factory.config.BeanFactoryPostProcessor;
import springframework.core.io.DefaultResourceLoader;
import springframework.core.io.Resource;

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
                    String strVal = (String) value;
                    StringBuilder buffer = new StringBuilder(strVal);
                    int startIdx = strVal.indexOf(DEFAULT_PLACEHOLDER_PREFIX);
                    int stopIdx = strVal.indexOf(DEFAULT_PLACEHOLDER_SUFFIX);
                    
                    while (startIdx != -1 && stopIdx != -1 && startIdx < stopIdx) {
                        buffer.replace(startIdx, stopIdx + 1, properties.getProperty(strVal.substring(startIdx + 2, stopIdx)));
                        strVal = buffer.toString();
                        startIdx = strVal.indexOf(DEFAULT_PLACEHOLDER_PREFIX);
                        stopIdx = strVal.indexOf(DEFAULT_PLACEHOLDER_SUFFIX);
                    }
                    propertyValues.addPropertyValue(new PropertyValue(propertyValue.getName(), strVal));
                }
            }
        } catch (IOException e) {
            throw new BeansException("Could not load properties from location: " + location, e);
        }
    }
}
