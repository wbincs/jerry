package cn.wb.jerry;

import cn.hutool.core.io.FileUtil;
import cn.wb.jerry.classloader.CommonClassLoader;
import cn.wb.jerry.util.ThreadPool;
import sun.misc.Launcher;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;

/**
 * Jerry主启动类
 */
public class BootStrap {

    public static void main(String[] args) throws Exception {
        // 打印Logo
        String banner =
                "   ___\n" +
                "  |_  |\n" +
                "    | | ___ _ __ _ __ _   _\n" +
                "    | |/ _ \\ '__| '__| | | |\n" +
                "/\\__/ /  __/ |  | |  | |_| |\n" +
                "\\____/ \\___|_|  |_|   \\__, |\n" +
                "                       __/ |\n" +
                "                      |___/";
        System.out.println(banner);

        // 除了BootStrap和CommonClassLoader两个类由AppClassLoader加载之外
        // Jerry中的所有类都由CommonClassLoader来加载
        CommonClassLoader commonClassLoader = new CommonClassLoader();
        Thread.currentThread().setContextClassLoader(commonClassLoader);

        // 加载Server类
        String serverClassName = "cn.wb.jerry.catalina.Server";
        Class<?> serverClazz = commonClassLoader.loadClass(serverClassName);

        // 创建Server实例
        Object serverObject = serverClazz.newInstance();
        Method m = serverClazz.getMethod("start");
        // 调用Server的start方法，启动服务器
        m.invoke(serverObject);
    }

}
