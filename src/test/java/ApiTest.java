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
        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:spring.xml");

        IUserService userService = applicationContext.getBean("userService", IUserService.class);
        System.out.println(userService.queryUserInfo());
    }
}

