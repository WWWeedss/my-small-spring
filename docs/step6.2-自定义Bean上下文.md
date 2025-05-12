### 自定义 Bean 上下文

#### 前置思路

前面我们已经让用户能够通过 xml 配置文件来配置 Bean 了，但是这还不够，完成了“配置一键使用”，下一步就得是“定制化”。易用性和灵活性向来是有些冲突，但任何一个好软件都得有的特性。

那么体现在 Spring 框架中，这种灵活性要如何体现呢？即用户自定义方法让 Spring 框架去执行。

我们可以想象一下这个过程：

1. Spring 框架定义一些接口，内部调用它们，但是自己不实现。
2. 用户实现这些接口。

好，我们清理一下相关问题：

1. 操作对象：BeanDefinition 或者 Bean 实例，或者啥也不操作，打印个日志。
2. 接口调用的时机：Spring 主要就是创建 Bean，管理 Bean。那么我们不妨人为地给出三个时间点：
   1. BeanDefinition 定义之后，可修改 BeanDefinition 的属性。
   2. Bean 初始化之前，可修改 Bean 属性。
   3. Bean 初始化之后，可修改 Bean 属性。
   4. 上述三个时间点的接口分别设为接口(1)、接口(2)、接口(3)。
3. 用户如何把实现类告诉 Spring 框架：要么是方法参数，要么存到框架里当属性，这种配置的量级是不可估计的，只能选后者。那我们就用现成的实现就行，即将实现类转化为 Bean，要用的时候通过对应的 Class 类别去寻找（还记得我们在 6.1 中实现的 getBeansOfType() 么）。
4. 对于接口(1)，反正是对 BeanDefinition 做操作，那 LoadBeanDefinition 完了就顺手做了，上下文类先 getBean 再调用嘛。 
5. 对于接口(2)和接口(3)，LoadBeanDefinition 完了我们就可以得到了，但是要到 createBean 的时候才能用（你是不是想问为什么不等 createBean 的时候再 getBean？遵循单一职责原则嘛，管创建存储的和管调用的拆开来）。我们可以在这个时刻实例化之后把它存储在类里做属性，那么存哪里去呢？
   1. 一组配置文件 -> 一个上下文类 -> 上下文类能够创建 Bean，也就是说它需要继承/实现 + 持有 BeanFactory（遵循组合原则我们选后者）。
   2. 我们想象一下，上下文类调用 loadBeanDefinition 之后，可以执行接口(1)，调用 createBean() 时，可以执行接口(2)和接口(3)。
   3. 上述思考过程说明，这三个接口应当是对一组 Bean （属于同一组配置文件，同一个 BeanFactory）进行同样的操作，它们的持有者应当是 BeanFactory 这个粒度。

大致思路明确之后，让我们开始实现。

#### 具体实现

```bash
my-small-spring
├── LICENSE
├── README.md
├── pom.xml
├── src
│   ├── main
│   │   ├── java
│   │   │   └── springframework
│   │   │       └── beans
│   │   │           ├── BeansException.java
│   │   │           ├── PropertyValue.java 
│   │   │           ├── PropertyValues.java 
│   │   │           ├── context
│   │   │           │   ├── ApplicationContext.java --new 
│   │   │           │   ├── ConfigurableApplicationContext.java --new
│   │   │           │   └── support
│   │   │           │       ├── AbstractApplicationContext.java --new
│   │   │           │       ├── AbstractRefreshableApplicationContext.java --new
│   │   │           │       ├── AbstractXmlApplicationContext.java --new
│   │   │           │       └── ClassPathXmlApplicationContext.java --new
│   │   │           ├── core
│   │   │           │   └── io
│   │   │           │       ├── ClassPathResource.java
│   │   │           │       ├── DefaultResourceLoader.java
│   │   │           │       ├── FileSystemResource.java
│   │   │           │       ├── Resource.java
│   │   │           │       ├── ResourceLoader.java
│   │   │           │       └── UrlResource.java
│   │   │           ├── factory
│   │   │           │   ├── BeanFactory.java
│   │   │           │   ├── ConfigurableListableBeanFactory.java
│   │   │           │   ├── HierarchicalBeanFactory.java
│   │   │           │   ├── ListableBeanFactory.java
│   │   │           │   ├── config
│   │   │           │   │   ├── AutowireCapableBeanFactory.java --new
│   │   │           │   │   ├── BeanDefinition.java
│   │   │           │   │   ├── BeanFactoryPostProcessor.java --new
│   │   │           │   │   ├── BeanPostProcessor.java --new
│   │   │           │   │   ├── BeanReference.java
│   │   │           │   │   ├── ConfigurableBeanFactory.java --new
│   │   │           │   │   └── SingletonBeanRegistry.java
│   │   │           │   ├── support
│   │   │           │   │   ├── AbstractAutowireCapableBeanFactory.java --change
│   │   │           │   │   ├── AbstractBeanDefinitionReader.java 
│   │   │           │   │   ├── AbstractBeanFactory.java --change
│   │   │           │   │   ├── BeanDefinitionReader.java
│   │   │           │   │   ├── BeanDefinitionRegistry.java
│   │   │           │   │   ├── CglibSubclassingInstantiationStrategy.java
│   │   │           │   │   ├── DefaultListableBeanFactory.java 
│   │   │           │   │   ├── DefaultSingletonBeanRegistry.java 
│   │   │           │   │   ├── InstantiationStrategy.java
│   │   │           │   │   └── SimpleInstantiationStrategy.java
│   │   │           │   └── xml
│   │   │           │       └── XmlBeanDefinitionReader.java
│   │   │           └── utils
│   │   │               └── BeanUtil.java
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

项目结构真是越来越庞大了，看到要加这么多已经有点想跪了，但是做程序员就得坚强！今天就让我们来理一理这里边儿的头绪。

---

先定义我们抛给用户的接口。它们等着用户去实现。

```java
public interface BeanFactoryPostProcessor {
    /**
     * 在 BeanDefinition 加载完成后，实例化 Bean 对象之前，调用此方法，可以对 BeanDefinition 进行一些修改
     */
    void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException;
}

```

```java
public interface BeanPostProcessor {
    /**
     * 在 Bean 对象执行初始化方法之前，调用此方法
     * @param bean
     * @param beanName
     * @return
     */
    Object postProcessBeforeInitialization(Object bean, String beanName);

    /**
     * 在 Bean 对象执行初始化方法之后，调用此方法
     * @param bean
     * @param beanName
     * @return
     */
    Object postProcessAfterInitialization(Object bean, String beanName);
}
```

---

之前提过，实现类的持有者应当是 BeanFactory 这个粒度。这种能支持自定义接口的 BeanFactory 我们就可以在前面加上 Configurable 这个修饰词。

之前我们的初步思考是让上下文类去调用自定义接口，然后穿插 createBean() 操作。但是前面我们的设计是将 createBean() 隐藏在 BeanFactory 内部，外部只能 getBean()，所以我们只能将接口实现类的存储数据结构放到 BeanFactory 内部，让它去决定如何调用。

```java
public interface AutowireCapableBeanFactory extends BeanFactory {
    /**
     * 执行 BeanPostProcessors 接口实现类的 postProcessBeforeInitialization 方法
     *
     * @param existingBean
     * @param beanName
     * @return
     * @throws BeansException
     */
    Object applyBeanPostProcessorsBeforeInitialization(Object existingBean, String beanName) throws BeansException;

    /**
     * 执行 BeanPostProcessors 接口实现类的 postProcessorsAfterInitialization 方法
     *
     * @param existingBean
     * @param beanName
     * @return
     * @throws BeansException
     */
    Object applyBeanPostProcessorsAfterInitialization(Object existingBean, String beanName) throws BeansException;

}
```

```java
public interface ConfigurableBeanFactory extends HierarchicalBeanFactory, SingletonBeanRegistry {
    String SCOPE_SINGLETON = "singleton";
    //这里能看到还有 prototype 这种 Bean，以后再说吧。
    String SCOPE_PROTOTYPE = "prototype";

    //操作 BeanFactory 内部的接口实现类数据结构
    void addBeanPostProcessor(BeanPostProcessor beanPostProcessor);
}
```

---

存储放在 AbstractBeanFactory：

```java
public abstract class AbstractBeanFactory extends DefaultSingletonBeanRegistry implements ConfigurableBeanFactory {

    private final List<BeanPostProcessor> beanPostProcessors = new ArrayList<BeanPostProcessor>();

    @Override
    public void addBeanPostProcessor(BeanPostProcessor beanPostProcessor) {
        // 因为BeanPostProcessor是有顺序的，所以需要先移除，再添加，保证顺序
        this.beanPostProcessors.remove(beanPostProcessor);
        this.beanPostProcessors.add(beanPostProcessor);
    }

    public List<BeanPostProcessor> getBeanPostProcessors() {
        return this.beanPostProcessors;
    }
}
```

---

执行放在 AbstractAutowireCapableBeanFactory：

```java
public abstract class AbstractAutowireCapableBeanFactory extends AbstractBeanFactory implements AutowireCapableBeanFactory {

    @Override
    protected Object createBean(String beanName, BeanDefinition beanDefinition, Object[] args) throws BeansException {
        Object bean;
        try {
            bean = createBeanInstance(beanDefinition, beanName, args);
            // 给 Bean 填充属性
            applyPropertyValues(beanName, bean, beanDefinition);

            // 执行 Bean 的初始化方法和 BeanPostProcessor 的前置和后置处理方法
            bean = initializeBean(beanName, bean, beanDefinition);
        } catch (Exception e) {
            throw new BeansException("Instantiation of bean failed", e);
        }

        addSingleton(beanName, bean);
        return bean;
    }

    private Object initializeBean(String beanName, Object bean, BeanDefinition beanDefinition) {
        // 1. 执行 BeanPostProcessor Before 处理
        Object wrappedBean = applyBeanPostProcessorsBeforeInitialization(bean, beanName);

        // 待完成内容：invokeInitMethods(beanName, wrappedBean, beanDefinition);
        invokeInitMethods(beanName, wrappedBean, beanDefinition);

        // 2. 执行 BeanPostProcessor After 处理
        wrappedBean = applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);
        return wrappedBean;
    }

    private void invokeInitMethods(String beanName, Object wrappedBean, BeanDefinition beanDefinition) {

    }

    @Override
    public Object applyBeanPostProcessorsBeforeInitialization(Object existingBean, String beanName) throws BeansException {
        Object result = existingBean;
        for (BeanPostProcessor beanPostProcessor : getBeanPostProcessors()) {
            Object current = beanPostProcessor.postProcessBeforeInitialization(result, beanName);
            if (current == null) {
                // 返回 null 说明 Bean 实例化前的操作链已经到底了，直接返回
                return result;
            }
            result = current;
        }
        return result;
    }

    @Override
    public Object applyBeanPostProcessorsAfterInitialization(Object existingBean, String beanName) throws BeansException {
        Object result = existingBean;
        for (BeanPostProcessor beanPostProcessor : getBeanPostProcessors()) {
            Object current = beanPostProcessor.postProcessAfterInitialization(result, beanName);
            if (current == null) {
                return result;
            }
            result = current;
        }
        return result;
    }
}
```

---

好，工厂部分修改完毕，我们考虑创建上下文类。

看了这么多遍也应该熟悉了吧，老规矩，接口 -> 抽象类 -> 实现类。

今天这个类就是个空类，小伙伴们可以点进 ListableBeanFactory 看一看，那就是我们昨天搞出来的接口。

```java
public interface ApplicationContext extends ListableBeanFactory {
}
```

看到这种 “refresh” 方法，各位如果有经验肯定晓得，这大概率是个不做事只调度的父方法，像 main 函数那样子的。

```java
public interface ConfigurableApplicationContext extends ApplicationContext{
    /**
     * 刷新容器
     * @throws BeansException
     */
    void refresh() throws BeansException;
}
```

---

第一层抽象类。实现调度函数，执行接口(1)，存储接口(2)和接口(3)到 BeanFactory 中。

```java
public abstract class AbstractApplicationContext extends DefaultResourceLoader implements ConfigurableApplicationContext {
    @Override
    public void refresh() throws BeansException {
        // 1. 创建 BeanFactory，并加载 BeanDefinition
        refreshBeanFactory();

        // 2. 获取 BeanFactory
        ConfigurableListableBeanFactory beanFactory = getBeanFactory();

        // 3. 在 Bean 实例化之前，执行 BeanFactoryPostProcessor (Invoke factory processors registered as beans in the context.)
        invokeBeanFactoryPostProcessors(beanFactory);

        // 4. BeanPostProcessor 需要提前于其他 Bean 对象实例化之前执行注册操作
        registerBeanPostProcessors(beanFactory);

        // 5. 提前实例化单例Bean对象
        beanFactory.preInstantiateSingletons();
    }

    protected abstract void refreshBeanFactory() throws BeansException;

    protected abstract ConfigurableListableBeanFactory getBeanFactory();

    // 运行 BeanFactoryPostProcessor 接口中用户自定义实现的 postProcessBeanFactory 方法，修改 BeanDefinition 属性值
    private void invokeBeanFactoryPostProcessors(ConfigurableListableBeanFactory beanFactory) {
        Map<String, BeanFactoryPostProcessor> beanFactoryPostProcessorMap = beanFactory.getBeansOfType(BeanFactoryPostProcessor.class);
        for (BeanFactoryPostProcessor beanFactoryPostProcessor : beanFactoryPostProcessorMap.values()) {
            beanFactoryPostProcessor.postProcessBeanFactory(beanFactory);
        }
    }

    private void registerBeanPostProcessors(ConfigurableListableBeanFactory beanFactory) {
        Map<String, BeanPostProcessor> beanPostProcessorMap = beanFactory.getBeansOfType(BeanPostProcessor.class);
        for (BeanPostProcessor beanPostProcessor : beanPostProcessorMap.values()) {
            beanFactory.addBeanPostProcessor(beanPostProcessor);
        }
    }

    //看到底下这一坨了不，组合/聚合原则就体现在这了。持有一个对象，然后实现对应的接口。
    @Override
    public <T> Map<String, T> getBeansOfType(Class<T> type) throws BeansException {
        return getBeanFactory().getBeansOfType(type);
    }

    @Override
    public String[] getBeanDefinitionNames() {
        return getBeanFactory().getBeanDefinitionNames();
    }

    @Override
    public Object getBean(String name) throws BeansException {
        return getBeanFactory().getBean(name);
    }

    @Override
    public Object getBean(String name, Object... args) throws BeansException {
        return getBeanFactory().getBean(name, args);
    }

    @Override
    public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
        return getBeanFactory().getBean(name, requiredType);
    }
}
```

---

第二层抽象类，存储与管理 BeanFactory。

```java
public abstract class AbstractRefreshableApplicationContext extends AbstractApplicationContext{

    private DefaultListableBeanFactory beanFactory;

    @Override
    protected void refreshBeanFactory() throws BeansException {
        DefaultListableBeanFactory beanFactory = createBeanFactory();
        loadBeanDefinitions(beanFactory);
        this.beanFactory = beanFactory;
    }

    private DefaultListableBeanFactory createBeanFactory() {
        return new DefaultListableBeanFactory();
    }

    protected abstract void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) throws BeansException;

    @Override
    protected ConfigurableListableBeanFactory getBeanFactory() {
        return beanFactory;
    }
}

```

---

第三层抽象类，读取 Xml 配置，获取 BeanDefinitions 信息。

```java
public abstract class AbstractXmlApplicationContext extends AbstractRefreshableApplicationContext{
    @Override
    protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) throws BeansException {
        XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(beanFactory, this);
        String[] configLocations = getConfigLocations();
        if (null != configLocations){
            beanDefinitionReader.loadBeanDefinitions(configLocations);
        }
    }

    protected abstract String[] getConfigLocations();
}

```

---

第四层抽象类，从用户处获取路径，调用 refresh() 调度函数。这个类也是用户实例化并使用的类。

```java
public class ClassPathXmlApplicationContext extends AbstractXmlApplicationContext{
    private String[] configLocations;

    public ClassPathXmlApplicationContext() {
    }

    public ClassPathXmlApplicationContext(String configLocations) {
        this(new String[]{configLocations});
    }

    public ClassPathXmlApplicationContext(String[] configLocations) {
        this.configLocations = configLocations;
        refresh();
    }
    @Override
    protected String[] getConfigLocations() {
        return configLocations;
    }
}
```

搞定收工！

---

##### 测试部分

UserDao 不变，改一改 UserService。

```java
package bean;

public class UserService {

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
}
```

写个上下文的配置文件，即 springPostProcessor.xml。

```java
<?xml version="1.0" encoding="UTF-8"?>
<beans>

    <bean id="userDao" class="bean.UserDao"/>

    <bean id="userService" class="bean.UserService">
        <property name="uId" value="10001"/>
        <property name="company" value="腾讯"/>
        <property name="location" value="深圳"/>
        <property name="userDao" ref="userDao"/>
    </bean>

    <bean class="common.MyBeanPostProcessor"/>
    <bean class="common.MyBeanFactoryPostProcessor"/>

</beans>
```

实现三个接口：

```java
public class MyBeanFactoryPostProcessor implements BeanFactoryPostProcessor {
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

        BeanDefinition beanDefinition = beanFactory.getBeanDefinition("userService");
        PropertyValues propertyValues = beanDefinition.getPropertyValues();

        propertyValues.addPropertyValue(new PropertyValue("company", "改为：字节跳动"));
    }
}
```

```java
public class MyBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if ("userService".equals(beanName)) {
            UserService userService = (UserService) bean;
            userService.setLocation("改为：北京");
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }
}
```

开始测试！上边这个是我们手动完成上下文类干的事情，测的是 BeanFactory 搞得对不对，下面那个就是让框架自动帮我们搞定上下文，测的是 Context。

```java
public class ApiTest {

    @Test
    public void test_BeanFactoryPostProcessorAndBeanPostProcessor(){
        // 1.初始化 BeanFactory
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

        // 2. 读取配置文件&注册Bean
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(beanFactory);
        reader.loadBeanDefinitions("classpath:spring.xml");

        // 3. BeanDefinition 加载完成 & Bean实例化之前，修改 BeanDefinition 的属性值
        MyBeanFactoryPostProcessor beanFactoryPostProcessor = new MyBeanFactoryPostProcessor();
        beanFactoryPostProcessor.postProcessBeanFactory(beanFactory);

        // 4. Bean实例化之后，修改 Bean 属性信息
        MyBeanPostProcessor beanPostProcessor = new MyBeanPostProcessor();
        beanFactory.addBeanPostProcessor(beanPostProcessor);

        // 5. 获取Bean对象调用方法
        UserService userService = beanFactory.getBean("userService", UserService.class);
        String result = userService.queryUserInfo();
        System.out.println("测试结果：" + result);
    }

    @Test
    public void test_xml() {
        // 1.初始化 BeanFactory
        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:springPostProcessor.xml");

        // 2. 获取Bean对象调用方法
        UserService userService = applicationContext.getBean("userService", UserService.class);
        String result = userService.queryUserInfo();
        System.out.println("测试结果：" + result);
    }
}
```

#### 从 Spring 看设计模式

##### 层层抽象的考量

每次最上层定义了一堆接口下来，我们都要添加茫茫多的抽象类，直到最后才写一个实现类。而且抽象类也有自己的职责，它“一点儿也不抽象”。

这里运用的应当包含 “单一职责” 设计原则。每个类只准管一件事情。譬如我们要在 BeanFactory 里实现调用自定义接口这件事。

1. AbstractBeanFactory 原来啥也不管，就是实现一下 getBean()，现在加了一个管理接口实现类数据结构的职责。
2. AbstractAutowireCapableBeanFactory 原来负责 createBean()，这些接口正好和 createBean() 强耦合，那么就分配给它去完成调用。



由这个例子我们还可以获得一个很有趣的规律：**越是需要最先使用的东西，在继承树上就越应当推迟实现。**

继承树：AbstractBeanFactory -> AbstractAutowireCapableBeanFactory -> DefaultListableBeanFactory

职责：getBean() -> createBean() -> registerBeanDefinition()

实际使用：定义 BeanDefinition -> 创建 Bean -> 获取 Bean

实际使用顺序越靠前的玩意，它修改带来的影响就会越大，而把它放到下层的子类中，可以帮助我们限制这种影响，从而遵守开闭原则。（这里我的表达不太好，期望有更好的表达）

#### 疑惑与思考

##### 初始化？实例化？

关于暴露给用户的这两个接口：postProcessBeforeInitialization 和 postProcessAfterInitialization。一开始看到的时候其实挺疑惑的，Bean 实例创造出来之前我怎么对它做操作？

我们在实现中是这样的：

```java
  protected Object createBean(String beanName, BeanDefinition beanDefinition, Object[] args) throws BeansException {
        Object bean;
        try {
            //创造 Bean
            bean = createBeanInstance(beanDefinition, beanName, args);
            // 给 Bean 填充属性
            applyPropertyValues(beanName, bean, beanDefinition);
            // 执行 Bean 的初始化方法和 BeanPostProcessor 的前置和后置处理方法
            bean = initializeBean(beanName, bean, beanDefinition);
        } catch (Exception e) {
            throw new BeansException("Instantiation of bean failed", e);
        }

        addSingleton(beanName, bean);
        return bean;
    }

 private Object initializeBean(String beanName, Object bean, BeanDefinition beanDefinition) {
        // 1. 执行 BeanPostProcessor Before 处理
        Object wrappedBean = applyBeanPostProcessorsBeforeInitialization(bean, beanName);

        // 待完成内容：invokeInitMethods(beanName, wrappedBean, beanDefinition);
        invokeInitMethods(beanName, wrappedBean, beanDefinition);

        // 2. 执行 BeanPostProcessor After 处理
        wrappedBean = applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);
        return wrappedBean;
    }
```

哦，那就没问题了，这两个接口都是在调用了 Bean 对象的构造函数之后运行的。被它俩夹在中间的是 “InitMethods” 这不由得让我思考，实例也实例过了，属性也填充完了，这初始化方法能做个啥？

以后的步骤中必有答案，慢慢来吧。 