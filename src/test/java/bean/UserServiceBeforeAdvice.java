package bean;

import springframework.aop.MethodBeforeAdvice;
import springframework.stereotype.Component;

import java.lang.reflect.Method;

@Component(value = "beforeAdvice")
public class UserServiceBeforeAdvice implements MethodBeforeAdvice {
    @Override
    public void before(Method method, Object[] args, Object target) throws Throwable {
        System.out.println("拦截方法：" + method.getName());
    }
}
