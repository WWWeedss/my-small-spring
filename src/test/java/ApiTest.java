import bean.UserDao;
import bean.UserService;
import cn.hutool.core.io.IoUtil;
import common.MyBeanFactoryPostProcessor;
import common.MyBeanPostProcessor;
import org.junit.Before;
import org.junit.Test;
import springframework.beans.PropertyValue;
import springframework.beans.PropertyValues;
import springframework.beans.context.support.ClassPathXmlApplicationContext;
import springframework.beans.core.io.DefaultResourceLoader;
import springframework.beans.core.io.Resource;
import springframework.beans.factory.config.BeanDefinition;
import springframework.beans.factory.config.BeanReference;
import springframework.beans.factory.support.DefaultListableBeanFactory;
import springframework.beans.factory.xml.XmlBeanDefinitionReader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public class ApiTest {

    @Test
    public void test_xml() {
        // 1.初始化 BeanFactory
        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:spring.xml");
        applicationContext.registerShutdownHook();

        // 2. 获取Bean对象调用方法
        UserService userService = applicationContext.getBean("userService", UserService.class);
        String result = userService.queryUserInfo();
        System.out.println("测试结果：" + result);
    }
}

