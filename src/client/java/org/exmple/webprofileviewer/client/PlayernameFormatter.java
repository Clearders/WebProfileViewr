package org.exmple.webprofileviewer.client;

import net.minecraft.ChatFormatting;

public class PlayernameFormatter {
    public String cleanPlayerName(String rawName) {
        if (rawName == null || rawName.isEmpty()) {
            return "";
        }
        // StripFormatting的作用:一键移除所有§开头的格式符（如§r等）
        return ChatFormatting.stripFormatting(rawName);
    }// 方法作用：移除玩家名字中的格式符，确保后续处理时使用干净的名字
}
