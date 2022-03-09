package cn.wb.jerry.watcher;

import cn.hutool.core.io.watch.WatchMonitor;
import cn.hutool.core.io.watch.WatchUtil;
import cn.hutool.core.io.watch.Watcher;
import cn.hutool.log.LogFactory;
import cn.wb.jerry.catalina.Context;
import java.nio.file.Path;
import java.nio.file.WatchEvent;

/**
 * 应用文件夹的热部署，监听解压后的应用，在context类中使用，监听context文件变化，然后重新加载context
 */
public class ContextFileChangeWatcher {

    // 监视器，监视文件夹是否发生变化
    private WatchMonitor monitor;

    // 防止在监听到文件变化到重新发布完成之间重复发布，一个监视器只负责一次重新部署
    private boolean stop = false;

    public ContextFileChangeWatcher(final Context context) {
        // 参数一： 监听的文件夹
        // 参数二： 代表监听的深入，如果是0或者1，表示只监听当前目录，而不监听子目录
        // 参数三： 当有文件发生变化，就回访问watcher对应的方法
        this.monitor = WatchUtil.createAll(context.getDocBase(), Integer.MAX_VALUE, new Watcher() {
            private void dealWith(WatchEvent<?> event) {
                // TODO synchronized效率问题
                synchronized (ContextFileChangeWatcher.class) {
                    String fileName = event.context().toString();
                    // 如果没有加载过的话
                    if (!stop) {
                        if (fileName.endsWith(".jar") || fileName.endsWith(".class") || fileName.endsWith(".xml")) {
                            stop = true;
                            LogFactory.get().info(this + " 检测到了Web应用下的重要文件变化 {} ", fileName);
                            context.reload();
                        }
                    }
                }
            }

            @Override
            public void onCreate(WatchEvent<?> event, Path currentPath) {
                this.dealWith(event);
            }

            @Override
            public void onModify(WatchEvent<?> event, Path currentPath) {
                this.dealWith(event);
            }

            @Override
            public void onDelete(WatchEvent<?> event, Path currentPath) {
                this.dealWith(event);
            }

            @Override
            public void onOverflow(WatchEvent<?> event, Path currentPath) {
                this.dealWith(event);
            }
        });
        this.monitor.setDaemon(true);
    }

    public void start() {
        this.monitor.start();
    }

    public void stop() {
        this.monitor.close();
    }

}
