### BeanDefinition 存储 Class 信息，Factory 返回单例 Bean

#### 前置思路

上文提到，我们得实现这两个功能：

1. 把类实例化的逻辑交给框架来做，BeanDefinition 只存储 Class 信息而不存储类的实例。
2. 复用已有的 Bean（一般来说是复用单例 Bean，但是这里我们先假设所有的 Bean 都可以复用）。

但说实话，这一步的设计实在是有些跳跃，我只能尽量理解。

#### 具体实现

本步骤完成后的项目结构：

![](https://typora-images-gqy.oss-cn-nanjing.aliyuncs.com/image-20250321102001527.png)

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

#### 从 Spring 看设计模式

从流程来看，复用 Bean 和修改 BeanDefinition 只需要把 BeanFactory 修改掉，里面存两个 Map 就好。但是这搞了一堆的接口、实现、继承……

上一个 step 还只有两个类，现在一下就暴增起来。

我们来看一看其中都用了啥模式，有什么道理。

##### 工厂方法

AbstractBeanFactory，让子类去实现 createBean。

现在仅有一个 AbstractAutowireCapableBeanFactory 去生成单例 Bean 并放入 Map 里面，后续可以增加更多的实现类去生成非单例 Bean 或者啥啥，而不需要修改现有的类。这就遵守了开闭原则。

##### 策略模式？依赖倒置？

嗯…… 虽然定义了许多的接口，但是方法实现类都仅有一个，而且我也想象不出会有其他的实现，策略模式还看不出来。

使用的时候 Client 调用的是抽象的接口，这能算是依赖倒置么？即使 getBean() 的实现修改了，因为 Client 依赖的是抽象的接口，所以不用修改相关代码是吗？这有些道理。

这个坑就留在这边，也许后面能看到 BeanFactory、BeanDefinitionRegistry 之类的接口更多的作用。

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

##### 为啥这里又可以用 HashMap，而不是 ConcurrentHashMap 了？

DefaultListableBeanFactory 存储的是 name 到 Class 的映射。

DefaultSingletonBeanRegistry 存储的是 name 到单例 Object 的映射。

在完善的 Spring 框架下，类信息和单例 Object 应当是框架扫描完就自动生成了，这俩玩意在运行时大概都不需要去修改（Write）。那多线程去读，这就没有并发问题了。