package net.sf.rails.game.ai;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Data class for a valid train buy option.
 * Added for the AI Playground.
 */
public class BuyTrainOption {

    private String trainId;
    private int cost;
    private String from; // e.g., "IPO"

    private BuyTrainOption(String trainId, int cost, String from) {
        this.trainId = trainId;
        this.cost = cost;
        this.from = from;
    }

    public static BuyTrainOption parse(String s) {
        // Example: POSSIBLE_ACTION[0]: TYPE=BuyTrain, TRAIN=2_0, COST=80, FROM=IPO
        Map<String, String> parts = parseKeyValueString(s);
        return new BuyTrainOption(
            parts.get("TRAIN"),
            Integer.parseInt(parts.get("COST")),
            parts.get("FROM")
        );
    }

    @Override
    public String toString() {
        return String.format("[BuyTrain: Train=%s, Cost=%d, From=%s]", trainId, cost, from);
    }

    // --- Getters ---
    public String getTrainId() { return trainId; }
    public int getCost() { return cost; }
    public String getFrom() { return from; }

    // Helper
    private static Map<String, String> parseKeyValueString(String s) {
        Map<String, String> map = new HashMap<>();
        Pattern p = Pattern.compile("(\\w+)=([\\w_]+)");
        Matcher m = p.matcher(s);
        while (m.find()) {
            map.put(m.group(1), m.group(2));
        }
        return map;
    }
}