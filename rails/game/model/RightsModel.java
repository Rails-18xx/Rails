/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/model/RightsModel.java,v 1.6 2008/06/04 19:00:37 evos Exp $*/
package rails.game.model;

import rails.game.state.HashMapState;
import tools.Util;

public class RightsModel extends ModelObject {

    private HashMapState<String, String> rights;

    public RightsModel() {
    }
    
    /** Split off from the constructor to allow the rights map to exist only if needed */
    public void init (HashMapState<String, String> rights) {
        this.rights = rights;
    }

    public String getText() {

        if (rights == null) return "";
        
        StringBuilder buf = new StringBuilder("<html>");
        for (String name : rights.viewKeySet()) {
            if (buf.length() > 6) buf.append("<br>");
            buf.append(name);
            String value = rights.get(name);
            if (Util.hasValue(value)) buf.append("=").append(value);
        }
        if (buf.length() > 6) {
            buf.append("</html>");
        }
        return buf.toString();

    }

}
