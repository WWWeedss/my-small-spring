package springframework.beans.factory.support;

import cn.hutool.core.lang.hash.Hash;
import springframework.beans.factory.DisposableBean;
import springframework.beans.factory.ObjectFactory;
import springframework.beans.factory.config.SingletonBeanRegistry;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DefaultSingletonBeanRegistry implements SingletonBeanRegistry {
        protected static final Object NULL_OBJECT = new Object();

        // 一级缓存，普通对象
        private final Map<String, Object> singletonObjects = new HashMap<>();

        // 二级缓存，没有完全实例化的对象
        protected final Map<String, Object> earlySingletonObjects = new HashMap<>();

        // 三级缓存，存放代理对象
        private final Map<String, ObjectFactory<?>> singletonFactories = new HashMap<>();

        private final Map<String, DisposableBean> disposableBeans = new HashMap<>();
        @Override
        public Object getSingleton(String beanName) {
            Object singletonObject = singletonObjects.get(beanName);
            if (singletonObject == null) {
                // 如果一级缓存中不存在，则从二级缓存中获取
                singletonObject = earlySingletonObjects.get(beanName);
                if (singletonObject == null) {
                    // 如果二级缓存中也不存在，说明这个对象是代理对象，只有代理对象才会放到三级缓存中
                    ObjectFactory<?> singletonFactory = singletonFactories.get(beanName);
                    if (singletonFactory != null) {
                        singletonObject = singletonFactory.getObject();
                        // 从三级缓存中的代理对象中的真实对象取出，放入二级缓存中
                        earlySingletonObjects.put(beanName, singletonObject);
                        singletonFactories.remove(beanName);
                    }
                }
            }
            return singletonObject;
        }

        @Override
        public void registerSingleton(String beanName, Object singletonObject) {
            singletonObjects.put(beanName, singletonObject);
            earlySingletonObjects.remove(beanName);
            singletonFactories.remove(beanName);
        }

        protected void addSingletonFactory(String beanName, ObjectFactory<?> singletonFactory) {
            if (!this.singletonObjects.containsKey(beanName)) {
                this.singletonFactories.put(beanName, singletonFactory);
                this.earlySingletonObjects.remove(beanName);
            }
        }

        public void registerDisposableBean(String beanName, DisposableBean bean){
            disposableBeans.put(beanName, bean);
        }

        public void destroySingletons() {
            Set<String> keySet = this.disposableBeans.keySet();
            Object[] disposableBeanNames = keySet.toArray();

            for (int i = disposableBeanNames.length - 1; i >= 0; i--) {
                Object beanName = disposableBeanNames[i];
                DisposableBean disposableBean = disposableBeans.remove(beanName);
                try {
                    disposableBean.destroy();
                } catch (Exception e) {
                    throw new RuntimeException("Destroy method on bean with name '" + beanName + "' threw an exception", e);
                }
            }
        }
}
