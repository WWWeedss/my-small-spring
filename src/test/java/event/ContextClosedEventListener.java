package event;

import springframework.context.ApplicationListener;
import springframework.context.event.ContextClosedEvent;

public class ContextClosedEventListener implements ApplicationListener<ContextClosedEvent> {

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        System.out.println("上下文关闭事件：" + this.getClass().getName());
    }
}
