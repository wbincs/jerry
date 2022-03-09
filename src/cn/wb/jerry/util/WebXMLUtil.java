package cn.wb.jerry.util;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.wb.jerry.catalina.Context;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 与servlet.xml配置文件相关的工具类，通过该工具类获取配置文件内的信息
 */
public class WebXMLUtil {

    // 用来存储web.xml文件中的mime-mapping，key为extension，也就是拓展名，value为对应的mime-type
    private static Map<String, String> mimeTypeMapping = new HashMap<>();

    // 初始化mimeTypeMapping，从web.xml文件中读取
    static {
        String xml = FileUtil.readUtf8String(Constant.webXmlFile);
        Document d = Jsoup.parse(xml);
        Elements es = d.select("mime-mapping");
        for (Element e : es) {
            String extName = e.select("extension").first().text();
            String mimeType = e.select("mime-type").first().text();
            mimeTypeMapping.put(extName, mimeType);
        }
    }

    // 获取默认的欢迎页
    public static String getWelcomeFile(Context context) {
        String xml = FileUtil.readUtf8String(Constant.webXmlFile);
        Document d = Jsoup.parse(xml);
        Elements es = d.select("welcome-file");
        for (Element e : es) {
            String welcomeFileName = e.text();
            File f = new File(context.getDocBase(), welcomeFileName);
            if (f.exists()) {
                return f.getName();
            }
        }
        return "/";
    }

    // 获取web.xml中偶配置的session的过期时间，不配置默认为30分钟
    public static int getTimeout() {
        byte defaultResult = 30;
        try {
            Document d = Jsoup.parse(Constant.webXmlFile, "utf-8");
            Elements es = d.select("session-config session-timeout");
            return es.isEmpty() ? defaultResult : Convert.toInt(((Element) es.get(0)).text());
        } catch (IOException e) {
            return defaultResult;
        }
    }

    public static String getMimeType(String extName) {
        String mimeType = mimeTypeMapping.get(extName);
        return null == mimeType ? "text/html" : mimeType;
    }

}
