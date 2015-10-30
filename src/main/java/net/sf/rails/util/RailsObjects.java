package net.sf.rails.util;

import com.google.common.collect.Sets;

import rails.game.action.PossibleAction;
import net.sf.rails.game.RailsItem;

/**
 * Utility class to work with Rails objects 
 */
public class RailsObjects {

    public static class StringHelper {
        
        private final StringBuilder text = new StringBuilder();
        
        private StringHelper() {
            // do nothing => empty
        }
        
        private boolean testIfNull(Object test, String replace) {
            if (test == null) {
                text.append(replace);
                return true;
            }
            return false;
        }
        
        private StringHelper(RailsItem item) {
            text.append(item.getId() + "(" + item.getClass().getName() + ")");
        }
        
        public StringHelper addToString(String name, Object value) {
            if (testIfNull(value, "NULL")) return this; 

            text.append(name + " = " +  value.toString());
            return this;
        }

        public StringHelper addToText(String name, RailsItem value) {
            if (testIfNull(value, "NULL")) return this; 

            text.append(name + " = " + value.toText());
            return this;
        }

        public StringHelper addId(String name, RailsItem value) {
            if (testIfNull(value, "NULL")) return this; 

            text.append(name + " = " + value.getId());
            return this;
        }

        public StringHelper addURI(String name, RailsItem value) {
            if (testIfNull(value, "NULL")) return this; 

            text.append(name + " = " + value.getURI());
            return this;
        }

        public StringHelper addFullURI(String name, RailsItem value) {
            if (testIfNull(value, "NULL")) return this; 

            text.append(name + " = " + value.getFullURI());
            return this;
        }
        
        public StringHelper append(String text) {
            if (testIfNull(text, "")) return this; 
            
            this.text.append(text);
            return this;
        }
        
        public String toString() {
            return text.toString();
        }
    }
    
    public static class StringHelperForActions {
        
        private final StringBuilder text = new StringBuilder();
        private final PossibleAction action;
        
        private StringHelperForActions(PossibleAction action) {
            this.action = action;
        }
        
        public StringHelperForActions addBaseText() {
            text.append(action.getPlayer().getId());
            if (action.hasActed()) {
                text.append(" executed ");
            } else {
                text.append(" may ");
            }
            text.append(action.getClass().getSimpleName());
            return this;
        }
        
        private Object testIfNull(Object value, String replace) {
            if (value == null) {
                return replace;
            } else {
                return value;
            }
        }

        public StringHelperForActions addToString(String name, Object value) {
            value = testIfNull(value, "NULL");
            
            text.append(", " + name + " = " +  value.toString());
            return this;
        }
        
        public StringHelperForActions addToStringOnlyActed(String name, Object value) {
            if (action.hasActed()) {
                this.addToString(name, value);
            }
            return this;
        }

        public String toString() {
            return text.toString();
        }
    }

    
    public static StringHelper stringHelper(RailsItem item) {
        return new StringHelper(item); 
    }

    public static StringHelperForActions stringHelper(PossibleAction action) {
        return new StringHelperForActions(action);
    }
    
    public static boolean elementEquals(Iterable<?> a, Iterable<?> b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        
        return Sets.newHashSet(a).equals(Sets.newHashSet(b));
        
    }
    
}
