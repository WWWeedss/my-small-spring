package springframework.context.event;

import cn.hutool.core.util.ClassUtil;
import springframework.beans.BeansException;
import springframework.beans.factory.BeanFactory;
import springframework.beans.factory.BeanFactoryAware;
import springframework.context.ApplicationEvent;
import springframework.context.ApplicationListener;
import springframework.utils.ClassUtils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;

public abstract class AbstractApplicationEventMulticaster implements ApplicationEventMulticaster, BeanFactoryAware{

    public final Set<ApplicationListener<ApplicationEvent>> applicationListeners = new LinkedHashSet<>();
    private BeanFactory beanFactory;
    @Override
    public final void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void addApplicationListener(ApplicationListener<?> listener) {
        applicationListeners.add((ApplicationListener<ApplicationEvent>) listener);
    }

    @Override
    public void removeApplicationListener(ApplicationListener<?> listener) {
        applicationListeners.remove(listener);
    }


    protected Collection<ApplicationListener> getApplicationListeners(ApplicationEvent event){
        LinkedList<ApplicationListener> allListeners = new LinkedList<>();
        for (ApplicationListener<ApplicationEvent> listener : applicationListeners) {
            if (supportsEvent(listener, event)) {
                allListeners.add(listener);
            }
        }
        return allListeners;
    }

    // 检查 ApplicationListener 是否监听当前事件
    protected boolean supportsEvent(ApplicationListener<ApplicationEvent> applicationListener, ApplicationEvent event) {
        Class<? extends ApplicationListener> listenerClass = applicationListener.getClass();

        // 按照 Cglib 和 Simple 两种 Strategy 不同的实例化类型，获取对应的目标 class，因为 Cglib 是通过继承实现的
        Class<?> targetClass = ClassUtils.isCglibProxyClass(listenerClass) ? listenerClass.getSuperclass() : listenerClass;
        // 获取实现的第一个泛型接口 -> ApplicationListener<ApplicationEvent>
        Type genericInterface = targetClass.getGenericInterfaces()[0];
        Type actualTypeArgument = ((ParameterizedType) genericInterface).getActualTypeArguments()[0];
        String className = actualTypeArgument.getTypeName();
        Class<?> eventClassName;
        try {
            // 将对应的事件类加载进内存
            eventClassName = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new BeansException("wrong event class name: " + className);
        }

        // A.isAssignableFrom(B) 的作用是判断 B 是否是 A 的子类或实现类
        // AssignableFrom 就是 B 是否可以赋值给 A 的意思
        return eventClassName.isAssignableFrom(event.getClass());
    }
}
