package bean;

public class UserService {
    private String uId;

    private UserDao userDao;
    public UserService() {
    }

    public UserService(String uId) {
        this.uId = uId;
    }
    public String queryUserInfo() {
        System.out.println("查询用户信息：" + userDao.queryUserName(uId));
        return userDao.queryUserName(uId);
    }

    public String getuId() {
        return uId;
    }

    public void setuId(String uId) {
        this.uId = uId;
    }

    public UserDao getUserDao() {
        return userDao;
    }

    public void setUserDao(UserDao userDao) {
        this.userDao = userDao;
    }

    public static void main(String[]  args){
        Class<?> clazz = UserService.class;
        System.out.println(clazz.getName());
    }
}
