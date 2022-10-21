package net.sf.rails.game.model;

import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.special.SpecialRight;
import net.sf.rails.game.state.HashSetState;
import net.sf.rails.game.state.IntegerState;

public class ActiveSharesCountModel extends RailsModel {

    /**
     * The number of shares in play for a company.
     * The value always is 100 / shareSize.
     * In practice, values range from 1 to 20.
     * In some games, companies exist that can grow their share number
     * at certain events. E.g. 1826: from 5 to 10 shares.
     * In such cases, the number should be displayed in the GameStatus window.
     */
    private IntegerState activeSharesCount = IntegerState.create(
            this, "activeSharesCount_"+getParent().getId());

    private ActiveSharesCountModel(PublicCompany parent, String id) {
        super(parent, id);
    }

    public static ActiveSharesCountModel create(PublicCompany parent, String id) {
        return new ActiveSharesCountModel(parent, id);
    }

    public void set(int count) {
        activeSharesCount.set(count);
    }

    public int getCount() {
        return activeSharesCount.value();
    }

    @Override
    public String toText() {

        if (((PublicCompany)getParent()).isClosed()) {
            return "";
        } else {
            return String.valueOf(activeSharesCount.value());
        }
    }

}
