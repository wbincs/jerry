package cn.wb.jerry.util;

import cn.hutool.core.io.FileUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * 与context.xml配置文件相关的工具类，通过该工具类获取配置文件内的信息
 */
public class ContextXMLUtil {

    // 获取资源监听的文件路径
    // 在context.xml文件中进行配置
    public static String getWatchedResource() {
        try {
            String xml = FileUtil.readUtf8String(Constant.contextXmlFile);
            Document d = Jsoup.parse(xml);
            Element e = d.select("WatchedResource").first();
            return e.text();
        } catch (Exception e) {
            e.printStackTrace();
            return "WEB-INF/web.xml";
        }
    }

}
