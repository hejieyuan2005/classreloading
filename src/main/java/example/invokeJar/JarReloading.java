package example.invokeJar;

import qj.util.ReflectUtil;
import qj.util.ThreadUtil;
import qj.util.lang.DynamicClassLoader;

/**
 * Created by Hejy on 2018/3/6.
 */
public class JarReloading {
    public static void main(String[] args) {
        Reloadtest();

        //ReloadContext();
    }

    private static void Reloadtest(){
        for (;;) {
            Object context = createContext("cmbchina.example.relfect.invokejar.test1");
            invokeInit(context);
            invokeSay(context);
            ThreadUtil.sleep(2000);
        }
    }

    private static void ReloadContext(){
        for (;;) {
            Object context = createContext("cmbchina.example.relfect.invokejar.Context");
            invokeInit(context);
            invokeHobby(context);
            ThreadUtil.sleep(3500);
        }
    }
    private static Object createContext(String clsName) {//
        Class<?> contextClass = new DynamicClassLoader("target/classes/testJar-1.0.jar")
                .load(clsName);
        Object context = ReflectUtil.newInstance(contextClass);
        return context;
    }

    private static void invokeSay(Object context) {
        //Object hobbyService = ReflectUtil.getFieldValue("hobbyService", context);
        try {
            ReflectUtil.invoke("say", context);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private static void invokeInit(Object context){
        ReflectUtil.invoke("init", context);
    }

    private static void invokeHobby(Object context) {
        //Object hobbyService = ReflectUtil.getFieldValue("hobbyService", context);
        Object obj = ReflectUtil.invoke("hobby", context);
        System.out.println(obj.toString());
    }
}
