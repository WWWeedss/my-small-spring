package bean;

public class UserService {
    private String name;

    public UserService() {
    }
    public UserService(String name) {
        this.name = name;
    }

    public void queryUserInfo(){
        System.out.println("查询用户信息");
    }

    @Override
    public String toString() {
        return "UserService{" + name + '}';
    }
}
