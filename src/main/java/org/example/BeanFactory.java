package org.example;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BeanFactory {

    //使用 ConcurrentHashMap 保证线程安全，key = beanName，value = BeanDefinition
    private Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>();

    public Object getBean (String name) {
        return beanDefinitionMap.get(name).getBean();
    }

    public void registerBeanDefinition(String name, BeanDefinition beanDefinition) {
        beanDefinitionMap.put(name, beanDefinition);
    }
}
