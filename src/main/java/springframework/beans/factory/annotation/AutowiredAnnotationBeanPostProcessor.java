package springframework.beans.factory.annotation;

import springframework.beans.BeansException;
import springframework.beans.PropertyValues;
import springframework.beans.factory.BeanFactory;
import springframework.beans.factory.BeanFactoryAware;
import springframework.beans.factory.ConfigurableListableBeanFactory;
import springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import springframework.core.convert.ConversionService;
import springframework.utils.BeanUtil;
import springframework.utils.ClassUtils;

import java.lang.reflect.Field;

public class AutowiredAnnotationBeanPostProcessor implements InstantiationAwareBeanPostProcessor, BeanFactoryAware {
    private ConfigurableListableBeanFactory beanFactory;
    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
    }

    @Override
    public PropertyValues postProcessPropertyValues(PropertyValues pvs, Object bean, String beanName) throws BeansException {
        // 处理 @Value
        Class<?> clazz = bean.getClass();
        clazz = ClassUtils.isCglibProxyClass(clazz) ? clazz.getSuperclass() : clazz;

        Field[] declaredFields = clazz.getDeclaredFields();
        
        for (Field field : declaredFields) {
            Value valueAnnotation = field.getAnnotation(Value.class);
            if (valueAnnotation != null) {
                Object value = valueAnnotation.value();
                value = beanFactory.resolveEmbeddedValue((String)value);
                
                // 类型转换
                Class<?> sourceType = value.getClass();
                Class<?> targetType = field.getType();
                ConversionService conversionService = beanFactory.getConversionService();
                if (conversionService != null) {
                    if (conversionService.canConvert(sourceType, targetType)) {
                        value = conversionService.convert(value, targetType);
                    }
                }
                
                try {
                    BeanUtil.setFieldValue(bean, field.getName(), value);
                } catch (NoSuchFieldException e) {
                    throw new BeansException("Failed to set field value for " + field.getName(), e);
                }
            }
        }
        
        // 处理 @Autowired
        for (Field field : declaredFields) {
            Autowired autowiredAnnotation = field.getAnnotation(Autowired.class);
           if (autowiredAnnotation != null) {
               Class<?> fieldType = field.getType();
                String dependentBeanName = null;
                Qualifier qualifierAnnotation = field.getAnnotation(Qualifier.class);
                Object dependentBean = null;
                if (qualifierAnnotation != null) {
                    dependentBeanName = qualifierAnnotation.value();
                    dependentBean = beanFactory.getBean(dependentBeanName, fieldType);
                } else {
                    dependentBean = beanFactory.getBean(fieldType);
                }
                try {
                    BeanUtil.setFieldValue(bean, field.getName(), dependentBean);
                } catch (NoSuchFieldException e) {
                    throw new BeansException("Failed to set field value for " + field.getName(), e);
                }
           }
        }
        
        return pvs;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        return bean;
    }

    @Override
    public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
        return null;
    }
}
