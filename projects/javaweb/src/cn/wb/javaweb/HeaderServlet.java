package cn.wb.javaweb;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class HeaderServlet extends HttpServlet {

    public void doGet(HttpServletRequest req, HttpServletResponse resp) {
        try {
            String userAgetn = req.getHeader("User-Agent");
            resp.getWriter().println(userAgetn);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
