package bean;

import springframework.beans.factory.annotation.Autowired;
import springframework.stereotype.Component;

import java.util.Random;

@Component(value = "userService1")
public class UserService1 implements IUserService{
    @Autowired
    private UserService2 userService2;

    @Override
    public String queryUserInfo() {
        return "这是 UserService1 的用户信息";
    }

    @Override
    public String queryAnotherUserInfo() {
        return "这是在 UserService1 中调用的：" + userService2.queryUserInfo();
    }
}
