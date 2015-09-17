package net.sf.rails.game.round;

import rails.game.action.PossibleActions;

import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.MutableClassToInstanceMap;

import net.sf.rails.game.RailsAbstractItem;

/**
 * Activities allows to bundle several Activity objects
 */
public class Activities extends RailsAbstractItem {

    private final ClassToInstanceMap<Activity> activities = MutableClassToInstanceMap.create();
    
    protected Activities(RoundNG parent, String id) {
        super(parent, id);
    }

    /**
     * @param activityClass by which the activity was stored
     * @return the related activity
     */
    public <T extends Activity> T getActivity(Class<T> activityClass) {
        return activities.getInstance(activityClass);
    }
    
    /**
     * @param activityClass store the activity with the class as key
     * @param activity to be stored
     */
    public <T extends Activity> void addActivity(Class<T> activityClass, T activity) {
        activities.putInstance(activityClass, activity);
    }
    
    /**
     * @param activity to be stored using its class as key
     */
    public void addActivity(Activity activity) {
        activities.put(activity.getClass(), activity);
    }

    /**
     * create actions and add them to the possibleActions object
     */
    public void createActions(Actor actor, PossibleActions actions) {
        for (Activity activity:activities.values()) {
            if (activity.isEnabled()) {
                activity.createActions(actor, actions);
            }
        }
    }
    
    
}
