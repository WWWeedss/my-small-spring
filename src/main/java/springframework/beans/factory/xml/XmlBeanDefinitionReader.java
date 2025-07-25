package springframework.beans.factory.xml;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.XmlUtil;
import springframework.beans.BeansException;
import springframework.beans.PropertyValue;
import springframework.context.annotation.ClassPathBeanDefinitionScanner;
import springframework.core.io.Resource;
import springframework.core.io.ResourceLoader;
import springframework.beans.factory.config.BeanDefinition;
import springframework.beans.factory.config.BeanReference;
import springframework.beans.factory.support.AbstractBeanDefinitionReader;
import springframework.beans.factory.support.BeanDefinitionRegistry;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import java.io.IOException;
import java.io.InputStream;

public class XmlBeanDefinitionReader extends AbstractBeanDefinitionReader {

    public XmlBeanDefinitionReader(BeanDefinitionRegistry registry){
        super(registry);
    }

    public XmlBeanDefinitionReader(BeanDefinitionRegistry registry, ResourceLoader resourceLoader){
        super(registry, resourceLoader);
    }

    @Override
    public void loadBeanDefinitions(String... locations) throws BeansException {
        for (String location : locations) {
            loadBeanDefinitions(location);
        }
    }

    @Override
    public void loadBeanDefinitions(String location) throws BeansException {
        ResourceLoader resourceLoader = getResourceLoader();
        Resource resource = resourceLoader.getResource(location);
        loadBeanDefinitions(resource);
    }

    @Override
    public void loadBeanDefinitions(Resource... resources) throws BeansException {
        for (Resource resource : resources) {
            loadBeanDefinitions(resource);
        }
    }

    @Override
    public void loadBeanDefinitions(Resource resource) throws BeansException {
        try (InputStream inputStream = resource.getInputStream()){
            doLoadBeanDefinitions(inputStream);
        } catch (IOException | ClassNotFoundException e) {
            throw new BeansException("IOException parsing XML document from " + resource, e);
        }
    }

    protected void doLoadBeanDefinitions(InputStream inputStream) throws ClassNotFoundException {
        Document doc = XmlUtil.readXML(inputStream);
        Element root = doc.getDocumentElement();

        Element componentScan = XmlUtil.getElement(root, "component-scan");
        if (componentScan != null) {
            String scanPath = componentScan.getAttribute("base-package");
            if(StrUtil.isEmpty(scanPath)) {
                throw new BeansException("The value of base-package attribute must not be empty");
            }
            scanPackage(scanPath);
        }

        NodeList childNodes = root.getChildNodes();

        for(int i = 0; i < childNodes.getLength(); i++){
            if(!(childNodes.item(i) instanceof Element)){
                continue;
            }
            // 判断是否是 bean 对象
            if(!"bean".equals(childNodes.item(i).getNodeName())){
                continue;
            }

            parseBeanElement((Element)childNodes.item(i));
        }
    }

    protected void parseBeanElement(Element bean) throws ClassNotFoundException {
        String id = bean.getAttribute("id");
        String name = bean.getAttribute("name");
        String className = bean.getAttribute("class");

        //换汤不换药啊，还是用反射
        Class<?> clazz = Class.forName(className);
        // isEmpty() : length == 0 || str == null
        // 反正我们只要唯一标识，所以优先用 id
        String beanName = StrUtil.isNotEmpty(id) ? id : name;
        if(StrUtil.isEmpty(beanName)){
            beanName = StrUtil.lowerFirst(clazz.getSimpleName());
        }

        // 创建 BeanDefinition
        BeanDefinition beanDefinition = new BeanDefinition(clazz);
        beanDefinition.setInitMethodName(bean.getAttribute("init-method"));
        beanDefinition.setDestroyMethodName(bean.getAttribute("destroy-method"));
        String beanScope = bean.getAttribute("scope");

        if (StrUtil.isNotEmpty(beanScope)) {
            beanDefinition.setScope(beanScope);
        }

        // 解析 Bean 的属性并填充
        for(int j = 0; j < bean.getChildNodes().getLength(); j++){
            if(!(bean.getChildNodes().item(j) instanceof Element)){
                continue;
            }
            if(!"property".equals(bean.getChildNodes().item(j).getNodeName())){
                continue;
            }
            parsePropertyElement((Element)bean.getChildNodes().item(j), beanDefinition);
        }

        if (getRegistry().containsBeanDefinition(beanName)) {
            throw new BeansException("Duplicate beanName[" + beanName + "] is not allowed");
        }

        // 注册 BeanDefinition
        getRegistry().registerBeanDefinition(beanName, beanDefinition);
    }

    protected void parsePropertyElement(Element property, BeanDefinition beanDefinition) {
        String attrName = property.getAttribute("name");
        String attrValue = property.getAttribute("value");
        String attrRef = property.getAttribute("ref");

        // 如果有 ref 属性，那么就是 BeanReference，否则就是普通的值对象
        Object value = StrUtil.isNotEmpty(attrRef) ? new BeanReference(attrRef) : attrValue;

        PropertyValue propertyValue = new PropertyValue(attrName, value);
        beanDefinition.getPropertyValues().addPropertyValue(propertyValue);
    }

    private void scanPackage(String scanPath) {
        String[] basePackages = StrUtil.splitToArray(scanPath, ",");
        ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(getRegistry());
        scanner.doScan(basePackages);
    }
}
