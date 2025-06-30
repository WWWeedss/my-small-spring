package springframework.context.event;

public class ContextClosedEvent extends ApplicationContextEvent{
    public ContextClosedEvent(Object source){
        super(source);
    }
}
