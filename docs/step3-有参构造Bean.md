### 有参构造 Bean

#### 前置思路

上一次我们实现了对 singleBean 的复用，并且 BeanDefinition 内只存储了 Class 信息，把实例化放到了容器内部。但是紧跟着就是新的问题：

Bean 的构造函数需要参数怎么办？

能传参数，就能进一步考虑 Bean 的依赖关系处理啦，当然那个我们后面再说，现在先来实现 Bean 的有参构造。

#### 具体实现

本步骤完成后的项目目录：

```bash
└── src
    ├── main
    │   └── java
    │       └── springframework.beans
    │           ├── factory
    │           │   ├── config
    │           │   │   ├── BeanDefinition.java
    │           │   │   └── SingletonBeanRegistry.java
    │           │   ├── support
    │           │   │   ├── AbstractAutowireCapableBeanFactory.java --change
    │           │   │   ├── AbstractBeanFactory.java --change
    │           │   │   ├── BeanDefinitionRegistry.java
    │           │   │   ├── CglibSubclassingInstantiationStrategy.java --new
    │           │   │   ├── DefaultListableBeanFactory.java
    │           │   │   ├── DefaultSingletonBeanRegistry.java
    │           │   │   ├── InstantiationStrategy.java --new
    │           │   │   └── SimpleInstantiationStrategy.java --new
    │           │   └── BeanFactory.java --change
    │           └── BeansException.java
    └── test
        └── java
            └── test
                ├── bean
                │   └── UserService.java
                └── ApiTest.java
```



要实例化 Bean 有多种方法，一种是之前我们用的，用反射去实例化，我们还可以通过 Cglib 或者 bytebuddy 之类的库去实例化。为啥要用它们呢？我们后面可以看到作用。

既然有多种实例化的方式，且它们都应该遵循同一个接口，那这里应用策略模式就很自然了。

---

实例化接口类

```java
public interface InstantiationStrategy {
    /**
     *
     * @param beanDefinition 获取将要实例化的类信息
     * @param beanName 将要实例化的类名
     * @param ctor 构造器，即符合对应参数的构造函数
     * @param args 构造器参数
     * @return
     * @throws BeansException
     */
    Object instantiate(BeanDefinition beanDefinition, String beanName, Constructor<?> ctor, Object[] args) throws BeansException;
}
```

---

使用反射机制的实现类

```java 
public class SimpleInstantiationStrategy implements  InstantiationStrategy{
    @Override
    public Object instantiate(BeanDefinition beanDefinition, String beanName, Constructor<?> ctor, Object[] args) throws BeansException {
        Class clazz = beanDefinition.getBeanClass();
        try {
            if (ctor != null) {
                return clazz.getDeclaredConstructor(ctor.getParameterTypes()).newInstance(args);
            } else {
                //无参构造, 跟 clazz.getDeclaredConstructor().newInstance() 结果一样
                return clazz.newInstance();
            }
        }
//         四个异常分别对应
//          没找到对应参数类型的构造函数
//          无法实例化，譬如尝试实例化一个接口
//          构造函数是私有的
//          构造函数抛出异常
        catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new BeansException("Failed to instantiate [" + clazz.getName() + "]", e);
        }
    }
}

```

---

pom.xml 添加 Cglib 依赖

```xml
<dependency>
    <groupId>cglib</groupId>
    <artifactId>cglib</artifactId>
    <version>3.3.0</version>  <!-- 你可以根据需要使用最新版本 -->
</dependency>
```

使用 Cglib 的实现类

```java
public class CglibSubclassingInstantiationStrategy implements InstantiationStrategy{
    @Override
    public Object instantiate(BeanDefinition beanDefinition, String beanName, Constructor<?> ctor, Object[] args) throws BeansException {
        Enhancer enhancer = new Enhancer();

        // enhancer 会生成一个动态子类实例给我们用
        enhancer.setSuperclass(beanDefinition.getBeanClass());

        // setCallback 的意思即调用该实例的任何方法之后都会触发回调。NoOp 是一个空的回调，即不做任何事情。
        enhancer.setCallback(new NoOp() {
            @Override
            public int hashCode() {
                return super.hashCode();
            }
        });

        // 无参与有参构造函数
        if (ctor == null) {
            return enhancer.create();
        }
        return enhancer.create(ctor.getParameterTypes(), args);
    }
}
```

好吧，从这里我们就可以看出来了，Cglib 除了把类的实例给我们之外，还支持我们做别的操作，譬如设置回调函数，这个函数将在该实例的每个方法调用之后都被调用，嗯，我们简直可以从中看到 AOP 的影子了。

看来光写 Spring 框架还不行，还得去读一读 Cglib 这个回调函数是咋实现的，才能从最底层去理解 AOP 之类的……再说吧就。

---

在 BeanFactory 增加有参 getBean() 接口

```java
public interface BeanFactory {
    Object getBean(String name) throws BeansException;

    Object getBean(String name, Object... args) throws BeansException;
}
```

---

然后是 AbstractBeanFactory 的修改。

```java
public abstract class AbstractBeanFactory extends DefaultSingletonBeanRegistry implements BeanFactory {

    @Override
    public Object getBean(String name) throws BeansException {
        return doGetBean(name, null);
    }

    //构造函数参数由 Client 传进来
    //这里用了 Java 的可变长参数特性，这个和传一个 Object[] 没啥区别，就是调用方省事了，不需要显式地构造一个数组
    //在方法内部，args 就是一个 Object[]，如果不传参数就是个空数组
    @Override
    public Object getBean(String name, Object... args) throws BeansException {
        return doGetBean(name, args);
    }

    protected Object doGetBean(final String name, final Object[] args){
        Object bean = getSingleton(name);
        if (bean != null) {
            return bean;
        }

        BeanDefinition beanDefinition = getBeanDefinition(name);
        return createBean(name, beanDefinition, args);
    }

    protected abstract BeanDefinition getBeanDefinition(String beanName) throws BeansException;
	
    //这里的 createBean 的参数列表增加了一个 Object[] args
    protected abstract Object createBean(String beanName, BeanDefinition beanDefinition, Object[] args) throws BeansException;
}
```

最后我们在 AbstractAutowireCapableBeanFactory 这个类里（它在上一个步骤内就是实现 createBean 方法的那个类）来调用我们写好的生成策略。

```java
public abstract class AbstractAutowireCapableBeanFactory extends AbstractBeanFactory {
    private InstantiationStrategy instantiationStrategy = new CglibSubclassingInstantiationStrategy();
    @Override
    protected Object createBean(String beanName, BeanDefinition beanDefinition, Object[] args) throws BeansException {
        Object bean;
        try {
            bean = createBeanInstance(beanDefinition, beanName, args);
        } catch (Exception e) {
            throw new BeansException("Instantiation of bean failed", e);
        }

        addSingleton(beanName, bean);
        return bean;
    }

    protected Object createBeanInstance(BeanDefinition beanDefinition, String beanName, Object[] args) throws BeansException {
        Constructor constructorToUse = null;
        Class<?> beanClass = beanDefinition.getBeanClass();
        //这里获取了 beanClass 声明的全部构造函数
        //这里我们复习一下知识点，啥构造函数不声明，就有个默认无参构造，但是一旦声明了有参构造函数，默认构造函数就没了，各位可以打个断点看一看 declaredConstructors，确实是这个样子。
        Constructor<?>[] declaredConstructors = beanClass.getDeclaredConstructors();
        for (Constructor ctor : declaredConstructors) {
            if (args != null && checkConstructorArguments(ctor, args)) {
                constructorToUse = ctor;
                break;
            }
        }

        // 当传入的参数不为空，但是没有找到合适的构造函数，抛出参数不匹配异常
        if(args != null && constructorToUse == null){
            throw new BeansException("can't find a apporiate constructor");
        }

        return instantiationStrategy.instantiate(beanDefinition, beanName, constructorToUse, args);
    }

    boolean checkConstructorArguments(Constructor<?> ctor, Object[] args) {
        if (ctor.getParameterTypes().length != args.length) {
            return false;
        }
        // 参数类型匹配
        for (int i = 0; i < ctor.getParameterTypes().length; i++) {
            if (!ctor.getParameterTypes()[i].isAssignableFrom(args[i].getClass())) {
                return false;
            }
        }
        return true;
    }

    public void setInstantiationStrategy(InstantiationStrategy instantiationStrategy) {
        this.instantiationStrategy = instantiationStrategy;
    }
}
```

---

测试文件：给原来的 UserService 加一个字段。

```java
public class UserService {
    private String name;

    public UserService() {
    }
    public UserService(String name) {
        this.name = name;
    }

    public void queryUserInfo(){
        System.out.println("查询用户信息");
    }

    @Override
    public String toString() {
        return "UserService{" + name + '}';
    }
}

```

然后是测试用例。

```java
public class ApiTest {

    @Test
    public void test_BeanFactory(){
        // 1.初始化 BeanFactory
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

        // 2.注入bean
        BeanDefinition beanDefinition = new BeanDefinition(UserService.class);
        beanFactory.registerBeanDefinition("userService", beanDefinition);

        // 3.获取bean
        UserService userService = (UserService) beanFactory.getBean("userService","WWWeeds");

        assert Objects.equals(userService.toString(), "UserService{WWWeeds}");
    }
}
```

注意这里跑的时候，要在运行参数里加上：

```bash
--add-opens java.base/java.lang=ALL-UNNAMED 
```

这是因为 Cglib 库调用了 java.lang.ClassLoader.defineClass，在 Java9 之后，java.lang 包就不开放给未命名模块了，我们得在运行里加上参数来开放 java.lang 包。

综上，我们用策略模式实现了 Bean 的有参构造函数。

#### 疑问与思考

这一节比较简单，我没啥问题。

但我们可以思考一下下一节或许会干啥，我猜得处理 Bean 的依赖了，或者是其他的 Bean 初始化之类的事情。嗯，等着看吧。