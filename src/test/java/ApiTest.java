import bean.UserService1;
import bean.UserService2;
import org.junit.Test;
import springframework.context.support.ClassPathXmlApplicationContext;

public class ApiTest {
    @Test
    public void test_circleDependency(){
        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:spring.xml");
        UserService1 userService1 = (UserService1) applicationContext.getBean("userService1");
        System.out.println(userService1.queryUserInfo());
        System.out.println(userService1.queryAnotherUserInfo());
        UserService2 userService2 = (UserService2) applicationContext.getBean("userService2");
        System.out.println(userService2.queryUserInfo());
        System.out.println(userService2.queryAnotherUserInfo());
    }
}

