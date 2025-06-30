package springframework.context;

import java.util.EventListener;

public interface ApplicationListener<E extends ApplicationEvent> extends EventListener {
    /**
     * 事件处理方法
     * @param event the event to respond to
     */
    void onApplicationEvent(E event);
}
