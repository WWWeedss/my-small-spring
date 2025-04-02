package springframework.beans.factory.support;

import cn.hutool.core.util.StrUtil;
import springframework.beans.BeansException;
import springframework.beans.PropertyValue;
import springframework.beans.PropertyValues;
import springframework.beans.factory.DisposableBean;
import springframework.beans.factory.InitializingBean;
import springframework.beans.factory.config.AutowireCapableBeanFactory;
import springframework.beans.factory.config.BeanDefinition;
import springframework.beans.factory.config.BeanPostProcessor;
import springframework.beans.factory.config.BeanReference;
import springframework.utils.BeanUtil;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public abstract class AbstractAutowireCapableBeanFactory extends AbstractBeanFactory implements AutowireCapableBeanFactory {
    private InstantiationStrategy instantiationStrategy = new CglibSubclassingInstantiationStrategy();

    public void setInstantiationStrategy(InstantiationStrategy instantiationStrategy) {
        this.instantiationStrategy = instantiationStrategy;
    }
    @Override
    protected Object createBean(String beanName, BeanDefinition beanDefinition, Object[] args) throws BeansException {
        Object bean;
        try {
            bean = createBeanInstance(beanDefinition, beanName, args);
            // 给 Bean 填充属性
            applyPropertyValues(beanName, bean, beanDefinition);

            // 执行 Bean 的初始化方法和 BeanPostProcessor 的前置和后置处理方法
            bean = initializeBean(beanName, bean, beanDefinition);
        } catch (Exception e) {
            throw new BeansException("Instantiation of bean failed", e);
        }

        // 注册实现了 DisposableBean 接口的 Bean 对象
        registerDisposableBeanIfNecessary(beanName, bean, beanDefinition);

        addSingleton(beanName, bean);
        return bean;
    }

    protected void registerDisposableBeanIfNecessary(String beanName, Object bean, BeanDefinition beanDefinition){
        if (bean instanceof DisposableBean || StrUtil.isNotEmpty(beanDefinition.getDestroyMethodName())){
            registerDisposableBean(beanName, new DisposableBeanAdapter(bean, beanName, beanDefinition));
        }
    }

    protected Object createBeanInstance(BeanDefinition beanDefinition, String beanName, Object[] args) throws BeansException {
        Constructor constructorToUse = null;
        Class<?> beanClass = beanDefinition.getBeanClass();
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

    protected void applyPropertyValues(String beanName, Object bean, BeanDefinition beanDefinition) {
        try {
            PropertyValues propertyValues = beanDefinition.getPropertyValues();
            for (PropertyValue propertyValue : propertyValues.getPropertyValues()){

                String name = propertyValue.getName();
                Object value = propertyValue.getValue();

                if (value instanceof BeanReference){
                    BeanReference beanReference = (BeanReference) value;
                    // 记得吗，getBean 在 Bean 不存在的时候会先实例化对应的 Bean
                    // 现在的处理方法如果有环形依赖会有问题，后面我们再解决
                    value = getBean(beanReference.getBeanName());
                }

                BeanUtil.setFieldValue(bean, name, value);
            }
        } catch (Exception e) {
            throw new BeansException("Error setting property values for bean: " + beanName);
        }
    }

    private Object initializeBean(String beanName, Object bean, BeanDefinition beanDefinition) {
        // 1. 执行 BeanPostProcessor Before 处理
        Object wrappedBean = applyBeanPostProcessorsBeforeInitialization(bean, beanName);

        // 2. 执行 Bean 的初始化方法
        try {
            invokeInitMethods(beanName, wrappedBean, beanDefinition);
        } catch (Exception e) {
            throw new BeansException("Invocation of init method of bean[" + beanName + "] failed", e);
        }

        // 2. 执行 BeanPostProcessor After 处理
        wrappedBean = applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);
        return wrappedBean;
    }

    private void invokeInitMethods(String beanName, Object wrappedBean, BeanDefinition beanDefinition) throws Exception {
        // 1. 实现了 InitializingBean 接口 afterPropertiesSet 方法
        if (wrappedBean instanceof InitializingBean) {
            ((InitializingBean) wrappedBean).afterPropertiesSet();
        }

        // 2. 通过 xml 配置 <bean init-method="init"> 指定的初始化方法
        // 额，要是两种方式都配置了，那么 afterPropertiesSet 方法会先执行，init 方法后执行，而不是相互覆盖
        String initMethodName = beanDefinition.getInitMethodName();
        if (StrUtil.isNotEmpty(initMethodName)) {
            // 利用反射机制获取定义在 Bean 对应 Class 中的初始化方法
            Method initMethod = beanDefinition.getBeanClass().getMethod(initMethodName);
            if (initMethod == null) {
                throw new BeansException("Could not find an init method named '" + initMethodName + "' on bean with name '" + beanName + "'");
            }
            initMethod.invoke(wrappedBean);
        }
    }

    @Override
    public Object applyBeanPostProcessorsBeforeInitialization(Object existingBean, String beanName) throws BeansException {
        Object result = existingBean;
        for (BeanPostProcessor beanPostProcessor : getBeanPostProcessors()) {
            Object current = beanPostProcessor.postProcessBeforeInitialization(result, beanName);
            if (current == null) {
                // 返回 null 说明 Bean 实例化前的操作链已经到底了，直接返回
                return result;
            }
            result = current;
        }
        return result;
    }

    @Override
    public Object applyBeanPostProcessorsAfterInitialization(Object existingBean, String beanName) throws BeansException {
        Object result = existingBean;
        for (BeanPostProcessor beanPostProcessor : getBeanPostProcessors()) {
            Object current = beanPostProcessor.postProcessAfterInitialization(result, beanName);
            if (current == null) {
                return result;
            }
            result = current;
        }
        return result;
    }
}
