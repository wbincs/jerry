package cn.wb.jerry.servlet;

import cn.hutool.core.util.ReflectUtil;
import cn.wb.jerry.catalina.Context;
import cn.wb.jerry.http.Request;
import cn.wb.jerry.http.Response;
import cn.wb.jerry.util.Constant;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * InvokerServlet用来调用用户自己编写的servlet业务逻辑
 */
public class InvokerServlet extends HttpServlet {

    // 饿汉式单例
    private static InvokerServlet instance = new InvokerServlet();

    public static InvokerServlet getInstance() {
        return instance;
    }

    public void service(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        Request request = (Request) httpServletRequest;
        Response response = (Response) httpServletResponse;
        // 获取请求uri
        String uri = request.getUri();
        // 获取应用上下文
        Context context = request.getContext();
        // 根据servlet的uri获取servlet类名称
        String servletClassName = context.getServletClassName(uri);

        try {
            Class<?> servletClass = context.getWebappClassLoader().loadClass(servletClassName);
            // 通过反射创建servlet对象  !!! 并非单例，每次都要new新对象，不妥
            // Object servletObject = ReflectUtil.newInstance(servletClass);
            // 通过上下文对象获取servlet，context中包含servlet对象的单例池，不需要重复创建servlet对象
            Object servletObject = context.getServlet(servletClass);
            // 通过反射调用servlet对象的service方法
            // 不需要管调用doGet还是调用doPost，因为用户编写的Servlet继承自HttpServlet
            // HttpServlet的service会根据request的method，来决定调用doGet还是doPost
            ReflectUtil.invoke(servletObject, "service", request, response);

            // 如果response包含重定向路径，设置302状态码
            if (null != response.getRedirectPath()) {
                response.setStatus(Constant.CODE_302);
            } else {
                response.setStatus(Constant.CODE_200);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
