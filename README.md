# my-small-spring

学习 small-spring 和 tiny-spring，尝试自己从头写一个简单的 spring 框架，并撰写开发日志。

## 开发日志

### BeanDefinition 和 BeanFactory 初步实现

#### 前置思考

Spring 框架的一大功能就在于用 Bean 去管理类实例的生命周期。大概流程就是：

1. 创建一个类实例，包装成为一个 Bean 对象。
2. 把 Bean 对象放入 Spring 框架的 Bean 管理容器内。
3. 由 Spring 统一进行装配。
   1. Bean 的初始化和属性填充。
   2. 注入 Bean 等。

这个管理容器最后就是要通过 name 去寻找到对应的 Object 实例并进行操作，所以存储的容器就是 HashMap 了。

#### 具体实现

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

---

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

---

至此，我们就可以做这些事情：

1. 把任意的类实例包装为 BeanDefinition 实例
2. 存储到 BeanFactory 的 Map 里去。
3. 通过 name 去获取对应实例。

但是这才刚刚开始，可以很明显地看到

1. Spring 的 Bean 注册是只有 name 的，没有初始化流程。
2. 单例 Bean 的复用怎么解决？

跟我们的这个逻辑并不符合，慢慢来吧！

#### 疑问与思考

##### 既然 BeanFactory 全局唯一，要不要把它做成单例模式？

嗯…… 毕竟要在多个类文件里使用 BeanFactory 的，逻辑上来说是要给它做成单例模式的，而且还得考虑并发安全性。嗯，跟着 small-spring 走吧。项目刚刚开始，这应该并非重点。

### BeanDefinition 存储 Class 信息，Factory 返回单例 Bean

#### 前置思考

上文提到，我们得实现这两个功能：

1. 把类实例化的逻辑交给框架来做，BeanDefinition 只存储 Class 信息而不存储类的实例。
2. 复用已有的 Bean（一般来说是复用单例 Bean，但是这里我们先假设所有的 Bean 都可以复用）。

但说实话，这一步的设计实在是有些跳跃，我只能尽量理解。

#### 具体实现

既然把类实例化的逻辑交给框架做，那么就可能抛出实例化异常。此外，还有找不到对应 name 的 bean 的异常等，我们创建一个 Exception 类统一处理。

增加 BeansException：

```java
public class BeansException extends RuntimeException {

    public BeansException(String msg) {
        super(msg);
    }

    public BeansException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
```

---

修改 BeanDefinition：

```java
@SuppressWarnings({"rawtypes"})
public class BeanDefinition {
    //原来存储的是 Object 实例，现在存储的是 Class 了。
    private Class beanClass;

    public BeanDefinition(Class beanClass) {
        this.beanClass = beanClass;
    }

    public Class getBeanClass() {
        return beanClass;
    }

    public void setBeanClass(Class beanClass) {
        this.beanClass = beanClass;
    }
}
```

---

将原来的 BeanFactory 拆分为 BeanFactory (getBean) 和 BeanDefinitionRegistry (registerBeanDefinition) 两个接口。

```java
public interface BeanFactory {
    Object getBean(String name) throws BeansException;
}
```

```java
public interface BeanDefinitionRegistry {
    void registerBeanDefinition(String beanName, BeanDefinition beanDefinition);
}
```

---

单例的处理：

```java
public interface SingletonBeanRegistry {
    Object getSingleton(String beanName);
}
```

```java
public class DefaultSingletonBeanRegistry implements SingletonBeanRegistry {
    //对于单例的 Bean 来说，全局只有一个实例对象，因此我们只需要存储 Object 即可。
        private final Map<String, Object> singletonObjects = new HashMap<>();
        @Override
        public Object getSingleton(String beanName) {
            return singletonObjects.get(beanName);
        }

        protected void addSingleton(String beanName, Object singletonObject) {
            singletonObjects.put(beanName, singletonObject);
        }
}
```

---

BeanFactory 具体实现，是多层级的。

```java
public abstract class AbstractBeanFactory extends DefaultSingletonBeanRegistry implements BeanFactory {

    @Override
    public Object getBean(String name) throws BeansException {
        //现在默认所有的 Bean 都是单例
        Object bean = getSingleton(name);
        if (bean != null) {
            return bean;
        }
		//如果找不到，那就创建一个，这就把实例化的过程放在框架内部了
        BeanDefinition beanDefinition = getBeanDefinition(name);
        return createBean(name, beanDefinition);
    }

    //这两个方法让子类去实现
    protected abstract BeanDefinition getBeanDefinition(String beanName) throws BeansException;

    protected abstract Object createBean(String beanName, BeanDefinition beanDefinition) throws BeansException;
}
```

```java
public abstract class AbstractAutowireCapableBeanFactory extends AbstractBeanFactory {
    @Override
    protected Object createBean(String beanName, BeanDefinition beanDefinition) throws BeansException {
        Object bean;
        try {
            // 获取 beanDefinition 中的 beanClass，使用反射机制将它实例化
            bean = beanDefinition.getBeanClass().newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new BeansException("Instantiation of bean failed", e);
        }

        addSingleton(beanName, bean);
        return bean;
    }
}
```

```java
public class DefaultListableBeanFactory extends AbstractAutowireCapableBeanFactory implements BeanDefinitionRegistry {

    private final Map<String, BeanDefinition> beanDefinitionMap = new HashMap<>();
    @Override
    //注册这个事情现在还是用户去干的，以后得把它用扫描搞定
    public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition) {
        beanDefinitionMap.put(beanName, beanDefinition);
    }

    @Override
    public BeanDefinition getBeanDefinition(String beanName) throws BeansException {
        BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
        if (beanDefinition == null) {
            throw new BeansException("No bean named '" + beanName + "' is defined");
        }
        return beanDefinition;
    }
}
```

至此，我们就可以想象现在这个框架执行的流程了：

1. 用户用 String 和 Class 去注册一个 BeanDefinition。
2. 用户尝试 getBean。
3. 框架发现没有对应 Bean 的实例，创建一个，放到 Singleton 的 Map 里去。
4. 框架返回新创建的 Bean 实例。
5. 用户再次尝试 getBean。
6. 返回已经创建好的原来的实例。

---

测试文件：

```java
@Test
public void test_BeanFactory(){
    // 1.初始化 BeanFactory
    DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

    // 2.注册 bean
    BeanDefinition beanDefinition = new BeanDefinition(UserService.class);
    beanFactory.registerBeanDefinition("userService", beanDefinition);

    // 3.第一次获取 bean，此时框架执行了实例化
    UserService userService = (UserService) beanFactory.getBean("userService");
    userService.queryUserInfo();

    // 4.第二次获取 bean from Singleton
    UserService userService_singleton = (UserService) beanFactory.getSingleton("userService");
    userService_singleton.queryUserInfo();
    

    //我们此时的实现是默认所有的 bean 都可以复用，所以两次返回的是同一个实例
    assert userService == (UserService) beanFactory.getBean("userService");
}
```

#### 疑问与思考

##### 为什么 getSingleton() 方法不抛出异常？

```java
@Override
public Object getSingleton(String beanName) {
    return singletonObjects.get(beanName);
}
```

这里没有抛出异常，因为我们需要在 getBean 的时候判断，外部收到 null 就可以处理了，而不是得去 catch Exception。

```java
public Object getBean(String name) throws BeansException {
    Object bean = getSingleton(name);
    //这里要是抛个异常，逻辑就变得复杂了
    if (bean != null) {
        return bean;
    }
    BeanDefinition beanDefinition = getBeanDefinition(name);
    return createBean(name, beanDefinition);
}
```

##### 为啥要设计得这么复杂……

从流程来看，复用 Bean 和修改 BeanDefinition 只需要把 BeanFactory 修改掉，里面存两个 Map 就好。但是这搞了一堆的接口、实现、继承……

不知道，我估计原因是大家都照着 Spring 源码去写的，所以后序的拓展（譬如非单例的 Bean）就比较明确，于是提前设计了这么一大坨。

##### 为啥这里又可以用 HashMap，而不是 ConcurrentHashMap 了？

DefaultListableBeanFactory 存储的是 name 到 Class 的映射。

DefaultSingletonBeanRegistry 存储的是 name 到单例 Object 的映射。

在完善的 Spring 框架下，类信息和单例 Object 应当是框架扫描完就自动生成了，这俩玩意在运行时大概都不需要去修改（Write）。那多线程去读，这就没有并发问题了。
