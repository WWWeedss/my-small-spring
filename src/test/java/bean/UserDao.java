package bean;

import java.util.HashMap;
import java.util.Map;

public class UserDao {
    private static Map<String, String> hashMap = new HashMap<>();

    static {
        hashMap.put("10001", "WWWeeds1");
        hashMap.put("10002", "WWWeeds2");
        hashMap.put("10003", "WWWeeds3");
    }

    public String queryUserName(String uId) {
        return hashMap.get(uId);
    }
}
