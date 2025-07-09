### 使用注解与包扫描注册 Bean，自动解析 properties 配置文件

#### 前置思考

想要实现两件事情：

1. 用户在 properties 中的配置可以直接注入 Bean。
2. xml 配置 Bean 还是太过繁琐，我们希望可以直接通过注解来进行 Bean 的注册。

##### 注入配置

token.properties：

```java
token=HelloWorld
```

Xml 配置文件：

```xml
  <bean id="userService" class="bean.UserService">
      <property name="token" value="${token}"/>
  </bean>
```

我们希望可以直接把 ${token} 替换为 HelloWorld。

思路：写一个 BeanFactoryPostProcessor 去修订 BeanDefintion，将所有的 BeanDefinition 取出来，看一看它们的 PropertyValues 的 value 定义是不是有占位符 ${}，如果有的话就把内容替换掉。

##### 注解注册 Bean

我们可以通过反射去获取特定包下增加了某个注解的类，这就是所谓“包扫描”了，把这些类取出来注册成 BeanDefinition，就完成了。可以再额外定义一些注解来配置 BeanDefinition 的 scope 等简单属性。

#### 具体实现

```bash
src
├── main
│   ├── java
│   │   └── springframework
│   │       ├── aop
│   │       │   ├── AdvisedSupport.java
│   │       │   ├── Advisor.java
│   │       │   ├── BeforeAdvice.java
│   │       │   ├── ClassFilter.java
│   │       │   ├── MethodBeforeAdvice.java
│   │       │   ├── MethodMatcher.java
│   │       │   ├── Pointcut.java
│   │       │   ├── PointcutAdvisor.java
│   │       │   ├── TargetSource.java
│   │       │   ├── aspectj
│   │       │   │   ├── AspectJExpressionPointcut.java
│   │       │   │   └── AspectJExpressionPointcutAdvisor.java
│   │       │   └── framework
│   │       │       ├── AopProxy.java
│   │       │       ├── Cglib2AopProxy.java
│   │       │       ├── JdkDynamicAopProxy.java
│   │       │       ├── ProxyFactory.java
│   │       │       ├── ReflectiveMethodInvocation.java
│   │       │       ├── adapter
│   │       │       │   └── MethodBeforeAdviceInterceptor.java
│   │       │       └── autoproxy
│   │       │           └── DefaultAdvisorAutoProxyCreator.java 
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
│   │       │       ├── PropertyPlaceholderConfigurer.java --new
│   │       │       ├── config
│   │       │       │   ├── AutowireCapableBeanFactory.java
│   │       │       │   ├── BeanDefinition.java
│   │       │       │   ├── BeanFactoryPostProcessor.java
│   │       │       │   ├── BeanPostProcessor.java
│   │       │       │   ├── BeanReference.java
│   │       │       │   ├── ConfigurableBeanFactory.java
│   │       │       │   ├── InstantiationAwareBeanPostProcessor.java
│   │       │       │   └── SingletonBeanRegistry.java
│   │       │       ├── support
│   │       │       │   ├── AbstractAutowireCapableBeanFactory.java
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
│   │       │           └── XmlBeanDefinitionReader.java --change
│   │       ├── context
│   │       │   ├── ApplicationContext.java
│   │       │   ├── ApplicationContextAware.java
│   │       │   ├── ApplicationEvent.java
│   │       │   ├── ApplicationEventPublisher.java
│   │       │   ├── ApplicationListener.java
│   │       │   ├── ConfigurableApplicationContext.java
│   │       │   ├── annotation
│   │       │   │   ├── ClassPathBeanDefinitionScanner.java --new
│   │       │   │   ├── ClassPathScanningCandidateComponentProvider.java --new
│   │       │   │   └── Scope.java --new
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
│   │       ├── stereotype
│   │       │   └── Component.java --new
│   │       └── utils
│   │           ├── BeanUtil.java
│   │           └── ClassUtils.java
│   └── resources
│       ├── spring-property.xml --new
│       ├── spring-scan.xml --new
│       └── token.properties --new
└── test
    └── java
        ├── ApiTest.java --change
        └── bean
            ├── IUserService.java 
            └── UserService.java --change
```

##### 配置注入

实现 BeanFactoryPostProcessor 的接口，它会在注册完全部的 BeanDefinition 后调用，去更改 BeanDefinition 的内容。

```java
public class PropertyPlaceholderConfigurer implements BeanFactoryPostProcessor {
    
  	// 占位符
    public static final String DEFAULT_PLACEHOLDER_PREFIX = "${";
    
    public static final String DEFAULT_PLACEHOLDER_SUFFIX = "}";
    
  
  	// 配置文件的路径
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
                    String strVal = (String) value;
                    StringBuilder buffer = new StringBuilder(strVal);
                    int startIdx = strVal.indexOf(DEFAULT_PLACEHOLDER_PREFIX);
                    int stopIdx = strVal.indexOf(DEFAULT_PLACEHOLDER_SUFFIX);
                    
                  // 使用 while 循环，让多个占位符的情况也可以正常替换，如 "{token} a {token}"
                    while (startIdx != -1 && stopIdx != -1 && startIdx < stopIdx) {
                        buffer.replace(startIdx, stopIdx + 1, properties.getProperty(strVal.substring(startIdx + 2, stopIdx)));
                        strVal = buffer.toString();
                        startIdx = strVal.indexOf(DEFAULT_PLACEHOLDER_PREFIX);
                        stopIdx = strVal.indexOf(DEFAULT_PLACEHOLDER_SUFFIX);
                    }
                    propertyValues.addPropertyValue(new PropertyValue(propertyValue.getName(), strVal));
                }
            }
        } catch (IOException e) {
            throw new BeansException("Could not load properties from location: " + location, e);
        }
    }
}
```

后面我们只需要在 xml 中注册这个 bean，在 Context 流程中就会自动被调用了。

##### 注解注册

###### 注解类定义

Component 注解，用来标记 Bean。

```java
// 作用在类上
@Target(ElementType.TYPE)
// 保留到运行时
@Retention(RetentionPolicy.RUNTIME)
// 会生成到 JavaDoc 中
@Documented
public @interface Component {

    String value() default "";

}
```

Scope 注解，用来确定 Bean 的类型。

```java
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Scope {

    String value() default "singleton";

}
```

###### 包扫描器

实现一个通用方法，获取指定包下的所有 Component 类：

```java
public class ClassPathScanningCandidateComponentProvider {

    public Set<BeanDefinition> findCandidateComponents(String basePackage) {
        Set<BeanDefinition> candidates = new LinkedHashSet<>();
        // 扫描指定包下的所有 Component 类
        Set<Class<?>> classes = ClassUtil.scanPackageByAnnotation(basePackage, Component.class);
        for (Class<?> clazz : classes) {
            candidates.add(new BeanDefinition(clazz));
        }
        return candidates;
    }
}
```

这个类内会将给定的所有包下的 Component 类都注册成为 BeanDefinition：

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
    }

  // 通过注解获取 Bean 的 Scope
    private String resolveBeanScope(BeanDefinition beanDefinition){
        Class<?> beanClass = beanDefinition.getBeanClass();
        // 因为 Scope 的注解的 Retention 属性是 RUNTIME，所以可以通过反射获取到注解
        Scope scope = beanClass.getAnnotation(Scope.class);
        if (scope != null) {
            return scope.value();
        }
        return StrUtil.EMPTY;
    }

  // 先注解 value 再类名，获取 beanName
    private String determineBeanName(BeanDefinition beanDefinition) {
        Class<?> beanClass = beanDefinition.getBeanClass();
        Component component = beanClass.getAnnotation(Component.class);
        String value = component.value();
        if (StrUtil.isEmpty(value)) {
            value = StrUtil.lowerFirst(beanClass.getSimpleName());
        }
        return value;
    }

}
```

这个继承就只是为了复用代码了。扫描获取 Component 注解类和注册 BeanDefinition 显然不是一个逻辑层次的事情，放在两个类也很合理。

###### 在 XmlBeanDefinitionReader 中使用包扫描器

```java
public class XmlBeanDefinitionReader extends AbstractBeanDefinitionReader {
    protected void doLoadBeanDefinitions(InputStream inputStream) throws ClassNotFoundException {
        Document doc = XmlUtil.readXML(inputStream);
        Element root = doc.getDocumentElement();

      // 如果是 comopnent-scan 标签，那么获取其中的包路径并扫描
        Element componentScan = XmlUtil.getElement(root, "component-scan");
        if (componentScan != null) {
            String scanPath = componentScan.getAttribute("base-package");
            if(StrUtil.isEmpty(scanPath)) {
                throw new BeansException("The value of base-package attribute must not be empty");
            }
            scanPackage(scanPath);
        }

        NodeList childNodes = root.getChildNodes();

        for(int i = 0; i < childNodes.getLength(); i++){
            if(!(childNodes.item(i) instanceof Element)){
                continue;
            }
            // 判断是否是 bean 对象
            if(!"bean".equals(childNodes.item(i).getNodeName())){
                continue;
            }
          // 如果是 bean 的标签，那么进行 bean 解析
            parseBeanElement((Element)childNodes.item(i));
        }
    }

    private void scanPackage(String scanPath) {
        String[] basePackages = StrUtil.splitToArray(scanPath, ",");
        ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(getRegistry());
        scanner.doScan(basePackages);
    }
}

```

后续我们只需要在 xml 配置文件中填写含有 bean 的包路径即可。

##### 测试

###### 准备 xml 和 properties 配置文件

spring-property.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans>
    <bean class="springframework.beans.factory.PropertyPlaceholderConfigurer">
        <property name="location" value="classpath:token.properties"/>
    </bean>

    <bean id="userService" class="bean.UserService">
        <property name="token" value="${token} a ${token}"/>
    </bean>
</beans>
```

spring-scan.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans>
    <component-scan base-package="bean"/>
</beans>
```

token.properties

```properties
token=HelloWorld
```

###### ApiTest

```java
public class ApiTest {
    @Test
    public void test_scan() {
        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:spring-scan.xml");
        IUserService userService = applicationContext.getBean("userService", IUserService.class);
        System.out.println(userService.queryUserInfo());
    }

    @Test
    public void test_property(){
        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:spring-property.xml");
        IUserService userService = applicationContext.getBean("userService", IUserService.class);
        System.out.println("测试结果：" + userService);
    }
}
```

#### 疑惑与思考

##### 为什么配置注入器不继承 BeanPostProcessor

可能因为读取 token.properties 是一个 IO 操作，我们要尽可能地减少它。把它在 BeanFactoryPostProcessor 的接口内实现就只触发一次，如果放在 BeanPostProcessor 就会触发多次；这时候要么把 properties 的内容存下来，要么把这个读取逻辑散在 createBean 的 BeanFactory 内，都不够自然。

#### 其他相关

##### 关于注解

@Target、@Retention 是两个元注解，也就是“注解的注解”。它们会决定这个注解的标记粒度与保留的时间。

@Target 注解的值以及对应含义 （可多个值）

| 值              | 含义                                               |
| --------------- | -------------------------------------------------- |
| TYPE            | 类、接口、枚举                                     |
| FIELD           | 字段                                               |
| METHOD          | 方法                                               |
| PARAMETER       | 方法参数                                           |
| CONSTRUCTOR     | 构造函数                                           |
| LOCAL_VARIABLE  | 局部变量                                           |
| ANNOTATION_TYPE | 用于其他注解                                       |
| PACKAGE         | 包声明                                             |
| TYPE_PARAMETER  | 用于泛型参数                                       |
| TYPE_USE        | 用于任何使用类型的场景（new、cast、implements 等） |

@Retention 的值以及对应含义

| 值      | 含义                                                 |
| ------- | ---------------------------------------------------- |
| SOURCE  | 注解只保留在源码中，编译时丢弃                       |
| CLASS   | 注解在 .class 字节码文件中存在，运行时不可见（默认） |
| RUNTIME | 注解保留到运行时，可以通过反射获取                   |

注解本身只是一个装饰作用，可以让我们拿到一些额外信息，它们本身不会对被注解的对象产生任何作用，这些产生作用的额外操作都是我们的框架在扫描获取包，拿到有注解的类、方法、变量信息之后去做的。

##### Spring xml 配置的命名空间

如果你看真正的 spring-scan.xml 配置文件，它可能是这么写的：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns:context="http://www.springframework.org/schema/context">
    <context:component-scan base-package="bean"/>
</beans>
```

上面的 xmlns 指的是命名空间模板。下面的 context: 就是命名空间了，这是 spring 为了防止命名冲突所做的。
