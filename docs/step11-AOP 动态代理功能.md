### AOP 动态代理功能

#### 前置思考

权限校验、方法打标、日志监控，这些放在业务逻辑里难免显得冗杂，而且每多一个方法都要放一下，太麻烦了。AOP 就可以很好地解决这个问题，通过代理的方式转发请求，并在转发的过程中（invoke 的前后）实现额外需求。

但是简单代理的问题是没有进行方法的匹配，我们想要代理部分方法就没有办法做了。所以考虑增加一个方法匹配器的逻辑。

此外，我们还想让用户来实现具体的拦截逻辑，这就需要暴露一个接口出去让用户实现。

大概就像这样：

![image-20250703073925782](https://typora-images-gqy.oss-cn-nanjing.aliyuncs.com/image-20250703073925782.png)

算一算我们需要什么：

1. 方便用户调用的代理对象生成方法
2. 方便用户实现的拦截接口
3. 用户自定义的方法匹配器

#### 具体实现

```bash
src
├── main
│   ├── java
│   │   └── springframework
│   │       ├── aop
│   │       │   ├── AdvisedSupport.java --new
│   │       │   ├── ClassFilter.java --new
│   │       │   ├── MethodMatcher.java --new
│   │       │   ├── Pointcut.java --new
│   │       │   ├── TargetSource.java --new
│   │       │   ├── aspectj --new
│   │       │   │   └── AspectJExpressionPointcut.java --new
│   │       │   └── framework
│   │       │       ├── AopProxy.java --new 
│   │       │       ├── Cglib2AopProxy.java --new
│   │       │       ├── JdkDynamicAopProxy.java --new 
│   │       │       └── ReflectiveMethodInvocation.java --new
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
│   │       │       │   └── SingletonBeanRegistry.java
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
│   │       │   ├── ApplicationEvent.java
│   │       │   ├── ApplicationEventPublisher.java
│   │       │   ├── ApplicationListener.java
│   │       │   ├── ConfigurableApplicationContext.java
│   │       │   ├── event
│   │       │   │   ├── AbstractApplicationEventMulticaster.java
│   │       │   │   ├── ApplicationContextEvent.java
│   │       │   │   ├── ApplicationEventMulticaster.java
│   │       │   │   ├── ContextClosedEvent.java
│   │       │   │   ├── ContextRefreshedEvent.java
│   │       │   │   └── SimpleApplicationEventMulticaster.java
│   │       │   └── support
│   │       │       ├── AbstractApplicationContext.java
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
│   │           ├── BeanUtil.java
│   │           └── ClassUtils.java
│   └── resources
│       ├── important.properties
│       └── spring.xml
└── test
    └── java
        ├── ApiTest.java --change
        └── bean
            ├── IUserService.java --new
            ├── UserService.java --change
            └── UserServiceInterceptor.java --new

```

##### 引入依赖

```java
<dependency>
    <groupId>aopalliance</groupId>
    <artifactId>aopalliance</artifactId>
    <version>1.0</version>
</dependency>
<dependency>
    <groupId>org.aspectj</groupId>
    <artifactId>aspectjweaver</artifactId>
    <version>1.9.7</version>
</dependency>
```

##### 匹配器

定义接口：

```java
public interface Pointcut {
    ClassFilter getClassFilter();

    MethodMatcher getMethodMatcher();
}
```

```java
public interface ClassFilter {
    // 接口/类维度的匹配
    boolean matches(Class<?> clazz);
}
```

```java
public interface MethodMatcher {
    // 方法维度的匹配
    boolean matches(Method method, Class<?> targetClass);
}
```

我们用 AspectJ 中的切点表达式来实现。

```java
public class AspectJExpressionPointcut implements Pointcut, ClassFilter, MethodMatcher {
    private static final Set<PointcutPrimitive> SUPPORTED_PRIMITIVES = new HashSet<>();

    static {
        SUPPORTED_PRIMITIVES.add(PointcutPrimitive.EXECUTION);
    }

    private final PointcutExpression pointcutExpression;

    public AspectJExpressionPointcut(String expression){
        PointcutParser pointcutParser = PointcutParser.getPointcutParserSupportingSpecifiedPrimitivesAndUsingSpecifiedClassLoaderForResolution(SUPPORTED_PRIMITIVES, this.getClass().getClassLoader());
        pointcutExpression = pointcutParser.parsePointcutExpression(expression);
    }
    @Override
    // 类级别匹配，本次没有用到，但实际可以进行粗筛
    public boolean matches(Class<?> clazz) {
        return pointcutExpression.couldMatchJoinPointsInType(clazz);
    }

    @Override
    public boolean matches(Method method, Class<?> targetClass) {
        // 这里其实没有用到 targetClass 参数啦，我也不知道为什么要留这个，也许会有单凭 method 信息无法判断的场景呢？
        return pointcutExpression.matchesMethodExecution(method).alwaysMatches();
    }

    @Override
    public ClassFilter getClassFilter() {
        return this;
    }

    @Override
    public MethodMatcher getMethodMatcher() {
        return this;
    }
}
```

关于这个切点表达式，我做了一些整理放在下面。

##### 打包信息

代理对象：

```java
public class TargetSource {

    private final Object target;

    public TargetSource(Object target) {
        this.target = target;
    }

    public Class<?>[] getTargetClass() {
        // 获取目标对象实现的所有接口，对于 JDK 的动态代理，必须要有接口才行
        return target.getClass().getInterfaces();
    }

    public Object getTarget() {
        return target;
    }
}
```

拦截切面：我们直接使用 aopalliance 中定义好的 MethodInterceptor 即可。

然后我们把要用到的信息全部打包起来。

```java
import org.aopalliance.intercept.MethodInterceptor;

public class AdvisedSupport {

    // 被代理的目标对象
    private TargetSource targetSource;

    // 方法拦截器，提供给用户实现做自定义切面
    private MethodInterceptor methodInterceptor;

    // 方法匹配器(检查目标方法是否符合通知条件)
    private MethodMatcher methodMatcher;
}

```

##### 代理实现

我们使用的 MethodInterceptor 的 invoke 方法需要传入一个 MethodInvocation 对象，我们不妨自己继承一个出来，把传参调用的逻辑封装起来：

```java
public class ReflectiveMethodInvocation implements MethodInvocation {
    // 目标对象
    protected final Object target;
    // 方法
    protected final Method method;
    // 入参
    protected final Object[] arguments;

    public ReflectiveMethodInvocation(Object target, Method method, Object[] arguments) {
        this.target = target;
        this.method = method;
        this.arguments = arguments;
    }

    @Override
    public Method getMethod() {
        return method;
    }

    @Override
    public Object[] getArguments() {
        return arguments;
    }

    @Override
    public Object proceed() throws Throwable {
        return method.invoke(target, arguments);
    }

    @Override
    public Object getThis() {
        return target;
    }

    @Override
    public AccessibleObject getStaticPart() {
        return method;
    }
}
```

定义获取代理的接口：

```java
public interface AopProxy {
    Object getProxy();
}
```

这是 JDK 的动态代理实现：

```java
public class JdkDynamicAopProxy implements AopProxy, InvocationHandler {
    private final AdvisedSupport advised;

    public JdkDynamicAopProxy(AdvisedSupport advised) {
        this.advised = advised;
    }
    @Override、
    public Object getProxy() {
        // 我们并不是代理 targetSource 中的全部方法，而是它实现的某些接口的方法
        return Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), advised.getTargetSource().getTargetClass(), this);
    }
    @Override 
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (advised.getMethodMatcher().matches(method, advised.getTargetSource().getTarget().getClass())) {
            MethodInterceptor methodInterceptor = advised.getMethodInterceptor();
            // 参数被传入我们定义的 ReflectiveMethodInvocation 并调用方法，这里经过了 methodInterceptor 的拦截逻辑
            return methodInterceptor.invoke(new ReflectiveMethodInvocation(advised.getTargetSource().getTarget(), method, args));
        }
        return method.invoke(advised.getTargetSource().getTarget(), args);
    }
}

```

Cglib 实现：因为 Cglib 是通过生成一个子类来代理原类的，所以这里要复杂一些。

```java
import net.sf.cglib.proxy.MethodInterceptor;
public class Cglib2AopProxy implements AopProxy {
    private final AdvisedSupport advised;

    public Cglib2AopProxy(AdvisedSupport advised) {
        this.advised = advised;
    }

    @Override
    public Object getProxy() {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(advised.getTargetSource().getTarget().getClass());
        enhancer.setInterfaces(advised.getTargetSource().getTargetClass());
        enhancer.setCallback(new DynamicAdvisedInterceptor(advised));
        return enhancer.create();
    }

    private static class DynamicAdvisedInterceptor implements MethodInterceptor {

        private final AdvisedSupport advised;

        public DynamicAdvisedInterceptor(AdvisedSupport advised) {
            this.advised = advised;
        }

        @Override
        public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
            CglibMethodInvocation methodInvocation = new CglibMethodInvocation(advised.getTargetSource().getTarget(), method, objects, methodProxy);
            if (advised.getMethodMatcher().matches(method, advised.getTargetSource().getTarget().getClass())) {
                return advised.getMethodInterceptor().invoke(methodInvocation);
            }
            return methodInvocation.proceed();
        }
    }

    private static class CglibMethodInvocation extends ReflectiveMethodInvocation {
        private final MethodProxy methodProxy;

        public CglibMethodInvocation(Object target, Method method, Object[] arguments, MethodProxy methodProxy) {
            super(target, method, arguments);
            this.methodProxy = methodProxy;
        }

        @Override
        public Object proceed() throws Throwable {
            return methodProxy.invoke(getThis(), getArguments());
        }
    }
}
```

> 注意这里的 MethodInterceptor 和 advised 里面那个不一样哈，这是 Cglib 的特殊实现，通过 methodProxy 绕过了 java 反射机制直接生成字节码，效率会提高很多。

为什么我们不能直接代理全部方法，这是 jdk 的限制，详见其他相关 - jdk 与 Cglib 代理的实现原理与调用流程。

##### 测试

interface：

```java
public interface IUserService {
    String queryUserInfo();
    String register(String userName);
}
```

实现类：

```java
public class UserService implements IUserService {
    @Override
    public String queryUserInfo() {
        try {
            Thread.sleep(new Random(1).nextInt(100));
        } catch (InterruptedException e){
            e.printStackTrace();
        }
        return "WWWeeds, 100001, 杭州";
    }


    @Override
    public String register(String userName) {
        try {
            Thread.sleep(new Random(1).nextInt(100));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "注册用户:" + userName + "success!";
    }
}
```

拦截器：

```java
public class UserServiceInterceptor implements MethodInterceptor {
    @Override
    public Object invoke(MethodInvocation methodInvocation) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            return methodInvocation.proceed();
        } finally {
            System.out.println("监控 - Begin By AOP");
            Method method = methodInvocation.getMethod();
            System.out.println("方法名称：" + method.getDeclaringClass().getName() + "." + method.getName());
            System.out.println("方法耗时：" + (System.currentTimeMillis() - start) + "ms");
            System.out.println("监控 - End\r\n");
        }
    }
}
```

ApiTest：

```java
public class ApiTest {
    @Test
    public void test_dynamic() {
        IUserService userService = new UserService();

        AdvisedSupport advisedSupport = new AdvisedSupport();
        advisedSupport.setTargetSource(new TargetSource(userService));
        advisedSupport.setMethodInterceptor(new UserServiceInterceptor());
        advisedSupport.setMethodMatcher(new AspectJExpressionPointcut("execution(* bean.IUserService.*(..))"));

        IUserService proxy_jdk = (IUserService) new JdkDynamicAopProxy(advisedSupport).getProxy();
        System.out.println("测试结果：" + proxy_jdk.queryUserInfo());

        IUserService proxy_cglib = (IUserService) new Cglib2AopProxy(advisedSupport).getProxy();
        System.out.println("测试结果：" + proxy_cglib.register("小草"));
    }
}
```

#### 疑惑与思考

##### 如何理解 ClassFilter

如我们所见，其实 AspectJ 切点表达式就是方法维度的，它没有一个去专门匹配包、类的表达式。那么 ClassFilter 的作用是什么？

我们可以写出这样一个表达式，匹配 IUserService 中的所有方法：

```json
execution(* bean.IUserService.*(..))
```

IUserService 是一个接口：
```java
public interface IUserService {
    String queryUserInfo();
}
```

那么 ClassFilter 就会匹配所有 IUserService 的实现类中的 queryUserInfo 方法。ClassFilter 起到的就是一个匹配实现类的作用。对于那些非 IUserService 实现类的类，我们的 AOP 根本不会去尝试匹配其中的方法。各位可以试一试保留 queryUserInfo 方法，但是把 implements 删掉，结果应当是没有执行拦截的。

#### 其他相关

##### AspectJ 切点表达式简要语法

基本结构：

依次是：修饰符匹配、返回值匹配、声明类匹配（匹配方法所属的类/接口）、方法名匹配、参数匹配、抛出异常匹配。

其中，没有问号的返回值匹配、方法名匹配、参数匹配是必选的，如果不想匹配就用 * 代替吧。

```java
execution(modifiers-pattern? ret-type-pattern declaring-type-pattern?name-pattern(param-pattern) throws-pattern?)
```

方法注解匹配 @annotation：

```java
// 匹配带有@Transactional注解的方法
@annotation(org.springframework.transaction.annotation.Transactional)
```

类注解匹配 @within：

```java
// 匹配带有@Controller注解的类中的所有方法
@within(org.springframework.stereotype.Controller)
```

参数匹配 args：

```java
// 匹配第一个参数为String类型的方法
args(String,..)
```

可以用逻辑运算符进行组合：

```java
// 匹配service包下且带有@Transactional注解的方法
// 三个 * 分别代表返回值、类名、方法名，(..)则是参数列表
execution(* com.example.service.*.*(..)) && @annotation(org.springframework.transaction.annotation.Transactional)

// 匹配service包下但不包含UserService的方法
execution(* com.example.service.*.*(..)) && !execution(* com.example.service.UserService.*(..))
```

##### 关于私有静态类

Cglib 的代理实现中我们使用了两个私有静态类 DynamicAdvisedInterceptor 和 CglibMethodInvocation。复习一下知识点：

1. 静态内部类不需要持有外部类实例的隐含引用。
2. 静态内部类只能访问外部类的静态成员。这两条很好理解，因为静态内部类显然不是和实例绑定而是和外部类绑定的。
3. 私有内部内不会被外界访问。

这个东西就起到一个封装逻辑的作用，毕竟它们也不会在别的类里面被用到了。

##### jdk 与 Cglib 代理的实现原理与调用流程

jdk：通过反射调用实现，proxy 和 serviceImpl 共同实现接口，所以没有接口的方法代理不了。

![image-20250704075820485](https://typora-images-gqy.oss-cn-nanjing.aliyuncs.com/image-20250704075820485.png)

Cglib：通过继承实现。

![image-20250704080931328](https://typora-images-gqy.oss-cn-nanjing.aliyuncs.com/image-20250704080931328.png)

所以  enhancer.setInterfaces(advised.getTargetSource().getTargetClass()); 这一句其实只是显式声明了接口方法，子类本身就是会继承父类实现的接口的。

Cglib 会自动创建三个类：代理类（子类）、FastClass 类 A（for Target），FastClass 类 B（for Proxy），FastClass 类内会存储索引化的调用器。

对每一个方法，Cglib 都会有一个 MethodProxy，存储着两个 FastClass 内的索引，调用时就会根据 invoke() 还是 invokeSuper() 去选择调用原始类还是代理增强类的方法，都是通过索引直接找到，比反射更快。

##### IDEA 切屏丢失空格问题解决

取消勾选 Remove trailing spaces ……

![image-20250702232633742](https://typora-images-gqy.oss-cn-nanjing.aliyuncs.com/image-20250702232633742.png)