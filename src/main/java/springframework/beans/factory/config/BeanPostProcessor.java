package springframework.beans.factory.config;


public interface BeanPostProcessor {
    /**
     * 在 Bean 对象执行初始化方法之前，调用此方法
     * @param bean
     * @param beanName
     * @return
     */
    Object postProcessBeforeInitialization(Object bean, String beanName);

    /**
     * 在 Bean 对象执行初始化方法之后，调用此方法
     * @param bean
     * @param beanName
     * @return
     */
    Object postProcessAfterInitialization(Object bean, String beanName);
}
