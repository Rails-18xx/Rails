package net.sf.rails.game.ai.playground.stub;

import java.util.HashMap;
import java.util.Map;

/** Stub data class for a parsed BuyTrain action*/
public class ParsedBuyTrainAction {
    private String train;
    private int cost;
    private String from;

    public ParsedBuyTrainAction(String train, int cost, String from) {
        this.train = train;
        this.cost = cost;
        this.from = from;
    }

    public static ParsedBuyTrainAction parse(String s) {
        Map<String, String> parts = parseKeyValueString(s);
        return new ParsedBuyTrainAction(
            parts.get("TRAIN"),
            Integer.parseInt(parts.get("COST")),
            parts.get("FROM")
        );
    }

    @Override
    public String toString() {
        return String.format("[BuyTrain: Train=%s, Cost=%d, From=%s]", train, cost, from);
    }

    // Getters
    public String getTrain() { return train; }
    public int getCost() { return cost; }
    public String getFrom() { return from; }

    // Helper
    private static Map<String, String> parseKeyValueString(String s) {
        Map<String, String> map = new HashMap<>();
        // Special split for "TYPE=BuyTrain, TRAIN=2_0, ..."
        for (String part : s.substring(s.indexOf(',') + 1).split(",")) {
            String[] kv = part.trim().split("=");
            if (kv.length == 2) {
                map.put(kv[0].trim(), kv[1].trim());
            }
        }
        return map;
    }
}