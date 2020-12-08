public abstract class DataChecker<T> {

    public abstract void check(T data) throws DataAuthenticationException;

    public abstract void check(Collection<T> data) throws DataAuthenticationException;

    public void check(String method, Object... params) throws Throwable {
        Class<?>[] classes = Stream.of(params)
                .map(Object::getClass)
                .collect(Collectors.toList())
                .toArray(new Class<?>[]{});
        try {
            this.getClass().getDeclaredMethod(method, classes).invoke(this, params);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }
}
