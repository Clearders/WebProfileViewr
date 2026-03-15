package org.exmple.webprofileviewer.client;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class BWStatsExtractor {
    public BWStats extractBWStats(String playername) throws Exception {
        String url = "https://hypixel.net/player/" + playername;
        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0")
                .get();

        String finalKD4v4 = "未找到";
        String finalKD2v2 = "未找到";
        String totalWins = "未找到";
        String finalKD = "未找到";

        for (Element tdName : doc.select("#stats-content-bedwars td.statName")) {
            String nameText = tdName.text().trim();
            Element tr = tdName.parent();
            if (tr == null) continue;
            Element tdValue = tr.selectFirst("td.statValue");
            if (tdValue == null) continue;

            String value = tdValue.text();

            if ("4v4v4v4 Final K/D".equalsIgnoreCase(nameText) && "未找到".equals(finalKD4v4)) {
                finalKD4v4 = value;
            } else if ("Doubles Final K/D".equalsIgnoreCase(nameText) && "未找到".equals(finalKD2v2)) {
                finalKD2v2 = value;
            } else if ("Wins".equalsIgnoreCase(nameText) && "未找到".equals(totalWins)) {
                totalWins = value;
            } else if ("Final K/D".equalsIgnoreCase(nameText) && "未找到".equals(finalKD)) {
                finalKD = value;
            }

            if (!"未找到".equals(finalKD4v4) && !"未找到".equals(finalKD2v2) && !"未找到".equals(totalWins) && !"未找到".equals(finalKD)) {
                break;
            }
        }

        return new BWStats(finalKD, finalKD2v2, finalKD4v4, totalWins);
    }

    public static class BWStats {
        private String finalKD;
        private String finalKD2v2;
        private String finalKD4v4;
        private String totalWins;

        public BWStats(String finalKD, String finalKD2v2, String finalKD4v4, String totalWins) {
            this.finalKD = finalKD;
            this.finalKD2v2 = finalKD2v2;
            this.finalKD4v4 = finalKD4v4;
            this.totalWins = totalWins;
        }

        public String getFinalKD() {
            return finalKD;
        }

        public String getFinalKD2v2() {
            return finalKD2v2;
        }

        public String getFinalKD4v4() {
            return finalKD4v4;
        }

        public String getTotalWins() {
            return totalWins;
        }
    }
}
