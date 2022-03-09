package cn.wb.javaweb;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class SetSessionServlet extends HttpServlet {

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            resp.setContentType("text/html;chartset=UTF-8");
            req.getSession().setAttribute("name_in_session", "wubin_in_session");
            resp.getWriter().println(req.getSession().getId());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
