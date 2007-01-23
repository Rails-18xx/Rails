package rails.game;

import java.util.List;

public interface PrivateCompanyI extends CompanyI, Certificate
{

	public static final String TYPE_TAG = "Private";
	// public static final String BASE_PRICE = "basePrice";
	public static final String REVENUE = "revenue";

	/**
	 * @return
	 */
	public int getPrivateNumber();

	/**
	 * @return
	 */
	public int getBasePrice();

	/**
	 * @return
	 */
	public int getRevenue();

	public void setHolder(Portfolio portfolio);

	public void payOut();

	public List getSpecialProperties();
}
