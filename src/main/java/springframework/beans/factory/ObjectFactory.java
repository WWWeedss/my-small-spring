package springframework.beans.factory;

import springframework.beans.BeansException;

public interface ObjectFactory<T> {
    T getObject() throws BeansException;
}
