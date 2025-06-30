package springframework.context.event;

public class ContextRefreshedEvent extends ApplicationContextEvent{
    public ContextRefreshedEvent(Object source){
        super(source);
    }
}
