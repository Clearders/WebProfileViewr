package org.exmple.webprofileviewer.client;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class BWStatsExtractor {
    public String extractBWStats(String playername) throws Exception {
        String url = "https://hypixel.net/player/" + playername;
        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0")
                .get();
        //抓取各项数据部分：

        //4s final
        String finalKD4v4 = doc.select("#stats-content-bedwars td.statName").stream()
                .filter(td -> "4v4v4v4 Final K/D".equalsIgnoreCase(td.text().trim()))
                .findFirst()
                .map(Element::parent)                  // tr
                .map(tr -> tr.selectFirst("td.statValue"))
                .map(Element::text)
                .orElse("未找到");
        //2s final
        String finalKD2v2 = doc.select("#stats-content-bedwars td.statName").stream()
                .filter(td -> "Doubles Final K/D".equalsIgnoreCase(td.text().trim()))
                .findFirst()
                .map(Element::parent)                  // tr
                .map(tr -> tr.selectFirst("td.statValue"))
                .map(Element::text)
                .orElse("未找到");
        //total wins
        String totalWins = doc.select("#stats-content-bedwars td.statName").stream()
                .filter(td -> "Wins".equalsIgnoreCase(td.text().trim()))
                .findFirst()
                .map(Element::parent)                  // tr
                .map(tr -> tr.selectFirst("td.statValue"))
                .map(Element::text)
                .orElse("未找到");
        //final K/D
        String finalKD = doc.select("#stats-content-bedwars td.statName").stream()
                .filter(td -> "Final K/D".equalsIgnoreCase(td.text().trim()))
                .findFirst()
                .map(Element::parent)                  // tr
                .map(tr -> tr.selectFirst("td.statValue"))
                .map(Element::text)
                .orElse("未找到");
        //拼接数据后返回
        String stats="Final K/D:" + finalKD + "\nDoubles Final K/D:" + finalKD2v2 + "\n4v4v4v4 Final K/D:" + finalKD4v4 + "\nTotal Wins:" + totalWins;
        return stats;

    }
}
