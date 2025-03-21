package springframework.beans.factory.support;

import springframework.beans.BeansException;
import springframework.beans.factory.config.BeanDefinition;

import java.lang.reflect.Constructor;

public interface InstantiationStrategy {
    /**
     *
     * @param beanDefinition 获取将要实例化的类信息
     * @param beanName 将要实例化的类名
     * @param ctor 构造器，即符合对应参数的构造函数
     * @param args 构造器参数
     * @return
     * @throws BeansException
     */
    Object instantiate(BeanDefinition beanDefinition, String beanName, Constructor<?> ctor, Object[] args) throws BeansException;
}
