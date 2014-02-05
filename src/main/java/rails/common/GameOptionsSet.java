package rails.common;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.SortedSet;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * A class to store several GameOptions, including values
 */
public class GameOptionsSet {

    private final LinkedHashMap<String, String> optionsToValues= Maps.newLinkedHashMap();
    
    private GameOptionsSet(int nbPlayers) {
        optionsToValues.put(GameOption.NUMBER_OF_PLAYERS, Integer.toString(nbPlayers));
    }
    
    private static GameOptionsSet create(int nbPlayers, List<GameOption> options) {
        GameOptionsSet set = new GameOptionsSet(nbPlayers);
        for (GameOption option:options) {
            set.optionsToValues.put(option.getName(), option.getSelectedValue());
        }
        return set;
    }
    
    public ImmutableMap<String, String> getOptions() {
        return ImmutableMap.copyOf(optionsToValues);
    }
    
    public String get(String option) {
        return optionsToValues.get(option);
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private final SortedSet<GameOption> options = Sets.newTreeSet();
        
        private Builder() { }
        
        public void add(GameOption option) {
            options.add(option);
        }
        
        public ImmutableList<GameOption> getOptions() {
            return ImmutableList.copyOf(options);
        }
        
        public GameOptionsSet build(int nbPlayers) {
            return GameOptionsSet.create(nbPlayers, this.getOptions());
        }
    }
    
}
