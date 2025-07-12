package bean;

import springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class UserDao {

    private static Map<String, String> hashMap = new HashMap<>();

    static {
        hashMap.put("10001", "victor_G，北京，亦庄");
        hashMap.put("10002", "猫猫，香港，九龙");
    }

    public String queryUserName(String uId) {
        return hashMap.get(uId);
    }
}
