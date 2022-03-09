package cn.wb.jerry.servlet;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.wb.jerry.catalina.Context;
import cn.wb.jerry.classloader.JspClassLoader;
import cn.wb.jerry.http.Request;
import cn.wb.jerry.http.Response;
import cn.wb.jerry.util.JspUtil;
import cn.wb.jerry.util.WebXMLUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

/**
 *
 */
public class JspServlet extends HttpServlet {

    // 懒汉式单例
    private static JspServlet instance = new JspServlet();

    public static JspServlet getInstance() {
        return instance;
    }

    public void service(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException, ServletException {
        try {
            Request request = (Request) httpServletRequest;
            Response response = (Response) httpServletResponse;
            // 获取请求的uri
            String uri = request.getRequestURI();
            if ("/".equals(uri)) {
                uri = WebXMLUtil.getWelcomeFile(request.getContext());
            }
            // 获取jsp的filename
            String fileName = StrUtil.removePrefix(uri, "/");
            // 获取jsp文件
            File file = FileUtil.file(request.getRealPath(fileName));
            if (file.exists()) {
                Context context = request.getContext();
                String path = context.getPath();
                String subFolder;
                // 如果访问的是ROOT目录，则编译后的文件放在work目录下的 _ 目录（其实就是ROOT）
                if ("/".equals(path)) {
                    subFolder = "_";
                } else {
                    subFolder = StrUtil.subAfter(path, '/', false);
                }
                // 获取servlet的类路径
                String servletClassPath = JspUtil.getServletClassPath(uri, subFolder);
                // 获取servlet
                File jspServletClassFile = new File(servletClassPath);
                // 如果没有class文件，则编译class
                if (!jspServletClassFile.exists()) {
                    JspUtil.compileJsp(context, file);
                    // 如果jsp文件时间晚于class文件，则重新编译class
                } else if (file.lastModified() > jspServletClassFile.lastModified()) {
                    JspUtil.compileJsp(context, file);
                    // jsp和jspclassload脱钩
                    JspClassLoader.invalidJspClassLoader(uri, context);
                }

                // 拿到mineType
                String extName = FileUtil.extName(file);
                String mimeType = WebXMLUtil.getMimeType(extName);
                response.setContentType(mimeType);

                //根据uri和context获取当前jsp对应的JspClassLoader
                JspClassLoader jspClassLoader = JspClassLoader.getJspClassLoader(uri, context);
                //获取jsp对应的servlet Class Name
                String jspServletClassName = JspUtil.getJspServletClassName(uri, subFolder);
                //通过JspClassLoader根据servlet Class Name 加载类对象:jspServletClass
                Class<?> jspServletClass = jspClassLoader.loadClass(jspServletClassName);
                // 从上下文对象中获取servlet对象实例
                HttpServlet servlet = context.getServlet(jspServletClass);

                // 调用service
                servlet.service(request, response);

                if (null != response.getRedirectPath()) {
                    response.setStatus(302);
                } else {
                    response.setStatus(200);
                }
            } else {
                response.setStatus(404);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
