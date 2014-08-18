package net.sf.rails.game.specific._1837;

import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.RailsItem;
import net.sf.rails.game.Train;

public class PublicCompany_1837 extends PublicCompany {

    public PublicCompany_1837(RailsItem parent, String id) {
        super(parent, id);
        // TODO Auto-generated constructor stub
    }

    /* (non-Javadoc)
     * @see net.sf.rails.game.PublicCompany#mayBuyTrainType(net.sf.rails.game.Train)
     */
    @Override
    public boolean mayBuyTrainType(Train train) { // Coal trains in 1837 areonly allowed to buy/operate G-Trains
        if (this.getType().getId().equals("Coal")){
            if (train.getType().getInfo().contains("G")){
                return true;
            }
            else {
                return false;
            }
        }
        return super.mayBuyTrainType(train);
    }

}
