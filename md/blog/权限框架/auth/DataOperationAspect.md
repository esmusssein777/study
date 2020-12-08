
@Aspect
@Configuration
public class DataOperationAspect {

    private static final Logger logger = LoggerFactory.getLogger(DataOperationAspect.class);

    private DataSecurityConfig dataSecurityConfig;

    public DataOperationAspect(DataSecurityConfig dataSecurityConfig) {
        this.dataSecurityConfig = dataSecurityConfig;
    }

    @Pointcut("@annotation(com.daimler.otr.common.datasecurity.CheckAuthority)")
    public void validateMethod() {
    }

    @Before("validateMethod()")
    public void validate(JoinPoint joinPoint) throws Throwable {
        logger.debug("check method {}", joinPoint.toShortString());
        if (joinPoint.getArgs() == null || joinPoint.getArgs().length == 0) {
            return;
        }
        if (!(joinPoint.getSignature() instanceof MethodSignature)) {
            return;
        }
        if (dataSecurityConfig.canSkipSecurityCheck()) {
            return;
        }
        List<Pair<Parameter, Object>> paramArgPairs = getParamArgPairs(joinPoint);
        for (Pair<Parameter, Object> paramPair : paramArgPairs) {
            checkParameter(paramPair.getLeft(), paramPair.getRight());
        }
    }

    private List<Pair<Parameter, Object>> getParamArgPairs(JoinPoint joinPoint) {
        List<Pair<Parameter, Object>> pairs = new ArrayList<>();
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Parameter[] parameters = method.getParameters();
        Object[] values = joinPoint.getArgs();
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            if (!parameter.isAnnotationPresent(ParamChecker.class)) {
                continue;
            }
            Pair<Parameter, Object> valuePair = Pair.of(parameter, values[i]);
            pairs.add(valuePair);
        }
        return pairs;
    }

    private void checkParameter(Parameter param, Object value) throws Throwable {
        ParamChecker paramChecker = param.getAnnotation(ParamChecker.class);
        DataChecker checker = dataSecurityConfig.getChecker(paramChecker.value());
        if ("check".equals(paramChecker.method())) {
            checker.check(value);
        } else {
            checker.check(paramChecker.method(), value);
        }
    }
}
