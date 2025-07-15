### 更改代理对象的代理位置

#### 前置思考

我们在 step12 的时候实现了返回 ProxyBean，让用户可以自由地对 Bean 中的方法调用进行切面拦截。但是那时候我们返回的 Bean 是一个裸 Bean，我们在整个 Bean 的生命周期之前返回了 Bean。我们现在要在 Bean 完成初始化周期之后才进行代理

简明的 Bean 生命周期：
![image-20250712161515357](https://typora-images-wwweeds.oss-cn-hangzhou.aliyuncs.com/image-20250712161515357.png)

在任意的生命周期阶段之间，都有对应的 BeanPostProcessors.process() 方法可以实现，已达成对 BeanDefinition 和 Bean 的修改。

看看我们已经实现了哪些修改吧：

![image-20250712161621884](https://typora-images-wwweeds.oss-cn-hangzhou.aliyuncs.com/image-20250712161621884.png)

> 真不少啊，其实还有 postProcessAfterInstantiation 等 BeanPostProcessors 没有实现，但是都是类似的逻辑。

#### 具体实现

本次没有新增文件，只更改了 TargetSouce.java 和 DefaultAdvisorAutoProxyCreator。

##### 判断 Cglib 对象

TargetSource 是存储代理对象的 Class，现在我们要存储实例化之后的 Bean，而我们的实现都是 Cglib 的增强对象，我们要取它的父类来完成之前的操作。

我们要将所有原来使用 getTarget().getClass() 的地方替换为 getTargetClass()，因为如果用 Cglib 去重复增强一个 Cglib 类，就会报 duplicate 的错。

```java
public class TargetSource {

    private final Object target;

    public TargetSource(Object target) {
        this.target = target;
    }

    public Class<?>[] getTargetInterfaces() {
        // 获取目标对象实现的所有接口，对于 JDK 的动态代理，必须要有接口才行
        Class<?> clazz = this.target.getClass();
        clazz = ClassUtils.isCglibProxyClass(clazz) ? clazz.getSuperclass() : clazz;
        return clazz.getInterfaces();
    }

    public Class<?> getTargetClass() {
        Class<?> clazz = this.target.getClass();
        clazz = ClassUtils.isCglibProxyClass(clazz) ? clazz.getSuperclass() : clazz;
        return clazz;
    }

    public Object getTarget() {
        return this.target;
    }
}
```

##### 迁移 AOP 的 ProxyBean 的创建时机

我们将代理 Bean 的时机从 postProcessBeforeInstantiation

```java
public class DefaultAdvisorAutoProxyCreator implements InstantiationAwareBeanPostProcessor, BeanFactoryAware{

    private DefaultListableBeanFactory beanFactory;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = (DefaultListableBeanFactory) beanFactory;
    }
    
    @Override
    public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
        return null;
    }
    
    

    @Override
    public PropertyValues postProcessPropertyValues(PropertyValues pvs, Object bean, String beanName) throws BeansException {
        return pvs;
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
            advisedSupport.setProxyTargetClass(false);
            
            // 返回代理对象
            return new ProxyFactory(advisedSupport).getProxy();
        }
        
        return bean;
    }
}

```

##### 测试

###### 配置文件

spring.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans>
    <bean class="springframework.beans.factory.PropertyPlaceholderConfigurer">
        <property name="location" value="classpath:token.properties"/>
    </bean>

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

token.properties

```properties
token=HelloWorld
```

###### Bean

IuserService

```java
public interface IUserService {
    String queryUserInfo();

    String register(String userName);
}
```

```java
@Component
public class UserDao {

    private static Map<String, String> hashMap = new HashMap<>();

    static {
        hashMap.put("10001", "victor_G，北京，亦庄");
        hashMap.put("10002", "猫猫，香港，九龙");
    }

    public String queryUserName(String uId) {
        return hashMap.get(uId);
    }
}
```

```java
@Component
public class UserService implements IUserService{
    
    @Value("${token}")
    private String token;
    
    @Autowired
    private UserDao userDao;

    @Override
    public String queryUserInfo() {
        try {
            Thread.sleep(new Random(1).nextInt(100));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return userDao.queryUserName("10001") + token;
    }

    @Override
    public String register(String userName) {
        try {
            Thread.sleep(new Random(1).nextInt(100));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "注册用户：" + userName + " success！";
    }
    
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
}
```

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
    public void test_autoProxy(){
        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:spring.xml");
        IUserService userService = applicationContext.getBean("userService", IUserService.class);
        System.out.println("测试结果：" + userService.queryUserInfo());
    }
}
```

本章完结！

#### 疑惑与思考

