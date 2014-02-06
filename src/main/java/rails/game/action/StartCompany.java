package rails.game.action;

import net.sf.rails.game.*;

import com.google.common.primitives.Ints;


public class StartCompany extends BuyCertificate {

    // Server parameters
    protected int[] startPrices;

    public static final long serialVersionUID = 1L;

    public StartCompany(PublicCompany company, int[] prices,
            int maximumNumber) {
        super(company, company.getPresidentsShare().getShare(),
                RailsRoot.getInstance().getBank().getIpo(),
                0, maximumNumber);
        this.startPrices = prices.clone();
    }

    public StartCompany(PublicCompany company, int[] startPrice) {
        this(company, startPrice, 1);
    }

    public StartCompany(PublicCompany company, int price,
            int maximumNumber) {
        super(company, company.getPresidentsShare().getShare(),
                RailsRoot.getInstance().getBank().getIpo(),
                0, maximumNumber);
        this.price = price;
    }

    public StartCompany(PublicCompany company, int price) {
        this(company, price, 1);
    }

    public int[] getStartPrices() {
        return startPrices;
    }

    public boolean mustSelectAPrice() {
        return startPrices != null/* && startPrices.length > 1*/;
    }

    public void setStartPrice(int startPrice) {
        price = startPrice;
    }

    @Override
    public boolean equalsAsOption(PossibleAction action) {
        if (!(action.getClass() == StartCompany.class)) return false;
        StartCompany a = (StartCompany) action;
        return a.company == company && a.from == from 
                && (startPrices == null && a.startPrices == null || Ints.asList(startPrices).contains(a.price));
    }
    
    @Override
    public String toString() {
        log.debug("company= " + company);
        StringBuilder text = new StringBuilder();
        text.append("StartCompany: ").append(company != null ? company.getId() : null);
        if (price > 0) {
            text.append(" price=").append(Currency.format(company, price));
            if (numberBought > 1) {
                text.append(" number=").append(numberBought);
            }
        } else {
            text.append(" prices=");
            for (int i = 0; i < startPrices.length; i++) {
                text.append(Currency.format(company, startPrices[i]));
            }
        }
        return text.toString();
    }

}
