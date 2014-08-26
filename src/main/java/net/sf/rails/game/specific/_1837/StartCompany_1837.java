package net.sf.rails.game.specific._1837;

import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.Stop;
import rails.game.action.StartCompany;

public class StartCompany_1837 extends StartCompany {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    protected transient Stop selectedHomeStation = null;
    protected String selectedHomeStationName = null;


    public StartCompany_1837(PublicCompany company, int[] prices,
            int maximumNumber) {
        super(company, prices, maximumNumber);
        // TODO Auto-generated constructor stub
    }

    public StartCompany_1837(PublicCompany company, int[] startPrice) {
        super(company, startPrice);
        // TODO Auto-generated constructor stub
    }

    public StartCompany_1837(PublicCompany company, int price, int maximumNumber) {
        super(company, price, maximumNumber);
        // TODO Auto-generated constructor stub
    }

    public StartCompany_1837(PublicCompany company, int price) {
        super(company, price);
        // TODO Auto-generated constructor stub
    }
    public void setHomeStation(Stop homeStation) {
        selectedHomeStation = homeStation;
        selectedHomeStationName = homeStation.getSpecificId();
    }

}
