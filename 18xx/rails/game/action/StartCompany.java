package rails.game.action;

import rails.game.Bank;
import rails.game.PublicCertificateI;

public class StartCompany extends BuyCertificate {

    // Server parameters
	protected int[] startPrices;

    public static final long serialVersionUID = 1L;

    public StartCompany(PublicCertificateI certificate,
			int[] prices, int maximumNumber) {
		super (certificate, Bank.getIpo(), 0, maximumNumber);
		this.startPrices = prices.clone();
	}

	public StartCompany(PublicCertificateI certificate, int[] startPrice) {
		this(certificate, startPrice, 1);
	}

    public int[] getStartPrices() {
    	return startPrices;
    }

    public boolean mustSelectAPrice() {
    	return startPrices.length > 1;
    }

    public void setStartPrice (int startPrice) {
    	price = startPrice;
    }

	@Override
    public String toString() {
		StringBuffer text = new StringBuffer();
        text.append("StartCompany: ")
        	.append(certificate.getName());
        if (price > 0) {
            text.append(" price=").append(Bank.format(price));
            if (numberBought > 1) {
                text.append(" number=").append(numberBought);
            }
        } else {
            text.append(" prices=");
            for (int i=0; i<startPrices.length; i++) {
            	text.append(Bank.format(startPrices[i]));
            }
        }
        return text.toString();
    }

}
