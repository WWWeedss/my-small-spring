package springframework.context;

import springframework.beans.factory.HierarchicalBeanFactory;
import springframework.beans.factory.ListableBeanFactory;
import springframework.core.io.ResourceLoader;

public interface ApplicationContext extends ListableBeanFactory, ApplicationEventPublisher {
}
