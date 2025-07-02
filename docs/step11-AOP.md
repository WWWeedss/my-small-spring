### AOP

#### 前置思考

1. 引入依赖
2. 生成代理对象 - 多种代理方式
3. 方法匹配、拦截与配置
4. 可实现的拦截方法
5. 反射调用实际方法

#### 具体实现

1. 切点匹配，实现三个接口 Pointcut、MehodMatcher 和 ClassFilter（这玩意有啥用啊）
2. TargetSource -> 被代理的对象、 AdvisedSupport -> 包装匹配器、拦截器、代理对象、ReflectiveMethodInvocation -> 反射方法调用
3. AopProxy、Jdk、Cglib Proxy -> 策略模式生成代理对象

#### 疑惑与思考

#### 其他相关

1. AspectJ weaver ，PointcutPrimitive 以及相关的接口的意思
2. JDK 的动态代理与接口、对比 Cglib 的动态代理
3. aopalliance 与 MethodInterceptor 的相关语义
4. ClassFilter 与 MethodMatcher 的具体意义
5. 私有静态类
6. 为什么 finally 中的信息先打印出来？

##### IDEA 切屏丢失空格问题解决

取消勾选 Remove trailing spaces ……

![image-20250702232633742](https://typora-images-gqy.oss-cn-nanjing.aliyuncs.com/image-20250702232633742.png)