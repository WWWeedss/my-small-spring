package springframework.aop;

import java.lang.reflect.Method;

public interface MethodBeforeAdvice extends BeforeAdvice{

    // target 指被代理的原始对象，感觉不一定用得到
    // 本方法在目标方法被调用前执行
    void before(Method method, Object[] args, Object target) throws Throwable;
}
