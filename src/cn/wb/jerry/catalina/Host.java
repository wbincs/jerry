package cn.wb.jerry.catalina;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.LogFactory;
import cn.wb.jerry.util.Constant;
import cn.wb.jerry.util.ServerXMLUtil;
import cn.wb.jerry.watcher.WarFileWatcher;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 一个Host包含多个应用
 */
public class Host {

    private String name;

    private Engine engine;

    // 保存应用
    private Map<String, Context> contextMap;

    public Host(String name, Engine engine) {
        this.contextMap = new HashMap<>();
        this.name = name;
        this.engine = engine;
        // 扫描并加载webapps文件夹下的应用
        scanContextsOnWebAppsFolder();
        // 扫描并加载server.xml中配置的应用
        scanContextsInServerXML();
        // 扫描并加载webapps文件夹下的war包，解压并加载
        scanWarOnWebAppsFolder();
        // 监听war包，实现动态部署
        WarFileWatcher warFileWatcher = new WarFileWatcher(this);
        warFileWatcher.start();
    }

    /**
     * 扫描webapps目录下的应用
     */
    private void scanContextsOnWebAppsFolder() {
        File[] folders = Constant.webappsFolder.listFiles();
        if (folders != null) {
            for (File folder : folders) {
                if (folder.isDirectory()) {
                    loadContext(folder);
                }
            }
        }
    }

    /**
     * 根据server.xml加载应用
     */
    private void scanContextsInServerXML() {
        List<Context> contexts = ServerXMLUtil.getContexts(this);
        for (Context context : contexts) {
            contextMap.put(context.getPath(), context);
        }
    }

    /**
     * 加载上下文应用
     */
    private void loadContext(File folder) {
        String path = folder.getName();
        if ("ROOT".equals(path)) {
            path = "/";
        } else {
            path = "/" + path;
        }
        String docBase = folder.getAbsolutePath();
        Context context = new Context(path, docBase, this, true);
        contextMap.put(context.getPath(), context);
    }

    /**
     * 重新加载context
     */
    public void reload(Context context) {
        LogFactory.get().info("Reloading Context with name [{}] has started", context.getPath());
        String path = context.getPath();
        String docBase = context.getDocBase();
        boolean reloadable = context.isReloadable();
        // stop
        context.stop();
        // remove
        this.contextMap.remove(path);
        // allocate ew context
        Context newContext = new Context(path, docBase, this, reloadable);
        // asign it to map
        this.contextMap.put(newContext.getPath(), newContext);
        LogFactory.get().info("Reloading Context with name [{}] has completed", context.getPath());
    }

    /**
     * 扫描webapp目录下的所有war文件
     */
    private void scanWarOnWebAppsFolder() {
        File folder = FileUtil.file(Constant.webappsFolder);
        File[] files = folder.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.getName().toLowerCase().endsWith(".war")) {
                loadWar(file);
            }
        }
    }

    /**
     * 把war解压 再把解压后的应用文件夹进行加载，不支持war包的热更新，只支持war包解压后的文件夹热更新。
     * @param warFile
     */
    public void loadWar(File warFile) {
        // 获取应用名称，war包名称
        String fileName = warFile.getName();
        String folderName = StrUtil.subBefore(fileName, ".", true);
        // 判断是否已经加载过同名的应用，如果有同名的，则不解压加载
        Context context = this.getContext("/" + folderName);
        if (null == context) {
            File folder = new File(Constant.webappsFolder, folderName);
            if (!folder.exists()) {
                File tempWarFile = FileUtil.file(Constant.webappsFolder, folderName, fileName);
                File contextFolder = tempWarFile.getParentFile();
                contextFolder.mkdir();
                FileUtil.copyFile(warFile, tempWarFile);
                String command = "jar xvf " + fileName;
                Process p = RuntimeUtil.exec(null, contextFolder, command);

                try {
                    p.waitFor();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                tempWarFile.delete();
                this.loadContext(contextFolder);
            }
        }
    }

    public String getName() {
        return this.name;
    }

    public Context getContext(String path) {
        return this.contextMap.get(path);
    }
}
