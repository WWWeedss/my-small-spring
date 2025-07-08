### 使用注解与包扫描注册 Bean

#### 前置思考

#### 具体实现

```java
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
│   │       │       ├── PropertyPlaceholderConfigurer.java
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
│   │       │           └── XmlBeanDefinitionReader.java
│   │       ├── context
│   │       │   ├── ApplicationContext.java
│   │       │   ├── ApplicationContextAware.java
│   │       │   ├── ApplicationEvent.java
│   │       │   ├── ApplicationEventPublisher.java
│   │       │   ├── ApplicationListener.java
│   │       │   ├── ConfigurableApplicationContext.java
│   │       │   ├── annotation
│   │       │   │   ├── ClassPathBeanDefinitionScanner.java
│   │       │   │   ├── ClassPathScanningCandidateComponentProvider.java
│   │       │   │   └── Scope.java
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
│   │       │   └── Component.java
│   │       └── utils
│   │           ├── BeanUtil.java
│   │           └── ClassUtils.java
│   └── resources
│       ├── spring-property.xml
│       ├── spring-scan.xml
│       └── token.properties
└── test
    └── java
        ├── ApiTest.java
        └── bean
            ├── IUserService.java
            └── UserService.java

```



#### 疑惑与思考

#### 其他相关

##### 关于注解

@Retention

##### Spring xml 配置的命名空间

如果你看真正的 spring xml 配置文件，它可能是这么写的：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns:context="http://www.springframework.org/schema/context">
    <context:component-scan base-package="bean"/>
</beans>
```

上面的 xmlns 指的是命名空间模板。下面的 context: 就是命名空间了，这是 spring 为了防止命名冲突所做的。

