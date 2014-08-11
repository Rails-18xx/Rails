package net.sf.rails.game.specific._1862;

import net.sf.rails.game.*;
import net.sf.rails.game.state.IntegerState;

public final class PublicCompany_1862 extends PublicCompany {
    
    protected final IntegerState status = IntegerState.create(this, "status");

    public static final int UNAVAILABLE = 0;
    public static final int BIDDABLE = 1;

    public static final String[] statusName =
            new String[] { "Unavailable", "Biddable" };


    public PublicCompany_1862(RailsItem parent, String id) {
        super(parent, id);
        status.set(BIDDABLE);
    }

    public boolean isStartable() {
        return true;
    }

    public IntegerState getStatusModel() {
        return status;
    }
    


}
