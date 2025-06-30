package springframework.context.event;

import springframework.context.ApplicationContext;
import springframework.context.ApplicationEvent;

public class ApplicationContextEvent extends ApplicationEvent {
    public ApplicationContextEvent(Object source) {
        super(source);
    }

    public final ApplicationContext getApplicationContext() {
        return (ApplicationContext) getSource();
    }
}
