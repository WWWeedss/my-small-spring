### 多级缓存解决循环依赖

#### 前置思考

现在我们要解决循环依赖的问题。我们可以思考一下日常使用 java 时是如何解决的？

下面的代码不会有任何问题：

```java
A a = new A();
B b = new B();
a.setB(b);
b.setA(a);
```

关键就是：创建一个半成品之后就赋值，利用引用一致性来最终完成对象。

那么在 Spring 框架中，我们可以在实例化 Bean 之后就把它放入缓存给别的 Bean 来引用。而 Spring 框架的实现细节是三层缓存，我们先把它做出来，再来理解它的意义。

三级缓存：

1. 第一层存储初始化完成的 Bean。
2. 第二层存储没有初始化完成的 Bean、或者代理类。
3. 第三层存储 ObjectFactory，是所有 Bean 初始进入的地方，它有一个 getObject() 方法，对于需要 AOP 代理的 Bean，会返回代理类，对于普通 Bean 则会直接返回。

#### 具体实现

只新增了 springframework.beans.factory.ObjectFactory。

##### 让 getSingleton 使用三级缓存

增加一个函数式接口 objectFactory\<T>

```java
public interface ObjectFactory<T> {
    T getObject() throws BeansException;
}
```

然后修订 getSingleton、addSingleton 逻辑，把三层缓存用起来。

```java
public class DefaultSingletonBeanRegistry implements SingletonBeanRegistry {
        // 一级缓存，普通对象
        private final Map<String, Object> singletonObjects = new HashMap<>();

        // 二级缓存，没有完全实例化的对象
        protected final Map<String, Object> earlySingletonObjects = new HashMap<>();

        // 三级缓存，存放代理对象
        private final Map<String, ObjectFactory<?>> singletonFactories = new HashMap<>();

        private final Map<String, DisposableBean> disposableBeans = new HashMap<>();
        @Override
        public Object getSingleton(String beanName) {
            Object singletonObject = singletonObjects.get(beanName);
            if (singletonObject == null) {
                // 如果一级缓存中不存在，则从二级缓存中获取
                singletonObject = earlySingletonObjects.get(beanName);
                if (singletonObject == null) {
                    // 如果二级缓存中也不存在，去三级缓存中寻找
                    ObjectFactory<?> singletonFactory = singletonFactories.get(beanName);
                    if (singletonFactory != null) {
                        singletonObject = singletonFactory.getObject();
                         // 调用 getObject() 方法，代理 Bean 或者取出 Bean
                        earlySingletonObjects.put(beanName, singletonObject);
                        singletonFactories.remove(beanName);
                    }
                }
            }
            return singletonObject;
        }

        @Override
        public void registerSingleton(String beanName, Object singletonObject) {
            singletonObjects.put(beanName, singletonObject);
            earlySingletonObjects.remove(beanName);
            singletonFactories.remove(beanName);
        }

        protected void addSingletonFactory(String beanName, ObjectFactory<?> singletonFactory) {
            if (!this.singletonObjects.containsKey(beanName)) {
                this.singletonFactories.put(beanName, singletonFactory);
                this.earlySingletonObjects.remove(beanName);
            }
        }
}
```

##### 增加getEarlyBeanReference 接口

其实这就是 getObject 的实现。

```java
public interface InstantiationAwareBeanPostProcessor extends BeanPostProcessor {
    /**
     * 在 Spring 中由 SmartInstantiationAwareBeanPostProcessor#getEarlyBeanReference 提供
     * @param bean
     * @param beanName
     * @return
     */
    default Object getEarlyBeanReference(Object bean, String beanName) {
        return bean;
    }
}

```

在 DefaultAdvisorAutoProxyCreator 实现它（实际的 Spring 中由 SmartInstantiationAwareBeanPostProcessor 实现）：

```java
public class DefaultAdvisorAutoProxyCreator implements InstantiationAwareBeanPostProcessor, BeanFactoryAware{

    private final Set<Object> earlyProxyReferences = Collections.synchronizedSet(new HashSet<>());


    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (earlyProxyReferences.contains(beanName)) {
            return bean;
        }
        return warpIfNecessary(bean, beanName);
    }

    protected Object warpIfNecessary(Object bean, String beanName) {
        if(isInfrastructureClass(bean.getClass())) return bean;

        Collection<AspectJExpressionPointcutAdvisor> advisors = beanFactory.getBeansOfType(AspectJExpressionPointcutAdvisor.class).values();

        for (AspectJExpressionPointcutAdvisor advisor : advisors) {
            ClassFilter classFilter = advisor.getPointcut().getClassFilter();

            // 在类维度上进行过滤
            if (!classFilter.matches(bean.getClass())) continue;

            AdvisedSupport advisedSupport = new AdvisedSupport();

            TargetSource targetSource = new TargetSource(bean);
            advisedSupport.setTargetSource(targetSource);
            advisedSupport.setMethodInterceptor((MethodInterceptor) advisor.getAdvice());
            advisedSupport.setMethodMatcher(advisor.getPointcut().getMethodMatcher());
            // 注意这里是 true，我们使用 Cglib 生成 AOP 动态代理
            advisedSupport.setProxyTargetClass(true);

            // 返回代理对象
            return new ProxyFactory(advisedSupport).getProxy();
        }

        return bean;
    }

    @Override
    public Object getEarlyBeanReference(Object bean, String beanName) {
        earlyProxyReferences.add(beanName);
        return warpIfNecessary(bean, beanName);
    }
}

```

##### 修改 createBean 的流程

现在我们实例化 Bean 之后就会把 Bean 包装成一个 ObjectFactory，放到三级缓存里面，如果它被循环引用了，那么就会进入二级缓存，初始化完成之后，进入一级缓存。

这里有个一致化引用的逻辑，在疑惑与思考中进行详细解答。

```java
public abstract class AbstractAutowireCapableBeanFactory extends AbstractBeanFactory implements AutowireCapableBeanFactory {
    @Override
    protected Object createBean(String beanName, BeanDefinition beanDefinition, Object[] args) throws BeansException {
        Object bean;
        try {
            bean = resolveBeforeInstantiation(beanName, beanDefinition);
            if (bean != null) {
                return bean;
            }
            
            // 实例化 Bean
            bean = createBeanInstance(beanDefinition, beanName, args);

            // 处理循环依赖，将实例化后的 bean 提前放入缓存中储存起来
            if (beanDefinition.isSingleton()) {
                Object finalBean = bean;
                // 这里的语法你可能比较奇怪，可以参考 其他相关-函数式接口
                addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, beanDefinition, finalBean));
            }
            
            // 在设置 Bean 属性之前，使用 BeanPostProcessor 修改属性值
            applyBeanPostProcessorsBeforeApplyingPropertyValues(beanName, bean, beanDefinition);
            
            // 给 Bean 填充属性
            applyPropertyValues(beanName, bean, beanDefinition);

            // 执行 Bean 的初始化方法和 BeanPostProcessor 的前置和后置处理方法
            bean = initializeBean(beanName, bean, beanDefinition);
        } catch (Exception e) {
            throw new BeansException("Instantiation of bean failed", e);
        }

        // 注册实现了 DisposableBean 接口的 Bean 对象
        registerDisposableBeanIfNecessary(beanName, bean, beanDefinition);

        // 判断二级缓存中是否有对应的 Bean 对象，如果有，保持引用一致
        // 这里是防止这个 Bean 已经被代理，此时需要保持引用一致
        Object earlySingletonRef = getSingleton(beanName);
        if (earlySingletonRef != null) {
            bean = earlySingletonRef;
        }

        // 判断 SCOPE_SINGLETON、SCOPE_PROTOTYPE
        if (beanDefinition.isSingleton()) {
            registerSingleton(beanName, bean);
        }
        return bean;
    }

    protected Object getEarlyBeanReference(String beanName, BeanDefinition beanDefinition, Object bean) {
        Object exposedObject = bean;
        for (BeanPostProcessor beanPostProcessor : getBeanPostProcessors()) {
            if (beanPostProcessor instanceof InstantiationAwareBeanPostProcessor) {
                exposedObject = ((InstantiationAwareBeanPostProcessor) beanPostProcessor).getEarlyBeanReference(exposedObject, beanName);
                if (exposedObject == null) {
                    return exposedObject;
                }
            }
        }
        return exposedObject;
    }
}

```

##### 测试

###### 配置文件

spring.xml

```java
<?xml version="1.0" encoding="UTF-8"?>
<beans>
    <bean class="springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator"/>

    <bean id="methodInterceptor" class="springframework.aop.framework.adapter.MethodBeforeAdviceInterceptor">
        <property name="advice" ref="beforeAdvice"/>
    </bean>

    <bean id="pointcutAdvisor" class="springframework.aop.aspectj.AspectJExpressionPointcutAdvisor">
        <property name="expression" value="execution(* bean.IUserService.*(..))"/>
        <property name="advice" ref="methodInterceptor"/>
    </bean>

    <component-scan base-package="bean"/>
</beans>
```

###### Bean

接口

```java
public interface IUserService {
    String queryUserInfo();
}
```

userService1

```java
@Component(value = "userService1")
public class UserService1 implements IUserService{
    @Autowired
    private UserService2 userService2;

    @Override
    public String queryUserInfo() {
        try {
            Thread.sleep(new Random(1).nextInt(100));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "这是 UserService1 的用户信息";
    }

    public String queryAnotherUserInfo() {
        return "这是在 UserService1 中调用的：" + userService2.queryUserInfo();
    }
}

```

userService2

```java
@Component(value = "userService2")
public class UserService2 implements IUserService{

    @Autowired
    private UserService1 userService1;

    @Override
    public String queryUserInfo() {
        try {
            Thread.sleep(new Random(1).nextInt(100));
        }                                                                                                                                                                                                                                 catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "这是 UserService2 的用户信息";
    }

    public String queryAnotherUserInfo() {
        return "这是在 UserService2 中调用的：" + userService1.queryUserInfo();
    }
}
```

UserServiceBeforeAdvice

```java
@Component(value = "beforeAdvice")
public class UserServiceBeforeAdvice implements MethodBeforeAdvice {
    @Override
    public void before(Method method, Object[] args, Object target) throws Throwable {
        System.out.println("拦截方法：" + method.getName());
    }
}
```

###### ApiTest

```java
public class ApiTest {
    @Test
    public void test_circleDependency(){
        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:spring.xml");
        UserService1 userService1 = (UserService1) applicationContext.getBean("userService1");
        System.out.println(userService1.queryUserInfo());
        System.out.println(userService1.queryAnotherUserInfo());
        UserService2 userService2 = (UserService2) applicationContext.getBean("userService2");
        System.out.println(userService2.queryUserInfo());
        System.out.println(userService2.queryAnotherUserInfo());
    }
}
```

如果你报了这个错：java.lang.IllegalArgumentException: Can not set bean.UserService2 field bean.UserService1.userService2 to com.sun.proxy.$Proxy6。可以参考其他相关 - xxx 报错的解释。

![image-20250713103744092](https://typora-images-gqy.oss-cn-nanjing.aliyuncs.com/image-20250713103744092.png)

#### 疑惑与思考

##### 引用一致化

为什么我们需要在注册之前检查二级缓存并一致化引用呢，这么去考虑：

A、B 循环引用，A 需要生成 AOP 代理，A 先被 Spring 加载，假设我们没有这个一致化引用的逻辑，可以看下图的变化：

首先 Spring 实例化 A：

![image-20250713102744469](https://typora-images-gqy.oss-cn-nanjing.aliyuncs.com/image-20250713102744469.png)

然后 Spring 在 A 注入属性的时候实例化 B：

![image-20250713102816700](https://typora-images-gqy.oss-cn-nanjing.aliyuncs.com/image-20250713102816700.png)

给 B 注入属性的时候，调用了 getEarlyRefence，A 被代理了，放入了二级缓存，并且返回给 B：

![image-20250713102935632](https://typora-images-gqy.oss-cn-nanjing.aliyuncs.com/image-20250713102935632.png)

B 完成初始化流程，进入一级缓存，并返回给 A：

![image-20250713103015630](https://typora-images-gqy.oss-cn-nanjing.aliyuncs.com/image-20250713103015630.png)

A 完成初始化，也要进入一级缓存：

![image-20250713103111877](https://typora-images-gqy.oss-cn-nanjing.aliyuncs.com/image-20250713103111877.png)

我们发现，B 持有的是正确的代理类，一级缓存中的 A 却是没有被代理的，初始化完成进入一级缓存后，二级缓存中的 A' 被删除了。这就产生了错误。所以我们在将 A 放入一级缓存之前，必须要检查二级缓存中是否已经有 A' 了，如果有，要将现在 A 的引用指向 A'。

现在在看这段代码，应该就可以理解了：

```java
// 判断二级缓存中是否有对应的 Bean 对象，如果有，保持引用一致
// 这里是防止这个 Bean 已经被代理，此时需要保持引用一致
Object earlySingletonRef = getSingleton(beanName);
if (earlySingletonRef != null) {
    bean = earlySingletonRef;
}
```

##### Spring 三级缓存存在的意义是什么

从前置思考中就可以看到，解决循环依赖问题，只用一层缓存也完全可以。这三级缓存更像是逻辑上的设计而没有什么实现上的意义，下面是我的学习与思考：

引用[知乎上的一个答案](https://zhuanlan.zhihu.com/p/496273636)：

**使用三级缓存的目的是为了尽量避免改变原有的执行流程。**

首先，用户可以通过扩展点操作缓存内的 Bean，所以用户是知道一级缓存的存在的。

我们希望只有循环依赖的 Bean 才会提前暴露给用户，这句话就可以翻译为：只有完全创建完成的 Bean 才会放到一级缓存中。

这就引出了需要一个二级缓存去保存没有完全创建完毕的 Bean。

而三级缓存其实是为了 AOP ProxyBean 服务的。假设只有两层缓存，我们在 Bean 实例化之后注入属性之前就要把 Bean 存到二级缓存里，假设这个 Bean 需要实现动态代理，如果按照原本的代理逻辑，在 Bean 初始化完成之后进行代理，那循环引用路径中的其他 Bean 引用到的就是代理前的对象了。

所以我们必须要做出选择，如果还是用二级缓存，我们就要在实例化之后直接决定是否代理，并把创建好的代理对象放入二级缓存，或者就是像 Spring 一样再加一层缓存，只有当产生循环引用时，才提前代理。

这样就有了三级缓存。

#### 其他相关

##### 函数式接口、lambda 表达式类型

这是 java8 的一个特性，这种只有一个 public 方法的 interface 的类型就是函数式接口。

```java 
public interface ObjectFactory<T> {
    T getObject() throws BeansException;
}
```

我们可以直接用 lambda 表达式来替代 new 一个匿名内部类。

```java
addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, beanDefinition, finalBean));
```

这里的 () -> getEarlyBeanReference 与下方的代码等价：

```java
new ObjectFactory<>() {
    @Override
    public Object getObject() {
        return getEarlyBeanReference(beanName, beanDefinition, finalBean);
    }
}
```

##### java.lang.IllegalArgumentException: Can not set bean.UserService2 field bean.UserService1.userService2 to com.sun.proxy.$Proxy6 的报错

如果你没有听我的话，这一句还是 false：

```java
 advisedSupport.setProxyTargetClass(true);
```

那么就会报错。

如果使用 JDK 生成动态代理，那么我们只能赋值给接口（IUserService），就像下图：

```java
@Autowired
@Qualifier(value = "userService1")
IUserService userService;
```

如果要像测试中一样直接赋给实现类，那么就要使用 Cglib 进行 AOP 动态代理。

```java
@Autowired
UserService1 userService1
```

