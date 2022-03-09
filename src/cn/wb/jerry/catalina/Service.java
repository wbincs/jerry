package cn.wb.jerry.catalina;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.log.LogFactory;
import cn.wb.jerry.util.ServerXMLUtil;
import java.util.List;

/**
 * 用来提供对外服务
 */
public class Service {

    // Catalina
    private String name;

    private Server server;

    private Engine engine;

    private List<Connector> connectors;

    public Service(Server server) {
        this.server = server;
        this.name = ServerXMLUtil.getServiceName();
        this.engine = new Engine(this);
        this.connectors = ServerXMLUtil.getConnectors(this);
    }

    public void start() {
        init();
    }

    private void init() {
        TimeInterval timeInterval = DateUtil.timer();
        // 先初始化connector，实际上内部只打印了日志
        for (Connector connector : connectors) {
            connector.init();
        }
        LogFactory.get().info("Initialization processed in {} ms", timeInterval.intervalMs());
        // 启动每个connector，一个connector对应一个配置文件项，对应 不同的端口 和 不同的解压配置参数
        // 每个connector开启一个线程，监听某个配置的端口，并使用阻塞IO的方式接收客户端的请求
        for (Connector connector : connectors) {
            connector.start();
        }
    }

    public String getName() {
        return name;
    }

    public Server getServer() {
        return server;
    }

    public Engine getEngine() {
        return engine;
    }

    public List<Connector> getConnectors() {
        return connectors;
    }
}
