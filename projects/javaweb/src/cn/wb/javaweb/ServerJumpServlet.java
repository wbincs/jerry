package cn.wb.javaweb;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ServerJumpServlet extends HttpServlet {

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            request.setAttribute("name", "wubin");
            request.getRequestDispatcher("hello.jsp").forward(request, response);
            System.out.println("服务端跳转后，继续执行");
        } catch (ServletException e) {
            e.printStackTrace();
        }
    }

}
