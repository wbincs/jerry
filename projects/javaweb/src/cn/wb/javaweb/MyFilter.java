package cn.wb.javaweb;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Enumeration;

public class MyFilter implements Filter {

    public void init(FilterConfig filterConfig) throws ServletException {
        System.out.println("MyFilter 初始化");
        Enumeration<String> e = filterConfig.getInitParameterNames();
        while(e.hasMoreElements()) {
            String name = e.nextElement();
            String value = filterConfig.getInitParameter(name);
            System.out.println(name+" : " + value);
        }
    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest)servletRequest;
        String url = request.getRequestURL().toString();
        long start = System.currentTimeMillis();
        filterChain.doFilter(servletRequest, servletResponse);
        long end = System.currentTimeMillis();
        System.out.println(end - start + " ms elapsed on url:" + url);
    }

    public void destroy() {
    }

}
