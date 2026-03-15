package org.exmple.webprofileviewer.client;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public class StatsFormatter {
    //将统计数据格式化存储在Component数组中，方便后续对不同部分进行对应的格式化处理
    public static Component[] formatStats(BWStatsExtractor.BWStats stats) {
        Component[] components = new Component[4];
        components[0] = formatLine("Final K/D", stats.getFinalKD());
        components[1] = formatLine("Doubles Final K/D", stats.getFinalKD2v2());
        components[2] = formatLine("4v4v4v4 Final K/D", stats.getFinalKD4v4());
        components[3] = formatLine("Total Wins", stats.getTotalWins());
        return components;
    }

    private static Component formatLine(String label, String value) {
        return Component.literal(label + ": ")
                .withStyle(ChatFormatting.AQUA)
                .append(Component.literal(value).withStyle(ChatFormatting.WHITE));
    }

    public static double parseStatAsDouble(String statValue) {
        //将返回的字符串类型的统计数据解析为double格式
        try {
            return Double.parseDouble(statValue);
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }
}
