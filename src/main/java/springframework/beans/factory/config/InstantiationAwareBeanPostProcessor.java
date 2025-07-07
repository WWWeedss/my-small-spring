package springframework.beans.factory.config;

import springframework.beans.BeansException;

public interface InstantiationAwareBeanPostProcessor extends BeanPostProcessor {
    /**
     * 在 Bean 对象实例化之前，执行此方法
     */
    Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException;
}
