
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface ParamChecker {
    String value();

    String method() default "check";
}