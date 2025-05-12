### 管理Bean的初始化与销毁

#### 前置思路

在上一个章节我们实现了 BeanFactoryPostProcessor 的接口，它可以在 BeanDefinition 创建之后，Bean 实例化之前修改 BeanDefinition 的属性。以及 BeanPostProcessor 中的两个接口 postProcessBeforeInitialization 和 postProcessAfterInitialization，它们把 initalMethods（注意不是构造函数哦）夹在中间，由用户自定义操作。

好，那么这一章节要实现的东西就很明确了，initialMethods 我们得实现一下，那么除了初始化方法，销毁方法我们也实现一下。初始化方法的调用时机我们已经知道了，那么销毁方法的调用时机是什么呢？我们在运行时没法确定单例 Bean 在将来会不会被调用，所以我们只能在 JVM 关闭的时候去销毁它。为了确保 JVM 关闭之前调用销毁方法，这里用到了 [Hook](#什么是 Hook（钩子）)。

#### 具体实现

我们梳理一下，如果只是实现这两个方法当然很简单，但我们得让用户自定义它们，这才有意义。那我们就要仿照上一章的内容了，给出两个接口让用户实现，因为这个方法是跟 Bean 实例相关联的，我们让 Bean 类去实现它们，Bean 类在用户那里就是 DAO 类、Service 类这些东西。

如果用户连方法名也想自定义，或者说用户想指定类内某个方法作为初始化方法而不是实现接口，那就得通过 xml 配置 + 反射去实现。我们也很熟悉了。让我们开始吧，这一节不会比上一节更麻烦。

项目结构：

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
│   │   │           │   ├── ApplicationContext.java
│   │   │           │   ├── ConfigurableApplicationContext.java --change
│   │   │           │   └── support
│   │   │           │       ├── AbstractApplicationContext.java --change
│   │   │           │       ├── AbstractRefreshableApplicationContext.java
│   │   │           │       ├── AbstractXmlApplicationContext.java
│   │   │           │       └── ClassPathXmlApplicationContext.java
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
│   │   │           │   ├── DisposableBean.java --new
│   │   │           │   ├── HierarchicalBeanFactory.java
│   │   │           │   ├── InitializingBean.java --new
│   │   │           │   ├── ListableBeanFactory.java
│   │   │           │   ├── config
│   │   │           │   │   ├── AutowireCapableBeanFactory.java
│   │   │           │   │   ├── BeanDefinition.java --change
│   │   │           │   │   ├── BeanFactoryPostProcessor.java
│   │   │           │   │   ├── BeanPostProcessor.java
│   │   │           │   │   ├── BeanReference.java
│   │   │           │   │   ├── ConfigurableBeanFactory.java --change
│   │   │           │   │   └── SingletonBeanRegistry.java
│   │   │           │   ├── support
│   │   │           │   │   ├── AbstractAutowireCapableBeanFactory.java --change
│   │   │           │   │   ├── AbstractBeanDefinitionReader.java
│   │   │           │   │   ├── AbstractBeanFactory.java
│   │   │           │   │   ├── BeanDefinitionReader.java
│   │   │           │   │   ├── BeanDefinitionRegistry.java
│   │   │           │   │   ├── CglibSubclassingInstantiationStrategy.java
│   │   │           │   │   ├── DefaultListableBeanFactory.java 
│   │   │           │   │   ├── DefaultSingletonBeanRegistry.java --change
│   │   │           │   │   ├── DisposableBeanAdapter.java --new
│   │   │           │   │   ├── InstantiationStrategy.java
│   │   │           │   │   └── SimpleInstantiationStrategy.java
│   │   │           │   └── xml
│   │   │           │       └── XmlBeanDefinitionReader.java --change
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

---

给出用户可以实现的接口：

```java
public interface InitializingBean {
    // Bean 属性填充后调用
    void afterPropertiesSet() throws Exception;
}
```

```java
public interface DisposableBean {
    void destroy() throws Exception;
}
```

---

在 BeanDefinition 中存储配置的初始化方法名与销毁方法名。这是用户用 xml 配置的方式。

```java
@SuppressWarnings({"rawtypes"})
public class BeanDefinition {

    private String initMethodName;

    private String destroyMethodName;

    public String getInitMethodName() {
        return initMethodName;
    }

    public void setInitMethodName(String initMethodName) {
        this.initMethodName = initMethodName;
    }

    public String getDestroyMethodName() {
        return destroyMethodName;
    }

    public void setDestroyMethodName(String destroyMethodName) {
        this.destroyMethodName = destroyMethodName;
    }
}
```

---

调用初始化方法。这里的实现就涉及到上面说的，用户既可以通过实现接口的方式进行初始化，也可以在 xml 文件里配置指定初始化方法。在我们的实现中，如果用户同时采用两种方法，则会先调用接口，后调用指定的方法。

```java
public abstract class AbstractAutowireCapableBeanFactory extends AbstractBeanFactory implements AutowireCapableBeanFactory {
    private Object initializeBean(String beanName, Object bean, BeanDefinition beanDefinition) {
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

    private void invokeInitMethods(String beanName, Object wrappedBean, BeanDefinition beanDefinition) throws Exception {
        // 1. 实现了 InitializingBean 接口 afterPropertiesSet 方法
        if (wrappedBean instanceof InitializingBean) {
            ((InitializingBean) wrappedBean).afterPropertiesSet();
        }

        // 2. 通过 xml 配置 <bean init-method="init"> 指定的初始化方法
        // 额，要是两种方式都配置了，那么 afterPropertiesSet 方法会先执行，init 方法后执行，而不是相互覆盖
        String initMethodName = beanDefinition.getInitMethodName();
        if (StrUtil.isNotEmpty(initMethodName)) {
            // 利用反射机制获取定义在 Bean 对应 Class 中的初始化方法
            Method initMethod = beanDefinition.getBeanClass().getMethod(initMethodName);
            if (initMethod == null) {
                throw new BeansException("Could not find an init method named '" + initMethodName + "' on bean with name '" + beanName + "'");
            }
            initMethod.invoke(wrappedBean);
        }
    }
}
```

---

调用销毁方法就要麻烦一些，首先这里我们用了一个适配器包装 DisposableBean，目的是为了让调用方可以直接调用 destroy 方法而不必检查时接口实现还是 xml 配置了。跟刚才的初始化方法有啥不同呢，为啥这里就得包装？

我想到两个原因：

1. 一是因为这两种 destroy 的途径不可以被同时执行，毕竟一个 Bean 不能被销毁两次，逻辑稍复杂。
2. 二是因为调用时机上来说，销毁方法被调用的位置距离它定义的位置要远得多，这就让我们想要拿个啥包装一下它，否则到了很远的地方（另一个包内）还判断 Bean 的类型，稍显突兀。

所以就包装一下吧，又能怎的……

```java
public class DisposableBeanAdapter implements DisposableBean {

    private final Object bean;

    private final String beanName;

    private String destroyMethodName;

    public DisposableBeanAdapter(Object bean, String beanName, BeanDefinition beanDefinition) {
        this.bean = bean;
        this.beanName = beanName;
        this.destroyMethodName = beanDefinition.getDestroyMethodName();
    }
    @Override
    public void destroy() throws Exception {
        // 1. 实现接口 DisposableBean 中的 destroy() 方法
        if (bean instanceof DisposableBean) {
            ((DisposableBean) bean).destroy();
        }

        // 2. 通过 xml 配置 <bean destroy-method="destroy"> 指定的销毁方法
        // 这里需要判断一下，避免二次执行销毁方法
        if (StrUtil.isNotEmpty(destroyMethodName) && !(bean instanceof DisposableBean && this.destroyMethodName.equals("destroy"))){
            Method destroyMethod = bean.getClass().getMethod(destroyMethodName);
            if (destroyMethod == null) {
                throw new BeansException("Couldn't find a destroy method named '" + destroyMethodName + "' on bean with name '" + beanName + "'");
            }
            destroyMethod.invoke(bean);
        }

    }
}
```

在创建 Bean 的时候，如果它实现了接口，那么就把它放到某个容器里，在这里的体现就是调用 registerDisposableBean 方法。

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

        // 注册实现了 DisposableBean 接口的 Bean 对象
        registerDisposableBeanIfNecessary(beanName, bean, beanDefinition);

        addSingleton(beanName, bean);
        return bean;
    }
}

 protected void registerDisposableBeanIfNecessary(String beanName, Object bean, BeanDefinition beanDefinition){
        if (bean instanceof DisposableBean || StrUtil.isNotEmpty(beanDefinition.getDestroyMethodName())){
            //这就用上了我们的包装器了
            registerDisposableBean(beanName, new DisposableBeanAdapter(bean, beanName, beanDefinition));
        }
    }
```

我们在这里看到了存储 disposableBeans 的容器，同时也看到 destroy 方法在 DefaultSingletonBeanRegistry 中被调用——这其实还是个包装。要知道 DefaultSingletonBeanRegistry 是个 BeanFactory，它定义的 destroySingletons 仍然需要一个调用方，并不直接存在于我们之前规划好的执行流程内。

```java
public class DefaultSingletonBeanRegistry implements SingletonBeanRegistry {
        private final Map<String, DisposableBean> disposableBeans = new HashMap<>();

        public void registerDisposableBean(String beanName, DisposableBean bean){
            disposableBeans.put(beanName, bean);
        }

        public void destroySingletons() {
            Set<String> keySet = this.disposableBeans.keySet();
            Object[] disposableBeanNames = keySet.toArray();

            for (int i = disposableBeanNames.length - 1; i >= 0; i--) {
                Object beanName = disposableBeanNames[i];
                DisposableBean disposableBean = disposableBeans.remove(beanName);
                try {
                    disposableBean.destroy();
                } catch (Exception e) {
                    throw new RuntimeException("Destroy method on bean with name '" + beanName + "' threw an exception", e);
                }
            }
        }
}
```

忘了说了，destroySingletons() 作为接口被添加在了 ConfigurableBeanFactory 中——同志们，牢记依赖倒置原则，让外界都依赖咱的接口，而不是咱的方法。

```java
public interface ConfigurableBeanFactory extends HierarchicalBeanFactory, SingletonBeanRegistry {
    // 销毁单例对象
    void destroySingletons();
}
```

让我们继续寻找销毁方法的调用方吧。

```java
public interface ConfigurableApplicationContext extends ApplicationContext{
    void registerShutdownHook();

    void close();
}
```

找着了，就在这……还真草率是吧，注册了个 Hook 调用刚定义的方法，然后把这个接口给用户让用户自己决定调不调用。咱也不知道为啥这非得暴露给用户让用户决定，用户都写了个销毁方法了能不想用？为啥不能给人自动调了？咱也不知道……

```java
public abstract class AbstractApplicationContext extends DefaultResourceLoader implements ConfigurableApplicationContext {
    @Override
    public void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::close));
    }

    @Override
    public void close() {
        getBeanFactory().destroySingletons();
    }
}
```

---

最后的最后，大家想想有没有忘掉啥。既然要有 xml 文件配置，那咱的 xm 解析器得加点东西是不。

```java

public class XmlBeanDefinitionReader extends AbstractBeanDefinitionReader {

    protected void parseBeanElement(Element bean) throws ClassNotFoundException {
        ……

        // 创建 BeanDefinition
        BeanDefinition beanDefinition = new BeanDefinition(clazz);
        beanDefinition.setInitMethodName(bean.getAttribute("init-method"));
        beanDefinition.setDestroyMethodName(bean.getAttribute("destroy-method"));
        
        ……
        // 注册 BeanDefinition
        getRegistry().registerBeanDefinition(beanName, beanDefinition);
    }
}
```

---

##### 测试部分

UserDao，咱把原来的构造函数换成了俩成员方法。

```java
package bean;

import java.util.HashMap;
import java.util.Map;

public class UserDao {
    private static Map<String, String> hashMap = new HashMap<>();

    public void initDataMethod(){
        System.out.println("执行：init-method");
        hashMap.put("10001", "WWWeeds");
        hashMap.put("10002", "八杯水");
        hashMap.put("10003", "阿毛");
    }

    public void destroyDataMethod(){
        System.out.println("执行：destroy-method");
        hashMap.clear();
    }

    public String queryUserName(String uId) {
        return hashMap.get(uId);
    }
}
```

UserService，实现了两个接口。

```java
package bean;

import springframework.beans.factory.DisposableBean;
import springframework.beans.factory.InitializingBean;

public class UserService implements InitializingBean, DisposableBean {

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
    public void destroy() throws Exception {
        System.out.println("执行：UserService.destroy()");
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("执行：UserService.afterPropertiesSet()");
    }
}
```

xml 配置文件：给 UserDao 配置初始化方法和销毁方法，UserService 既然实现了接口就不用配置了。

```java
<?xml version="1.0" encoding="UTF-8"?>
<beans>

    <bean id="userDao" class="bean.UserDao" init-method="initDataMethod" destroy-method="destroyDataMethod"/>

    <bean id="userService" class="bean.UserService">
        <property name="uId" value="10001"/>
        <property name="company" value="淘天"/>
        <property name="location" value="杭州"/>
        <property name="userDao" ref="userDao"/>
    </bean>

</beans>
```

测试文件：

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
    }
}
```

输出：

```bash
执行：init-method
执行：UserService.afterPropertiesSet()
测试结果：WWWeeds,淘天,杭州
执行：UserService.destroy()
执行：destroy-method
```

搞定收工。

#### 疑惑与思考

##### 对于一个既有 set 方法也有 get 方法的字段，设置为 private 有啥意义？

额，在 BeanDefinition 里面新设置两个 String 变量的时候我就又想起了这个经典的问题。在这个例子中，还真是毫无意义。嗯，很政治正确地说，设置 set 和 put 可能在将来可以加上一些检查，但我还是想反驳：现在用不上，就真的别想当然，写好测试，等真用到了再在测试的保障下重构，这路子符合直觉得多。

#### 其他相关

##### 什么是 Hook（钩子）

简单来说，Hook 就是允许程序在特定的事件或条件发生时执行自定义的代码。你可以想象成传一个函数指针作为参数，然后由框架/操作系统/JVM在某个时刻自动调用。当然， Java 中传的不是函数指针，而是一个线程。

有一些常用的 Hook：

Shutdown Hook（关闭钩子），在 JVM 关闭时自动执行一些代码。

```java
public class ShutdownHookExample {
    public static void main(String[] args) {
        // 注册关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("JVM 正在关闭，执行钩子代码");
        }));

        System.out.println("程序正在运行...");
    }
}
```

EventHook（事件钩子），在 GUI 编程中用的比较多，它会监听并响应用户的操作或事件，如果你写过 Vue 或者 React 等 Web 前端项目，应该会对这玩意非常熟悉。

