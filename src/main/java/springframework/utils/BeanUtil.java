package springframework.utils;

import java.lang.reflect.Field;

public class BeanUtil {
    public static void setFieldValue(Object bean, String name, Object value) throws NoSuchFieldException {
        // 尝试找到指定字段
        Field field = getField(bean.getClass(), name);
        if (field == null) {
            throw new NoSuchFieldException("Field not found: " + name);
        }

        field.setAccessible(true); // 设置字段可访问
        try {
            field.set(bean, value);  // 设置字段值
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private static Field getField(Class<?> clazz, String name) {
        // 检查当前类的字段
        try {
            return clazz.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            // 如果当前类没有该字段，检查父类
            Class<?> superclass = clazz.getSuperclass();
            if (superclass != null) {
                return getField(superclass, name);
            }
            // 如果没有找到，返回 null
            return null;
        }
    }
}
