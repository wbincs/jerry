package cn.wb.javaweb;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class ContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent e) {
        System.out.println("我是javaweb，我监听到web 应用 " + e.getSource() + " 的初始化事件  ");
    }

    @Override
    public void contextDestroyed(ServletContextEvent e) {
        System.out.println("我是javaweb，我监听到web 应用 " + e.getSource() + " 的销毁事件  ");
    }

}
