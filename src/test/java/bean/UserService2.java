package bean;

import springframework.beans.factory.annotation.Autowired;
import springframework.stereotype.Component;

@Component(value = "userService2")
public class UserService2 implements IUserService{

    @Autowired
    private UserService1 userService1;

    @Override
    public String queryUserInfo() {
        return "这是 UserService2 的用户信息";
    }

    @Override
    public String queryAnotherUserInfo() {
        return "这是在 UserService2 中调用的：" + userService1.queryUserInfo();
    }
}
