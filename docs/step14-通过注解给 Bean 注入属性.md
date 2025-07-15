### 通过注解给 Bean 注入属性

#### 前置思考

我们之前实现了解析 properties 文件以及替换占位符，以及用注解注册 Bean，现在我们需要用注解去给 Bean 添加字段属性，即 property 里的内容。

我们可以用 BeanDefinition 找到对应的 BeanClass，然后用反射取出其中的字段，找到用注解标注的那部分。

1. 基础类型，用 @Value 直接标注要注入的值。
2. 引用类型，用 @Autowired 标注，我们会用 getBean 创建它。

 这个显然是 Bean 实例化之后，用 xml 配置填充属性之前做的事情，所以我们可以用 BeanPostProcessor 实现这个字段注入器。

步骤：

1. 定义相关注解
2. 编写实现了 BeanPostProcessor 的字段注入器
3. 将字段注入器注册到单例 Bean 内

#### 具体实现

```bash
src
├── main
│   ├── java
│   │   └── springframework
│   │       ├── aop
│   │       │   ├── AdvisedSupport.java
│   │       │   ├── Advisor.java
│   │       │   ├── BeforeAdvice.java
│   │       │   ├── ClassFilter.java
│   │       │   ├── MethodBeforeAdvice.java
│   │       │   ├── MethodMatcher.java
│   │       │   ├── Pointcut.java
│   │       │   ├── PointcutAdvisor.java
│   │       │   ├── TargetSource.java
│   │       │   ├── aspectj
│   │       │   │   ├── AspectJExpressionPointcut.java
│   │       │   │   └── AspectJExpressionPointcutAdvisor.java
│   │       │   └── framework
│   │       │       ├── AopProxy.java
│   │       │       ├── Cglib2AopProxy.java
│   │       │       ├── JdkDynamicAopProxy.java
│   │       │       ├── ProxyFactory.java
│   │       │       ├── ReflectiveMethodInvocation.java
│   │       │       ├── adapter
│   │       │       │   └── MethodBeforeAdviceInterceptor.java
│   │       │       └── autoproxy
│   │       │           └── DefaultAdvisorAutoProxyCreator.java --change
│   │       ├── beans
│   │       │   ├── BeansException.java
│   │       │   ├── PropertyValue.java
│   │       │   ├── PropertyValues.java
│   │       │   └── factory
│   │       │       ├── Aware.java
│   │       │       ├── BeanFactory.java --change
│   │       │       ├── BeanFactoryAware.java
│   │       │       ├── BeanNameAware.java
│   │       │       ├── ConfigurableListableBeanFactory.java
│   │       │       ├── DisposableBean.java
│   │       │       ├── FactoryBean.java
│   │       │       ├── HierarchicalBeanFactory.java
│   │       │       ├── InitializingBean.java
│   │       │       ├── ListableBeanFactory.java
│   │       │       ├── PropertyPlaceholderConfigurer.java
│   │       │       ├── annotation
│   │       │       │   ├── Autowired.java --new
│   │       │       │   ├── AutowiredAnnotationBeanPostProcessor.java --new
│   │       │       │   ├── Qualifier.java --new
│   │       │       │   └── Value.java --new
│   │       │       ├── config
│   │       │       │   ├── AutowireCapableBeanFactory.java
│   │       │       │   ├── BeanDefinition.java
│   │       │       │   ├── BeanFactoryPostProcessor.java
│   │       │       │   ├── BeanPostProcessor.java
│   │       │       │   ├── BeanReference.java
│   │       │       │   ├── ConfigurableBeanFactory.java --change
│   │       │       │   ├── InstantiationAwareBeanPostProcessor.java
│   │       │       │   └── SingletonBeanRegistry.java
│   │       │       ├── support
│   │       │       │   ├── AbstractAutowireCapableBeanFactory.java --change
│   │       │       │   ├── AbstractBeanDefinitionReader.java
│   │       │       │   ├── AbstractBeanFactory.java --change
│   │       │       │   ├── BeanDefinitionReader.java
│   │       │       │   ├── BeanDefinitionRegistry.java
│   │       │       │   ├── CglibSubclassingInstantiationStrategy.java
│   │       │       │   ├── DefaultListableBeanFactory.java --change
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
│   │       │   ├── annotation
│   │       │   │   ├── ClassPathBeanDefinitionScanner.java
│   │       │   │   ├── ClassPathScanningCandidateComponentProvider.java
│   │       │   │   └── Scope.java
│   │       │   ├── event
│   │       │   │   ├── AbstractApplicationEventMulticaster.java
│   │       │   │   ├── ApplicationContextEvent.java
│   │       │   │   ├── ApplicationEventMulticaster.java
│   │       │   │   ├── ContextClosedEvent.java
│   │       │   │   ├── ContextRefreshedEvent.java
│   │       │   │   └── SimpleApplicationEventMulticaster.java
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
│   │       ├── stereotype
│   │       │   └── Component.java
│   │       └── utils
│   │           ├── BeanUtil.java
│   │           ├── ClassUtils.java
│   │           └── StringValueResolver.java --new
│   └── resources
│       ├── spring.xml --change
│       └── token.properties
└── test
    └── java
        ├── ApiTest.java --change
        └── bean
            ├── IUserService.java 
            ├── UserDao.java --new 
            └── UserService.java --change

```

##### 让 @Value 也可以使用占位符拿取 properties 配置值

在正式开始之前，先来延续一下上次的内容。现在我们在 xml 配置内可以使用占位符，原理是使用 PropertyPlaceholderConfigurer 去读取 properties 文件并修改 BeanDefinition 中 property 的 value 值。BeanFactoryProcessor 的作用在 BeanProcessor 之前，显然没法直接复用了。

我们采取这种方式：让 PropertyPlaceholderConfigurer 把读取的 properties 保存下来，并向外暴露一个字符串解析器，输入一个字符串，解析掉所有能解析的占位符。

![image-20250711082232880](https://typora-images-wwweeds.oss-cn-hangzhou.aliyuncs.com/image-20250711082232880.png)

定义一个解析字符串的接口：

```java
public interface StringValueResolver {
    String resolveStringValue(String strVal);
}
```

在 BeanFactory 中定义增加字符串解析器与解析字符的接口。

```java
public interface ConfigurableBeanFactory extends HierarchicalBeanFactory, SingletonBeanRegistry {
    //……
    // 添加一个字符串解析器，用于处理注解标记的配置注入
    void addEmbeddedValueResolver(StringValueResolver valueResolver);
    
    // 解析${}嵌入的值
    String resolveEmbeddedValue(String value);
}
```

```java
public abstract class AbstractBeanFactory extends FactoryBeanRegistrySupport implements ConfigurableBeanFactory {

    
    private final List<StringValueResolver> embeddedValueResolvers = new ArrayList<>();

    @Override
    public void addEmbeddedValueResolver(StringValueResolver valueResolver) {
        this.embeddedValueResolvers.add(valueResolver);
    }

    @Override
    public String resolveEmbeddedValue(String value) {
        String result = value;
        for (StringValueResolver resolver : this.embeddedValueResolvers) {
            result = resolver.resolveStringValue(result);
        }
        return result;
    }
}

```

在 PropertyPlaceholderConfigurer 中添加一个私有类，持有解析获得的 properties，放到 BeanFactory 里面去。

```java
public class PropertyPlaceholderConfigurer implements BeanFactoryPostProcessor {
    
    public static final String DEFAULT_PLACEHOLDER_PREFIX = "${";
    
    public static final String DEFAULT_PLACEHOLDER_SUFFIX = "}";
    
    private String location;
    
    public void setLocation(String location) {
        this.location = location;
    }
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        try {
            DefaultResourceLoader resourceLoader = new DefaultResourceLoader();
            Resource resource = resourceLoader.getResource(location);
            Properties properties = new Properties();
            properties.load(resource.getInputStream());
            
            String[] beanDefinitionNames = beanFactory.getBeanDefinitionNames();
            for (String beanName : beanDefinitionNames) {
                BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);

                PropertyValues propertyValues = beanDefinition.getPropertyValues();
                for (PropertyValue propertyValue : propertyValues.getPropertyValues()) {
                    Object value = propertyValue.getValue();
                    if (!(value instanceof String)) continue;
                    // 解析占位符
                    value = resolvePlaceholder((String) value, properties);
                    // 更新属性值
                    propertyValues.addPropertyValue(new PropertyValue(propertyValue.getName(), value));
                }
            }

            StringValueResolver valueResolver = new PlaceholderResolvingStringValueResolver(properties);
            // 向容器中添加字符串解析器，供解析@Value注解使用
            beanFactory.addEmbeddedValueResolver(valueResolver);
        } catch (IOException e) {
            throw new BeansException("Could not load properties from location: " + location, e);
        }
    }
    
    private String resolvePlaceholder(String value, Properties properties) {
        String strVal = value;
        StringBuilder buffer = new StringBuilder(strVal);
        int startIdx = strVal.indexOf(DEFAULT_PLACEHOLDER_PREFIX);
        int stopIdx = strVal.indexOf(DEFAULT_PLACEHOLDER_SUFFIX);
        while (startIdx != -1 && stopIdx != -1 && startIdx < stopIdx) {
            String propKey = strVal.substring(startIdx + 2, stopIdx);
            String propVal = properties.getProperty(propKey);
            // 如果属性值不存在，则跳过当前占位符
            if (propVal == null) {
                startIdx = strVal.indexOf(DEFAULT_PLACEHOLDER_PREFIX, stopIdx + 1);
                stopIdx = strVal.indexOf(DEFAULT_PLACEHOLDER_SUFFIX, startIdx + 1);
                continue;
            }
            // 替换占位符
            buffer.replace(startIdx, stopIdx + 1, propVal);
            strVal = buffer.toString();
            startIdx = strVal.indexOf(DEFAULT_PLACEHOLDER_PREFIX);
            stopIdx = strVal.indexOf(DEFAULT_PLACEHOLDER_SUFFIX);
        }
        return buffer.toString();
    }
    
    private class PlaceholderResolvingStringValueResolver implements StringValueResolver {
        
        private final Properties properties;
        
        public PlaceholderResolvingStringValueResolver(Properties properties) {
            this.properties = properties;
        }
        @Override
        public String resolveStringValue(String strVal) {
            // 引用外部类实例
            return PropertyPlaceholderConfigurer.this.resolvePlaceholder(strVal, properties);
        }
    }
}
```

好了，后续我们就可以使用这个字符串解析器了。

##### 定义注解

@Autowired

```java
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.METHOD})
public @interface Autowired {
}
```

@Qualifier，与 @Autowired 联合使用，指定要注入的 BeanName。

```java
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE, ElementType.ANNOTATION_TYPE}) 
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Qualifier {
    String value() default "";
}
```

@Value，注入基础字段

```java
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Value {
    String value();
}
```

##### 字段注入器

这是一个 spring 的内嵌能力，有特定的参数和返回值，我们先在 InstantiationAwareBeanPostProcessor 添加接口：
```java
public interface InstantiationAwareBeanPostProcessor extends BeanPostProcessor {
    /**
     * Post-process the given property values before the factory applies them
     * to the given bean. Allows for checking whether all dependencies have been
     * satisfied, for example based on a "Required" annotation on bean property setters.
     *
     * 在 Bean 对象实例化完成后，设置属性操作之前执行此方法
     *
     * @param pvs
     * @param bean
     * @param beanName
     * @return
     * @throws BeansException
     */
    PropertyValues postProcessPropertyValues(PropertyValues pvs, Object bean, String beanName) throws BeansException;

}
```

在不需要实现 postProcessPropertyValues 的 BeanPostProcessor 中直接返回 pvs 就好了，注意不要返回 null，这个 pvs 返回出去是要给 BeanFactory 更新 Bean 的 BeanDefinition 的。

然后再再 BeanFactory 中增加一个 getBean(Class\<T> requiredType) 的接口，可以直接通过 Class 信息去 getBean，不需要 BeanName，用在只有 @Autowired 没有 @Qualifier 的时候。

```java
public interface BeanFactory {    
    <T> T getBean(Class<T> requiredType) throws BeansException;
}
```

实现一下：

```java
public abstract class AbstractApplicationContext extends DefaultResourceLoader implements ConfigurableApplicationContext {
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

    @Override
    public <T> T getBean(Class<T> requiredType) throws BeansException {
        return getBeanFactory().getBean(requiredType);
    }

}

```

可以看到，当匹配到多个符合的类型时会报错。在现代 SpringBoot 中，可以用 @Autowired 去修饰 List，以获取 ServiceImpl 的列表，这就是把所有符合类型的匹配类都放进来了。

```java
public class DefaultListableBeanFactory extends AbstractAutowireCapableBeanFactory implements BeanDefinitionRegistry, ConfigurableListableBeanFactory {

    private final Map<String, BeanDefinition> beanDefinitionMap = new HashMap<>();


    @Override
    public <T> T getBean(Class<T> requiredType) throws BeansException {
        List<String> beanNames = new ArrayList<>();
        for (Map.Entry<String, BeanDefinition> entry : beanDefinitionMap.entrySet()) {
            Class<?> beanClass = entry.getValue().getBeanClass();
            if (requiredType.isAssignableFrom(beanClass)) {
                beanNames.add(entry.getKey());
            }
        }
        
        if (beanNames.size() == 1) {
            return getBean(beanNames.get(0), requiredType);
        }
        
        throw new BeansException(requiredType + " expected single bean but found " + beanNames.size() + ": " + beanNames);
    }
}
```

这就是字段注入器，可以看到传入了 PropertyValues，但是我们并不用 pvs 里面的值去做任何判断，这里的 pvs 是用来传给 BeanFactory 修改 BeanDefinition 来让 BeanDefinition 和 Bean 的实际字段一致的（虽然这里并没有任何体现，postProcessPropertyValues 并没有修改 pvs）。

```java
public class AutowiredAnnotationBeanPostProcessor implements InstantiationAwareBeanPostProcessor, BeanFactoryAware {
    private ConfigurableListableBeanFactory beanFactory;
    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
    }

    @Override
    public PropertyValues postProcessPropertyValues(PropertyValues pvs, Object bean, String beanName) throws BeansException {
        // 处理 @Value
        Class<?> clazz = bean.getClass();
        clazz = ClassUtils.isCglibProxyClass(clazz) ? clazz.getSuperclass() : clazz;

        Field[] declaredFields = clazz.getDeclaredFields();
        
        for (Field field : declaredFields) {
            Value valueAnnotation = field.getAnnotation(Value.class);
            if (valueAnnotation != null) {
                String value = valueAnnotation.value();
                value = beanFactory.resolveEmbeddedValue(value);
                try {
                    BeanUtil.setFieldValue(bean, field.getName(), value);
                } catch (NoSuchFieldException e) {
                    throw new BeansException("Failed to set field value for " + field.getName(), e);
                }
            }
        }
        
        // 处理 @Autowired
        for (Field field : declaredFields) {
            Autowired autowiredAnnotation = field.getAnnotation(Autowired.class);
           if (autowiredAnnotation != null) {
               Class<?> fieldType = field.getType();
                String dependentBeanName = null;
                Qualifier qualifierAnnotation = field.getAnnotation(Qualifier.class);
                Object dependentBean = null;
                if (qualifierAnnotation != null) {
                    dependentBeanName = qualifierAnnotation.value();
                    dependentBean = beanFactory.getBean(dependentBeanName, fieldType);
                } else {
                    dependentBean = beanFactory.getBean(fieldType);
                }
                try {
                    BeanUtil.setFieldValue(bean, field.getName(), dependentBean);
                } catch (NoSuchFieldException e) {
                    throw new BeansException("Failed to set field value for " + field.getName(), e);
                }
           }
        }
        
        return pvs;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        return null;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        return null;
    }

    @Override
    public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
        return null;
    }
}
```

##### 注册并使用字段注入器

先找个地方把字段注入器注册成 Bean。其实在任何在字段注入之前的流程，并且拥有 BeanDefinitionRegistry，都可以做这件事。我们不妨把它和扫描 Component 的 Bean 注册器放到一起。

> 其实感觉使用 registerSingleton 也可以，之前的事件发布者就是用 registerSingleton 注册的，对比 registerBeanDefinition 来说没有走 BeanDefinition -> Bean 的这一步。
>
> 用 addBeanPostProcessor 也行，负责感知功能的 BeanPostProcessor 就是用 addBeanPostProcessor 注册的，对比 registerBeanDefinition 来说，不光没有 BeanDefiniton，DI 容器里面也没有这个 Bean，只作为 BeanPostProcessor 存在。

```java
public class ClassPathBeanDefinitionScanner extends ClassPathScanningCandidateComponentProvider{

    private BeanDefinitionRegistry registry;

    public ClassPathBeanDefinitionScanner(BeanDefinitionRegistry registry) {
        this.registry = registry;
    }

    public void doScan(String... basePackages) {
        for (String basePackage : basePackages){
            Set<BeanDefinition> candidates = findCandidateComponents(basePackage);
            for (BeanDefinition candidate : candidates) {
                String beanScope = resolveBeanScope(candidate);
                if (StrUtil.isNotEmpty(beanScope)) {
                    candidate.setScope(beanScope);
                }
                registry.registerBeanDefinition(determineBeanName(candidate), candidate);
            }
        }

        // 注册处理 @Autowired、@Value 的解析器
        registry.registerBeanDefinition("springframework.context.annotation.internalAutowiredAnnotationBeanPostProcessor", new BeanDefinition(AutowiredAnnotationBeanPostProcessor.class));
    }
}

```

在 createBean 的 BeanFactory 中使用：

```java
public abstract class AbstractAutowireCapableBeanFactory extends AbstractBeanFactory implements AutowireCapableBeanFactory {
    private InstantiationStrategy instantiationStrategy = new CglibSubclassingInstantiationStrategy();

    public void setInstantiationStrategy(InstantiationStrategy instantiationStrategy) {
        this.instantiationStrategy = instantiationStrategy;
    }
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

        // 判断 SCOPE_SINGLETON、SCOPE_PROTOTYPE
        if (beanDefinition.isSingleton()) {
            registerSingleton(beanName, bean);
        }
        return bean;
    }
    
    protected void applyBeanPostProcessorsBeforeApplyingPropertyValues(String beanName, Object bean, BeanDefinition beanDefinition) {
        
        PropertyValues pvs = beanDefinition.getPropertyValues();
        for (BeanPostProcessor beanPostProcessor : getBeanPostProcessors()) {
            if (beanPostProcessor instanceof InstantiationAwareBeanPostProcessor) {
                InstantiationAwareBeanPostProcessor ibp = (InstantiationAwareBeanPostProcessor) beanPostProcessor;
                PropertyValues postProcessedPvs = ibp.postProcessPropertyValues(pvs, bean, beanName);
                if (postProcessedPvs != null) {
                    pvs = postProcessedPvs;
                }
            }
        }
        
        // 将处理后的属性值重新设置到 BeanDefinition 中
        for (PropertyValue propertyValue : pvs.getPropertyValues()) {
            beanDefinition.getPropertyValues().addPropertyValue(propertyValue);
        }
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

    <component-scan base-package="bean"/>
</beans>
```

token.properties

```properties
token=HelloWorld
```

###### Bean

```java
@Component("userDao")
public class UserDao {
    private static Map<String, String> hashMap = new HashMap<>();
    
    static {
        hashMap.put("1", "张三");
        hashMap.put("2", "李四");
        hashMap.put("3", "王五");
    }
    
    public String queryUserName(String uId) {
        return hashMap.get(uId);
    }
}
```

```java
@Component("userService")
public class UserService implements IUserService {

    @Value("${token}")
    private String token;
    
    @Autowired
    private UserDao userDao;

    @Override
    public String queryUserInfo() {
        try {
            Thread.sleep(new Random(1).nextInt(100));
        } catch (InterruptedException e){
            e.printStackTrace();
        }
        return userDao.queryUserName("1") + "，" + token;
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

    @Override
    public String toString() {
        return "UserService#token = " + token;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
    
    public UserDao getUserDao() {
        return userDao;
    }
    
    public void setUserDao(UserDao userDao) {
        this.userDao = userDao;
    }
}

```

###### ApiTest

```java
public class ApiTest {
    @Test
    public void test_scan() {
        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:spring.xml");
        IUserService userService = applicationContext.getBean("userService", IUserService.class);
        System.out.println(userService.queryUserInfo());
    }
}
```

#### 疑惑与思考