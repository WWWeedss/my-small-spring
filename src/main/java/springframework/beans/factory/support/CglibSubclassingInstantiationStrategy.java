package springframework.beans.factory.support;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.NoOp;
import springframework.beans.BeansException;
import springframework.beans.factory.config.BeanDefinition;

import java.lang.reflect.Constructor;

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
