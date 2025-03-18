# my-small-spring
学习 small-spring 和 tiny-spring，尝试自己从头写一个简单的 spring 框架，并撰写开发日志。

# 开发日志

## Day1

### Bean 的包装类以及全局 Bean 管理类

#### 实现思路

Spring 框架的一大功能就在于用 Bean 去管理类实例的生命周期。大概流程就是：

1. 创建一个类实例，包装成为一个 Bean 对象。
2. 把 Bean 对象放入 Spring 框架的 Bean 管理容器内。
3. 由 Spring 统一进行装配。
   1. Bean 的初始化和属性填充。
   2. 注入 Bean 等。

这个管理容器最后就是要通过 name 去寻找到对应的 Object 实例并进行操作，所以存储的容器就是 HashMap 了，为了保证并发安全性，使用 ConcurrentHashMap。

#### 具体实现

```java
//BeanDefiniton，包装类，将 Object 实例包装为 Bean 对象。
public class BeanDefinition {

    private Object bean;

    public BeanDefinition(Object bean) {
        this.bean = bean;
    }

    public Object getBean() {
        return bean;
    }
}
```

```java
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
```

至此，我们就可以把任意的类实例包装为 BeanDefinition 实例，并且存储到 BeanFactory 的 Map 里去。然后就可以通过 name 去获取对应实例了。

#### 疑问与思考

##### 既然 BeanFactory 全局唯一，要不要把它做成单例模式？

嗯…… 毕竟要在多个类文件里使用 BeanFactory 的，逻辑上来说是要给它做成单例模式的，而且还得考虑并发安全性。但是毕竟才刚刚开始，先走一走吧。

## Day2

## Day3

# 项目结构
