package cn.wb.javaweb;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class SetCookieServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Cookie cookie = new Cookie("name", "wubin_in_cookie");
        cookie.setMaxAge(60 * 24 * 60);
        cookie.setPath("/");
        resp.addCookie(cookie);
        resp.getWriter().println("set cookie successfully");
    }

}
