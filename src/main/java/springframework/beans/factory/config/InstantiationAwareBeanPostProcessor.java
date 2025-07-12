package springframework.beans.factory.config;

import springframework.beans.BeansException;
import springframework.beans.PropertyValues;

public interface InstantiationAwareBeanPostProcessor extends BeanPostProcessor {
    /**
     * 在 Bean 对象实例化之前，执行此方法，可能返回 Proxy Bean
     */
    Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException;
    /**
     * 在 Bean 对象实例化完成后，设置属性操作之前执行此方法
     * 用于扫描注解，将有注解的字段注入 Bean
     */
    PropertyValues postProcessPropertyValues(PropertyValues pvs, Object bean, String beanName) throws BeansException;
    
    
}
