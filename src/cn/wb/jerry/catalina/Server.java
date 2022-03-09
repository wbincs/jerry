package cn.wb.jerry.catalina;

import cn.hutool.core.date.TimeInterval;
import cn.hutool.log.LogFactory;
import cn.hutool.system.SystemUtil;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 一个Jerry对应一个Server
 */
public class Server {

    private Service service;

    public Server() {
        this.service = new Service(this);
    }

    /**
     * 服务器启动
     */
    public void start() {
        // 计算服务器启动时间
        TimeInterval timeInterval = new TimeInterval();
        // 打印JVM信息
        logJVM();
        System.out.println();

        // 服务器初始化
        init();
        System.out.println();
        LogFactory.get().info("Server startup in {} ms", timeInterval.intervalMs());
    }

    /**
     * 启动jvm，记录日志信息
     */
    private static void logJVM() {

        Map<String, String> infos = new LinkedHashMap<>();

        // 输出服务器信息
        infos.put("Server version", "Jerry/1.0.0");

        // 输出操作系统信息
        infos.put("OS Name\t", SystemUtil.get("os.name"));
        infos.put("OS Version", SystemUtil.get("os.version"));
        infos.put("Architecture", SystemUtil.get("os.arch"));

        // 输出JVM信息
        infos.put("Java Home", SystemUtil.get("java.home"));
        infos.put("JVM Version", SystemUtil.get("java.runtime.version"));
        infos.put("JVM Vendor", SystemUtil.get("java.vm.specification.vendor"));

        Set<String> keys = infos.keySet();
        for (String key : keys) {
            LogFactory.get().info(key + ":\t\t" + infos.get(key));
        }
    }

    private void init() {
        service.start();
    }

}
