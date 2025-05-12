### 修缮框架

step6 到 step5 有一些跨度，我们要为了 step6 进行一些准备工作。

这些准备工作会影响到我们 step6 的主线，因此我单开一章，将这些零零碎碎的东西整理一下，拣选了一些比较重要的（也就是说还有一些新增的方法我没有写在这里）。

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
│   │   │       └── beans
│   │   │           ├── BeansException.java
│   │   │           ├── PropertyValue.java
│   │   │           ├── PropertyValues.java
│   │   │           ├── core
│   │   │           │   └── io
│   │   │           │       ├── ClassPathResource.java 
│   │   │           │       ├── DefaultResourceLoader.java 
│   │   │           │       ├── FileSystemResource.java
│   │   │           │       ├── Resource.java 
│   │   │           │       ├── ResourceLoader.java
│   │   │           │       └── UrlResource.java
│   │   │           ├── factory
│   │   │           │   ├── BeanFactory.java 
│   │   │           │   ├── ConfigurableListableBeanFactory.java --new
│   │   │           │   ├── HierarchicalBeanFactory.java --new
│   │   │           │   ├── ListableBeanFactory.java --new
│   │   │           │   ├── config
│   │   │           │   │   ├── AutowireCapableBeanFactory.java 
│   │   │           │   │   ├── BeanDefinition.java
│   │   │           │   │   ├── BeanReference.java
│   │   │           │   │   ├── ConfigurableBeanFactory.java 
│   │   │           │   │   └── SingletonBeanRegistry.java
│   │   │           │   ├── support
│   │   │           │   │   ├── AbstractAutowireCapableBeanFactory.java 
│   │   │           │   │   ├── AbstractBeanDefinitionReader.java 
│   │   │           │   │   ├── AbstractBeanFactory.java 
│   │   │           │   │   ├── BeanDefinitionReader.java --change
│   │   │           │   │   ├── BeanDefinitionRegistry.java 
│   │   │           │   │   ├── CglibSubclassingInstantiationStrategy.java
│   │   │           │   │   ├── DefaultListableBeanFactory.java 
│   │   │           │   │   ├── DefaultSingletonBeanRegistry.java
│   │   │           │   │   ├── InstantiationStrategy.java
│   │   │           │   │   └── SimpleInstantiationStrategy.java
│   │   │           │   └── xml
│   │   │           │       └── XmlBeanDefinitionReader.java --change
│   │   │           └── utils
│   │   │               └── BeanUtil.java
│   │   └── resources
│   └── test
│       └── java
│           ├── ApiTest.java
│           └── bean
│               ├── UserDao.java
│               └── UserService.java
```

---

多个地址资源的读入

```java
public interface BeanDefinitionReader {
    void loadBeanDefinitions(String... locations) throws BeansException;
}
```

```java
public class XmlBeanDefinitionReader extends AbstractBeanDefinitionReader {
    @Override
    public void loadBeanDefinitions(String... locations) throws BeansException {
        for (String location : locations) {
            loadBeanDefinitions(location);
        }
    }
}
```

---

使用泛型获取指定类型的 Bean

```java
public interface ListableBeanFactory extends BeanFactory{
    /**
     * 按照类型返回 Bean 实例
     * @param type
     * @param <T>
     * @return
     */
    <T> Map<String,T> getBeansOfType(Class<T> type) throws BeansException;


    /**
     * 返回注册表中所有的Bean名称
     */
    String[] getBeanDefinitionNames();
}
```

```java
public class DefaultListableBeanFactory extends AbstractAutowireCapableBeanFactory implements BeanDefinitionRegistry, ConfigurableListableBeanFactory {

    private final Map<String, BeanDefinition> beanDefinitionMap = new HashMap<>();

    @Override
    public <T> Map<String, T> getBeansOfType(Class<T> type) throws BeansException {
        Map<String, T> result = new HashMap<>();
        beanDefinitionMap.forEach((beanName, beanDefinition) -> {
            Class beanClass = beanDefinition.getBeanClass();
            if (type.isAssignableFrom(beanClass)) {
                result.put(beanName, (T) getBean(beanName));
            }
        });
        return result;
    }
}
```

---

新增接口

```java
public interface ConfigurableListableBeanFactory extends ListableBeanFactory, AutowireCapableBeanFactory, ConfigurableBeanFactory {

    BeanDefinition getBeanDefinition(String beanName) throws BeansException;
	//提前实例化单例 Bean 对象，这是框架自动做的，不需要用户主动调用（但是现在我们只有单例 Bean）
    void preInstantiateSingletons() throws BeansException;
}
```

```java
public class DefaultListableBeanFactory extends AbstractAutowireCapableBeanFactory implements BeanDefinitionRegistry, ConfigurableListableBeanFactory {

    private final Map<String, BeanDefinition> beanDefinitionMap = new HashMap<>();

    @Override
    public BeanDefinition getBeanDefinition(String beanName) throws BeansException {
        BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
        if (beanDefinition == null) {
            throw new BeansException("No bean named '" + beanName + "' is defined");
        }
        return beanDefinition;
    }
    

    @Override
    public void preInstantiateSingletons() throws BeansException {
        beanDefinitionMap.keySet().forEach(this::getBean);
    }
}
```

---

空类，以后可能会用到。

```java
public interface HierarchicalBeanFactory extends BeanFactory {
}
```

#### 其他相关

当项目逐渐膨胀的时候，我们阅读起来就越来越费劲了。好在我们有 IDE，这会让阅读过程简单不少。

##### 如何用 IDEA 查看实现类？

###### 方法零

鼠标点这个绿色小图标就好。

![image-20250511232325317](https://typora-images-gqy.oss-cn-nanjing.aliyuncs.com/image-20250511232325317.png)

###### 方法一

以查看 ListableBeanFactory 实现类为例。

选中接口类名，右键，按下图选择。

![image-20250324094647567](https://typora-images-gqy.oss-cn-nanjing.aliyuncs.com/image-20250324094647567.png)

进入类关系页面，再右键按下图选择

![image-20250324094927411](https://typora-images-gqy.oss-cn-nanjing.aliyuncs.com/image-20250324094927411.png)

ctrl + A 全选，回车

![image-20250324094938220](https://typora-images-gqy.oss-cn-nanjing.aliyuncs.com/image-20250324094938220.png)

获得类关系图

![image-20250324095011214](https://typora-images-gqy.oss-cn-nanjing.aliyuncs.com/image-20250324095011214.png)

###### 方法二

ctrl + shift + 左键 快速查看实现类。

###### 方法三

如果你只是想查看某一个方法的实现，那这是最快捷方便的。

选中方法，按 ctrl + shift + h。

##### 如何查看这个方法被谁调用

选中该方法，按 ctrl + alt + f7 或者 ctrl + alt + h。