package cn.wb.jerry.classloader;

import cn.hutool.core.util.StrUtil;
import cn.wb.jerry.catalina.Context;
import cn.wb.jerry.util.Constant;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JspClassLoader 的特点:
 * 1.一个jsp文件就对应一个JspClassLoader
 * 2.如果这个jsp文件修改了，那么就要换一个新的JspClassLoader
 * 3.JspClassLoader基于由jsp文件转移并编译出来的class文件，进行类的加载
 */
public class JspClassLoader extends URLClassLoader {

    // map属性就是用来做jsp文件和JspClassLoader 的映射的
    private static Map<String, JspClassLoader> map = new ConcurrentHashMap<>();

    // 让jsp和classloader取消关联，脱钩
    public static void invalidJspClassLoader(String uri, Context context) {
        String key = context.getPath() + "/" + uri;
        map.remove(key);
    }

    // 获取jsp对应的classloader，如果没有的话，就创建一个
    public static JspClassLoader getJspClassLoader(String uri, Context context) {
        String key = context.getPath() + "/" + uri;
        JspClassLoader loader = map.get(key);
        if (null == loader) {
            loader = new JspClassLoader(context);
            map.put(key, loader);
        }
        return loader;
    }

    // 构造方法，JspClassLoader会基于WebClassLoader来创建。
    // 然后很据contxs的信息获取到%TOMCAT_HOME%work目录下对应的目录，
    // 并且把这个目最作为URL加入到当前Classloader里，
    // 这样通过当前jspolistader加载jsp类的时候，就可以找划时应的美文件了，
    private JspClassLoader(Context context) {
        super(new URL[0], context.getWebClassLoader());
        try {
            String path = context.getPath();
            String subFolder;
            if ("/".equals(path)) {
                subFolder = "_";
            } else {
                subFolder = StrUtil.subAfter(path, '/', false);
            }
            File classesFolder = new File(Constant.workFolder, subFolder);
            URL url = new URL("file:" + classesFolder.getAbsolutePath() + "/");
            this.addURL(url);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
