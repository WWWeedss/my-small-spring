package springframework.context;

public interface ApplicationEventPublisher {
    // 向全体监听者广播事件
    void publishEvent(ApplicationEvent event);
}
