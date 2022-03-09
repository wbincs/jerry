package cn.wb.javaweb;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class GetCookieServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            Cookie[] cookies = req.getCookies();
            if (null != cookies) {
                for (Cookie cookie : cookies) {
                    resp.getWriter().print(cookie.getName() + ":" + cookie.getValue() + "</br>");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
