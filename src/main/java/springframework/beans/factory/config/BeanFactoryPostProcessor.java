package springframework.beans.factory.config;

import springframework.beans.BeansException;
import springframework.beans.factory.ConfigurableListableBeanFactory;

public interface BeanFactoryPostProcessor {
    /**
     * 在 BeanDefinition 加载完成后，实例化 Bean 对象之前，调用此方法，可以对 BeanDefinition 进行一些修改
     */
    void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException;
}
