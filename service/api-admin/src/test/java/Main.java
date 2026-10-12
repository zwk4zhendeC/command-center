import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Main {

    public static void main(String[] args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        S s = new S();
        s.print();
        Method setF = F.class.getMethod("setA", null);
        Method setS = S.class.getMethod("setA", null);
        System.out.println(setF.equals(setS));
        setF.invoke(s);
        s.print();
    }
}
