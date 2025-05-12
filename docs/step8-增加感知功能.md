### 增加感知功能

#### 前置思路

“感知功能”是啥玩意？听着怪牛的哈。问问 gpt-4o 老师才知道，这玩意没啥神秘的，“感知”就是让 Bean 知道自己的 BeanName、创建自己的 BeanFactory、以及对应的 ApplicationContext。

那 Bean 知道了能干啥？开发者拥有 Bean 的实例，Bean 知道了就是开发者知道了，开发者知道了就能去调用 BeanFactory、ApplicationContext 的方法，修改它们的属性了。 

额，灵活是灵活了，但是听起来真是 crazy，这用户一个没搞好就把封装全搞坏了啊……算了，用户是上帝，上帝嘛是全知全能的，真搞坏啦也不能怪我咯。

#### 具体实现

老规矩，先上项目结构。额，之前我的项目结构一直有点儿问题，context 啥的全都塞到了 beans 包里，这次把 context、utils 啥的都抽出来了，IDEA 里看着舒服多了是不是。

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
│   │   │       │       ├── Aware.java --new
│   │   │       │       ├── BeanFactory.java
│   │   │       │       ├── BeanFactoryAware.java --new
│   │   │       │       ├── BeanNameAware.java --new
│   │   │       │       ├── ConfigurableListableBeanFactory.java
│   │   │       │       ├── DisposableBean.java
│   │   │       │       ├── HierarchicalBeanFactory.java
│   │   │       │       ├── InitializingBean.java
│   │   │       │       ├── ListableBeanFactory.java
│   │   │       │       ├── config
│   │   │       │       │   ├── AutowireCapableBeanFactory.java 
│   │   │       │       │   ├── BeanDefinition.java
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
│   │   │       │       │   ├── InstantiationStrategy.java
│   │   │       │       │   └── SimpleInstantiationStrategy.java
│   │   │       │       └── xml
│   │   │       │           └── XmlBeanDefinitionReader.java
│   │   │       ├── context
│   │   │       │   ├── ApplicationContext.java
│   │   │       │   ├── ApplicationContextAware.java --new
│   │   │       │   ├── ConfigurableApplicationContext.java
│   │   │       │   └── support
│   │   │       │       ├── AbstractApplicationContext.java --change
│   │   │       │       ├── AbstractRefreshableApplicationContext.java
│   │   │       │       ├── AbstractXmlApplicationContext.java
│   │   │       │       ├── ApplicationContextAwareProcessor.java --new
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

告诉各位一个很好的消息，这一章超简单！事不宜迟，马上开始。

我们要干啥来着？要把 BeanFactory、BeanName 都告诉 Bean，我们可以从后往前想：开发者怎么从 Bean 那里拿到这俩玩意？肯定是存为 Bean 的类内属性了。那怎么在外界修改这俩字段呢？用 set 方法。

那我们大概就有个思路了：

1. 写几个接口，里面定义了一些 set 方法。
2. Bean 类去实现这些接口。
3. 框架内判断，在 createBean 的流程中（其实仅有 Bean 实例化之后的流程，毕竟实例后我们才能去调用 set 方法），如果 Bean 实现了这个接口，就调用它，把它想要的东西给它。

好嘞，开始吧。

---

```java
// 标记类接口，只是用来做 instanceof 判断的
public interface Aware {
}
```

```java
public interface BeanFactoryAware extends Aware{
    void setBeanFactory(BeanFactory beanFactory) throws BeansException;
}
```

```java
public interface BeanNameAware extends Aware{
    void setBeanName(String name);
}
```

首先是这俩，那为什么 ApplicationContextAware 接口不写出来呢？因为它跟这俩不是在同一个地方调用的。前面我们说过，Context 持有 BeanFactory，而 Bean 实例化之后，createBean 的流程都是在 BeanFactory 内的，Bean 的引用也是直接被 BeanFactory 持有。我们不能在 BeanFactory 内去持有一个 Context 实例，不然就循环依赖了，我们只能通过传参的方式去做，具体怎么做后面再说。

调用上面这俩接口。

```java
public abstract class AbstractAutowireCapableBeanFactory extends AbstractBeanFactory implements AutowireCapableBeanFactory {
    private Object initializeBean(String beanName, Object bean, BeanDefinition beanDefinition) {
        // Aware 接口就在这被用到了，但说实话我是不太能理解的，感觉有点用，但是不多。
        if (bean instanceof Aware){
            if (bean instanceof BeanFactoryAware){
                ((BeanFactoryAware) bean).setBeanFactory(this);
            }
            if (bean instanceof BeanNameAware){
                ((BeanNameAware) bean).setBeanName(beanName);
            }
        }

        // 1. 执行 BeanPostProcessor Before 处理
        Object wrappedBean = applyBeanPostProcessorsBeforeInitialization(bean, beanName);

        // 2. 执行 Bean 的初始化方法
        try {
            invokeInitMethods(beanName, wrappedBean, beanDefinition);
        } catch (Exception e) {
            throw new BeansException("Invocation of init method of bean[" + beanName + "] failed", e);
        }

        // 2. 执行 BeanPostProcessor After 处理
        wrappedBean = applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);
        return wrappedBean;
    }
}
```

---

然后就是我们上面说的 ApplicationContextAware 接口。

```java
public interface ApplicationContextAware extends Aware {
    void setApplicationContext(ApplicationContext applicationContext) throws BeansException;
}
```

我们说过要传参的对吧。但是如果直接在 BeanFactory 里面新加方法，这又循环依赖了。想想我们之前实现的东西有没有能直接用上的？有的，在 step 6.2 中我们在 Bean 实例化之后，空了两个接口出来，它们会在流程中被调用。我们不是可以正好实现其中一个接口吗，在接口中，我们传入 ApplicationContext。

```java
public class ApplicationContextAwareProcessor implements BeanPostProcessor {
    private final ApplicationContext applicationContext;

    public ApplicationContextAwareProcessor(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
    
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        if (bean instanceof ApplicationContextAware) {
            ((ApplicationContextAware) bean).setApplicationContext(applicationContext);
        }
        return bean;
    }

    //这个接口就没啥用了，我们只需要实现其中一个
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        return bean;
    }
}
```

然后我们注册这个 Processor，即在 refresh() 中添加了第三步。

```java
public abstract class AbstractApplicationContext extends DefaultResourceLoader implements ConfigurableApplicationContext {
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

        // 6. 提前实例化单例Bean对象
        beanFactory.preInstantiateSingletons();
    }
}
```

这就搞定了，是不是超简单？

---

##### 测试部分

UserDao 和 spring.xml 都没有变化。

UserService：

```java
public class UserService implements BeanNameAware, ApplicationContextAware, BeanFactoryAware {

    private ApplicationContext applicationContext;
    private BeanFactory beanFactory;

    private String uId;
    private String company;
    private String location;
    private UserDao userDao;

    public String queryUserInfo() {
        return userDao.queryUserName(uId) + "," + company + "," + location;
    }

    public String getuId() {
        return uId;
    }

    public void setuId(String uId) {
        this.uId = uId;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public UserDao getUserDao() {
        return userDao;
    }

    public void setUserDao(UserDao userDao) {
        this.userDao = userDao;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void setBeanName(String name) {
        System.out.println("Bean Name is：" + name);
    }

    public BeanFactory getBeanFactory() {
        return beanFactory;
    }

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }
}
```

ApiTest：

```java
public class ApiTest {

    @Test
    public void test_xml() {
        // 1.初始化 BeanFactory
        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:spring.xml");
        applicationContext.registerShutdownHook();

        // 2. 获取Bean对象调用方法
        UserService userService = applicationContext.getBean("userService", UserService.class);
        String result = userService.queryUserInfo();
        System.out.println("测试结果：" + result);
        System.out.println("ApplicationContextAware："+userService.getApplicationContext());
        System.out.println("BeanFactoryAware："+userService.getBeanFactory());
    }
}
```

输出：

```java
执行：init-method
Bean Name is：userService
测试结果：WWWeeds,淘天,杭州
ApplicationContextAware：springframework.context.support.ClassPathXmlApplicationContext@7cbd213e
BeanFactoryAware：springframework.beans.factory.support.DefaultListableBeanFactory@192d3247
执行：destroy-method
```

#### 疑惑与思考

##### Aware 父接口的意义是什么？

我来想一个勉强的答案：

咱就在这里用到了对吧：

```java
if (bean instanceof Aware){
    if (bean instanceof BeanFactoryAware){
        ((BeanFactoryAware) bean).setBeanFactory(this);
    }
    if (bean instanceof BeanNameAware){
        ((BeanNameAware) bean).setBeanName(beanName);
    }
}
```

现在我们让我们做个假想实验，并且搓一些数据：

1. 假设里层的 if 在实际场景中有很多，有 10 个。
2. 假设需要感知某样东西的 Bean 占所有 Bean 的 1/10。
3. 假设共有 10 个 Bean。

那没有父类 Aware 的话，总共需要执行 10 * 10 = 100 个 if 判断。

有父类 Aware 的话，总共需要执行 10 + 10 = 20 个 if 判断。

如果实际情况下数据确实是这样的话（里层 if 多，要感知的 Bean 很少），那代码的效率差距确实还是挺明显的。

#### 其他相关

简单到没啥想不到要给各位普及啥其他知识。