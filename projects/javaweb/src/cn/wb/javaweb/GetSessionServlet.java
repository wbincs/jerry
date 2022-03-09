package cn.wb.javaweb;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class GetSessionServlet extends HttpServlet {

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) {
        try {
            resp.setContentType("text/html;chartset=UTF-8");
            String name_in_session = (String) req.getSession().getAttribute("name_in_session");
            resp.getWriter().println(name_in_session);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
