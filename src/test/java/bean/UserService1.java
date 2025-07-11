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
        try {
            Thread.sleep(new Random(1).nextInt(100));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "这是 UserService1 的用户信息";
    }

    public String queryAnotherUserInfo() {
        return "这是在 UserService1 中调用的：" + userService2.queryUserInfo();
    }
}
