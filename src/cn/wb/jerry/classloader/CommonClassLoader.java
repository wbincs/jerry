package cn.wb.jerry.classloader;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * 用来加载lib文件夹下的所有jar包
 */
public class CommonClassLoader extends URLClassLoader {

    public CommonClassLoader() {
        super(new URL[0]);
        try {
            File libFolder = new File(System.getProperty("user.dir"), "lib");
            File[] jarFiles = libFolder.listFiles();
            if (null != jarFiles) {
                // jarFiles 可能为空
                for (File file : jarFiles) {
                    if (file.getName().endsWith("jar")) {
                        URL url = new URL("file:" + file.getAbsolutePath());
                        addURL(url);
                    }
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

}
