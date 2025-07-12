### 更改代理对象的代理位置

#### 前置思考

我们在 step12 的时候实现了返回 ProxyBean，让用户可以自由地对 Bean 中的方法调用进行切面拦截。但是那时候我们返回的 Bean 是一个裸 Bean，我们在整个 Bean 的生命周期之前返回了 Bean。我们现在要在 Bean 完成初始化周期之后才进行代理。

//示意图

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
│   │       │   ├── TargetSource.java --change
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
│   │       │       ├── BeanClassLoaderAware.java
│   │       │       ├── BeanFactory.java
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
│   │       │       │   ├── Autowired.java
│   │       │       │   ├── AutowiredAnnotationBeanPostProcessor.java
│   │       │       │   ├── Qualifier.java
│   │       │       │   └── Value.java
│   │       │       ├── config
│   │       │       │   ├── AutowireCapableBeanFactory.java
│   │       │       │   ├── BeanDefinition.java
│   │       │       │   ├── BeanFactoryPostProcessor.java
│   │       │       │   ├── BeanPostProcessor.java
│   │       │       │   ├── BeanReference.java
│   │       │       │   ├── ConfigurableBeanFactory.java
│   │       │       │   ├── InstantiationAwareBeanPostProcessor.java
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
│   │       ├── stereotype
│   │       │   └── Component.java
│   │       └── utils
│   │           ├── BeanUtil.java
│   │           ├── ClassUtils.java
│   │           └── StringValueResolver.java
│   └── resources
│       ├── spring.xml
│       ├── spring2.xml --new
│       └── token.properties
└── test
    └── java
        ├── ApiTest.java --change
        ├── bean
        │   ├── IUserService.java
        │   ├── UserService.java 
        │   └── UserServiceBeforeAdvice.java --new
        └── bean 
            ├── IUserService.java --new
            ├── UserDao.java --new
            ├── UserService.java --new
            └── UserServiceBeforeAdvice.java --new
```

1. 判断 Cglib 对象
2. 迁移 AOP 的 ProxyBean 的创建时机

#### 疑惑与思考

