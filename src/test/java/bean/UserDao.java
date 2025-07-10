package bean;

import springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component("userDao")
public class UserDao {
    private static Map<String, String> hashMap = new HashMap<>();
    
    static {
        hashMap.put("1", "张三");
        hashMap.put("2", "李四");
        hashMap.put("3", "王五");
    }
    
    public String queryUserName(String uId) {
        return hashMap.get(uId);
    }
}
