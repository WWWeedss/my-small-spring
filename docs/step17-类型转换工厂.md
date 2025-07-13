### 类型转换工厂

#### 前置思考

我们想要在注入属性的时候进行自定义的类型转换，比如字符串转数字，字符串转日期等等，这就需要做一个类型转换工厂，并把接口透出给用户实现。

思路是定义 Converter 接口和 ConverterFactory 接口，用户可以用 FactoryBean 来实现一个 ConverterFactory，这样 BeanFactory 在 createBean 的时候就可以从 ConverterFactory 中拿到 Converter，完成属性类型转换。

#### 具体实现

```bash
src
├── main
│   ├── java
│   │   └── springframework
│   │       ├── aop
│   │       │   ├── AdvisedSupport.java
│   │       │   ├── Advisor.java
│   │       │   ├── BeforeAdvice.java
│   │       │   ├── ClassFilter.java
│   │       │   ├── MethodBeforeAdvice.java
│   │       │   ├── MethodMatcher.java
│   │       │   ├── Pointcut.java
│   │       │   ├── PointcutAdvisor.java
│   │       │   ├── TargetSource.java
│   │       │   ├── aspectj
│   │       │   │   ├── AspectJExpressionPointcut.java
│   │       │   │   └── AspectJExpressionPointcutAdvisor.java
│   │       │   └── framework
│   │       │       ├── AopProxy.java
│   │       │       ├── Cglib2AopProxy.java
│   │       │       ├── JdkDynamicAopProxy.java
│   │       │       ├── ProxyFactory.java
│   │       │       ├── ReflectiveMethodInvocation.java
│   │       │       ├── adapter
│   │       │       │   └── MethodBeforeAdviceInterceptor.java
│   │       │       └── autoproxy
│   │       │           └── DefaultAdvisorAutoProxyCreator.java
│   │       ├── beans
│   │       │   ├── BeansException.java
│   │       │   ├── PropertyValue.java
│   │       │   ├── PropertyValues.java
│   │       │   └── factory
│   │       │       ├── Aware.java
│   │       │       ├── BeanClassLoaderAware.java
│   │       │       ├── BeanFactory.java --change
│   │       │       ├── BeanFactoryAware.java
│   │       │       ├── BeanNameAware.java
│   │       │       ├── ConfigurableListableBeanFactory.java
│   │       │       ├── DisposableBean.java
│   │       │       ├── FactoryBean.java
│   │       │       ├── HierarchicalBeanFactory.java
│   │       │       ├── InitializingBean.java
│   │       │       ├── ListableBeanFactory.java
│   │       │       ├── ObjectFactory.java
│   │       │       ├── PropertyPlaceholderConfigurer.java
│   │       │       ├── annotation
│   │       │       │   ├── Autowired.java
│   │       │       │   ├── AutowiredAnnotationBeanPostProcessor.java
│   │       │       │   ├── Qualifier.java
│   │       │       │   └── Value.java
│   │       │       ├── config
│   │       │       │   ├── AutowireCapableBeanFactory.java
│   │       │       │   ├── BeanDefinition.java
│   │       │       │   ├── BeanFactoryPostProcessor.java
│   │       │       │   ├── BeanPostProcessor.java
│   │       │       │   ├── BeanReference.java
│   │       │       │   ├── ConfigurableBeanFactory.java --change
│   │       │       │   ├── InstantiationAwareBeanPostProcessor.java
│   │       │       │   └── SingletonBeanRegistry.java
│   │       │       ├── support
│   │       │       │   ├── AbstractAutowireCapableBeanFactory.java --change
│   │       │       │   ├── AbstractBeanDefinitionReader.java
│   │       │       │   ├── AbstractBeanFactory.java --change
│   │       │       │   ├── BeanDefinitionReader.java
│   │       │       │   ├── BeanDefinitionRegistry.java
│   │       │       │   ├── CglibSubclassingInstantiationStrategy.java
│   │       │       │   ├── DefaultListableBeanFactory.java --change
│   │       │       │   ├── DefaultSingletonBeanRegistry.java
│   │       │       │   ├── DisposableBeanAdapter.java
│   │       │       │   ├── FactoryBeanRegistrySupport.java
│   │       │       │   ├── InstantiationStrategy.java
│   │       │       │   └── SimpleInstantiationStrategy.java
│   │       │       └── xml
│   │       │           └── XmlBeanDefinitionReader.java
│   │       ├── context
│   │       │   ├── ApplicationContext.java
│   │       │   ├── ApplicationContextAware.java 
│   │       │   ├── ApplicationEvent.java
│   │       │   ├── ApplicationEventPublisher.java
│   │       │   ├── ApplicationListener.java
│   │       │   ├── ConfigurableApplicationContext.java
│   │       │   ├── annotation
│   │       │   │   ├── ClassPathBeanDefinitionScanner.java
│   │       │   │   ├── ClassPathScanningCandidateComponentProvider.java
│   │       │   │   └── Scope.java
│   │       │   ├── event
│   │       │   │   ├── AbstractApplicationEventMulticaster.java
│   │       │   │   ├── ApplicationContextEvent.java
│   │       │   │   ├── ApplicationEventMulticaster.java
│   │       │   │   ├── ContextClosedEvent.java
│   │       │   │   ├── ContextRefreshedEvent.java
│   │       │   │   └── SimpleApplicationEventMulticaster.java
│   │       │   └── support
│   │       │       ├── AbstractApplicationContext.java --change
│   │       │       ├── AbstractRefreshableApplicationContext.java
│   │       │       ├── AbstractXmlApplicationContext.java
│   │       │       ├── ApplicationContextAwareProcessor.java
│   │       │       ├── ClassPathXmlApplicationContext.java
│   │       │       └── ConversionServiceFactoryBean.java --new
│   │       ├── core
│   │       │   ├── convert
│   │       │   │   ├── ConversionService.java --new
│   │       │   │   ├── converter
│   │       │   │   │   ├── Converter.java --new
│   │       │   │   │   ├── ConverterFactory.java --new
│   │       │   │   │   ├── ConverterRegistry.java --new
│   │       │   │   │   └── GenericConverter.java --new
│   │       │   │   └── support
│   │       │   │       ├── DefaultConversionService.java --new
│   │       │   │       ├── GenericConversionService.java --new
│   │       │   │       └── StringToNumberConverterFactory.java --new
│   │       │   └── io
│   │       │       ├── ClassPathResource.java
│   │       │       ├── DefaultResourceLoader.java
│   │       │       ├── FileSystemResource.java
│   │       │       ├── Resource.java
│   │       │       ├── ResourceLoader.java
│   │       │       └── UrlResource.java
│   │       ├── stereotype
│   │       │   └── Component.java
│   │       └── utils
│   │           ├── BeanUtil.java
│   │           ├── ClassUtils.java
│   │           ├── NumberUtils.java --new
│   │           └── StringValueResolver.java
│   └── resources
│       └── spring.xml --change
└── test
    └── java
        ├── ApiTest.java --change 
        ├── bean
        │   └── Husband.java --new
        └── converter
            ├── ConvertersFactoryBean.java --new
            ├── StringToIntegerConverter.java --new
            └── StringToLocalDateConverter.java --new

```

##### 定义接口

```java
public interface Converter<S,T> {
    // Convert the source object of type {@code S} to target type {@code T}.
    T convert(S source);
}
```

```java
public interface ConverterFactory<S, R> {
    /**
     * Return a converter that converts from the source type {@code S} to the target type {@code T}.
     *
     * @param targetType the target type to convert to
     * @param <T>        the target type
     * @return a converter that converts from the source type {@code S} to the target type {@code T}
     */
    <T extends R> Converter<S, T> getConverter(Class<T> targetType);
}
```

```java
public interface GenericConverter {
    
    Set<ConvertiblePair> getConvertibleTypes();
    
    Object convert(Object source, Class<?> sourceType, Class<?> targetType);

    final class ConvertiblePair {
        private final Class<?> sourceType;

        private final Class<?> targetType;

        public ConvertiblePair(Class<?> sourceType, Class<?> targetType) {
            Assert.notNull(sourceType, "Source type must not be null");
            Assert.notNull(targetType, "Target type must not be null");
            this.sourceType = sourceType;
            this.targetType = targetType;
        }

        public Class<?> getSourceType() {
            return this.sourceType;
        }

        public Class<?> getTargetType() {
            return this.targetType;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || obj.getClass() != ConvertiblePair.class) {
                return false;
            }
            ConvertiblePair other = (ConvertiblePair) obj;
            return this.sourceType.equals(other.sourceType) && this.targetType.equals(other.targetType);

        }

        @Override
        public int hashCode() {
            return this.sourceType.hashCode() * 31 + this.targetType.hashCode();
        }
    }

}
```

```java
public interface ConverterRegistry {
    
    void addConverter(Converter<?, ?> converter);
    
    void addConverter(GenericConverter converter);
    
    void addConverterFactory(ConverterFactory<?, ?> converterFactory);
}
```

```java
import org.jetbrains.annotations.Nullable;
public interface ConversionService {
    boolean canConvert(@Nullable Class<?> sourceType, Class<?> targetType);
    
    <T> T convert(Object source, Class<T> targetType);
}
```

这些接口把生成 Converter、convert、注册管理 Converter 都包含进去了。

##### 实现一个 ConverterFactory

字符串以及数字类型的转换是类型转换中最频繁的。框架代用户实现了一个转换器。

NumberUtil：提供将字符串转换为数字的功能，提供将 Number 转换为子类型 Double、Float、Integer 等等的功能。

```java
/**
 * 作者：DerekYRC https://github.com/DerekYRC/mini-spring
 */
public class NumberUtils {

    private static final BigInteger LONG_MIN = BigInteger.valueOf(Long.MIN_VALUE);

    private static final BigInteger LONG_MAX = BigInteger.valueOf(Long.MAX_VALUE);

    /**
     * Standard number types (all immutable):
     * Byte, Short, Integer, Long, BigInteger, Float, Double, BigDecimal.
     */
    public static final Set<Class<?>> STANDARD_NUMBER_TYPES;

    static {
        Set<Class<?>> numberTypes = new HashSet<>(8);
        numberTypes.add(Byte.class);
        numberTypes.add(Short.class);
        numberTypes.add(Integer.class);
        numberTypes.add(Long.class);
        numberTypes.add(BigInteger.class);
        numberTypes.add(Float.class);
        numberTypes.add(Double.class);
        numberTypes.add(BigDecimal.class);
        STANDARD_NUMBER_TYPES = Collections.unmodifiableSet(numberTypes);
    }


    /**
     * Convert the given number into an instance of the given target class.
     * @param number the number to convert
     * @param targetClass the target class to convert to
     * @return the converted number
     * @throws IllegalArgumentException if the target class is not supported
     * (i.e. not a standard Number subclass as included in the JDK)
     * @see java.lang.Byte
     * @see java.lang.Short
     * @see java.lang.Integer
     * @see java.lang.Long
     * @see java.math.BigInteger
     * @see java.lang.Float
     * @see java.lang.Double
     * @see java.math.BigDecimal
     */
    @SuppressWarnings("unchecked")
    public static <T extends Number> T convertNumberToTargetClass(Number number, Class<T> targetClass)
            throws IllegalArgumentException {

        Assert.notNull(number, "Number must not be null");
        Assert.notNull(targetClass, "Target class must not be null");

        if (targetClass.isInstance(number)) {
            return (T) number;
        }
        else if (Byte.class == targetClass) {
            long value = checkedLongValue(number, targetClass);
            if (value < Byte.MIN_VALUE || value > Byte.MAX_VALUE) {
                raiseOverflowException(number, targetClass);
            }
            return (T) Byte.valueOf(number.byteValue());
        }
        else if (Short.class == targetClass) {
            long value = checkedLongValue(number, targetClass);
            if (value < Short.MIN_VALUE || value > Short.MAX_VALUE) {
                raiseOverflowException(number, targetClass);
            }
            return (T) Short.valueOf(number.shortValue());
        }
        else if (Integer.class == targetClass) {
            long value = checkedLongValue(number, targetClass);
            if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
                raiseOverflowException(number, targetClass);
            }
            return (T) Integer.valueOf(number.intValue());
        }
        else if (Long.class == targetClass) {
            long value = checkedLongValue(number, targetClass);
            return (T) Long.valueOf(value);
        }
        else if (BigInteger.class == targetClass) {
            if (number instanceof BigDecimal) {
                // do not lose precision - use BigDecimal's own conversion
                return (T) ((BigDecimal) number).toBigInteger();
            }
            else {
                // original value is not a Big* number - use standard long conversion
                return (T) BigInteger.valueOf(number.longValue());
            }
        }
        else if (Float.class == targetClass) {
            return (T) Float.valueOf(number.floatValue());
        }
        else if (Double.class == targetClass) {
            return (T) Double.valueOf(number.doubleValue());
        }
        else if (BigDecimal.class == targetClass) {
            // always use BigDecimal(String) here to avoid unpredictability of BigDecimal(double)
            // (see BigDecimal javadoc for details)
            return (T) new BigDecimal(number.toString());
        }
        else {
            throw new IllegalArgumentException("Could not convert number [" + number + "] of type [" +
                    number.getClass().getName() + "] to unsupported target class [" + targetClass.getName() + "]");
        }
    }

    /**
     * Check for a {@code BigInteger}/{@code BigDecimal} long overflow
     * before returning the given number as a long value.
     * @param number the number to convert
     * @param targetClass the target class to convert to
     * @return the long value, if convertible without overflow
     * @throws IllegalArgumentException if there is an overflow
     * @see #raiseOverflowException
     */
    private static long checkedLongValue(Number number, Class<? extends Number> targetClass) {
        BigInteger bigInt = null;
        if (number instanceof BigInteger) {
            bigInt = (BigInteger) number;
        }
        else if (number instanceof BigDecimal) {
            bigInt = ((BigDecimal) number).toBigInteger();
        }
        // Effectively analogous to JDK 8's BigInteger.longValueExact()
        if (bigInt != null && (bigInt.compareTo(LONG_MIN) < 0 || bigInt.compareTo(LONG_MAX) > 0)) {
            raiseOverflowException(number, targetClass);
        }
        return number.longValue();
    }

    /**
     * Raise an <em>overflow</em> exception for the given number and target class.
     * @param number the number we tried to convert
     * @param targetClass the target class we tried to convert to
     * @throws IllegalArgumentException if there is an overflow
     */
    private static void raiseOverflowException(Number number, Class<?> targetClass) {
        throw new IllegalArgumentException("Could not convert number [" + number + "] of type [" +
                number.getClass().getName() + "] to target class [" + targetClass.getName() + "]: overflow");
    }

    /**
     * Parse the given {@code text} into a {@link Number} instance of the given
     * target class, using the corresponding {@code decode} / {@code valueOf} method.
     * <p>Trims all whitespace (leading, trailing, and in between characters) from
     * the input {@code String} before attempting to parse the number.
     * <p>Supports numbers in hex format (with leading "0x", "0X", or "#") as well.
     * @param text the text to convert
     * @param targetClass the target class to parse into
     * @return the parsed number
     * @throws IllegalArgumentException if the target class is not supported
     * (i.e. not a standard Number subclass as included in the JDK)
     * @see Byte#decode
     * @see Short#decode
     * @see Integer#decode
     * @see Long#decode
     * @see #decodeBigInteger(String)
     * @see Float#valueOf
     * @see Double#valueOf
     * @see java.math.BigDecimal#BigDecimal(String)
     */
    @SuppressWarnings("unchecked")
    public static <T extends Number> T parseNumber(String text, Class<T> targetClass) {
        Assert.notNull(text, "Text must not be null");
        Assert.notNull(targetClass, "Target class must not be null");
        String trimmed = trimAllWhitespace(text);

        if (Byte.class == targetClass) {
            return (T) (isHexNumber(trimmed) ? Byte.decode(trimmed) : Byte.valueOf(trimmed));
        }
        else if (Short.class == targetClass) {
            return (T) (isHexNumber(trimmed) ? Short.decode(trimmed) : Short.valueOf(trimmed));
        }
        else if (Integer.class == targetClass) {
            return (T) (isHexNumber(trimmed) ? Integer.decode(trimmed) : Integer.valueOf(trimmed));
        }
        else if (Long.class == targetClass) {
            return (T) (isHexNumber(trimmed) ? Long.decode(trimmed) : Long.valueOf(trimmed));
        }
        else if (BigInteger.class == targetClass) {
            return (T) (isHexNumber(trimmed) ? decodeBigInteger(trimmed) : new BigInteger(trimmed));
        }
        else if (Float.class == targetClass) {
            return (T) Float.valueOf(trimmed);
        }
        else if (Double.class == targetClass) {
            return (T) Double.valueOf(trimmed);
        }
        else if (BigDecimal.class == targetClass || Number.class == targetClass) {
            return (T) new BigDecimal(trimmed);
        }
        else {
            throw new IllegalArgumentException(
                    "Cannot convert String [" + text + "] to target class [" + targetClass.getName() + "]");
        }
    }

    /**
     * Parse the given {@code text} into a {@link Number} instance of the
     * given target class, using the supplied {@link NumberFormat}.
     * <p>Trims the input {@code String} before attempting to parse the number.
     * @param text the text to convert
     * @param targetClass the target class to parse into
     * @param numberFormat the {@code NumberFormat} to use for parsing (if
     * {@code null}, this method falls back to {@link #parseNumber(String, Class)})
     * @return the parsed number
     * @throws IllegalArgumentException if the target class is not supported
     * (i.e. not a standard Number subclass as included in the JDK)
     * @see java.text.NumberFormat#parse
     * @see #convertNumberToTargetClass
     * @see #parseNumber(String, Class)
     */
    public static <T extends Number> T parseNumber(
            String text, Class<T> targetClass, @Nullable NumberFormat numberFormat) {

        if (numberFormat != null) {
            Assert.notNull(text, "Text must not be null");
            Assert.notNull(targetClass, "Target class must not be null");
            DecimalFormat decimalFormat = null;
            boolean resetBigDecimal = false;
            if (numberFormat instanceof DecimalFormat) {
                decimalFormat = (DecimalFormat) numberFormat;
                if (BigDecimal.class == targetClass && !decimalFormat.isParseBigDecimal()) {
                    decimalFormat.setParseBigDecimal(true);
                    resetBigDecimal = true;
                }
            }
            try {
                Number number = numberFormat.parse(trimAllWhitespace(text));
                return convertNumberToTargetClass(number, targetClass);
            }
            catch (ParseException ex) {
                throw new IllegalArgumentException("Could not parse number: " + ex.getMessage());
            }
            finally {
                if (resetBigDecimal) {
                    decimalFormat.setParseBigDecimal(false);
                }
            }
        }
        else {
            return parseNumber(text, targetClass);
        }
    }

    public static String trimAllWhitespace(String str) {
        if (!hasLength(str)) {
            return str;
        }

        int len = str.length();
        StringBuilder sb = new StringBuilder(str.length());
        for (int i = 0; i < len; i++) {
            char c = str.charAt(i);
            if (!Character.isWhitespace(c)) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static boolean hasLength(@Nullable String str) {
        return (str != null && !str.isEmpty());
    }

    /**
     * Determine whether the given {@code value} String indicates a hex number,
     * i.e. needs to be passed into {@code Integer.decode} instead of
     * {@code Integer.valueOf}, etc.
     */
    private static boolean isHexNumber(String value) {
        int index = (value.startsWith("-") ? 1 : 0);
        return (value.startsWith("0x", index) || value.startsWith("0X", index) || value.startsWith("#", index));
    }

    /**
     * Decode a {@link java.math.BigInteger} from the supplied {@link String} value.
     * <p>Supports decimal, hex, and octal notation.
     * @see BigInteger#BigInteger(String, int)
     */
    private static BigInteger decodeBigInteger(String value) {
        int radix = 10;
        int index = 0;
        boolean negative = false;

        // Handle minus sign, if present.
        if (value.startsWith("-")) {
            negative = true;
            index++;
        }

        // Handle radix specifier, if present.
        if (value.startsWith("0x", index) || value.startsWith("0X", index)) {
            index += 2;
            radix = 16;
        }
        else if (value.startsWith("#", index)) {
            index++;
            radix = 16;
        }
        else if (value.startsWith("0", index) && value.length() > 1 + index) {
            index++;
            radix = 8;
        }

        BigInteger result = new BigInteger(value.substring(index), radix);
        return (negative ? result.negate() : result);
    }

}
```

在 StringToNumberConverterFactory 中使用它。可以看到 ConverterFactory 是支持一组类型的类型转换的。像下面这个就支持 String 到所有 Number 及其子类的转换。而 Converter 就只能支持两种类型的转换（虽然理论上来说，Converter<A, B> 可以支持所有 A 及 A 的子类转换到 B 及 B 的父类，这也是一组转换，但希望你们能 Get 我的意思）。

```java
public class StringToNumberConverterFactory implements ConverterFactory<String, Number> {
    @Override
    public <T extends Number> Converter<String, T> getConverter(Class<T> targetType) {
        return new StringToNumber<>(targetType);
    }
    private static final class StringToNumber<T extends Number> implements Converter<String, T> {

        private final Class<T> targetType;

        public StringToNumber(Class<T> targetType) {
            this.targetType = targetType;
        }

        @Override
        @Nullable
        public T convert(String source) {
            if (source.isEmpty()) {
                return null;
            }
            return NumberUtils.parseNumber(source, this.targetType);
        }
    }
    
}

```

##### 实现 ConversionService

这就是我们要提供给 beanFactory 的 ConversionService，它实现了我们定义的接口方法，将 converter 和 converterFactory 管理起来，并且用两个 Adapter 类来转接，让 converter 和 converterFactory 都实现 GenericConverter 内的方法。	

```java
public class GenericConversionService implements ConversionService, ConverterRegistry {
    
    private Map<GenericConverter.ConvertiblePair, GenericConverter> converters = new HashMap<>();
    @Override
    public boolean canConvert(@Nullable Class<?> sourceType, Class<?> targetType) {
        GenericConverter converter = getConverter(sourceType, targetType);
        return converter != null;
    }

    @Override
    public <T> T convert(Object source, Class<T> targetType) {
        Class<?> sourceType = source.getClass();
        GenericConverter converter = getConverter(sourceType, targetType);
        return (T) converter.convert(source, sourceType, targetType);
    }
    
    protected GenericConverter getConverter(Class<?> sourceType, Class<?> targetType) {
        List<Class<?>> sourceCandidates = getClassHierarchy(sourceType);
        List<Class<?>> targetCandidates = getClassHierarchy(targetType);
        for (Class<?> sourceCandidate : sourceCandidates) {
            for(Class<?> targetCandidate : targetCandidates) {
                GenericConverter.ConvertiblePair convertiblePair = new GenericConverter.ConvertiblePair(sourceCandidate, targetCandidate);
                GenericConverter converter = converters.get(convertiblePair);
                if (converter != null) {
                    return converter;
                }
            }
        }
        return null;
    }

    protected List<Class<?>> getClassHierarchy(Class<?> clazz) {
        List<Class<?>> hierarchy = new ArrayList<>();
        while (clazz != null) {
            hierarchy.add(clazz);
            clazz = clazz.getSuperclass();
        }
        return hierarchy;
    }
    
    
    @Override
    public void addConverter(Converter<?, ?> converter) {
        GenericConverter.ConvertiblePair typeInfo = getRequiredTypeInfo(converter);
        ConverterAdapter converterAdapter = new ConverterAdapter(typeInfo, converter);
        for (GenericConverter.ConvertiblePair convertibleType : converterAdapter.getConvertibleTypes()) {
            converters.put(convertibleType, converterAdapter);
        }
    }

    @Override
    public void addConverterFactory(ConverterFactory<?, ?> converterFactory) {
        GenericConverter.ConvertiblePair typeInfo = getRequiredTypeInfo(converterFactory);
        ConverterFactoryAdapter converterFactoryAdapter = new ConverterFactoryAdapter(typeInfo, converterFactory);
        for (GenericConverter.ConvertiblePair convertibleType : converterFactoryAdapter.getConvertibleTypes()) {
            converters.put(convertibleType, converterFactoryAdapter);
        }
    }

    private GenericConverter.ConvertiblePair getRequiredTypeInfo(Object object) {
        Type[] types = object.getClass().getGenericInterfaces();
        ParameterizedType parameterized = (ParameterizedType) types[0];
        Type[] actualTypeArguments = parameterized.getActualTypeArguments();
        Class sourceType = (Class) actualTypeArguments[0];
        Class targetType = (Class) actualTypeArguments[1];
        return new GenericConverter.ConvertiblePair(sourceType, targetType);
    }

    @Override
    public void addConverter(GenericConverter converter) {

    }

    private final class ConverterAdapter implements GenericConverter {

        private final ConvertiblePair typeInfo;

        private final Converter<Object, Object> converter;

        public ConverterAdapter(ConvertiblePair typeInfo, Converter<?, ?> converter) {
            this.typeInfo = typeInfo;
            this.converter = (Converter<Object, Object>) converter;
        }

        @Override
        public Set<ConvertiblePair> getConvertibleTypes() {
            return Collections.singleton(typeInfo);
        }

        @Override
        public Object convert(Object source, Class sourceType, Class targetType) {
            return converter.convert(source);
        }
    }

    private final class ConverterFactoryAdapter implements GenericConverter {

        private final ConvertiblePair typeInfo;

        private final ConverterFactory<Object, Object> converterFactory;

        public ConverterFactoryAdapter(ConvertiblePair typeInfo, ConverterFactory<?, ?> converterFactory) {
            this.typeInfo = typeInfo;
            this.converterFactory = (ConverterFactory<Object, Object>) converterFactory;
        }

        @Override
        public Set<ConvertiblePair> getConvertibleTypes() {
            return Collections.singleton(typeInfo);
        }

        @Override
        public Object convert(Object source, Class sourceType, Class targetType) {
            return converterFactory.getConverter(targetType).convert(source);
        }
    }
}
```

这其实就是把我们实现的 StringToNumberConverterFactory 加到 Map 里面去。

```java
public class DefaultConversionService extends GenericConversionService {
    public DefaultConversionService() {
        addDefaultConverters(this);
    }
    
    public static void addDefaultConverters(ConverterRegistry converterRegistry) {
        // 添加各类类型转换工厂
        converterRegistry.addConverterFactory(new StringToNumberConverterFactory());
    }
}
```

##### 将 ConversionService 注册到 Factory 中

在 BeanFactory 中定义一个工具接口：

```java
boolean containsBean(String name);
```

实现就是这样：

```java
@Override
public boolean containsBean(String name) {
    return beanDefinitionMap.containsKey(name);
}
```

定义注册 ConversionService 的接口：

```java
public interface ConfigurableBeanFactory extends HierarchicalBeanFactory, SingletonBeanRegistry {
    /**
     * Specify a Spring 3.0 ConversionService to use for converting
     * property values, as an alternative to JavaBeans PropertyEditors.
     * @since 3.0
     */
    void setConversionService(ConversionService conversionService);

    /**
     * Return the associated ConversionService, if any.
     * @since 3.0
     */
    @Nullable
    ConversionService getConversionService();
}
```

实现注册接口：

```java
public abstract class AbstractBeanFactory extends FactoryBeanRegistrySupport implements ConfigurableBeanFactory {
    private ConversionService conversionService;
    
    @Override
    public void setConversionService(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @Override
    public ConversionService getConversionService() {
        return conversionService;
    }

}
```

在 AbstractApplicationContext 中把 conversionService 放进去：

```java
public abstract class AbstractApplicationContext extends DefaultResourceLoader implements ConfigurableApplicationContext {

    @Override
    public void refresh() throws BeansException {
        // 1. 创建 BeanFactory，并加载 BeanDefinition
        refreshBeanFactory();

        // 2. 获取 BeanFactory
        ConfigurableListableBeanFactory beanFactory = getBeanFactory();

        // 3. 添加 ApplicationContextAwareProcessor，让继承自 ApplicationContextAware 的 Bean 对象都能感知所属的 ApplicationContext
        beanFactory.addBeanPostProcessor(new ApplicationContextAwareProcessor(this));

        // 4. 在 Bean 实例化之前，执行 BeanFactoryPostProcessor (Invoke factory processors registered as beans in the context.)
        invokeBeanFactoryPostProcessors(beanFactory);

        // 5. BeanPostProcessor 需要提前于其他 Bean 对象实例化之前执行注册操作
        registerBeanPostProcessors(beanFactory);

        // 6. 初始化事件发布者
        initApplicationEventMulticaster();

        // 7. 注册事件监听器
        registerListeners();

        // 8. 设置类型转换器、提前实例化单例Bean对象
        finishBeanFactoryInitialization(beanFactory);

        // 9. 发布容器刷新完成事件
        finishRefresh();
    }

    // 设置类型转换器、提前实例化单例Bean对象
    protected void finishBeanFactoryInitialization(ConfigurableListableBeanFactory beanFactory) {
        // 设置类型转换器
        if (beanFactory.containsBean("conversionService")) {
            Object conversionService = beanFactory.getBean("conversionService");
            if (conversionService instanceof ConversionService) {
                beanFactory.setConversionService((ConversionService) conversionService);
            }
        }

        // 提前实例化单例Bean对象
        beanFactory.preInstantiateSingletons();
    }
}
```

##### 在注解和 applyProperties 中使用 ConversionService

```java
public abstract class AbstractAutowireCapableBeanFactory extends AbstractBeanFactory implements AutowireCapableBeanFactory {

    protected void applyPropertyValues(String beanName, Object bean, BeanDefinition beanDefinition) {
        try {
            PropertyValues propertyValues = beanDefinition.getPropertyValues();
            for (PropertyValue propertyValue : propertyValues.getPropertyValues()){

                String name = propertyValue.getName();
                Object value = propertyValue.getValue();

                if (value instanceof BeanReference){
                    // A 依赖 B，获取 B 的实例
                    BeanReference beanReference = (BeanReference) value;
                    value = getBean(beanReference.getBeanName());
                }
                
                // 类型转换
                else {
                    Class<?> sourceType = value.getClass();
                    Class<?> targetType = (Class<?>) TypeUtil.getFieldType(bean.getClass(), name);
                    ConversionService conversionService = getConversionService();
                    if (conversionService != null) {
                        if (conversionService.canConvert(sourceType, targetType)) {
                            value = conversionService.convert(value, targetType);
                        }
                    }
                }

                BeanUtil.setFieldValue(bean, name, value);
            }
        } catch (Exception e) {
            throw new BeansException("Error setting property values for bean: " + beanName);
        }
    }
}
```

还有 @Value 的注入

```java
public class AutowiredAnnotationBeanPostProcessor implements InstantiationAwareBeanPostProcessor, BeanFactoryAware {
    @Override
    public PropertyValues postProcessPropertyValues(PropertyValues pvs, Object bean, String beanName) throws BeansException {
        // 处理 @Value
        Class<?> clazz = bean.getClass();
        clazz = ClassUtils.isCglibProxyClass(clazz) ? clazz.getSuperclass() : clazz;

        Field[] declaredFields = clazz.getDeclaredFields();
        
        for (Field field : declaredFields) {
            Value valueAnnotation = field.getAnnotation(Value.class);
            if (valueAnnotation != null) {
                Object value = valueAnnotation.value();
                value = beanFactory.resolveEmbeddedValue((String)value);
                
                // 类型转换
                Class<?> sourceType = value.getClass();
                Class<?> targetType = field.getType();
                ConversionService conversionService = beanFactory.getConversionService();
                if (conversionService != null) {
                    if (conversionService.canConvert(sourceType, targetType)) {
                        value = conversionService.convert(value, targetType);
                    }
                }
                
                try {
                    BeanUtil.setFieldValue(bean, field.getName(), value);
                } catch (NoSuchFieldException e) {
                    throw new BeansException("Failed to set field value for " + field.getName(), e);
                }
            }
        }
        
        // 处理 @Autowired
        for (Field field : declaredFields) {
            Autowired autowiredAnnotation = field.getAnnotation(Autowired.class);
           if (autowiredAnnotation != null) {
               Class<?> fieldType = field.getType();
                String dependentBeanName = null;
                Qualifier qualifierAnnotation = field.getAnnotation(Qualifier.class);
                Object dependentBean = null;
                if (qualifierAnnotation != null) {
                    dependentBeanName = qualifierAnnotation.value();
                    dependentBean = beanFactory.getBean(dependentBeanName, fieldType);
                } else {
                    dependentBean = beanFactory.getBean(fieldType);
                }
                try {
                    BeanUtil.setFieldValue(bean, field.getName(), dependentBean);
                } catch (NoSuchFieldException e) {
                    throw new BeansException("Failed to set field value for " + field.getName(), e);
                }
           }
        }
        return pvs;
    }
}
```

##### 测试

###### 配置

spring.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans>
    <bean id="conversionService" class="springframework.context.support.ConversionServiceFactoryBean">
        <property name="converters" ref="converters"/>
    </bean>

    <bean id="converters" class="converter.ConvertersFactoryBean"/>
    
    <component-scan base-package="bean"/>
</beans>
```

###### Bean

```java
@Component(value = "husband")
public class Husband {

    @Value(value = "你猜")
    private String wifiName;

    @Value(value = "2025-07-13")
    private LocalDate marriageDate;

    public String getWifiName() {
        return wifiName;
    }

    public void setWifiName(String wifiName) {
        this.wifiName = wifiName;
    }

    public LocalDate getMarriageDate() {
        return marriageDate;
    }

    public void setMarriageDate(LocalDate marriageDate) {
        this.marriageDate = marriageDate;
    }

    @Override
    public String toString() {
        return "Husband{" +
                "wifiName='" + wifiName + '\'' +
                ", marriageDate=" + marriageDate +
                '}';
    }
}
```

###### Converter

```java
public class StringToLocalDateConverter implements Converter<String, LocalDate> {
    private final DateTimeFormatter DATE_TIME_FORMATTER;
    
    public StringToLocalDateConverter(String pattern) {
        DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(pattern);
    }
    @Override
    public LocalDate convert(String source) {
        return LocalDate.parse(source, DATE_TIME_FORMATTER);
    }
}
```

```java
public class StringToIntegerConverter implements Converter<String, Integer> {

    @Override
    public Integer convert(String source) {
        return Integer.valueOf(source);
    }
}
```

```java
public class ConvertersFactoryBean implements FactoryBean<Set<?>> {
    @Override
    public Set<?> getObject() throws Exception {
        HashSet<Object> converters = new HashSet<>();
        StringToLocalDateConverter stringToLocalDateConverter = new StringToLocalDateConverter("yyyy-MM-dd");
        converters.add(stringToLocalDateConverter);
        return converters;
    }

    @Override
    public Class<?> getObjectType() {
        return null;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
```

###### ApiTest

```java
public class ApiTest {
    @Test
    public void test_convert() {
        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:spring.xml");
        Husband husband = applicationContext.getBean("husband", Husband.class);
        System.out.println("测试结果：" + husband);
    }

    @Test
    public void test_StringToIntegerConverter() {
        StringToIntegerConverter converter = new StringToIntegerConverter();
        Integer num = converter.convert("1234");
        System.out.println("测试结果：" + num);
    }

    @Test
    public void test_StringToNumberConverterFactory() {
        StringToNumberConverterFactory converterFactory = new StringToNumberConverterFactory();

        Converter<String, Integer> stringToIntegerConverter = converterFactory.getConverter(Integer.class);
        System.out.println("测试结果：" + stringToIntegerConverter.convert("1234"));

        Converter<String, Long> stringToLongConverter = converterFactory.getConverter(Long.class);
        System.out.println("测试结果：" + stringToLongConverter.convert("1234"));
    }
}
```

#### 恭喜你呀

完结啦！手写 Spring 到此为止啦。

回顾一路上，我们从最简单的 Map 开始，到制作 BeanDefinition，复用 SingleBean，使用构造函数实例化 Bean，给 Bean 做属性填充，改为 XML 配置，增加 ApplicationContext……再到 Aware、Event、AOP、注解与包扫描、多级缓存……我们真是搞了不少东西啊。

后续我会更新一篇博文在我的[个人网站](https://ww-weeds.xyz)，欢迎偶尔来逛逛呀。

