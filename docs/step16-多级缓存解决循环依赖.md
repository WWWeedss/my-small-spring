### 多级缓存解决循环依赖

#### 前置思考

#### 具体实现

1. 创建一个 ObjectFactory
1. 修改 DefaultSingletonBeanRegistry，增加三级缓存
1. 修改 InstantiationAwareBeanPostProcessor，增加 getEarlyBeanReference，在 DefaultAdvisorAutoProxyCreator 之中实现
1. 修改 AbstractAutowireCapableBeanFactory

所有的 Bean 都会经过 DefaultAdvisorAutoProxyCreator，当然切点表达式匹配不到的就原样返回了，否则就返回一个 ProxyBean。所有的 Bean 一开始都会进入三级缓存，被取出一次后放到二级缓存中，创建完毕就放到一级缓存里。

#### 疑惑与思考

##### Spring 三级缓存存在的意义是什么

从前置思考中就可以看到，解决循环依赖问题，只用一层缓存也完全可以。这三级缓存更像是逻辑上的设计而没有什么实现上的意义，下面是我的学习与思考：

引用[知乎上的一个答案](https://zhuanlan.zhihu.com/p/496273636)：

**使用三级缓存的目的是为了尽量避免改变原有的执行流程。**

首先，用户可以通过扩展点操作缓存内的 Bean，所以用户是知道一级缓存的存在的。

我们希望只有循环依赖的 Bean 才会提前暴露给用户，这句话就可以翻译为：只有完全创建完成的 Bean 才会放到一级缓存中。

这就引出了需要一个二级缓存去保存没有完全创建完毕的 Bean。

而三级缓存其实是为了 AOP ProxyBean 服务的。假设只有两层缓存，我们在 Bean 实例化之后注入属性之前就要把 Bean 存到二级缓存里，假设这个 Bean 需要实现动态代理，如果按照原本的代理逻辑，在 Bean 初始化完成之后进行代理，那循环引用路径中的其他 Bean 引用到的就是代理前的对象了。

所以我们必须要做出选择，如果还是用二级缓存，我们就要在实例化之后直接决定是否代理，并把创建好的代理对象放入二级缓存，或者就是像 Spring 一样再加一层缓存，只有当产生循环引用时，才提前代理。

这样就有了三级缓存。

#### 其他相关

##### 函数式接口、lambda 表达式类型	

```java
new ObjectFactory<>() {
    @Override
    public Object getObject() {
        return getEarlyBeanReference(beanName, beanDefinition, finalBean);
    }
}
```

##### java.lang.IllegalArgumentException: Can not set bean.UserService2 field bean.UserService1.userService2 to com.sun.proxy.$Proxy6 的报错

1. Cglib 进行 AOP 动态代理生成
2. 修订 step15           
