package org.exmple.webprofileviewer.client;


//作用：单例模式，防止不同命令注册使用多个BWStatsExtractor和PlayernameFormatter实例
public class ServiceContainer {
    //注册两个BWStatsExtractor和PlayernameFormatter的单例实例，供全局使用
    private static final BWStatsExtractor STATS_EXTRACTOR = new BWStatsExtractor();
    private static final PlayernameFormatter NAME_FORMATTER = new PlayernameFormatter();
//只返回已经存在的单例
    public static BWStatsExtractor getStatsExtractor() {
        return STATS_EXTRACTOR;
    }
    public static PlayernameFormatter getNameFormatter() {
        return NAME_FORMATTER;
    }
}
