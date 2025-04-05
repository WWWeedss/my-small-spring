### 原型Bean与FactoryBean

#### 前置思路

#### 具体实现

```bash
my-small-spring
├── LICENSE
├── README.md
├── pom.xml
├── src
│   ├── main
│   │   ├── java
│   │   │   └── springframework
│   │   │       ├── beans
│   │   │       │   ├── BeansException.java
│   │   │       │   ├── PropertyValue.java
│   │   │       │   ├── PropertyValues.java
│   │   │       │   └── factory
│   │   │       │       ├── Aware.java
│   │   │       │       ├── BeanFactory.java --new
│   │   │       │       ├── BeanFactoryAware.java
│   │   │       │       ├── BeanNameAware.java
│   │   │       │       ├── ConfigurableListableBeanFactory.java
│   │   │       │       ├── DisposableBean.java
│   │   │       │       ├── FactoryBean.java
│   │   │       │       ├── HierarchicalBeanFactory.java
│   │   │       │       ├── InitializingBean.java
│   │   │       │       ├── ListableBeanFactory.java
│   │   │       │       ├── config
│   │   │       │       │   ├── AutowireCapableBeanFactory.java
│   │   │       │       │   ├── BeanDefinition.java --change
│   │   │       │       │   ├── BeanFactoryPostProcessor.java
│   │   │       │       │   ├── BeanPostProcessor.java
│   │   │       │       │   ├── BeanReference.java
│   │   │       │       │   ├── ConfigurableBeanFactory.java
│   │   │       │       │   └── SingletonBeanRegistry.java
│   │   │       │       ├── support
│   │   │       │       │   ├── AbstractAutowireCapableBeanFactory.java --change
│   │   │       │       │   ├── AbstractBeanDefinitionReader.java
│   │   │       │       │   ├── AbstractBeanFactory.java
│   │   │       │       │   ├── BeanDefinitionReader.java
│   │   │       │       │   ├── BeanDefinitionRegistry.java
│   │   │       │       │   ├── CglibSubclassingInstantiationStrategy.java
│   │   │       │       │   ├── DefaultListableBeanFactory.java
│   │   │       │       │   ├── DefaultSingletonBeanRegistry.java
│   │   │       │       │   ├── DisposableBeanAdapter.java
│   │   │       │       │   ├── FactoryBeanRegistrySupport.java --change
│   │   │       │       │   ├── InstantiationStrategy.java
│   │   │       │       │   └── SimpleInstantiationStrategy.java
│   │   │       │       └── xml
│   │   │       │           └── XmlBeanDefinitionReader.java --change
│   │   │       ├── context
│   │   │       │   ├── ApplicationContext.java
│   │   │       │   ├── ApplicationContextAware.java
│   │   │       │   ├── ConfigurableApplicationContext.java
│   │   │       │   └── support
│   │   │       │       ├── AbstractApplicationContext.java
│   │   │       │       ├── AbstractRefreshableApplicationContext.java
│   │   │       │       ├── AbstractXmlApplicationContext.java
│   │   │       │       ├── ApplicationContextAwareProcessor.java
│   │   │       │       └── ClassPathXmlApplicationContext.java
│   │   │       ├── core
│   │   │       │   └── io
│   │   │       │       ├── ClassPathResource.java
│   │   │       │       ├── DefaultResourceLoader.java
│   │   │       │       ├── FileSystemResource.java
│   │   │       │       ├── Resource.java
│   │   │       │       ├── ResourceLoader.java
│   │   │       │       └── UrlResource.java
│   │   │       └── utils
│   │   │           └── BeanUtil.java
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

#### 疑惑与思考

##### 为啥非单例 Bean 就不需要执行销毁方法呢？

##### 这 FactoryBean 岂不是把实例化 Bean 的操作又交还给用户啦？

粒度问题。

#### 其他相关

##### InvocationHandler 解析

这玩意是个方法拦截器，

三个参数：

- proxy，即正在被代理的对象。
- method，即被调用的方法。
- args，即传给方法的参数。

它会直接替换对原来方法的调用。

