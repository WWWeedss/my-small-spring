package springframework.beans.factory.support;

import cn.hutool.core.util.StrUtil;
import springframework.beans.BeansException;
import springframework.beans.factory.DisposableBean;
import springframework.beans.factory.config.BeanDefinition;

import java.lang.reflect.Member;
import java.lang.reflect.Method;

public class DisposableBeanAdapter implements DisposableBean {

    private final Object bean;

    private final String beanName;

    private String destroyMethodName;

    public DisposableBeanAdapter(Object bean, String beanName, BeanDefinition beanDefinition) {
        this.bean = bean;
        this.beanName = beanName;
        this.destroyMethodName = beanDefinition.getDestroyMethodName();
    }
    @Override
    public void destroy() throws Exception {
        // 1. 实现接口 DisposableBean 中的 destroy() 方法
        if (bean instanceof DisposableBean) {
            ((DisposableBean) bean).destroy();
        }

        // 2. 通过 xml 配置 <bean destroy-method="destroy"> 指定的销毁方法
        // 这里需要判断一下，避免二次执行销毁方法
        if (StrUtil.isNotEmpty(destroyMethodName) && !(bean instanceof DisposableBean && this.destroyMethodName.equals("destroy"))){
            Method destroyMethod = bean.getClass().getMethod(destroyMethodName);
            if (destroyMethod == null) {
                throw new BeansException("Couldn't find a destroy method named '" + destroyMethodName + "' on bean with name '" + beanName + "'");
            }
            destroyMethod.invoke(bean);
        }

    }
}
