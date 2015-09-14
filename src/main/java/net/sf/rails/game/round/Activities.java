package net.sf.rails.game.round;

import java.util.Map;

import com.google.common.collect.Maps;

import net.sf.rails.game.RailsAbstractItem;

/**
 * Activities allows to bundle several Activity objects
 */
public abstract class Activities extends RailsAbstractItem {

    private Map<Class<? extends Activity>, Activity> activities = Maps.newHashMap();
    
    protected Activities(RoundNG parent, String id) {
        super(parent, id);
    }

    @SuppressWarnings("unchecked")
    public <T extends Activity> T getActivity(Class<T> activityClass) {
        return (T) activities.get(activityClass);
    }
    
    public void addActivity(Activity activity) {
        activities.put(activity.getClass(), activity);
    }
    
}
