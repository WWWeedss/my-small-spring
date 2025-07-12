package bean;

import springframework.beans.factory.annotation.Autowired;
import springframework.stereotype.Component;

import java.util.Random;

@Component(value = "userService2")
public class UserService2 implements IUserService{

    @Autowired
    private UserService1 userService1;

    @Override
    public String queryUserInfo() {
        try {
            Thread.sleep(new Random(1).nextInt(100));
        }                                                                                                                                                                                                                                 catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "这是 UserService2 的用户信息";
    }

    public String queryAnotherUserInfo() {
        return "这是在 UserService2 中调用的：" + userService1.queryUserInfo();
    }
}
