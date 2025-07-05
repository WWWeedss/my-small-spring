import bean.IUserService;
import bean.UserService;
import bean.UserServiceInterceptor;
import org.aopalliance.intercept.MethodInterceptor;
import org.junit.Test;
import springframework.aop.AdvisedSupport;
import springframework.aop.MethodMatcher;
import springframework.aop.TargetSource;
import springframework.aop.aspectj.AspectJExpressionPointcut;
import springframework.aop.framework.Cglib2AopProxy;
import springframework.aop.framework.JdkDynamicAopProxy;
import springframework.aop.framework.ReflectiveMethodInvocation;
import springframework.context.support.ClassPathXmlApplicationContext;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
public class ApiTest {
    @Test
    public void test_aop() throws NoSuchMethodException {
        AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut("execution(* bean.UserService.*(..))");
        Class<UserService> clazz = UserService.class;
        Method method = clazz.getDeclaredMethod("queryUserInfo");
        
        System.out.println(pointcut.matches(clazz));
        System.out.println(pointcut.matches(method, clazz));
    }
    
    @Test
    public void test_proxy_class() {
        IUserService userService = (IUserService) Proxy.newProxyInstance(
                Thread.currentThread().getContextClassLoader(),
                new Class[]{IUserService.class},
                (proxy, method, args) -> "你被代理了！"
        );
        String result = userService.queryUserInfo();
        System.out.println("测试结果：" + result);
    }
    
    @Test
    public void test_proxy_method() {
        Object target = new UserService();
        
        IUserService proxy = (IUserService) Proxy.newProxyInstance(
                Thread.currentThread().getContextClassLoader(),
                target.getClass().getInterfaces(),
                new InvocationHandler() {
                    MethodMatcher methodMatcher = new AspectJExpressionPointcut("execution(* bean.IUserService.*(..))");
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        if (methodMatcher.matches(method, target.getClass())){
                            MethodInterceptor methodInterceptor = invocation -> {
                                long start = System.currentTimeMillis();
                                try {
                                    return invocation.proceed();
                                } finally {
                                    System.out.println("监控 - Begin By AOP");
                                    Method m = invocation.getMethod();
                                    System.out.println("方法名称：" + m.getDeclaringClass().getName() + "." + m.getName());
                                    System.out.println("方法耗时：" + (System.currentTimeMillis() - start) + "ms");
                                    System.out.println("监控 - End\r\n");
                                }
                            };
                            return methodInterceptor.invoke(new ReflectiveMethodInvocation(target, method, args));
                        }
                        return method.invoke(target, args);
                    }
                }
        );
        String result = proxy.queryUserInfo();
        System.out.println("测试结果：" + result);
    }

    @Test
    public void test_dynamic() {
        IUserService userService = new UserService();

        AdvisedSupport advisedSupport = new AdvisedSupport();
        advisedSupport.setTargetSource(new TargetSource(userService));
        advisedSupport.setMethodInterceptor(new UserServiceInterceptor());
        advisedSupport.setMethodMatcher(new AspectJExpressionPointcut("execution(* bean.IUserService.*(..))"));

        IUserService proxy_jdk = (IUserService) new JdkDynamicAopProxy(advisedSupport).getProxy();
        System.out.println("测试结果：" + proxy_jdk.queryUserInfo());

        IUserService proxy_cglib = (IUserService) new Cglib2AopProxy(advisedSupport).getProxy();
        System.out.println("测试结果：" + proxy_cglib.register("占鑫"));
    }
}

