### Bean 属性填充

#### 前置思路

上一个步骤我们能有参构造 Bean 了，现在我们做 Bean 的属性注入……为啥有了有参构造之后还要属性注入啊！这个问题见下方的讨论。[关于实例的多种初始化操作](#关于实例的多种初始化操作)。



属性注入啊，这要怎么搞？

考虑把大象放进冰箱分为三步：

1. 把冰箱门打开。
2. 把大象放进去。
3. 关上冰箱门。

那属性注入怎么搞？

1. 获得属性。
2. 获得属性值。
3. 设置属性值。

好，获得属性有两个办法：

1. 用户设置类属性。
2. 通过反射去获取类属性。

我们今天先做简单的，做第一个。

获得属性值就没得选了，我们得让用户把值传进来。对于依赖的其他类，这个实例考虑让框架来创建。

设置属性值怎么弄，它还有可能是 private 的，那就只能上反射。



总结一下：

1. 用户设置一个 Bean 的属性有哪些，顺便把初始值也设置了。
2. 用户注册 Bean 的时候把属性 List 一起传进来。
3. 用户获取 Bean。
4. 框架没有 Bean，那么得先 Create，Create 之后根据之前传的属性 List 用反射做属性注入，遇到要实例化的类属性，则递归调用 getBean() 方法，这里头继续调用 createBean()（这就要求用户得先注册被依赖的 Bean）。
5. 用户拿到 Bean，做事情。

#### 具体实现

本阶段完成后项目目录如下：

```bash
└── src
    ├── main
    │   └── java
    │       └── springframework.beans
    │           ├── factory
    │           │   ├── config
    │           │   │   ├── BeanDefinition.java --change
    │           │   │   ├── BeanReference.java --new
    │           │   │   └── SingletonBeanRegistry.java
    │           │   ├── support
    │           │   │   ├── AbstractAutowireCapableBeanFactory.java --change
    │           │   │   ├── AbstractBeanFactory.java
    │           │   │   ├── BeanDefinitionRegistry.java 
    │           │   │   ├── CglibSubclassingInstantiationStrategy.java
    │           │   │   ├── DefaultListableBeanFactory.java
    │           │   │   ├── DefaultSingletonBeanRegistry.java
    │           │   │   ├── InstantiationStrategy.java
    │           │   │   └── SimpleInstantiationStrategy.java
    |           |   ├── utils
    │           │   │   └── BeanUtil.java --new
    │           │   └── BeanFactory.java
    │           ├── BeansException.java
    │           ├── PropertyValue.java --new
    │           └── PropertyValues.java --new
    └── test
        └── java
            └── test
                ├── bean
                │   ├── UserDao.java
                │   └── UserService.java
                └── ApiTest.java
```

---

我们先包装属性类。

```java
public class PropertyValue {

    private final String name;

    private final Object value;

    public PropertyValue(String name, Object value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public Object getValue() {
        return value;
    }
}
```

一个实例有多个属性，我们用 List 包装它。

```java
public class PropertyValues {

    private final List<PropertyValue> propertyValueList = new ArrayList<>();

    public void addPropertyValue(PropertyValue pv) {
        this.propertyValueList.add(pv);
    }

    public PropertyValue[] getPropertyValues() {
        return this.propertyValueList.toArray(new PropertyValue[0]);
    }

    public PropertyValue getPropertyValue(String propertyName) {
        for (PropertyValue pv : this.propertyValueList) {
            if (pv.getName().equals(propertyName)) {
                return pv;
            }
        }
        return null;
    }
}
```

这个包装类跟我们直接用 List<PropertyValue> 没啥区别，但是万一得做个迭代器呢，不好说的，包装一下没大问题。

---

定义 BeanReference 类，这个类说明该属性也是个 Bean 实例。我们已经有了框架帮我们存实例，所以这个里面只存 name 就可了。

```java
package springframework.beans.factory.config;

public class BeanReference {

        private final String beanName;

        public BeanReference(String beanName) {
            this.beanName = beanName;
        }

        public String getBeanName() {
            return beanName;
        }
}

```

---

在 AbstractAutowireCapableBeanFactory 里面做属性注入。省略了之前写的代码，只看有修改的部分。

```java
public abstract class AbstractAutowireCapableBeanFactory extends AbstractBeanFactory {
    private InstantiationStrategy instantiationStrategy = new CglibSubclassingInstantiationStrategy();
    @Override
    protected Object createBean(String beanName, BeanDefinition beanDefinition, Object[] args) throws BeansException {
        Object bean;
        try {
            bean = createBeanInstance(beanDefinition, beanName, args);
            applyPropertyValues(beanName, bean, beanDefinition);
        } catch (Exception e) {
            throw new BeansException("Instantiation of bean failed", e);
        }

        addSingleton(beanName, bean);
        return bean;
    }

    protected void applyPropertyValues(String beanName, Object bean, BeanDefinition beanDefinition) {
        try {
            PropertyValues propertyValues = beanDefinition.getPropertyValues();
            for (PropertyValue propertyValue : propertyValues.getPropertyValues()){

                String name = propertyValue.getName();
                Object value = propertyValue.getValue();

                if (value instanceof BeanReference){
                    BeanReference beanReference = (BeanReference) value;
                    // 记得吗，getBean 在 Bean 不存在的时候会先实例化对应的 Bean
                    // 现在的处理方法如果有环形依赖会有问题，后面我们再解决
                    value = getBean(beanReference.getBeanName());
                }

                BeanUtil.setFieldValue(bean, name, value);
            }
        } catch (Exception e) {
            throw new BeansException("Error setting property values for bean: " + beanName);
        }
    }
}
```

---

工具类，BeanUtil，这里要注意，我们用的是 Cglib 创建的实例，这个 Bean 实例是我们声明的类的一个子类，所以你直接在这里找 private 字段是找不到的，得到它父类去找。我们可以顺便复习一下访问修饰符知识点：

| 修饰符         | 类内部 | 同一包 | 子类 | 其他包 |
| -------------- | ------ | ------ | ---- | ------ |
| `public`       | ✔️      | ✔️      | ✔️    | ✔️      |
| `protected`    | ✔️      | ✔️      | ✔️    | ❌      |
| 默认（无修饰） | ✔️      | ✔️      | ❌    | ❌      |
| `private`      | ✔️      | ❌      | ❌    | ❌      |

> 表格引自随便找的一个博客：https://blog.csdn.net/m0_64432106/article/details/142083657

```java
public class BeanUtil {
    public static void setFieldValue(Object bean, String name, Object value) throws NoSuchFieldException {
        // 尝试找到指定字段
        Field field = getField(bean.getClass(), name);
        if (field == null) {
            throw new NoSuchFieldException("Field not found: " + name);
        }

        field.setAccessible(true); // 设置字段可访问
        try {
            field.set(bean, value);  // 设置字段值
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private static Field getField(Class<?> clazz, String name) {
        // 检查当前类的字段
        try {
            return clazz.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            // 如果当前类没有该字段，检查父类
            Class<?> superclass = clazz.getSuperclass();
            if (superclass != null) {
                return getField(superclass, name);
            }
            // 如果没有找到，返回 null
            return null;
        }
    }
}

```

---

##### 测试部分

```java
//这个类的引用会被 UserService 持有。
public class UserDao {
    private static Map<String, String> hashMap = new HashMap<>();

    static {
        hashMap.put("10001", "WWWeeds1");
        hashMap.put("10002", "WWWeeds2");
        hashMap.put("10003", "WWWeeds3");
    }

    public String queryUserName(String uId) {
        return hashMap.get(uId);
    }
}
```

```java
public class UserService {
    private String uId;

    private UserDao userDao;
    public UserService() {
    }

    public UserService(String uId) {
        this.uId = uId;
    }
    public void queryUserInfo() {
        System.out.println("查询用户信息：" + userDao.queryUserName(uId));
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
}
```

```java
public class ApiTest {

    @Test
    public void test_BeanFactory(){
        // 1.初始化 BeanFactory
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

        // 2. UserDao 注册
        beanFactory.registerBeanDefinition("userDao", new BeanDefinition(UserDao.class));

        // 3. UserService 设置属性[uId、userDao]
        PropertyValues propertyValues = new PropertyValues();
        propertyValues.addPropertyValue(new PropertyValue("uId", "10001"));
        propertyValues.addPropertyValue(new PropertyValue("userDao", new BeanReference("userDao")));

        // 4. UserService 注入bean
        BeanDefinition beanDefinition = new BeanDefinition(UserService.class, propertyValues);
        beanFactory.registerBeanDefinition("userService", beanDefinition);

        // 5. UserService 获取bean
        UserService userService = (UserService) beanFactory.getBean("userService");
        userService.queryUserInfo();
    }
}
```

搞定收工。

#### 疑问与思考

##### 关于实例的多种初始化操作

哎，实例的初始化操作不应该在构造函数里搞定吗？这怎么做了有参构造之后还得让咱的框架来填属性啊？

嗯，仔细回想一下咱在用 Spring 框架的时候，用 SpringBoot 的时候确实有两种 Autowired 方式，一个是构造函数注入，一个是属性注入。这俩玩意一般只用一个就行，好像还真是功能略有重叠。行吧，Spring 有咱就得有，那就做吧！

但是这就带来了几个问题：

- 这两种填充属性的方式对于使用者来说，有什么区别。

根据目前的代码，如果使用属性注入，那会由框架去管理依赖和实例化 reference，但是如果用构造函数注入，需要 Client 去实例化依赖，这是很不方便的。这就与我们使用的 Spring 框架不一样，以后得改进。

- 如果既用构造函数注入，又用属性注入，重复了咋办，会报错，还是会覆盖？

这就又涉及到两种方式的执行顺序问题，以目前的代码来看，我们先执行有参构造函数再执行属性注入，实测前者会被后者覆盖。这也是一个问题，后面需要改进。

例子如下：

```java
@Test
public void test_BeanFactory(){
    // 1.初始化 BeanFactory
    DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

    // 2. UserDao 注册
    beanFactory.registerBeanDefinition("userDao", new BeanDefinition(UserDao.class));

    // 3. UserService 设置属性[uId、userDao]
    PropertyValues propertyValues = new PropertyValues();
    propertyValues.addPropertyValue(new PropertyValue("uId", "10001"));
    propertyValues.addPropertyValue(new PropertyValue("userDao", new BeanReference("userDao")));

    // 4. UserService 注入bean
    BeanDefinition beanDefinition = new BeanDefinition(UserService.class, propertyValues);
    beanFactory.registerBeanDefinition("userService", beanDefinition);

    // 5. UserService 获取bean
    // 这里用了有参构造
    UserService userService = (UserService) beanFactory.getBean("userService", "10002");
    userService.queryUserInfo();
}
```

输出不变，仍然是 uId = 10001 的输出，说明有参构造中传入的参数被覆盖。

##### 下一次可能会做啥

类里边儿明明都有字段的定义，可以通过反射机制获取字段啊，就算是具体实例得手动通过 xml 配置一下啥的，这 String 这种字段也得用户手动创建 PropertyValue？

我猜这东西得改。