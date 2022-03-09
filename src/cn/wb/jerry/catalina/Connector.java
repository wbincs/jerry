package cn.wb.jerry.catalina;

import cn.hutool.log.LogFactory;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * 负责监听端口，彩云BIO的方式接收请求，封装请求，然后交给线程池处理
 */
public class Connector implements Runnable {

    // 端口
    private int port;

    // Catalina
    private Service service;

    // 压缩功能是否打开
    private String compression;

    // 压缩的最小大小
    private int compressionMinSize;

    // 不压缩的客户端类型
    private String noCompressionUserAgents;

    // 可压缩的 mime Type
    private String compressibleMimeType;

    public Connector(Service service) {
        this.service = service;
    }

    // init初始化方法先被调用，全部项目初始化完成后调用start方法
    public void init() {
        LogFactory.get().info("Initializing ProtocolHandler [http-bio-{}]", port);
    }

    // start启动方法后被调用
    public void start() {
        LogFactory.get().info("Starting ProtocolHandler [http-bio-{}]", port);
        // 开启一个独立地线程，线程内部使用阻塞IO的方式监听配置的端口
        (new Thread(this)).start();
    }

    @Override
    public void run() {
        try {
            // TODO 当前是采用BIO的方式监听端口，处理网络请求，后续考虑使用NIO的方式
            ServerSocket ss = new ServerSocket(port);
            while (true) {
                // 等待连接socket
                Socket s = ss.accept();
                // 将Socket和Connector交给Engine处理
                service.getEngine().execute(s, this);
//                service.getEngine().execute2(request, response);
            } // while end
        } catch (IOException e) {
            LogFactory.get().error("Connector {} 停止服务", this.port);
            LogFactory.get().error(e);
            e.printStackTrace();
        }
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }

    public String getCompression() {
        return this.compression;
    }

    public void setCompression(String compression) {
        this.compression = compression;
    }

    public int getCompressionMinSize() {
        return this.compressionMinSize;
    }

    public void setCompressionMinSize(int compressionMinSize) {
        this.compressionMinSize = compressionMinSize;
    }

    public String getNoCompressionUserAgents() {
        return this.noCompressionUserAgents;
    }

    public void setNoCompressionUserAgents(String noCompressionUserAgents) {
        this.noCompressionUserAgents = noCompressionUserAgents;
    }

    public String getCompressibleMimeType() {
        return this.compressibleMimeType;
    }

    public void setCompressibleMimeType(String compressibleMimeType) {
        this.compressibleMimeType = compressibleMimeType;
    }
}
