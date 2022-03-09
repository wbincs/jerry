package cn.wb.jerry.catalina;

import cn.wb.jerry.http.Request;
import cn.wb.jerry.http.Response;
import cn.wb.jerry.util.ServerXMLUtil;
import cn.wb.jerry.util.ThreadPool;

import java.io.IOException;
import java.net.Socket;
import java.util.List;

/**
 * 存储多个host
 */
public class Engine {

    // localhost
    private String defaultHost;

    private Service service;

    private List<Host> hosts;

    public Engine(Service service) {
        this.service = service;
        this.defaultHost = ServerXMLUtil.getEngineDefaultHost();
        this.hosts = ServerXMLUtil.getHosts(this);
        checkDefault();
    }

    // 提交请求处理任务
    public void execute(Socket s, Connector connector) {
        // 创建Runable对象，交给线程池处理
        Runnable runnable = () -> {
            try {
                // 将Socket封装为Request
                Request request = new Request(s, connector);
                // 创建Response
                Response response = new Response();
                // 创建处理器
                HttpProcessor httpProcessor = new HttpProcessor();
                // 使用httpProcessor处理request和response
                httpProcessor.execute(request, response);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                // 关闭socket
                if (!s.isClosed()) {
                    try {
                        s.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } // end try
        };
        // 交给线程池处理
        try {
            ThreadPool.run(runnable);
            // 线程池执行可能会跑异常，因为默认的线程池策略是abort
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 检查是否存在默认host
    private void checkDefault() {
        // 没有默认host就抛出异常
        if (null == getDefaultHost()) {
            throw new RuntimeException("the defaultHost" + this.defaultHost + " does not exist!");
        }
    }

    public Host getDefaultHost() {
        for (Host host : hosts) {
            if (host.getName().equals(defaultHost)) {
                return host;
            }
        }
        return null;
    }
}
