package springframework.beans.factory;

import springframework.beans.BeansException;

public interface BeanFactoryAware extends Aware{
    void setBeanFactory(BeanFactory beanFactory) throws BeansException;
}
