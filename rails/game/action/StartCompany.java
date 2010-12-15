package rails.game.action;

import rails.game.*;

public class StartCompany extends BuyCertificate {

    // Server parameters
    protected int[] startPrices;

    public static final long serialVersionUID = 1L;

    public StartCompany(PublicCompanyI company, int[] prices,
            int maximumNumber) {
        super(company, company.getPresidentsShare().getShare(),
                GameManager.getInstance().getBank().getIpo(),
                0, maximumNumber);
        this.startPrices = prices.clone();
    }

    public StartCompany(PublicCompanyI company, int[] startPrice) {
        this(company, startPrice, 1);
    }

    public StartCompany(PublicCompanyI company, int price,
            int maximumNumber) {
        super(company, company.getPresidentsShare().getShare(),
                GameManager.getInstance().getBank().getIpo(),
                0, maximumNumber);
        this.price = price;
    }

    public StartCompany(PublicCompanyI company, int price) {
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
    public String toString() {
        StringBuffer text = new StringBuffer();
        text.append("StartCompany: ").append(company.getName());
        if (price > 0) {
            text.append(" price=").append(Bank.format(price));
            if (numberBought > 1) {
                text.append(" number=").append(numberBought);
            }
        } else {
            text.append(" prices=");
            for (int i = 0; i < startPrices.length; i++) {
                text.append(Bank.format(startPrices[i]));
            }
        }
        return text.toString();
    }

}
