### Spring Event 的实现

#### 前置思路

event 事件机制是一个在各种开发场景中被广泛使用的特性。亲爱的 Java 同学，还记得 metaQ/RakkitMQ 等等消息队列中间件的作用八股吗？

1. 异步执行，减少 RT 用户感知
2. 流量削峰填谷
3. 解耦服务

第二点其实是“队列”的作用，需要一个数据结构来把待消费的消息存储下来。第一点和第三点的差别就是是否新建线程去消费这个消息，逻辑上差别不大，但是要考虑的问题却会多很多，所以今天就先用同步无队列的消费来实现 event 机制。

我们的框架要负责的事情就是定义好 publishEvent 以及 EventListener 的接口，理想的用户调用情况是这样的：

1. 用户自定义一个 CustomEvent，存储一些数据。
2. 用户自定义一个 EventListener 并实现一个处理消息的接口，监听自定义的 Event。
3. 用户在程序随机点位 publishEvent(CustomEvent)，这样就可以发布一条信息出来。

那么我们要做什么呢：

1. 定义可继承的 Event。
2. 定义可实现的 Listener Interface。
3. 定义 Publisher 并发布事件，把事件传递给监听该事件的 Listener。
4. 在 Context 中注册 Publisher 和 Listener。  

![image-20250702073938345](https://typora-images-gqy.oss-cn-nanjing.aliyuncs.com/image-20250702073938345.png)

> 同步的 event 实现机制

#### 具体实现

```bash
src
├── main
│   ├── java
│   │   └── springframework
│   │       ├── beans
│   │       │   ├── BeansException.java
│   │       │   ├── PropertyValue.java
│   │       │   ├── PropertyValues.java
│   │       │   └── factory
│   │       │       ├── Aware.java
│   │       │       ├── BeanFactory.java
│   │       │       ├── BeanFactoryAware.java
│   │       │       ├── BeanNameAware.java
│   │       │       ├── ConfigurableListableBeanFactory.java
│   │       │       ├── DisposableBean.java
│   │       │       ├── FactoryBean.java
│   │       │       ├── HierarchicalBeanFactory.java
│   │       │       ├── InitializingBean.java
│   │       │       ├── ListableBeanFactory.java
│   │       │       ├── config
│   │       │       │   ├── AutowireCapableBeanFactory.java
│   │       │       │   ├── BeanDefinition.java
│   │       │       │   ├── BeanFactoryPostProcessor.java
│   │       │       │   ├── BeanPostProcessor.java
│   │       │       │   ├── BeanReference.java
│   │       │       │   ├── ConfigurableBeanFactory.java
│   │       │       │   └── SingletonBeanRegistry.java --change
│   │       │       ├── support
│   │       │       │   ├── AbstractAutowireCapableBeanFactory.java
│   │       │       │   ├── AbstractBeanDefinitionReader.java
│   │       │       │   ├── AbstractBeanFactory.java
│   │       │       │   ├── BeanDefinitionReader.java
│   │       │       │   ├── BeanDefinitionRegistry.java
│   │       │       │   ├── CglibSubclassingInstantiationStrategy.java
│   │       │       │   ├── DefaultListableBeanFactory.java
│   │       │       │   ├── DefaultSingletonBeanRegistry.java
│   │       │       │   ├── DisposableBeanAdapter.java
│   │       │       │   ├── FactoryBeanRegistrySupport.java
│   │       │       │   ├── InstantiationStrategy.java
│   │       │       │   └── SimpleInstantiationStrategy.java
│   │       │       └── xml
│   │       │           └── XmlBeanDefinitionReader.java
│   │       ├── context
│   │       │   ├── ApplicationContext.java
│   │       │   ├── ApplicationContextAware.java
│   │       │   ├── ApplicationEvent.java --new
│   │       │   ├── ApplicationEventPublisher.java --new
│   │       │   ├── ApplicationListener.java --new
│   │       │   ├── ConfigurableApplicationContext.java
│   │       │   ├── event
│   │       │   │   ├── AbstractApplicationEventMulticaster.java --new
│   │       │   │   ├── ApplicationContextEvent.java --new
│   │       │   │   ├── ApplicationEventMulticaster.java --new
│   │       │   │   ├── ContextClosedEvent.java --new
│   │       │   │   ├── ContextRefreshedEvent.java --new 
│   │       │   │   └── SimpleApplicationEventMulticaster.java --new
│   │       │   └── support
│   │       │       ├── AbstractApplicationContext.java --change
│   │       │       ├── AbstractRefreshableApplicationContext.java
│   │       │       ├── AbstractXmlApplicationContext.java
│   │       │       ├── ApplicationContextAwareProcessor.java
│   │       │       └── ClassPathXmlApplicationContext.java
│   │       ├── core
│   │       │   └── io
│   │       │       ├── ClassPathResource.java
│   │       │       ├── DefaultResourceLoader.java
│   │       │       ├── FileSystemResource.java
│   │       │       ├── Resource.java
│   │       │       ├── ResourceLoader.java
│   │       │       └── UrlResource.java
│   │       └── utils
│   │           └── BeanUtil.java
│   └── resources
│       ├── important.properties
│       ├── spring.xml --change
│       └── springPostProcessor.xml
└── test
    └── java
        ├── ApiTest.java --change
        ├── bean
        │   ├── IUserDao.java
        │   ├── ProxyBeanFactory.java
        │   ├── UserDao.java
        │   └── UserService.java
        ├── common
        │   ├── MyBeanFactoryPostProcessor.java
        │   └── MyBeanPostProcessor.java 
        └── event
            ├── ContextClosedEventListener.java --new
            ├── ContextRefreshedEventListener.java --new
            ├── CustomEvent.java --new
            └── CustomEventListener.java --new
```

##### Event

最上层的父类：ApplicationEvent，EventObject 几乎没有任何实现，就是定义了 source 参数（事件发生源）：

![image-20250701075028782](https://typora-images-gqy.oss-cn-nanjing.aliyuncs.com/image-20250701075028782.png)

```java
public class ApplicationEvent extends EventObject {
    public ApplicationEvent(Object source){
        super(source);
    }
}
```

然后是 ApplicationContextEvent，这类事件可以简单理解为容器生命周期的相关事件。

```java
public class ApplicationContextEvent extends ApplicationEvent {
    public ApplicationContextEvent(Object source) {
        super(source);
    }

    public final ApplicationContext getApplicationContext() {
        return (ApplicationContext) getSource();
    }
}
```

然后还定义了 ContextClosedEvent 以及 ContextRefreshEvent，可自行查看代码，没有任何特殊实现。

##### Listener

onApplicationEvent 就是消费事件的方法，而这个泛型代表的就是这个 Listener 想要监听的事件。为什么我们要用泛型来表示呢，可参考 **疑惑与思考 - 为什么要用泛型来监听**。

```java
public interface ApplicationListener<E extends ApplicationEvent> extends EventListener {
    /**
     * 事件处理方法
     * @param event the event to respond to
     */
    void onApplicationEvent(E event);
}
```

##### Multicaster（Publisher）

```java
public interface ApplicationEventPublisher {
    // 向全体监听者广播事件
    void publishEvent(ApplicationEvent event);
}
```

> 这是我们需要让 ApplicationContext 实现的接口。

虽然是要让 ApplicationContext 最终实现 Publish 方法，但是广播逻辑、包括匹配事件等，我们拆出来放到一个新的模块 Multicaster 中，遵循单一职责以及合成复用的原则。

很简明：维护 listener 列表，以及广播事件。

```java
public interface ApplicationEventMulticaster {
    void addApplicationListener(ApplicationListener<?> listener);

    void removeApplicationListener(ApplicationListener<?> listener);

    void multicastEvent(ApplicationEvent event);
}
```

抽象类：提取 Multicaster 的共性逻辑：
```java
public abstract class AbstractApplicationEventMulticaster implements ApplicationEventMulticaster, BeanFactoryAware{

    public final Set<ApplicationListener<ApplicationEvent>> applicationListeners = new LinkedHashSet<>();
    private BeanFactory beanFactory;
    
    @Override
    public final void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void addApplicationListener(ApplicationListener<?> listener) {
        applicationListeners.add((ApplicationListener<ApplicationEvent>) listener);
    }

    @Override
    public void removeApplicationListener(ApplicationListener<?> listener) {
        applicationListeners.remove(listener);
    }

    // 就是遍历匹配，如果事件发布的次数很多，也许可以用 map 去优化，但是这个东西数量级很小，事件列表再长也过不了 10^3，O(n) 实在是绰绰有余了。
    protected Collection<ApplicationListener> getApplicationListeners(ApplicationEvent event){
        LinkedList<ApplicationListener> allListeners = new LinkedList<>();
        for (ApplicationListener<ApplicationEvent> listener : applicationListeners) {
            if (supportsEvent(listener, event)) {
                allListeners.add(listener);
            }
        }
        return allListeners;
    }

    // 检查 ApplicationListener 是否监听当前事件
    protected boolean supportsEvent(ApplicationListener<ApplicationEvent> applicationListener, ApplicationEvent event) {
        Class<? extends ApplicationListener> listenerClass = applicationListener.getClass();

        // 按照 Cglib 和 Simple 两种 Strategy 不同的实例化类型，获取对应的目标 class，因为 Cglib 是通过继承实现的
        Class<?> targetClass = ClassUtils.isCglibProxyClass(listenerClass) ? listenerClass.getSuperclass() : listenerClass;
        // 获取实现的第一个泛型接口 -> ApplicationListener<ApplicationEvent>
        Type genericInterface = targetClass.getGenericInterfaces()[0];
        // 获取第一个泛型参数 -> ApplicationEvent
        Type actualTypeArgument = ((ParameterizedType) genericInterface).getActualTypeArguments()[0];
        String className = actualTypeArgument.getTypeName();
        Class<?> eventClassName;
        try {
            // 将对应的事件类加载进内存，Class.forName 会执行类的 Static 初始化逻辑
            eventClassName = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new BeansException("wrong event class name: " + className);
        }

        // A.isAssignableFrom(B) 的作用是判断 B 是否是 A 的子类或实现类
        // AssignableFrom 就是 B 是否可以赋值给 A 的意思
        return eventClassName.isAssignableFrom(event.getClass());
    }
}
```

没有任何额外操作的广播实现：

```java
public class SimpleApplicationEventMulticaster extends AbstractApplicationEventMulticaster{
    public SimpleApplicationEventMulticaster(BeanFactory beanFactory) {
        setBeanFactory(beanFactory);
    }
    @Override
    public void multicastEvent(ApplicationEvent event) {
        for (final ApplicationListener listener : getApplicationListeners(event)) {
            listener.onApplicationEvent(event);
        }
    }
}

```

##### 注册 Listener 和 Multicaster

现在我们要在 AbstractApplicationContext 注册 listener 和 Multicaster，并且实现 Publish 方法。

先让 ApplicationContext 实现接口：

```java
public interface ApplicationContext extends ListableBeanFactory, ApplicationEventPublisher {
}
```

然后在 AbstractApplicationContext 中增加逻辑：把 applicationEventMulticaster 的实例放到 DI 容器中维护，把 Listener 添加到 applicationMulticaster 中。

```java
public abstract class AbstractApplicationContext extends DefaultResourceLoader implements ConfigurableApplicationContext {

    public static final String APPLICATION_EVENT_MULTICASTER_BEAN_NAME = "applicationEventMulticaster";

    private ApplicationEventMulticaster applicationEventMulticaster;
    @Override
    public void refresh() throws BeansException {
        // 1. 创建 BeanFactory，并加载 BeanDefinition
        refreshBeanFactory();

        // 2. 获取 BeanFactory
        ConfigurableListableBeanFactory beanFactory = getBeanFactory();

        // 3 注册 ApplicationContextAwareProcessor，让继承了 ApplicationContextAware 的 Bean 对象都能获取所属的 ApplicationContext
        beanFactory.addBeanPostProcessor(new ApplicationContextAwareProcessor(this));

        // 4. 在 Bean 实例化之前，执行 BeanFactoryPostProcessor (Invoke factory processors registered as beans in the context.)
        invokeBeanFactoryPostProcessors(beanFactory);

        // 5. BeanPostProcessor 需要提前于其他 Bean 对象实例化之前执行注册操作
        registerBeanPostProcessors(beanFactory);

        // 6. 初始化事件发布者
        initApplicationEventMulticaster();

        // 7. 注册事件监听器
        registerListeners();

        // 8. 提前实例化单例Bean对象
        beanFactory.preInstantiateSingletons();
    }

    private void initApplicationEventMulticaster() {
        ConfigurableListableBeanFactory beanFactory = getBeanFactory();
        applicationEventMulticaster = new SimpleApplicationEventMulticaster(beanFactory);
        beanFactory.registerSingleton(APPLICATION_EVENT_MULTICASTER_BEAN_NAME, applicationEventMulticaster);
    }

    private void registerListeners() {
        Collection<ApplicationListener> applicationListeners = getBeanFactory().getBeansOfType(ApplicationListener.class).values();
        for (ApplicationListener listener : applicationListeners) {
            applicationEventMulticaster.addApplicationListener(listener);
        }
    }

    @Override
    public void publishEvent(ApplicationEvent event) {
        applicationEventMulticaster.multicastEvent(event);
    }
}

```

最后再来发布一下 refresh 事件和 close 事件：

```java
public abstract class AbstractApplicationContext extends DefaultResourceLoader implements ConfigurableApplicationContext {
    @Override
    public void refresh() throws BeansException {

        // 9. 发布容器刷新完成事件
        finishRefresh();
    }


    private void finishRefresh() {
        publishEvent(new ContextRefreshedEvent(this));
    }


    @Override
    public void close() {
        publishEvent(new ContextClosedEvent(this));
        getBeanFactory().destroySingletons();
    }

}
```

本节结束~ 击掌！

##### 测试

###### 定义 Event 和 Listener：

```java
public class CustomEvent extends ApplicationContextEvent {

    private Long id;
    private String message;

    public CustomEvent(Object source, Long id, String message) {
        super(source);
        this.id = id;
        this.message = message;
    }

    public Long getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }
}

```

```java
public class CustomEventListener implements ApplicationListener<CustomEvent> {
    @Override
    public void onApplicationEvent(CustomEvent event) {
        System.out.println("CustomEventListener received event: " + event.getMessage() + " with ID: " + event.getId());
    }
}
```

以及 RefreshedEventListener 和 ClosedEventListener。

###### 修改配置

我们要把 Listener 注册进去：

```xml
<?xml version="1.0" encoding="UTF-8"?>
    <beans>

        <bean class="event.ContextRefreshedEventListener"/>

        <bean class="event.CustomEventListener"/>

        <bean class="event.ContextClosedEventListener"/>

    </beans>
```

###### ApiTest

```java
public class ApiTest {
    @Test
    public void test_event(){
        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(("classpath:spring.xml"));
        applicationContext.publishEvent(new CustomEvent(applicationContext, 1001L, "Hello, this is a custom event!"));
        applicationContext.registerShutdownHook();
    }
}
```

测试成功！

![image-20250701081825173](https://typora-images-gqy.oss-cn-nanjing.aliyuncs.com/image-20250701081825173.png)

#### 疑惑与思考

##### 为什么要把监听的事件类型通过泛型来处理，而不是存储为成员变量？

因为是接口，我们没办法有成员变量，所以只能用泛型啦。当然，我们也可以用一个 Abstract 类去实现这个接口，同时持有一个 event，再让所有的 listener 去继承这个 Abstract 类，也可以达到类似的效果。

但是我们又不能让 Abstract 去持有一个 Event 实例，这并不符合直觉，于是我们就得去定义 Enum 类，每次增加一个新的事件，都要修改这个 Enum 类，那真是非常繁琐，一点也不符合 OCP，但是用泛型就可以动态地定义啦。

