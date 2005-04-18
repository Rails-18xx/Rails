
/*
 * Created on 05mar2005
 *
 */
package game;

/**
 * @author Erik Vos
 */
public abstract class Company implements CompanyI, ConfigurableComponentI {
	
	protected static int numberOfCompanies = 0;
	protected String name;
	protected CompanyType type;
	protected int companyNumber; // For internal use
	
	protected Portfolio portfolio = null;
	
	/**
	 * The value per certificate at the end of the game.
	 * Default 0 (for privates).
	 */
	protected int value = 0;
	/**
	 * Twice the amount each certificate counts against the limit
	 * (this way we take care for certs that count for 0.5)
	 */
	protected int certLimitCount = 2;
	
	protected boolean closed = false;

	public Company() {
		this.companyNumber = numberOfCompanies++;
	}
	
	public void init (String name, CompanyType type) {
		this.name = name;
		this.type = type;
	}
		
	/**
	 * @return
	 */
	public static int getNumberOfCompanies() {
		return numberOfCompanies;
	}

	/**
	 * @return
	 */
	public int getNumber() {
		return companyNumber;
	}

	/**
	 * @return
	 */
	public boolean isClosed() {
		return closed;
	}

	/**
	 * @param b
	 */
	public void setClosed(boolean b) {
		closed = b;
	}

	/**
	 * @return
	 */
	public CompanyType getType() {
		return type;
	}

	/**
	 * @return
	 */
	public String getTypeName() {
		return type.getName();
	}

	/**
	 * @return
	 */
	public String getName() {
		return name;
	}


	/**
	 * @return
	 */
	public int getCertLimitCount() {
		return certLimitCount;
	}

	/**
	 * @return
	 */
	public int getCompanyNumber() {
		return companyNumber;
	}

	/**
	 * @return
	 */
	public int getValue() {
		return value;
	}

	/**
	 * @param i
	 */
	public void setCertLimitCount(int i) {
		certLimitCount = i;
	}

	/**
	 * @param i
	 */
	public void setValue(int i) {
		value = i;
	}

	/**
	 * @return
	 */
	public Portfolio getPortfolio() {
		return portfolio;
	}


}
