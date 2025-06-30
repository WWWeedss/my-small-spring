package event;

import springframework.context.ApplicationListener;

public class CustomEventListener implements ApplicationListener<CustomEvent> {
    @Override
    public void onApplicationEvent(CustomEvent event) {
        System.out.println("CustomEventListener received event: " + event.getMessage() + " with ID: " + event.getId());
    }
}
