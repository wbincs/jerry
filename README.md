```
   ___
  |_  |
    | | ___ _ __ _ __ _   _
    | |/ _ \ '__| '__| | | |
/\__/ /  __/ |  | |  | |_| |
\____/ \___|_|  |_|   \__, |
                       __/ |
                      |___/
```
# 项目简介
本项目是一款模仿Tomcat组件架构的Web服务器，支持Servlet容器、Filter过滤器、Listener监听器、多项目部署、Session会话管理、重定向等基础功能，详见核心功能部分。

# Jerry架构
Jerry模仿Tomcat的整体架构，包括Server、Service、Connector、Engine、Host、Context。需要说明的是，本项目仅模仿Tomcat的几个核心组件类，并没有严格按照Tomcat的组件进行开发。
- 一个Jerry有一个Server，代表整个服务器
- Tomcat中一个Server至少包含一个Service，意思是可包含多个Service。而Jerry仅支持一个Service，用来对外提供服务
- Tomcat中一个Service包含一个Container（Engine）和多个Connector，Jerry与Tomcat相同，Connector用来连接用户连接，Engine具体处理用户的请求
- Tomcat中Engine可以有多个Host，每个Host可以有多个Context，而Jerry虽然可以定义多个Host，但是并没有实际效果，每次使用到Host的时候，仅会使用默认的Host
- Jerry中，每个Host虽然可以定义多个Context，但是扫描Context的时候，并不会关注具体是哪个Host下的，因此配置多个Host没有意义。实际上每个Host对象存储的Context都是一样的，存储的是xml文件内定义的所有Context，而不仅仅是自己Host标签下配置的Context

# 运行说明
- 在IDEA中完成编译，然后运行starup_idea.bat脚本，自动完成打包和运行。实际为package.bar与run.bat的组合
- package.bat，只打包，不运行。
    - 将out文件夹下编译好的class文件进行打包。运行之后有两个jar包生成，
    - 根目录下的bootstarp.jar，只用来启动服务器，只包含两个类
    - lib目录下的jerry.jar，为服务器的核心
- run.bat，仅运行。
    - 打包完成后，才可以使用run.bat启动服务器
    - 仅启动应用，不需要每次都打包

# webapps说明
在线体验地址:[http://49.232.216.238:18888/](http://49.232.216.238:18888/)
已部署的项目访问路径

- ROOT 项目，根路径/    
    - 默认主页 /index.html
    - html /a.html
    - 图片 /dog.png
    - pdf /java.pdf
    - 服务器模拟耗时操作 /timeConsume.html
- a项目，根路径 /a，用于模拟多项目部署
    - /a
    - /a/index.html
    - /a/a1/index.html
- b项目，根路径 /b，用于模拟多项目部署
    - /b
    - /b/index.html
- javaweb项目，根路径 /javaweb  
    JSP
    
    - /index.jsp java的主页
    - /hello.jsp 从request中取出name参数
    - /jump1.jsp 客户端跳转，跳转到/hello路径
    
    Servlet
    - /hello 类名：HelloServlet，作用：获取配置的Servlet参数，并打印信息
    - /param 类名：ParamServlet，获取用户携带的参数。如果是get请求，就从请求url中?之后提取，如果是post请求，那么从http正文中提取。实例：name=wbin&age=24
    - /header 类名：HeaderServlet。获取HTTP请求头中的user-agent
    - /setCookie 类名：SetCookieServlet。设置cookie，name=wubin
    - /getCookie 类名：GetCookieServlet。获取cookie，获取所有Cookie
    - /setSession 类名：SetSessionServlet。设置Session，name_in_Session=wubin
    - /getSession 类名：GetSessionServlet。获取Session，name_in_Session，value为wubin
    - /jump1 类名：ClientJumpServlet。客户端重定向，会重定向到 /hello
- /jump2 类名：ServerJumpServlet。服务端重定向，会重定向到 /hello.jsp，携带参数name=wubin
   
   Filter
   - URLFilter。监听所有URL请求，打印请求URL
   - PfmFilter。携带配置文件中配置的参数，打印用户请求的处理时间
   
   Listener
    - ContextListener。监听应用初始化和校徽
   
# 核心功能
## 配置文件

- Jerry的配置文件和Tomcat的核心配置文件一样，包含三个配置文件：context.xml,server.xml,web.xml
- 使用Jsoup来解析xml文件
- context.xml仅用来配置用户的web应用的配置文件路径，为WEB-INF/web.xml
- server.xml用来配置Server、Service、Connector、Engine、Host、Context
- web.xml用来配置默认欢迎页面、Session的过期时间、mime-type的映射

## 多项目部署

- 直接将项目解压到webapps目录下
- 直接将war包放在webapps目录下
- 在context.xml配置文件中，手动配置Context节点，可以配置本地磁盘上任何路径上的项目

## war包部署  

- Jerry启动的时候，扫描webapps目录下的war包  
    - 如果某个war包已经解压过了，即已经存在同名的文件夹的情况，那么直接跳过   
    - 如果某个war包没有解压过，即不存在同名的文件夹的情况，那么初次加载  
- 如果Jerry启动之后，新增加了一个war包，会自动解压并加载  

## 热更新

- 文件夹内资源更新
    - 动态资源，当jar,class,xml文件发生更新的时候，会重新加载项目
    - 静态资源，比如html，js等文件更新，并不会重新加载项目，但可以请求到最新的静态资源，这是由处理请求的Servlet所决定的。（静态资源由DefaultServlet处理，动态资源由InvokeServlet和JspServlet处理。
- war包更新。
    - Jerry运行过程中，更新了war包，并不会重新加载war包。
    - 更新war包的方式，在关闭Jerry的情况下，先删掉war包同名文件夹，然后更新war包，即可更新war包项目。

## Servlet

- DefaultServlet。用来处理静态资源，其他两个Servlet处理不了的请求，都交给DefaultServlet来处理，只要本地找不到对应的资源，就返回404
- InvokeServlet。用来处理用户编写的Servlet，使用反射的方式调用用户Servlet的service方法
- JspServlet。用来处理jsp请求，当用户请求jsp页面的时候，就交给JspServlet来处理

## Filter过滤器

- 匹配模式，只支持三种
    - 完全匹配
    - /*模式
    - 后缀名模式
- 支持过滤器链

## Listener监听器

- 当前只支持对web应用Context的生命周期监听，只支持ServletContextListener

## Session管理

- Session由SessionManager进行管理，包括创建、获取、过期检查等
- Session的存储。服务端使用ConcurrentHashMap管理jessionid和Session的映射，客户端使用cookie存储jsessionid
- Session的获取。用户请求到来，创建Request对象的时候，向SessionManager索要Session，SessionManager会根据请求的jsessionid创建新的Session，或者取到之前的Session
- Session过期检查。一个单独的线程，每30秒检查所有过期的Session
- Session的过期时间更新。每次请求到来的时候，都会修改Session的最后访问时间

## 重定向

- 客户端重定向，使用302返回，浏览器会自动进行重定向请求
- 服务端重定向， 使用ApplicationRequestDispatcher类进行服务端重定向

## 类加载器

- CommonClassLoader。用来加载lib文件夹下的所有jar包，除了Bootstrap和CommonClassLoader这两个类由AppClassLoader来加载，其余的都由CommonClassLoader来加载
- JspClassLoader。用来加载Jsp编译生成的类，一个jsp页面对应一个JspClassLoader
- WebappClassLoader。用来加载用户编写的Java类，一个应用对应一个WebappClassLoader

## 监视器

- ContextFileChangeWatcher。用来监听应用内部的文件变化，包括jar、class和xml
- WarFileWatcher。用来监听webapps目录下war文件的新增，虽然war文件的更细也会调用监视器的方法，但是目前并没有什么实际的作用。

## 异常

- WebConfigDuplicatedException。当检查到用户配置的Servlet存在重复的时候抛出。

## 日志

- 使用[Hutool的日志工具类](https://www.hutool.cn/docs/#/log/%E6%A6%82%E8%BF%B0)API实现日志记录
- 底层使用Log4j来打印日志

# 主要逻辑流程
## Jerry启动流程
- 由Bootstrap类使用反射，创建Server对象
    - Server对象内部创建Service对象
    - Service对象内部创建Engine对象和Connectors集合，从server.xml中获取Service的Name
        - Engine对象创建Hosts集合和默认Host
            - Hosts集合和默认Host从从server.xml解析得到
                - Host对象内部扫描Context，并创建contextMap
                    - 从webapps文件夹中扫描文件夹应用并加载
                    - 从serverl.xml中扫描手动配置的应用并加载
                    - 从webapps文件夹中扫描war包并解压加载
                    - 监听webapps文件夹下的新增war包
        - Connector对象从server.xml中获取配置的Connector信息，包括端口和压缩配置
- 调用Server对象对象的start方法
    - 打印JVM的信息
    - 服务器初始化，调用Service对象的start方法
        - Service对象的start方法调用init方法
        - init方法首先初始化所有的Connector，调用Connector的init方法（实际没有做任何事，只是打印了一条日志）
        - init方法然后启动所有Connector，调用Connector的start方法
            - Connector的start方法，开启一个独立地线程，使用BIO的方式监听配置的端口，无限循环处理用户的请求

## 处理用户请求的流程
- Jerry启动之后，每个Connector开启一个线程，使用ServerSocket监听配置的端口
- 用户请求到来，获取Socket对象
- Connector将Socket对象交给Engine对象进行处理
    -（这里按理应该是Connector将Socket封装为Request和Response，交给Engine处理的，
    但是由于采用了BIO的方式，解析Request的耗时较长，因此，为了迅速让Connector继续监听端口，就把封装Request的任务交给了Engine，Engine内部则是利用了线程池来封装，以此来提高效率）
    - Engine对象创建Runnable对象，并交给线程池处理
        - Runable对象内部，首先将Socket封装为Request、Response，Request内部，解析Socket，完成Request对象的初始化
            - 解析Http请求，将HTTP请求字符串保存到requestString中
            - 解析请求方法 GET/POST
            - 解析请求uri
            - 解析应用上下文Context，如果是/，那么应用是ROOT
            - 移除uri中的上下文路径
            - 解析请求参数
            - 解析Header
            - 解析Cookie
        - 将Request和Response交给HttpProcessor处理
            - 解析Session
            - 从request获取应用上下文context
            - 根据uri，从应用上下文context中获取Servlet类名，然后根据交给不同的工作Servlet
                - 如果从uri拿到了Servlet类名，就交给InvokeServlet处理
                - 如果后缀为jsp，就交给JspServlet处理
                - 如果前两个都不满足，就交给DefaultServlet处理
            - 根据请求uri，获取匹配的Session
            - 将Servlet和Session封装为过滤器链
            - 调用过滤器链
            - 根据过滤器链的执行结果（response的状态码），进行不同的逻辑处理
                - 服务端重定向。直接返回
                - 200。将response的正文输出到socket，根据connecter的配置参数决定是否进行gzip压缩
                - 302。向客户端返回重定向的http信息
                - 404。向客户端返回404页面
                - 500。向客户端返回500页面
            - 关闭socket

## 过滤器链的执行流程
- 过滤器链内保存所有匹配到的Session、工作Servlet、pos（Session的索引，表示执行到哪个Session了）
- 在HttpProcessor中，开始调用过滤器链的doFilter方法
    - doFilter方法内，根据pos获取到对应索引位置的用户Session
    - 执行用户Session的doFilter方法
    - 用户Session内部会继续调用SessionChain.doFilter方法，然后SessionChain就会继续调用下一个用户Session
    - 如果用户Session已经执行完最后一个了，那么调用工作Servlet的service方法。工作Servlet有三种
        - InvokeServlet。根据用户Servlet的类名，获取Servlet实例，然后调用其service方法。用户编写的service方法，会继承自HttpServlet类，会自动根据请求的method，来调用doGet、doPost
        - JspServlet。根据uri获取jsp文件的JspClassLoader，使用JspClassLoader加载jsp编译后的Servlet的class，获取Servlet对象，调用service方法
        - DefaultServlet。根据用户请求的uri，到相应的上下文目录内查找对应的文件，如果没有找到，则返回404
    - service方法执行完后，Session过滤器链会自动地进行回溯，继续从上一个Session的SessionChain.doFilter方法之后继续执行，该Session执行完后，继续回溯，直到所有Session回溯完毕

 ## Jsp执行流程
- JspServlet来执行jsp的请求
- 根据请求uri，找到jsp文件，如果jsp文件不存在，则返回404
- 根据请求uri，查找是否存在编译好的Servlet字节码文件
    - jsp编译后的Servlet文件存放在work目录下，ROOT目录存放在worl目录里的_目录,其他项目则与项目目录同名，namespace为org/apache/jsp
    - 如果class文件不存在，那么就编译一下jsp文件
    - 如果jsp的文件最后修改时间大于class文件的最后修改时间，就重新编译Servlet类，并且移除旧的JspClassLoader对象
- 根据请求uri和上下文，获取jsp文件的JspClassLoader，一个jsp文件对应一个JspClassLoader。
    - 从JspClassLoader中获取，有一个map存储，key为路径名，value为JspClassLoader对象
- 使用JspClassLoader加载编译后的Servlet
- 获取serlvet对象
- 调用Servlet对象的service方法

## 请求转发
- 服务端转发
    - 执行完所有代码，包括RequestDispatcher之后的所有代码，再跳转到目标页面。
    - 用户的Servlet逻辑中，调用request.getRequestDispatcher("hello.jsp").forward(request, response);
    - request.getRequestDispatcher方法会返回ApplicationRequestDispatcher对象
    - 调用ApplicationRequestDispatcher对象对象的forward(request, response)方法
        - 修改request的请求uri为请求转发后的uri
        - 重新创建一个HttpProcessor，调用HttpProcessor的execute方法，完成用户逻辑
        - 设置request的forwarded为true
    - 该request原本的Servlet继续执行，直到完成过滤器链
    - 因为request的forwarded为true，所以HttpProcessor后续什么都不用处理了，直接返回就好了，因为工作已经被另一个HttpProcessor做完了
- 客户端转发
    - 向客户端返回的http报文中，带上302状态码和转发url
    
## Session管理
- Session的管理
    - 服务端Session由SessionManager管理，使用map存储Session对象，key为jsessionid，val为Session对象
    - Session对象包含最大持续时间和最后访问时间，最大持续时间如果用户没有手动设置，那么默认为30分钟
- Session的获取
    - 从cookie中拿到jsessionid，可能为null
    - 将jsessionid交给SessionManager，获取Session对象
        - 如果jsessionid为null，那么创建新的Session
        - 如果根据jsessionid拿到的Session为null，那么创建新的Session
        - 如果获取到Session对象，就更新Session对象的最后访问时间
    - 创建cookie，将jsessionid放到cookie中，并返回给客户端
    - 将Session对象放到request对象内
- Session的创建
    - 生成jsessionid
    - 创建Session对象，并将jsessionid和应用上下万填充到Session对象
    - 将jsessionid和Session对象保存到map中
- Session的过期
    - SessionManager加载的时候，启动一个线程，每30秒扫描map中的所有Session，如果当前时间距Session的最后获取时间超过了最大持续时间，那么从map中删除掉这个Session
    
