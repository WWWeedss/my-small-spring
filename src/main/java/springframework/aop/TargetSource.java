package springframework.aop;

public class TargetSource {

    private final Object target;

    public TargetSource(Object target) {
        this.target = target;
    }

    public Class<?>[] getTargetClass() {
        // 获取目标对象实现的所有接口，对于 JDK 的动态代理，必须要有接口才行
        return target.getClass().getInterfaces();
    }

    public Object getTarget() {
        return target;
    }
}
