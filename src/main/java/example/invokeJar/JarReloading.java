package example.invokeJar;

import qj.util.ReflectUtil;
import qj.util.ThreadUtil;
import qj.util.lang.DynamicClassLoader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by Hejy on 2018/3/6.
 */
public class JarReloading {
    static Class<?> contextClass = null;
    static Object context = null;
    public static void main(String[] args) throws Exception {
        ReloadContext();
        //释放资源句柄，启动 JVM 垃圾回收机制
        contextClass = null; // 类加载器会缓存加载的类
        context = null;
        ThreadUtil.sleep(100);

        //更新 JAR 包，在线升级
        File source = new File("target/classes/testJar-1.1.jar");
        File dest = new File("target/classes/testJar-1.0.jar");
        if (dest.exists()) {
            dest.delete();
            JarReloading1.copyFile(source, dest);
        }

        ReloadContext();
    }

    private static void ReloadContext() {
        createContext();
        for (int i = 0; i < 5; i++) {
            System.out.print( i + ":");
            invokeInit();
            invokeHobby();
            ThreadUtil.sleep(3500);
        }
    }

    private static void createContext() {//
       contextClass = new DynamicClassLoader("target/classes/testJar-1.0.jar")
                .load("cmbchina.example.relfect.invokejar.Context");
        context = ReflectUtil.newInstance(contextClass);
    }

    private static void invokeInit() {
        ReflectUtil.invoke("init", context);
    }

    private static void invokeHobby() {
        //Object hobbyService = ReflectUtil.getFieldValue("hobbyService", context);
        Object obj = ReflectUtil.invoke("hobby", context);
        System.out.println(obj.toString());
    }

}
