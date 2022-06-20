package net.sf.rails.game.model;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.special.SpecialRight;
import net.sf.rails.game.state.BooleanState;
import net.sf.rails.game.state.HashSetState;

/**
 * RightsModel stores purchased SpecialRight(s)
 *
 */
public class RightsModel extends RailsModel {

    private HashSetState<SpecialRight> rights = HashSetState.create(this, "rightsModel");

    private RightsModel(PublicCompany parent, String id) {
        super(parent, id);
    }

    public static RightsModel create(PublicCompany parent, String id) {
        return new RightsModel(parent, id);
    }

    public void add(SpecialRight right) {
        rights.add(right);
    }

    public boolean contains(SpecialRight right) {
        return rights.contains(right);
    }

    /** Get the first (normally the only) right of a certain type */
    public <T extends SpecialRight> T getRightType (Class<T> rightClass) {
        for (SpecialRight right : rights) {
            if (right.getClass() == rightClass) return (T)right;
        }
        return null;
    }

    public boolean isEmpty() { return rights.isEmpty(); }

    @Override
    public String toText() {
        StringBuilder b = new StringBuilder("");
        for (SpecialRight right:rights) {
            if (b.length() > 0) b.append(" ");
            b.append(right.toText());
        }
        return b.toString();
    }

}
