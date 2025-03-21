### BeanDefinition 和 BeanFactory 初步实现

#### 前置思路

Spring 框架的一大功能就在于用 Bean 去管理类实例的生命周期。大概流程就是：

1. 创建一个类实例，包装成为一个 Bean 对象。
2. 把 Bean 对象放入 Spring 框架的 Bean 管理容器内。
3. 由 Spring 统一进行装配。
   1. Bean 的初始化和属性填充。
   2. 注入 Bean 等。

这个管理容器最后就是要通过 name 去寻找到对应的 Object 实例并进行操作，所以存储的容器就是 HashMap 了。考虑到并发安全性，用 ConcurrentHashMap 作为实例。

#### 具体实现

本步骤完成后的项目结构：

```bash
small-spring-step-01
└── src
    ├── main
       └── java
           └── springframework.beans
               ├── BeanDefinition.java
               └── BeanFactory.java
```

首先我们想办法把类实例包装成 Bean 对象：

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



------

然后再用一个类包装容器 beanMap。

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

------

至此，我们就可以做这些事情：

1. 把任意的类实例包装为 BeanDefinition 实例
2. 存储到 BeanFactory 的 Map 里去。
3. 通过 name 去获取对应实例。

但是这才刚刚开始，可以很明显地看到：

1. Spring 的 Bean 注册是只有 name 的，没有初始化流程。
2. 单例 Bean 的复用怎么解决？

跟我们的这个逻辑并不符合，慢慢来吧！

#### 疑问与思考

##### 既然 BeanFactory 全局唯一，要不要把它做成单例模式？

嗯…… 毕竟要在多个类文件里使用 BeanFactory 的，逻辑上来说是要给它做成单例模式的，而且还得考虑并发安全性。嗯，跟着 small-spring 走吧。项目刚刚开始，这应该并非重点。