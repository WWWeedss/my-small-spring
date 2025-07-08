package springframework.context.annotation;

import java.lang.annotation.*;
// 作用在类上和方法上
@Target({ElementType.TYPE, ElementType.METHOD})
// 保留到运行时
@Retention(RetentionPolicy.RUNTIME)
// 会生成到 JavaDoc 中
@Documented
public @interface Scope {

    String value() default "singleton";

}
