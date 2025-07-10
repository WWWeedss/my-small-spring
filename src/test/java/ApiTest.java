import bean.IUserService;
import org.junit.Test;
import springframework.beans.factory.config.BeanPostProcessor;
import springframework.context.support.ClassPathXmlApplicationContext;

public class ApiTest {
    @Test
    public void test_scan() {
        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:spring.xml");
        IUserService userService = applicationContext.getBean("userService", IUserService.class);
        System.out.println(userService.queryUserInfo());
    }
}

