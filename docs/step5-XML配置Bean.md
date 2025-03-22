### XML 配置 Bean

#### 前置思路

上一个步骤搞完咱立马就提了几个问题，这也配叫 Spring？啥啥属性都得 Client 手动在 java 代码里配置，这能行吗？

最少也得弄个 xml 配置下嘛，这不就把数据和业务逻辑解耦啦（瞎掰的，我也不知道）？

所以今天我们就来思考怎么把给 Bean 设置属性和依赖这事儿放到 xml 配置里去，让框架自动来读取。

#### 具体实现

项目结构：

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
│   │   │           │       ├── ClassPathResource.java --new
│   │   │           │       ├── DefaultResourceLoader.java --new
│   │   │           │       ├── FileSystemResource.java --new
│   │   │           │       ├── Resource.java --new
│   │   │           │       ├── ResourceLoader.java --new
│   │   │           │       └── UrlResource.java --new
│   │   │           ├── factory
│   │   │           │   ├── BeanFactory.java
│   │   │           │   ├── ConfigurableListableBeanFactory.java --new
│   │   │           │   ├── HierarchicalBeanFactory.java --new
│   │   │           │   ├── ListableBeanFactory.java --new
│   │   │           │   ├── config
│   │   │           │   │   ├── AutowireCapableBeanFactory.java --new
│   │   │           │   │   ├── BeanDefinition.java
│   │   │           │   │   ├── BeanReference.java
│   │   │           │   │   ├── ConfigurableBeanFactory.java --new
│   │   │           │   │   └── SingletonBeanRegistry.java
│   │   │           │   ├── support
│   │   │           │   │   ├── AbstractAutowireCapableBeanFactory.java
│   │   │           │   │   ├── AbstractBeanDefinitionReader.java --new
│   │   │           │   │   ├── AbstractBeanFactory.java
│   │   │           │   │   ├── BeanDefinitionReader.java --new
│   │   │           │   │   ├── BeanDefinitionRegistry.java
│   │   │           │   │   ├── CglibSubclassingInstantiationStrategy.java
│   │   │           │   │   ├── DefaultListableBeanFactory.java
│   │   │           │   │   ├── DefaultSingletonBeanRegistry.java
│   │   │           │   │   ├── InstantiationStrategy.java
│   │   │           │   │   └── SimpleInstantiationStrategy.java
│   │   │           │   └── xml
│   │   │           │       └── XmlBeanDefinitionReader.java --new
│   │   │           └── utils
│   │   │               ├── BeanUtil.java
│   │   │               └── ClassUtils.java --new
│   │   └── resources
│   └── test
│       └── java
│           ├── ApiTest.java
│           └── bean
│               ├── UserDao.java
│               └── UserService.java

```

哼哼，这次没抄，是我自己生成的。详见 [Windows 下输出目录结构](#Windows 下输出目录结构)。

呼…… 这次可真是个大工程啊。做好准备！



#### 从 Spring 看设计模式

##### Facade 外观模式

##### 再见工厂方法

#### 疑惑与思考

#### 其他相关

##### Windows 下输出目录结构

shell 自带的 tree 命令有点垃圾，连不输出某个目录都做不到，装个新工具

```bash
npm install -g tree-node-cli
```

然后输入

```bash
#避免和原有的 tree 命令冲突
treee -I "target|docs"
```

就可以得到没有 target 和 docs 文件夹的目录啦！