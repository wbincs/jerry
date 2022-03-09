package cn.wb.jerry.servlet;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.wb.jerry.catalina.Context;
import cn.wb.jerry.http.Request;
import cn.wb.jerry.http.Response;
import cn.wb.jerry.util.Constant;
import cn.wb.jerry.util.WebXMLUtil;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;

/**
 * DefaultServlet用来处理静态资源的获取
 */
public class DefaultServlet extends HttpServlet {

    // 饿汉式单例
    private static DefaultServlet instance = new DefaultServlet();

    public static DefaultServlet getInstance() {
        return instance;
    }

    public void service(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        Request request = (Request) httpServletRequest;
        Response response = (Response) httpServletResponse;
        // 获取请求的uri和上下文
        String uri = request.getUri();
        Context context = request.getContext();

        // 此处是模拟服务端错误
        if ("/500.html".equals(uri)) {
            throw new RuntimeException("this is a deliberately created exception");
        }
        // 如果访问的是根目录，那么修改uri为欢迎页面
        if ("/".equals(uri)) {
            uri = WebXMLUtil.getWelcomeFile(context);
        }

        // 获取uri中的
        String fileName = StrUtil.removePrefix(uri, "/");
        // 根据uri到项目的物理路径下查找实际的文件
        File file = FileUtil.file(request.getRealPath(fileName));
//        File file = FileUtil.file(context.getDocBase(), fileName);
        if (file.exists() && file.isFile()) {
            // 获取文件的拓展名
            String extName = FileUtil.extName(file);
            // 根据拓展名获取对应的mime-type
            String mimeType = WebXMLUtil.getMimeType(extName);
            response.setContentType(mimeType);
            // 获取文件字节内容
            byte[] body = FileUtil.readBytes(file);
            response.setBody(body);

            // 此处是为了模拟大文件的传输
            if (fileName.equals("timeConsume.html")) {
                ThreadUtil.sleep(1000);
            }

            response.setStatus(Constant.CODE_200);
        } else {
            // 文件不存在，返回404，设置code之后，会有后续的方法根据code进行不同的处理
            response.setStatus(Constant.CODE_404);
        }
    }

}
