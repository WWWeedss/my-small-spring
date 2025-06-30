```bash
src
├── main
│   ├── java
│   │   └── springframework
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
│   │       │       │   └── SingletonBeanRegistry.java --changhe
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
│   │       │   ├── ApplicationEvent.java --new
│   │       │   ├── ApplicationEventPublisher.java --new
│   │       │   ├── ApplicationListener.java --new
│   │       │   ├── ConfigurableApplicationContext.java
│   │       │   ├── event
│   │       │   │   ├── AbstractApplicationEventMulticaster.java --new
│   │       │   │   ├── ApplicationContextEvent.java --new
│   │       │   │   ├── ApplicationEventMulticaster.java --new
│   │       │   │   ├── ContextClosedEvent.java --new
│   │       │   │   ├── ContextRefreshedEvent.java --new 
│   │       │   │   └── SimpleApplicationEventMulticaster.java --new
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
│   │       └── utils
│   │           └── BeanUtil.java
│   └── resources
│       ├── important.properties
│       ├── spring.xml
│       └── springPostProcessor.xml
└── test
    └── java
        ├── ApiTest.java
        ├── bean
        │   ├── IUserDao.java
        │   ├── ProxyBeanFactory.java
        │   ├── UserDao.java
        │   └── UserService.java
        └── common
            ├── MyBeanFactoryPostProcessor.java
            └── MyBeanPostProcessor.java

```

1. ApplicationEvent、ApplicationContextEvent……各种 event 都继承一遍
2. AbstractApplicationEventMulticaster 事件广播器，ApplicationEventPublisher 事件广播器实现
3. 在 AbstractApplicationContext 注册 listener 和 Multicaster
4. 在 AbstractApplication 中的 refresh 和 close 完成时发布事件

#### 疑惑与思考

##### 为什么要把监听的事件类型通过泛型来处理，而不是存储为成员变量？

##### event 筛选的效率是否有点低，为什么不做成 MAP？

##### 用户如何发布自定义的事件呢