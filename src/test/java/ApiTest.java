import bean.UserService;
import org.junit.Test;
import springframework.beans.factory.config.BeanDefinition;
import springframework.beans.factory.support.DefaultListableBeanFactory;

import java.util.Objects;

public class ApiTest {

    @Test
    public void test_BeanFactory(){
        // 1.初始化 BeanFactory
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

        // 2.注入bean
        BeanDefinition beanDefinition = new BeanDefinition(UserService.class);
        beanFactory.registerBeanDefinition("userService", beanDefinition);

        // 3.获取bean
        UserService userService = (UserService) beanFactory.getBean("userService","WWWeeds");

        assert Objects.equals(userService.toString(), "UserService{WWWeeds}");
    }
}

