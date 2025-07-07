### AOP 配置化

#### 前置思考

上一节我们实现了 AOP 的核心功能，生成一个动态代理把匹配 ClassFilter.match() 的 Bean 包装起来，拦截匹配 MethodMatcher.match() 的方法，给出一个类供 User 继承以实现拦截方法逻辑。

那么我们就要经典地进行配置化，原来的 AdvisedSupport 是用户自己在代码里面封装的，我们要把它用 xml 配置起来，加入 ApplicationContext 的周期之中。

如何实现呢？我们要注册一个 BeanPostProcessor 并持有 AdvisedSupport，不妨称作 ProxyCreator。每一个 Service 类的 Bean 都会经过 ProxyCreator 的匹配看是否需要包装，如果需要，就返回 Proxy 对象，否则走正常流程。

![image-20250707194644039](https://typora-images-gqy.oss-cn-nanjing.aliyuncs.com/image-20250707194644039.png)

#### 具体实现

```bash
src
├── main
│   ├── java
│   │   └── springframework
│   │       ├── aop
│   │       │   ├── AdvisedSupport.java
│   │       │   ├── Advisor.java --new
│   │       │   ├── BeforeAdvice.java --new
│   │       │   ├── ClassFilter.java
│   │       │   ├── MethodBeforeAdvice.java --new
│   │       │   ├── MethodMatcher.java
│   │       │   ├── Pointcut.java
│   │       │   ├── PointcutAdvisor.java --new
│   │       │   ├── TargetSource.java
│   │       │   ├── aspectj
│   │       │   │   ├── AspectJExpressionPointcut.java
│   │       │   │   └── AspectJExpressionPointcutAdvisor.java --new
│   │       │   └── framework
│   │       │       ├── AopProxy.java
│   │       │       ├── Cglib2AopProxy.java
│   │       │       ├── JdkDynamicAopProxy.java
│   │       │       ├── ProxyFactory.java --new
│   │       │       ├── ReflectiveMethodInvocation.java
│   │       │       ├── adapter
│   │       │       │   └── MethodBeforeAdviceInterceptor.java --new
│   │       │       └── autoproxy
│   │       │           └── DefaultAdvisorAutoProxyCreator.java --new
│   │       ├── beans
│   │       │   ├── BeansException.java
│   │       │   ├── PropertyValue.java
│   │       │   ├── PropertyValues.java
│   │       │   └── factory
│   │       │       ├── Aware.java
│   │       │       ├── BeanFactory.java
│   │       │       ├── BeanFactoryAware.java
│   │       │       ├── BeanNameAware.java
│   │       │       ├── ConfigurableListableBeanFactory.java
│   │       │       ├── DisposableBean.java
│   │       │       ├── FactoryBean.java
│   │       │       ├── HierarchicalBeanFactory.java
│   │       │       ├── InitializingBean.java
│   │       │       ├── ListableBeanFactory.java
│   │       │       ├── config
│   │       │       │   ├── AutowireCapableBeanFactory.java
│   │       │       │   ├── BeanDefinition.java
│   │       │       │   ├── BeanFactoryPostProcessor.java
│   │       │       │   ├── BeanPostProcessor.java
│   │       │       │   ├── BeanReference.java
│   │       │       │   ├── ConfigurableBeanFactory.java
│   │       │       │   ├── InstantiationAwareBeanPostProcessor.java --new
│   │       │       │   └── SingletonBeanRegistry.java
│   │       │       ├── support
│   │       │       │   ├── AbstractAutowireCapableBeanFactory.java --change
│   │       │       │   ├── AbstractBeanDefinitionReader.java
│   │       │       │   ├── AbstractBeanFactory.java
│   │       │       │   ├── BeanDefinitionReader.java
│   │       │       │   ├── BeanDefinitionRegistry.java
│   │       │       │   ├── CglibSubclassingInstantiationStrategy.java
│   │       │       │   ├── DefaultListableBeanFactory.java
│   │       │       │   ├── DefaultSingletonBeanRegistry.java
│   │       │       │   ├── DisposableBeanAdapter.java
│   │       │       │   ├── FactoryBeanRegistrySupport.java
│   │       │       │   ├── InstantiationStrategy.java
│   │       │       │   └── SimpleInstantiationStrategy.java
│   │       │       └── xml
│   │       │           └── XmlBeanDefinitionReader.java
│   │       ├── context
│   │       │   ├── ApplicationContext.java
│   │       │   ├── ApplicationContextAware.java
│   │       │   ├── ApplicationEvent.java
│   │       │   ├── ApplicationEventPublisher.java
│   │       │   ├── ApplicationListener.java
│   │       │   ├── ConfigurableApplicationContext.java
│   │       │   ├── event
│   │       │   │   ├── AbstractApplicationEventMulticaster.java
│   │       │   │   ├── ApplicationContextEvent.java
│   │       │   │   ├── ApplicationEventMulticaster.java
│   │       │   │   ├── ContextClosedEvent.java
│   │       │   │   ├── ContextRefreshedEvent.java
│   │       │   │   └── SimpleApplicationEventMulticaster.java
│   │       │   └── support
│   │       │       ├── AbstractApplicationContext.java
│   │       │       ├── AbstractRefreshableApplicationContext.java
│   │       │       ├── AbstractXmlApplicationContext.java
│   │       │       ├── ApplicationContextAwareProcessor.java
│   │       │       └── ClassPathXmlApplicationContext.java
│   │       ├── core
│   │       │   └── io
│   │       │       ├── ClassPathResource.java
│   │       │       ├── DefaultResourceLoader.java
│   │       │       ├── FileSystemResource.java
│   │       │       ├── Resource.java
│   │       │       ├── ResourceLoader.java
│   │       │       └── UrlResource.java
│   │       └── utils
│   │           ├── BeanUtil.java
│   │           └── ClassUtils.java
│   └── resources
│       ├── important.properties
│       └── spring.xml --change
└── test
    └── java
        ├── ApiTest.java --change
        └── bean
            ├── IUserService.java
            ├── UserService.java
            └── UserServiceBeforeAdvice.java --new
```

将之前的 AdvisedSupport 拆一下，就是 PointCut（决定匹配哪些类、哪些方法） 和 Advise（决定执行什么样的截面方法）两个接口。

##### PointcutAdvisor 相关接口 

```java
import org.aopalliance.aop.Advice;

public interface Advisor {
    Advice getAdvice();
}
```

```java
public interface PointcutAdvisor extends Advisor {
    Pointcut getPointcut();
}
```

这里就是一种 Advice，在切点前调用。

```java
public interface BeforeAdvice extends Advice {
}

```

```java
public interface MethodBeforeAdvice extends BeforeAdvice{

    // target 指被代理的原始对象，感觉不一定用得到
    // 本方法在目标方法被调用前执行
    void before(Method method, Object[] args, Object target) throws Throwable;
}

```

> 除了 BeforeAdvice，还有 AfterReturningAdvice、ThrowsAdvice，我们原来实现的 MethodInterceptor 对应着 Around。

将 MethodBeforeAdvice 定义的方法放在 invoke 之前调用，就使用我们上次用的 MethodInterceptor 来实现：

```java
public class MethodBeforeAdviceInterceptor implements MethodInterceptor {

    private MethodBeforeAdvice advice;

    public MethodBeforeAdviceInterceptor() {
    }

    public MethodBeforeAdviceInterceptor(MethodBeforeAdvice advice) {
        this.advice = advice;
    }

    @Override
    public Object invoke(MethodInvocation methodInvocation) throws Throwable {
        this.advice.before(methodInvocation.getMethod(), methodInvocation.getArguments(), methodInvocation.getThis());
        return methodInvocation.proceed();
    }
}
```

> 所以理论上 MethodInterceptor 可以覆盖任何场景，Before 和 AfterReturning 都是一些类似“语法糖”的东西。

使用 AspectJ 切点表达式的 PointcutAdvisor 实现：

```java
public class AspectJExpressionPointcutAdvisor implements PointcutAdvisor {

    // 切面
    private AspectJExpressionPointcut pointcut;

    // 要执行的拦截方法
    private Advice advice;

    // 切面匹配表达式
    private String expression;

    public void setExpression(String expression) {
        this.expression = expression;
    }

    @Override
    public Pointcut getPointcut() {
        if (pointcut == null) {
            pointcut = new AspectJExpressionPointcut(expression);
        }
        return pointcut;
    }

    public void setAdvice(Advice advice) {
        this.advice = advice;
    }

    @Override
    public Advice getAdvice() {
        return advice;
    }
}
```

##### 增加 InstantiationAwareBeanPostProcessor

我们之前的 BeanPostProcessor 必须在已经创建出 bean 之后执行。

![image-20250707201532188](https://typora-images-gqy.oss-cn-nanjing.aliyuncs.com/image-20250707201532188.png)

我们增加一个作用在 beanClass 上的 BeanPostProcessor：

```java
public interface InstantiationAwareBeanPostProcessor extends BeanPostProcessor {
    /**
     * 在 Bean 对象实例化之前，执行此方法
     */
    Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException;
}
```

> 为什么不直接代理已经创建好的 Bean？

修改 AbstractAutowireCapableBeanFactory：

```java
public abstract class AbstractAutowireCapableBeanFactory extends AbstractBeanFactory implements AutowireCapableBeanFactory {
    @Override
    protected Object createBean(String beanName, BeanDefinition beanDefinition, Object[] args) throws BeansException {
        Object bean;
        try {
            // 判断是否返回代理 bean 对象
            bean = resolveBeforeInstantiation(beanName, beanDefinition);
            if (bean != null) {
                return bean;
            }
            // 实例化 Bean
            bean = createBeanInstance(beanDefinition, beanName, args);
            // 给 Bean 填充属性
            applyPropertyValues(beanName, bean, beanDefinition);

            // 执行 Bean 的初始化方法和 BeanPostProcessor 的前置和后置处理方法
            bean = initializeBean(beanName, bean, beanDefinition);
        } catch (Exception e) {
            throw new BeansException("Instantiation of bean failed", e);
        }
        ……
    }

    protected Object resolveBeforeInstantiation(String beanName, BeanDefinition beanDefinition) {
        Object bean = applyBeanPostProcessorsBeforeInstantiation(beanDefinition.getBeanClass(), beanName);
        if (bean != null) {
            bean = applyBeanPostProcessorsAfterInitialization(bean, beanName);
        }
        return bean;
    }

    protected Object applyBeanPostProcessorsBeforeInstantiation(Class<?> beanClass, String beanName) {
        for (BeanPostProcessor beanPostProcessor : getBeanPostProcessors()) {
            if (beanPostProcessor instanceof InstantiationAwareBeanPostProcessor) {
                Object result = ((InstantiationAwareBeanPostProcessor) beanPostProcessor).postProcessBeforeInstantiation(beanClass, beanName);
                if (result != null) return result;
            }
        }
        return null;
    }
}
```

##### 实现 DefaultAdvisorAutoProxyCreator

即要注册的 BeanPostProcessor：

```java
public class DefaultAdvisorAutoProxyCreator implements InstantiationAwareBeanPostProcessor, BeanFactoryAware{

    private DefaultListableBeanFactory beanFactory;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = (DefaultListableBeanFactory) beanFactory;
    }

    @Override
    public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {

        if (isInfrastructureClass(beanClass)) return null;

        Collection<AspectJExpressionPointcutAdvisor> advisors = beanFactory.getBeansOfType(AspectJExpressionPointcutAdvisor.class).values();

        for (AspectJExpressionPointcutAdvisor advisor : advisors) {
            ClassFilter classFilter = advisor.getPointcut().getClassFilter();
            if (!classFilter.matches(beanClass)) continue;

            AdvisedSupport advisedSupport = new AdvisedSupport();

            TargetSource targetSource = null;
            try {
                targetSource = new TargetSource(beanClass.getDeclaredConstructor().newInstance());
            } catch (Exception e){
                e.printStackTrace();
            }
            advisedSupport.setTargetSource(targetSource);
            advisedSupport.setMethodInterceptor((MethodInterceptor) advisor.getAdvice());
            advisedSupport.setMethodMatcher(advisor.getPointcut().getMethodMatcher());
            advisedSupport.setProxyTargetClass(false);

            return new ProxyFactory(advisedSupport).getProxy();
        }

        return null;
    }

    private boolean isInfrastructureClass(Class<?> beanClass){
        return Advice.class.isAssignableFrom(beanClass) || Pointcut.class.isAssignableFrom(beanClass) || Advisor.class.isAssignableFrom(beanClass);
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        return bean;
    }
}
```

##### 测试

IUserService 和 UserService 不变。

###### UserServiceBeforeAdvice

```java
public class UserServiceBeforeAdvice implements MethodBeforeAdvice {
    @Override
    public void before(Method method, Object[] args, Object target) throws Throwable {
        System.out.println("拦截方法：" + method.getName());
    }
}
```

###### spring.xml

```java
<?xml version="1.0" encoding="UTF-8"?>
<beans>

    <bean id="userService" class="bean.UserService"/>

    <bean class="springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator"/>

    <bean id="beforeAdvice" class="bean.UserServiceBeforeAdvice"/>

    <bean id="methodInterceptor" class="springframework.aop.framework.adapter.MethodBeforeAdviceInterceptor">
        <property name="advice" ref="beforeAdvice"/>
    </bean>

    <bean id="pointcutAdvisor" class="springframework.aop.aspectj.AspectJExpressionPointcutAdvisor">
        <property name="expression" value="execution(* bean.IUserService.*(..))"/>
        <property name="advice" ref="methodInterceptor"/>
    </bean>

</beans>
```

###### ApiTest

```java
public class ApiTest {
    @Test
    public void test_aop() throws NoSuchMethodException {
        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:spring.xml");

        IUserService userService = applicationContext.getBean("userService", IUserService.class);
        System.out.println(userService.queryUserInfo());
    }
}
```

#### 疑惑与思考

##### 这种代理的实现不是无法存储属性了么？

目前的逻辑是的，一旦匹配到返回代理，那么 applyProperties 等等逻辑根本不会被执行，被代理的 Bean 就是没有被注入属性的裸对象。

##### 为什么需要 BeforeAdvice？

MethodBeforeAdvice 是 BeforeAdvice 的唯一子类，BeforeAdvice 也没有定义接口方法，为什么还需要 BeforeAdvice 呢？这里目前只是有一个语义的作用，没有其他特殊含义，除非在后续增加 ClassBeforeAdvice（但是不可能，因为我们的 Pointcut 已经定义了 ClassFilter，这个功能已经被实现了）。

#### 其他相关

##### 缺乏对应构造器

如果 MethodBeforeAdviceInterceptor 中没有定义无参构造器的话，会报如下图的错：

![image-20250707150019171](https://typora-images-gqy.oss-cn-nanjing.aliyuncs.com/image-20250707150019171.png)

这是因为单例 Bean 在目前的流程中是这么初始化的：

1. 使用无参构造器构造一个裸对象。
2. 用反射注入属性值。

所以如果定义的 BeanClass 没有无参构造器，就会报错。

但是实际的 Spring 容器中是可以通过有参构造注入的，会有类似下面的 xml 配置：

```xml
<bean id="myService" class="com.example.MyService">
    <constructor-arg name="param" value="someValue"/>
</bean>
```

SpringBoot 的构造器注解：

```java
@Component
public class MyService {
    
  private final Dependency dependency;

    @Autowired
    public MyService(Dependency dependency) {
        this.dependency = dependency;
    }
}
```

