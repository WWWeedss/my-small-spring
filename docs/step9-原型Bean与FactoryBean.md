### 原型 Bean 与 FactoryBean

#### 前置思路

之前在 ConfigurableBeanFactory 我们留下了多种 Bean 类型的铺垫，如今是时候回收了，除了单例 Bean 之外，我们会新增一个原型 Bean。原型 Bean 就是不存在全局的 map 里，每次 get 都新建一个。原型 Bean 在哪里会被使用呐？为啥我们需要它？可以看看[我的提问 - 原型 Bean 有啥用](#原型 Bean 在哪里会被使用？)。

这件事情很简单，另外我们需要实现 FactoryBean。这是个啥玩意呢，就是可以让用户自定义实例化 Bean 的过程，设置一个接口让用户实现，抛出一个 Object，然后 Spring 框架内存储的 BeanName 对应的就是这个接口返回的对象……

你肯定有疑问了，我们用 Spring 不就是懒得实例化对象？这这这又把实例化对象的过程抛回来，意义何在啊？没办法，历史总是螺旋上升的，我们把软件做得越来越易用，用户反而开始越来越对自定义和个性化有要求，有需求就得实现。不过虽然说是“螺旋”，但“上升”也是肯定有的，我们可以从应用实例上分析一下它的意义，参考[我的提问 - 关于 FactoryBean 的讨论](#这 FactoryBean 岂不是把实例化 Bean 的操作又交还给用户啦？)。

#### 具体实现

老规矩，上项目结构。

```bash
my-small-spring
├── LICENSE
├── README.md
├── pom.xml
├── src
│   ├── main
│   │   ├── java
│   │   │   └── springframework
│   │   │       ├── beans
│   │   │       │   ├── BeansException.java
│   │   │       │   ├── PropertyValue.java
│   │   │       │   ├── PropertyValues.java
│   │   │       │   └── factory
│   │   │       │       ├── Aware.java
│   │   │       │       ├── BeanFactory.java 
│   │   │       │       ├── BeanFactoryAware.java
│   │   │       │       ├── BeanNameAware.java
│   │   │       │       ├── ConfigurableListableBeanFactory.java
│   │   │       │       ├── DisposableBean.java
│   │   │       │       ├── FactoryBean.java --new
│   │   │       │       ├── HierarchicalBeanFactory.java
│   │   │       │       ├── InitializingBean.java
│   │   │       │       ├── ListableBeanFactory.java
│   │   │       │       ├── config
│   │   │       │       │   ├── AutowireCapableBeanFactory.java
│   │   │       │       │   ├── BeanDefinition.java --change
│   │   │       │       │   ├── BeanFactoryPostProcessor.java
│   │   │       │       │   ├── BeanPostProcessor.java
│   │   │       │       │   ├── BeanReference.java
│   │   │       │       │   ├── ConfigurableBeanFactory.java 
│   │   │       │       │   └── SingletonBeanRegistry.java
│   │   │       │       ├── support
│   │   │       │       │   ├── AbstractAutowireCapableBeanFactory.java --change
│   │   │       │       │   ├── AbstractBeanDefinitionReader.java
│   │   │       │       │   ├── AbstractBeanFactory.java
│   │   │       │       │   ├── BeanDefinitionReader.java
│   │   │       │       │   ├── BeanDefinitionRegistry.java
│   │   │       │       │   ├── CglibSubclassingInstantiationStrategy.java
│   │   │       │       │   ├── DefaultListableBeanFactory.java
│   │   │       │       │   ├── DefaultSingletonBeanRegistry.java
│   │   │       │       │   ├── DisposableBeanAdapter.java
│   │   │       │       │   ├── FactoryBeanRegistrySupport.java --new
│   │   │       │       │   ├── InstantiationStrategy.java
│   │   │       │       │   └── SimpleInstantiationStrategy.java
│   │   │       │       └── xml
│   │   │       │           └── XmlBeanDefinitionReader.java --change
│   │   │       ├── context
│   │   │       │   ├── ApplicationContext.java
│   │   │       │   ├── ApplicationContextAware.java
│   │   │       │   ├── ConfigurableApplicationContext.java
│   │   │       │   └── support
│   │   │       │       ├── AbstractApplicationContext.java
│   │   │       │       ├── AbstractRefreshableApplicationContext.java
│   │   │       │       ├── AbstractXmlApplicationContext.java
│   │   │       │       ├── ApplicationContextAwareProcessor.java
│   │   │       │       └── ClassPathXmlApplicationContext.java
│   │   │       ├── core
│   │   │       │   └── io
│   │   │       │       ├── ClassPathResource.java
│   │   │       │       ├── DefaultResourceLoader.java
│   │   │       │       ├── FileSystemResource.java
│   │   │       │       ├── Resource.java
│   │   │       │       ├── ResourceLoader.java
│   │   │       │       └── UrlResource.java
│   │   │       └── utils
│   │   │           └── BeanUtil.java
│   │   └── resources
│   │       ├── important.properties
│   │       ├── spring.xml
│   │       └── springPostProcessor.xml
│   └── test
│       └── java
│           ├── ApiTest.java
│           ├── bean
│           │   ├── UserDao.java
│           │   └── UserService.java
│           └── common
│               ├── MyBeanFactoryPostProcessor.java
│               └── MyBeanPostProcessor.java
```

---

##### 增加原型 Bean

回顾一下 ConfigurableBeanFactory：

```java
public interface ConfigurableBeanFactory extends HierarchicalBeanFactory, SingletonBeanRegistry {
    String SCOPE_SINGLETON = "singleton";

    String SCOPE_PROTOTYPE = "prototype";

    ……
}
```

然后开始改 BeanDefinition，在里面添加 singleton 和 prototype 属性。

```java
public class BeanDefinition {

    String SCOPE_SINGLETON = ConfigurableBeanFactory.SCOPE_SINGLETON;

    String SCOPE_PROTOTYPE = ConfigurableBeanFactory.SCOPE_PROTOTYPE;

    private String scope = SCOPE_SINGLETON;

    private boolean singleton = true;

    private boolean prototype = false;

    //保持封装，让外界的调用操作尽可能简单。
    public void setScope(String scope) {
        this.scope = scope;
        this.singleton = SCOPE_SINGLETON.equals(scope);
        this.prototype = SCOPE_PROTOTYPE.equals(scope);
    }

    public boolean isSingleton() {
        return singleton;
    }

    public boolean isPrototype() {
        return prototype;
    }
}
```

---

然后在解析 xml 的时候把这一块加上去。

XmlBeanDefinitionReader#parseBeanElement

```java
 protected void parseBeanElement(Element bean) throws ClassNotFoundException {
        ……
        // 创建 BeanDefinition
        beanDefinition.setScope(bean.getAttribute("scope"));

        // 注册 BeanDefinition
        getRegistry().registerBeanDefinition(beanName, beanDefinition);
    }
```

---

我们已经有了属性之后，在创建 Bean 时就要进行判断，走不同的代码逻辑，这里当然可以用策略模式，但是当前只有两个选项，所以暂且放下。

```java
@Override
protected Object createBean(String beanName, BeanDefinition beanDefinition, Object[] args) throws BeansException {
    Object bean;
    ……
    // 判断 SCOPE_SINGLETON、SCOPE_PROTOTYPE
        //只有单例 Bean 才需要存储到 map 之中
    if (beanDefinition.isSingleton()) {
        addSingleton(beanName, bean);
    }
    return bean;
}
```

还有销毁 Bean，这里是注册销毁方法的函数，只有单例 Bean 才需要销毁，所以非单例 Bean 直接返回。至于为什么，详见[我的提问 - 非单例 Bean 为啥不需要销毁](#为啥非单例 Bean 就不需要执行销毁方法呢？)。

```java
protected void registerDisposableBeanIfNecessary(String beanName, Object bean, BeanDefinition beanDefinition){
    // 非 Singleton 类型的 Bean 不执行销毁方法
    if (!beanDefinition.isSingleton()) return;

    if (bean instanceof DisposableBean || StrUtil.isNotEmpty(beanDefinition.getDestroyMethodName())){
        registerDisposableBean(beanName, new DisposableBeanAdapter(bean, beanName, beanDefinition));
    }
}
```

---

##### 增加 FactoryBean

先定义 FactoryBean 的接口。

```java
public interface FactoryBean<T>{
    T getObject() throws Exception;

    Class<?> getObjectType();

    boolean isSingleton();
}
```

这个 getObject 将会在创建 Bean 的时候调用，取这个返回的 Object。

----

然后我们就要将注册 FactoryBean 的逻辑添加进 BeanFactory（有没有搞混？分分清楚 FactoryBean 和 BeanFactory 哦！）。

既然是单独的逻辑，我们不妨开一个类放到继承链中，那这个类得放在哪？详见[我的提问 - FactoryBean 注册器在继承链上的位置](#FactoryBean 注册器在继承链上的位置)。

```java
public class FactoryBeanRegistrySupport extends DefaultSingletonBeanRegistry{
    //这就是内存中存的 FactoryBean 对象
    private final Map<String, Object> factoryBeanObjectCache = new ConcurrentHashMap<>();

    //从内存里取对象
    protected Object getCachedObjectForFactoryBean(String beanName) {
        Object object = this.factoryBeanObjectCache.get(beanName);
        return (object != NULL_OBJECT ? object : null);
    }

    
    protected Object getObjectFromFactoryBean(FactoryBean factory, String beanName) {
        //如果是单例，就存到内存里
        if (factory.isSingleton()) {
            Object object = this.factoryBeanObjectCache.get(beanName);
            if (object == null) {
                object = doGetObjectFromFactoryBean(factory, beanName);
                this.factoryBeanObjectCache.put(beanName, (object != null ? object : NULL_OBJECT));
            }
            return (object != NULL_OBJECT ? object : null);
        } else {
            return doGetObjectFromFactoryBean(factory, beanName);
        }
    }

    //调用 getObject 去获取对象，说实话这个 factory 的命名是有讲究的，哈，因为都是在“创建对象”
    private Object doGetObjectFromFactoryBean(final FactoryBean factory, final String beanName) {
        try {
            return factory.getObject();
        } catch (Exception e) {
            throw new BeansException("FactoryBean threw exception on object[" + beanName + "] creation", e);
        }
    }
}
```

---

顺理成章地，我们来修改 getBean 的逻辑。

AbstractBeanFactory#doGetBean

```java
protected <T> T doGetBean(final String name, final Object[] args) {
    Object sharedInstance = getSingleton(name);
    if (sharedInstance != null) {
        // 如果是 FactoryBean，则需要调用 FactoryBean#getObject
        return (T) getObjectForBeanInstance(sharedInstance, name);
    }

    BeanDefinition beanDefinition = getBeanDefinition(name);
    Object bean = createBean(name, beanDefinition, args);
    return (T) getObjectForBeanInstance(bean, name);
}

//这就是专门为 FactoryBean 做的逻辑
private Object getObjectForBeanInstance(Object beanInstance, String beanName) {
    if (!(beanInstance instanceof FactoryBean)) {
        return beanInstance;
    }

    Object object = getCachedObjectForFactoryBean(beanName);

    if (object == null) {
        FactoryBean<?> factoryBean = (FactoryBean<?>) beanInstance;
        object = getObjectFromFactoryBean(factoryBean, beanName);
    }

    return object;
}
```

##### 测试部分

之前咱为了让 Cglib 适配 java17 还新建了一个 BeanUtil 类，现在算是彻底没辙了，还是得换 java8，不然报异常搞不定。

首先我们删去 UserDao，创建一个 IUserDao。

```java
public interface IUserDao {

    String queryUserName(String uId);

}
```

UserService 里的 UserDao 对象也改掉。

```java
public class UserService  {
    private IUserDao userDao;
}
```

然后我们来创建 FactoryBean 对象。关于这个 InvocationHandler，见[其他相关 - InvocationHandler 解析](#InvocationHandler 解析)。在这里的作用就是调用返回的 IUserDao 的任何方法，都会直接转而调用这个 handler 中定义的代理方法。

```java
public class ProxyBeanFactory implements FactoryBean<IUserDao> {
    @Override
    public IUserDao getObject() throws Exception {
        InvocationHandler handler = (proxy, method, args) -> {

            if ("toString".equals(method.getName())) return this.toString();

            Map<String, String> hashMap = new HashMap<>();
            hashMap.put("10001", "WWWeeds");
            hashMap.put("10002", "八杯水");
            hashMap.put("10003", "阿毛");

            return "你被代理了 " + method.getName() + "：" + hashMap.get(args[0].toString());
        };
        return (IUserDao) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[]{IUserDao.class}, handler);
    }

    @Override
    public Class<?> getObjectType() {
        return IUserDao.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
```

配置文件：顺便测试一下 prototype

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans>

    <bean id="userService" class="bean.UserService" scope="prototype">
        <property name="uId" value="10001"/>
        <property name="company" value="腾讯"/>
        <property name="location" value="深圳"/>
        <property name="userDao" ref="proxyUserDao"/>
    </bean>

    <bean id="proxyUserDao" class="bean.ProxyBeanFactory"/>

</beans>
```

测试文件：第一个方法测试创建的对象是否一致，第二个方法测试 Factory 是否正常地返回了代理对象。

```java
public class ApiTest {

    @Test
    public void test_prototype() {
        // 1.初始化 BeanFactory
        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:spring.xml");
        applicationContext.registerShutdownHook();

        // 2. 获取Bean对象调用方法
        UserService userService01 = applicationContext.getBean("userService", UserService.class);
        UserService userService02 = applicationContext.getBean("userService", UserService.class);

        // 3. 配置 scope="prototype/singleton"
        System.out.println(userService01);
        System.out.println(userService02);
    }

    @Test
    public void test_factory_bean() {
        // 1.初始化 BeanFactory
        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:spring.xml");
        applicationContext.registerShutdownHook();
        // 2. 调用代理方法
        UserService userService = applicationContext.getBean("userService", UserService.class);
        System.out.println("测试结果：" + userService.queryUserInfo());
    }
}
```

本节完结撒花~

#### 疑惑与思考

##### 为啥非单例 Bean 就不需要执行销毁方法呢？

这个是因为原型 Bean 的生命周期是不由 Spring 容器管理的，Spring 容器压根都没有持有原型 Bean 的引用，想销毁也没地方销毁。

##### 这 FactoryBean 岂不是把实例化 Bean 的操作又交还给用户啦？

确实。但这就提供了一个在 Bean 粒度上自定义操作的接口，在这之上可以加代理、加条件之类的，之前我们的 PostProcessor 这些自定义接口，都是在 BeanFactory 粒度的。

典型的一个应用实例是 Mybatis 框架的 SqlSessionFactoryBean，有机会再详细阐释。

##### 原型 Bean 在哪里会被使用？

最常见的应用场景就是有状态的 Bean 实例。在多线程环境下，要避免并发写，使用原型 Bean，就可以达到一个简单的快照效果。

##### FactoryBean 注册器在继承链上的位置

之前我们提到过，在执行逻辑上越靠前的，应该放在继承链的下层。FactoryBean 的判断逻辑是这样的，创建 BeanDefinition，根据 BeanDefinition 创建 Bean，然后如果这个创建出的 Bean 是 FactoryBean（实现了 FactoryBean 的接口），那就在 getBean 时返回 FactoryBean.getObject() 方法创建的对象，否则直接返回该 Bean。

所以，FactoryBean 注册器应当是“拥有 getBean 方法的类”的父类。实际中，就是 AbstractBeanFactory 的父类。

#### 其他相关

##### InvocationHandler 解析

这玩意是个方法拦截器，

三个参数：

- proxy，即正在被代理的对象。
- method，即被调用的方法。
- args，即传给方法的参数。

它会直接替换对原来方法的调用。如果想要调用原来的方法，就用 method.invoke() 去调用。

注意，这个拦截方法的返回值类型，必须和调用方法的类型兼容才可以。

比如说我再定义一个方法：

```java
public interface IUserDao {
    String queryUserName(String uId);
    Integer a(String a);
}
```

在代码中调用

```java
public String queryUserInfo() {
    return userDao.a(uId) + "," + company + "," + location;
}
```

这时就会报错 ClassCastException: String（这是 invoke 拦截方法的返回值类型） cannot be cast to Integer（这是 a 方法的返回值类型）。

如果把 a 方法返回值改为 String，则一切正常。
