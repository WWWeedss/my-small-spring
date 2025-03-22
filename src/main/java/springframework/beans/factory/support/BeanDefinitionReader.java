package springframework.beans.factory.support;

import springframework.beans.core.io.Resource;
import springframework.beans.core.io.ResourceLoader;
import springframework.beans.factory.config.BeanDefinition;

public interface BeanDefinitionReader {

    BeanDefinitionRegistry getRegistry();

    ResourceLoader getResourceLoader();

    void loadBeanDefinitions(Resource resource) throws Exception;

    void loadBeanDefinitions(Resource... resources) throws Exception;

    void loadBeanDefinitions(String location) throws Exception;
}
