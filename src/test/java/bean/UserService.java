package bean;

import java.util.Random;

public class UserService implements IUserService {
    @Override
    public String queryUserInfo() {
        try {
            Thread.sleep(new Random(1).nextInt(100));
        } catch (InterruptedException e){
            e.printStackTrace();
        }
        return "WWWeeds, 100001, 杭州";
    }


    @Override
    public String register(String userName) {
        try {
            Thread.sleep(new Random(1).nextInt(100));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "注册用户:" + userName + "success!";
    }
}

