package org.exmple.webprofileviewer.client;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

//集中管理线程池，这样不论注册多少个命令需要执行异步任务，都可以使用同一个线程池，避免资源浪费和线程泄漏
public class AsyncExecutor {
    private static ExecutorService createExecutor() {
        return Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable);
            thread.setDaemon(true);
            thread.setName("WebProfileViewer-IO");
            return thread;
        });
    }
    private static final ExecutorService IO_EXECUTOR = createExecutor();
   //创建ExecutorService实例
    public static ExecutorService getExecutor() {
        return IO_EXECUTOR;
    }
   //提供全局访问点，供其他类获取线程池实例
    public static void shutdown() {//用于关闭线程池，释放资源，防止内存泄漏
        if (!IO_EXECUTOR.isShutdown()) {
            IO_EXECUTOR.shutdown();
        }
    }
}
