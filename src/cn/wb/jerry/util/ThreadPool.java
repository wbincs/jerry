package cn.wb.jerry.util;

import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 线程池工具类，用来实际执行线程任务
 */
public class ThreadPool {

    // 7个参数
    // corePoolSize: 核心线程数量
    // maximumPoolSize: 最大线程数量，200表示最多200个线程
    // keepAliveTime: 60L表示非核心线程的最大闲置时间，超过这个时间后会回收
    // TimeUnit: keepAliveTime的时间单位
    // BlockingQueue: 阻塞队列，queue表示20个线程池满了之后，新来任务并不会立即创建新创建，而是加入queue中。当queue满了之后，再创建新线程
    // ThreadFactory: 线程工厂，用来创建线程
    // RejectedExecutionHandler: 拒绝策略，默认的拒绝策略是AbortPolicy，丢弃请求并且抛异常
    private static ThreadPoolExecutor threadPool =
            new ThreadPoolExecutor(20,
                    200,
                    10L,
                    TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(100),
                    Executors.defaultThreadFactory(),
                    new ThreadPoolExecutor.AbortPolicy());

    public static void run(Runnable r) {
        threadPool.execute(r);
    }

}
