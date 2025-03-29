package springframework.beans.factory;

public interface InitializingBean {
    // Bean 属性填充后调用
    void afterPropertiesSet() throws Exception;
}
