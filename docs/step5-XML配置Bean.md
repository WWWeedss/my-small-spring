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
│   │   │           │   ├── BeanFactory.java --change
│   │   │           │   ├── config
│   │   │           │   │   ├── AutowireCapableBeanFactory.java --new
│   │   │           │   │   ├── BeanDefinition.java
│   │   │           │   │   ├── BeanReference.java
│   │   │           │   │   ├── ConfigurableBeanFactory.java --new
│   │   │           │   │   └── SingletonBeanRegistry.java
│   │   │           │   ├── support
│   │   │           │   │   ├── AbstractAutowireCapableBeanFactory.java 
│   │   │           │   │   ├── AbstractBeanDefinitionReader.java --new
│   │   │           │   │   ├── AbstractBeanFactory.java --change
│   │   │           │   │   ├── BeanDefinitionReader.java --new
│   │   │           │   │   ├── BeanDefinitionRegistry.java --change
│   │   │           │   │   ├── CglibSubclassingInstantiationStrategy.java
│   │   │           │   │   ├── DefaultListableBeanFactory.java --change
│   │   │           │   │   ├── DefaultSingletonBeanRegistry.java
│   │   │           │   │   ├── InstantiationStrategy.java
│   │   │           │   │   └── SimpleInstantiationStrategy.java
│   │   │           │   └── xml
│   │   │           │       └── XmlBeanDefinitionReader.java --new
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

哼哼，这次没抄，是我自己生成的。详见 [Windows 下输出目录结构](#Windows 下输出目录结构)。

呼…… 这次工程量可不小啊，但理解上并不困难，做好准备！

---

开始之前，咱们先来随手加几个小方法。

```java
public interface BeanFactory {
    <T> T getBean(String name, Class<T> requiredType) throws BeansException;
}
```

```java
public abstract class AbstractBeanFactory extends DefaultSingletonBeanRegistry implements BeanFactory {
    @Override
    public Object getBean(String name) throws BeansException {
        return doGetBean(name, null);
    }

    @Override
    public Object getBean(String name, Object... args) throws BeansException {
        return doGetBean(name, args);
    }

    @Override
    public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
        return (T) getBean(name);
    }

    protected <T> T doGetBean(final String name, final Object[] args) {
        Object bean = getSingleton(name);
        if (bean != null) {
            return (T) bean;
        }

        BeanDefinition beanDefinition = getBeanDefinition(name);
        return (T) createBean(name, beanDefinition, args);
    }
}
```

这个方法是避免强制类型转换……嗯，避免了个鬼，还是 Object 在强制转换为 T 呐，无所谓了，看着挺高级，学着吧就。

```java
public interface BeanDefinitionRegistry {
    boolean containsBeanDefinition(String beanName);

    String[] getBeanDefinitionNames();
}
```

```java
public class DefaultListableBeanFactory extends AbstractAutowireCapableBeanFactory implements BeanDefinitionRegistry {
    
    private final Map<String, BeanDefinition> beanDefinitionMap = new HashMap<>();

    @Override
    public boolean containsBeanDefinition(String beanName) {
        return beanDefinitionMap.containsKey(beanName);
    }

    @Override
    public String[] getBeanDefinitionNames() {
        return beanDefinitionMap.keySet().toArray(new String[0]);
    }
}
```

加了两个工具方法，以后会有用。

---

好的，让我们开始干解析文件的事情。有三种给文件路径的方式：

1. Java 的 classpath 机制，暂且理解为从 Resources 文件夹下找文件就行。因为是一个统一标记，所以打包部署之后也能访问到。
2. 系统文件路径，一般从 src 开始给，给绝对路径还是不太好。
3. URL，这就是可以把配置文件放到云上。

现在调用方给了一条路径出来，我们要把文件的内容转化成字节流输出出来。那么这里就需要一个工具。

这个工具应当暴露一个唯一的接口，接受一个路径字符串，返回字节流。

---

假设我们已经把路径转化为了想要的文件，譬如 URL 对象，File 对象等，显然它们都需要实现一个共同的接口：返回字节流。



这是输出字节流的接口。这虽然是接口，但是对应文件就存在里面，因此取名“Resource”，是个名词。嗯，所以我感觉这玩意直觉上应该用继承做，但是每个子类的属性都不一样，只有这一个方法是一样的，所以算了吧。

```java
public interface Resource {
    InputStream getInputStream() throws IOException;
}

```

三种不同的实现。

```java
public class ClassPathResource implements Resource{

    private final String path;

    public ClassPathResource(String path) {
        // 委托给另一个构造器
        this.path = path;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        InputStream is = ClassLoader.getSystemResourceAsStream(path);
        if (is == null) {
            throw new IOException(path + " cannot be opened");
        }
        return is;
    }
}
```

```java
public class FileSystemResource implements Resource{

    private final File file;
    public FileSystemResource(File file) {
        this.file = file;
    }

    public FileSystemResource(String path) {
        this.file = new File(path);
    }
    @Override
    public InputStream getInputStream() throws IOException {
        return new FileInputStream(this.file);
    }
}
```

```java
public class UrlResource implements Resource{
    private final URL url;

    public UrlResource(URL url) {
        assert url != null : "URL must not be null";
        this.url = url;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        URLConnection con = this.url.openConnection();
        try {
            return con.getInputStream();
        }
        catch (IOException ex) {
            // Close the HTTP connection (if applicable).
            if (con instanceof java.net.HttpURLConnection) {
                ((java.net.HttpURLConnection) con).disconnect();
            }
            throw ex;
        }
    }
}
```

---

然后我们看面向调用方的接口。

```java
public interface ResourceLoader {

    String CLASSPATH_URL_PREFIX = "classpath:";

    Resource getResource(String location);
}
```

我们可以通过路径本身来判断用那种方式去解析它。

```java
public class DefaultResourceLoader implements ResourceLoader{
    @Override
    public Resource getResource(String location) {
        assert location != null : "Location must not be null";
        if(location.startsWith(CLASSPATH_URL_PREFIX)){
            return new ClassPathResource(location.substring(CLASSPATH_URL_PREFIX.length()));
        }
        else{
            try {
                URL url = new URL(location);
                return new UrlResource(url);
            } catch (MalformedURLException e) {
                return new FileSystemResource(location);
            }
        }
    }
}
```

---

现在我们有了字节流，开始解析吧。解析 xml 文件这种事情，肯定已经有现成的工具了，我们随便找一个。

```xml
<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-all</artifactId>
    <version>5.8.24</version>
</dependency>
```

想一想用户需要什么接口？用户给我们一条路径，那里头包含着 xml 写的 bean 依赖信息，我们得把 beanDefinition 生成好放到 Map 里面去。

所以这个接口是这样的，接受一个 String 参数，啥也不返回，因为信息都被存到 Map 里了。

```java
public interface BeanDefinitionReader {
    //这玩意是我们之前写的 Bean 注册器。
    BeanDefinitionRegistry getRegistry();

    //这玩意是我们刚写的文件获取器。
    ResourceLoader getResourceLoader();

    void loadBeanDefinitions(Resource resource) throws Exception;

    void loadBeanDefinitions(Resource... resources) throws Exception;

    //这个就是用户最终调用的接口
    void loadBeanDefinitions(String location) throws Exception;
}
```

前两个无关紧要的接口先拿个抽象类垫一下，这俩接口不存在多种实现。而后面三个接口，除了读 xml 配置之外有没有可能用别的方式？感觉是有可能的，SpringBoot 不就没拿 xml 去配置么，所以那些放到子类里，成为一种策略模式，便于以后添加。

```java
public abstract class AbstractBeanDefinitionReader implements BeanDefinitionReader{

    private final BeanDefinitionRegistry registry;

    private ResourceLoader resourceLoader;

    //这里用了一个委托构造的特性，不过其实也没啥特别的，就是为了避免 null 值出现。
    protected AbstractBeanDefinitionReader(BeanDefinitionRegistry registry) {
        this(registry, new DefaultResourceLoader());
    }

    public AbstractBeanDefinitionReader(BeanDefinitionRegistry registry, ResourceLoader resourceLoader){
        this.registry = registry;
        this.resourceLoader = resourceLoader;
    }

    @Override
    public BeanDefinitionRegistry getRegistry() {
        return registry;
    }

    @Override
    public ResourceLoader getResourceLoader() {
        return resourceLoader;
    }
}
```

现在我们来看实现类。

```java
public class XmlBeanDefinitionReader extends AbstractBeanDefinitionReader {

    public XmlBeanDefinitionReader(BeanDefinitionRegistry registry){
        super(registry);
    }

    public XmlBeanDefinitionReader(BeanDefinitionRegistry registry, ResourceLoader resourceLoader){
        super(registry, resourceLoader);
    }

    @Override
    public void loadBeanDefinitions(String location) throws Exception {
        ResourceLoader resourceLoader = getResourceLoader();
        Resource resource = resourceLoader.getResource(location);
        loadBeanDefinitions(resource);
    }

    @Override
    public void loadBeanDefinitions(Resource... resources) throws Exception {
        for (Resource resource : resources) {
            loadBeanDefinitions(resource);
        }
    }

    //三个接口最终都调用的是这个方法
    @Override
    public void loadBeanDefinitions(Resource resource) throws Exception {
        try (InputStream inputStream = resource.getInputStream()){
            doLoadBeanDefinitions(inputStream);
        } catch (IOException | ClassNotFoundException e) {
            throw new BeansException("IOException parsing XML document from " + resource, e);
        }
    }

    //转化为字节流之后，我们直接用 XmlUtil 这个现成的接口把 xml 文件翻译成树状结构
    protected void doLoadBeanDefinitions(InputStream inputStream) throws ClassNotFoundException {
        Document doc = XmlUtil.readXML(inputStream);
        Element root = doc.getDocumentElement();
        NodeList childNodes = root.getChildNodes();

        for(int i = 0; i < childNodes.getLength(); i++){
            if(!(childNodes.item(i) instanceof Element)){
                continue;
            }
            // 判断是否是 bean 对象
            if(!"bean".equals(childNodes.item(i).getNodeName())){
                continue;
            }

            // 找到 bean 就解析
            parseBeanElement((Element)childNodes.item(i));
        }
    }

    protected void parseBeanElement(Element bean) throws ClassNotFoundException {
        String id = bean.getAttribute("id");
        String name = bean.getAttribute("name");
        String className = bean.getAttribute("class");

        Class<?> clazz = Class.forName(className);
        // isEmpty() : length == 0 || str == null
        // 反正我们只要唯一标识，所以优先用 id
        String beanName = StrUtil.isNotEmpty(id) ? id : name;
        if(StrUtil.isEmpty(beanName)){
            beanName = StrUtil.lowerFirst(clazz.getSimpleName());
        }

        // 创建 BeanDefinition
        BeanDefinition beanDefinition = new BeanDefinition(clazz);

        // 解析 Bean 属性并填充
        for(int j = 0; j < bean.getChildNodes().getLength(); j++){
            if(!(bean.getChildNodes().item(j) instanceof Element)){
                continue;
            }
            if(!"property".equals(bean.getChildNodes().item(j).getNodeName())){
                continue;
            }
            parsePropertyElement((Element)bean.getChildNodes().item(j), beanDefinition);
        }

        if (getRegistry().containsBeanDefinition(beanName)) {
            throw new BeansException("Duplicate beanName[" + beanName + "] is not allowed");
        }

        // 注册 BeanDefinition
        getRegistry().registerBeanDefinition(beanName, beanDefinition);
    }

    protected void parsePropertyElement(Element property, BeanDefinition beanDefinition) {
        String attrName = property.getAttribute("name");
        String attrValue = property.getAttribute("value");
        String attrRef = property.getAttribute("ref");

        // 如果有 ref 属性，那么就是 BeanReference，否则就是普通的值对象
        Object value = StrUtil.isNotEmpty(attrRef) ? new BeanReference(attrRef) : attrValue;

        PropertyValue propertyValue = new PropertyValue(attrName, value);
        beanDefinition.getPropertyValues().addPropertyValue(propertyValue);
    }
}
```

完毕，这就是一个比较机械的过程，如果各位学过编译原理，或者做过 Java 类关系解析器应该会感觉非常非常熟悉。真正繁杂的字符串解析工作都被工具和库搞定了，我们封装一层就可以。

---

##### 测试部分

Bean：

UserDao 不用改。

```java
public class UserService {
    private String uId;

    private UserDao userDao;
    public UserService() {
    }

    public UserService(String uId) {
        this.uId = uId;
    }
    
    //这里加个返回值
    public String queryUserInfo() {
        System.out.println("查询用户信息：" + userDao.queryUserName(uId));
        return userDao.queryUserName(uId);
    }

    public String getuId() {
        return uId;
    }

    public void setuId(String uId) {
        this.uId = uId;
    }

    public UserDao getUserDao() {
        return userDao;
    }

    public void setUserDao(UserDao userDao) {
        this.userDao = userDao;
    }

    //可以通过这种方式获得类的全称类名，便于我们写 xml 配置文件
    public static void main(String[]  args){
        Class<?> clazz = UserService.class;
        System.out.println(clazz.getName());
    }
}
```

配置文件：

```properties
# Config File，用来测试文件读取器
system.key=OLpj9823dZ
```

这个文件用来测试 xml 解析器

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans>

    <bean id="userDao" class="bean.UserDao"/>

    <bean id="userService" class="bean.UserService1">
        <property name="uId" value="10001"/>
        <property name="userDao" ref="userDao"/>
    </bean>

</beans>
```

单元测试：

```java
public class ApiTest {
    private DefaultResourceLoader resourceLoader;

    @Before
    public void init() {
        resourceLoader = new DefaultResourceLoader();
    }

    @Test
    public void test_classpath() throws IOException {
        Resource resource = resourceLoader.getResource("classpath:important.properties");
        InputStream inputStream = resource.getInputStream();
        String content = IoUtil.readUtf8(inputStream);
        System.out.println(content);
    }

    @Test
    public void test_file() throws IOException {
        Resource resource = resourceLoader.getResource("src/main/resources/important.properties");
        InputStream inputStream = resource.getInputStream();
        String content = IoUtil.readUtf8(inputStream);
        System.out.println(content);
    }

    @Test
    public void test_url() throws IOException {
        Resource resource = resourceLoader.getResource("https://raw.githubusercontent.com/WWWeedss/my-small-spring/master/src/main/resources/important.properties");
        InputStream inputStream = resource.getInputStream();
        String content = IoUtil.readUtf8(inputStream);
        System.out.println(content);
    }

    @Test
    public void test_xml() throws Exception {
        // 1.初始化 BeanFactory
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

        // 2. 读取配置文件&注册Bean
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(beanFactory);
        reader.loadBeanDefinitions("classpath:spring.xml");

        // 3. 获取Bean对象调用方法
        UserService userService = beanFactory.getBean("userService", UserService.class);
        String result = userService.queryUserInfo();
        System.out.println("测试结果：" + result);
    }
}
```

在测试读取 url 之前，因为这资源在 github 上，小伙伴们记得配置一下梯子，[IDEA 使用代理](#IDEA 配置代理)。

#### 从 Spring 看设计模式

##### Facade 外观模式

ResourceLoader 的处理就是非常典型非常成功的 Facade 模式，把子系统的一系列复杂操作最后封装成一个单一的接口，调用者可以无脑调用。

仔细思考一下，三种文件路径我们都可以通过路径本身区别出来，这就不需要传递额外的控制参数，耦合度大大降低了。但是如果没法区分的话，这个接口要怎么做？用 1,2,3 参数去控制？还是做个策略模式？总之也是挺麻烦的事情。

但不管怎么样，我们还是力求要做到：

1. 绝不暴露调用方用不着的方法接口
2. 调用方的调用流程尽可能简短

##### 再一次：策略模式？工厂方法模式？

在 BeanDefinitionReader 和 AbstractBeanDefinitionReader 中，再次将关键方法放到了子类中去实现。这就让我们想起了当时的 AbstractBeanFactory，它把 createBean() 放到了子类中实现，当时我们还兴致勃勃地猜想是不是会有不同种类的 Bean 要创建。

今天我们又遇上了。loadBeanDefinitions() 也被放到了子类中实现，要是有不同的配置 Bean 方式，我们也可以符合开闭原则地去做。

但这玩意可不是创建实例的方法，用“工厂方法”称呼就不太妥当了。“策略模式”？好像搭得上边，又也不太准确，但是它的思想内核还是不变的：

1. 最上层只让 Client 依赖少量接口。
2. 中间使用抽象类，将不需要多态的接口实现。
3. 下层的实现类符合开闭原则，可以通过添加类的方式增加实现方式。

#### 疑惑与思考

##### 配置 Bean 还是很麻烦

确实，我们用不着写 java 代码了，可以写个 xml 去配置 bean 的依赖。但是我还是感觉麻烦！

可能这就是被 SpringBoot 惯坏了。啥时候能直接扫描或者通过类里面声明的信息直接把依赖关系解读出来？

哦，不过初始值还是要配置一下……SpringBoot 是怎么做的？忘球了，以后再说吧。

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

##### IDEA 配置代理

File - Settings - Appearance & Behavior - System Settings - Http Proxy

譬如我用的是 clash for windows，端口号 7890，像这样设置就可以。

![image-20250322155749078](https://typora-images-wwweeds.oss-cn-hangzhou.aliyuncs.com/image-20250322155749078.png)