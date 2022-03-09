package cn.wb.javaweb;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class HelloServlet extends HttpServlet{

    public HelloServlet(){
        System.out.println(this + "的构造方法");
    }

    public void init(ServletConfig config) {
        String author = config.getInitParameter("author");
        String site = config.getInitParameter("site");

        System.out.println(this + "的初始化方法 init");
        System.out.println("获取到了参数 author:" + author);
        System.out.println("获取到了参数 site:" + site);
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) {
        try {
            System.out.println(this + "的doGet方法");
            response.getWriter().println("Hello Jerry from HelloServlet@javaweb" + this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void destroy(){
        System.out.println(this + "被销毁");
    }

}
