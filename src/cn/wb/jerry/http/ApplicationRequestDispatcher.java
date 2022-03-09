package cn.wb.jerry.http;

import cn.wb.jerry.catalina.HttpProcessor;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

/**
 * Request类使用，用来进行请求转发
 */
public class ApplicationRequestDispatcher implements RequestDispatcher {

    // 请求转发地址
    private String uri;

    public ApplicationRequestDispatcher(String uri) {
        if (!uri.startsWith("/")) {
            uri = "/" + uri;
        }
        this.uri = uri;
    }

    // 用户主动调用forward方法
    public void forward(ServletRequest servletRequest, ServletResponse servletResponse) {
        Request request = (Request) servletRequest;
        Response response = (Response) servletResponse;
        request.setUri(this.uri);
        HttpProcessor processor = new HttpProcessor();
        processor.execute(request, response);
        request.setForwarded(true);
    }

    public void include(ServletRequest arg0, ServletResponse arg1){
    }

}
