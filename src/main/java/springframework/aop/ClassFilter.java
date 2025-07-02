package springframework.aop;

public interface ClassFilter {
    // Check if the given class/interface matches this filter.
    boolean matches(Class<?> clazz);
}
