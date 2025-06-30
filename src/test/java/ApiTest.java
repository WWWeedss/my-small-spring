import bean.UserService;
import event.CustomEvent;
import org.junit.Test;
import springframework.context.support.ClassPathXmlApplicationContext;

public class ApiTest {
    @Test
    public void test_event(){
        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(("classpath:spring.xml"));
        applicationContext.publishEvent(new CustomEvent(applicationContext, 1001L, "Hello, this is a custom event!"));
        applicationContext.registerShutdownHook();
    }
}

