package cn.wb.jerry.watcher;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.watch.WatchMonitor;
import cn.hutool.core.io.watch.WatchUtil;
import cn.hutool.core.io.watch.Watcher;
import cn.wb.jerry.catalina.Host;
import cn.wb.jerry.util.Constant;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;

/**
 * war包监视器，监听war包的变化，实现war包的动态热部署
 * 当前仅支持新增war包的解压和部署，不支持war包项目的更新
 */
public class WarFileWatcher {
    // 监视器
    private WatchMonitor monitor;

    public WarFileWatcher(final Host host) {
        // 参数一： 监听的文件夹
        // 参数二： 代表监听的深入，如果是0或者1，表示只监听当前目录，而不监听子目录，此处不监视子目录，只监视webapps文件夹
        // 参数三： 当有文件发生变化，就回访问watcher对应的方法
        this.monitor = WatchUtil.createAll(Constant.webappsFolder, 1, new Watcher() {
            private void dealWith(WatchEvent<?> event, Path currentPath) {
                synchronized (WarFileWatcher.class) {
                    String fileName = event.context().toString();
                    System.out.println(fileName);
                    if (fileName.toLowerCase().endsWith(".war") && StandardWatchEventKinds.ENTRY_CREATE.equals(event.kind())) {
                        File warFile = FileUtil.file(Constant.webappsFolder, fileName);
                        // 注意，这里虽然调用了host的loadWar，但是并不会起到热更新的作用
                        // 因为host发现已经存在同名的应用的话，压根不会解压更新后的war包
                        // TODO 此处war包热更新需要修改
                        host.loadWar(warFile);
                    }
                }
            }

            public void onCreate(WatchEvent<?> event, Path currentPath) {
                this.dealWith(event, currentPath);
            }

            public void onModify(WatchEvent<?> event, Path currentPath) {
                this.dealWith(event, currentPath);
            }

            public void onDelete(WatchEvent<?> event, Path currentPath) {
                this.dealWith(event, currentPath);
            }

            public void onOverflow(WatchEvent<?> event, Path currentPath) {
                this.dealWith(event, currentPath);
            }
        });
    }

    public void start() {
        this.monitor.start();
    }

    public void stop() {
        this.monitor.interrupt();
    }

}
