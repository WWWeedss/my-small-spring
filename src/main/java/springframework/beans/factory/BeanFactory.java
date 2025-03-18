package springframework.beans.factory;

import springframework.beans.BeansException;

public interface BeanFactory {
    Object getBean(String name) throws BeansException;
}
