package cn.wb.jerry.classloader;

import cn.hutool.core.io.FileUtil;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

/**
 * 用来加载用户编写的java类，一个应用对应一个WebappClassLoader
 */
public class WebappClassLoader extends URLClassLoader {

    public WebappClassLoader(String docBase, ClassLoader commonClassLoader) {
        super(new URL[0], commonClassLoader);
        try {
            File webInfFolder = new File(docBase, "WEB-INF");
            // 用户自己编写的类的目录
            File classesFolder = new File(webInfFolder, "classes");
            // 获取用户添加的lib包目录
            File libFolder = new File(webInfFolder, "lib");
            // 添加用户的类目录，/结尾
            URL url = new URL("file:" + classesFolder.getAbsolutePath() + "/");
            addURL(url);
            // 添加用户导入的jar包
            List<File> jarFiles = FileUtil.loopFiles(libFolder);
            for (File file : jarFiles) {
                url = new URL("file:" + file.getAbsolutePath());
                this.addURL(url);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        try {
            this.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
