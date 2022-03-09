package cn.wb.jerry.util;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.wb.jerry.catalina.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

/**
 * 与servlet.xml配置文件相关的工具类，通过该工具类获取配置文件内的信息
 */
public class ServerXMLUtil {

    // 获取service名称
    public static String getServiceName() {
        String xml = FileUtil.readUtf8String(Constant.serverXmlFile);
        Document d = Jsoup.parse(xml);
        Element host = d.select("Service").first();
        return host.attr("name");
    }

    // 获取指定service下配置的Connectors
    public static List<Connector> getConnectors(Service service) {
        List<Connector> connectors = new ArrayList<>();
        String xml = FileUtil.readUtf8String(Constant.serverXmlFile);
        Document d = Jsoup.parse(xml);
        Elements es = d.select("Connector");
        for (Element e : es) {
            Connector c = new Connector(service);

            int port = Convert.toInt(e.attr("port"));
            String compression = e.attr("compression");
            int compressionMinSize = Convert.toInt(e.attr("compressionMinSize"), 0);
            String noCompressionUserAgents = e.attr("noCompressionUserAgents");
            String compressableMimeType = e.attr("compressableMimeType");

            c.setPort(port);
            c.setCompression(compression);
            c.setCompressibleMimeType(compressableMimeType);
            c.setNoCompressionUserAgents(noCompressionUserAgents);
            c.setCompressibleMimeType(compressableMimeType);
            c.setCompressionMinSize(compressionMinSize);

            connectors.add(c);
        }
        return connectors;
    }

    // 获取hostname，读取第一个Host的name
    public static String getHostName() {
        String xml = FileUtil.readUtf8String(Constant.serverXmlFile);
        Document d = Jsoup.parse(xml);
        Element host = d.select("Host").first();
        return host.attr("name");
    }

    // 获取host列表
    public static List<Host> getHosts(Engine engine) {
        List<Host> hosts = new ArrayList<>();
        String xml = FileUtil.readUtf8String(Constant.serverXmlFile);
        Document d = Jsoup.parse(xml);
        Elements es = d.select("Host");
        for (Element e : es) {
            String name = e.attr("name");
            Host host = new Host(name, engine);
            hosts.add(host);
        }
        return hosts;
    }

    // 获取engine的默认host，根据Engine的defaultHost参数指定
    public static String getEngineDefaultHost() {
        String xml = FileUtil.readUtf8String(Constant.serverXmlFile);
        Document d = Jsoup.parse(xml);
        Element host = d.select("Engine").first();
        return host.attr("defaultHost");
    }

    // 获取传入的host中的所有应用
    public static List<Context> getContexts(Host host) {
        List<Context> contexts = new ArrayList<>();
        String xml = FileUtil.readUtf8String(Constant.serverXmlFile);
        Document d = Jsoup.parse(xml);
        Elements es = d.select("Context");
        for (Element e : es) {
            // 上下文路径
            String path = e.attr("path");
            // 磁盘上的文件夹位置
            String docBase = e.attr("docBase");
            // 是否可冲入加载，是否支持热部署，默认支持
            boolean reloadable = Convert.toBool(e.attr("reloadable"), true);

            Context context = new Context(path, docBase, host, reloadable);
            contexts.add(context);
        }
        return contexts;
    }

}
