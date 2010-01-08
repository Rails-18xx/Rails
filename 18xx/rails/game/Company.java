/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/Company.java,v 1.11 2010/01/08 21:30:37 evos Exp $ */
package rails.game;

import org.apache.log4j.Logger;

import rails.game.move.MoveableHolder;
import rails.game.state.BooleanState;

public abstract class Company implements CompanyI, ConfigurableComponentI,
        Cloneable {

    protected String name;
    protected String longName;
    protected CompanyTypeI type;
    protected int companyNumber; // For internal use
    protected Portfolio portfolio = null;

    /**
     * The value per certificate at the end of the rails.game. Default 0 (for
     * privates).
     */
    protected int value = 0;
    /**
     * Twice the amount each certificate counts against the limit (this way we
     * take care for certs that count for 0.5)
     */
    protected int certLimitCount = 2;

    protected BooleanState closedObject;

    protected static Logger log =
            Logger.getLogger(Company.class.getPackage().getName());

    public Company() {
    }

    public void init(String name, CompanyTypeI type) {
        this.name = name;
        this.type = type;
        closedObject = new BooleanState(name + "_Closed", false);

    }

    /**
     * @return This company's number
     */
    public int getNumber() {
        return companyNumber;
    }

    /**
     * @return whether this company is closed
     */
    public boolean isClosed() {
        return closedObject.booleanValue();
    }

    /**
     * Close this company.
     */
    public void setClosed() {
        closedObject.set(true);
    }

    /**
     * @return Type of company (Public/Private)
     */
    public CompanyTypeI getType() {
        return type;
    }

    /**
     * @return String for type of company (Public/Private)
     */
    public String getTypeName() {
        return type.getName();
    }

    /**
     * @return Name of company
     */
    public String getName() {
        return name;
    }

    public String getLongName() {
        return longName;
    }

    /**
     * @return
     */
    public int getCertLimitCount() {
        return certLimitCount;
    }

    /**
     * @return This company's number
     */
    public int getCompanyNumber() {
        return companyNumber;
    }

    /**
     * @return Value of this company
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

    public MoveableHolder getHolder() {
        return portfolio;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    /**
     * Stub method implemented to comply with TokenHolderI interface. Always
     * returns false.
     *
     * Use addToken(MapHex hex) method instead.
     */
    public boolean addToken(CompanyI company) {
        return false;
    }

    @Override
    public String toString() {
        return getTypeName() + ": " + getCompanyNumber() + ". " + getName()
               + " $" + this.getValue();
    }

    public boolean equals(CompanyI company) {
        if (this.companyNumber == company.getCompanyNumber()
            && this.name.equals(company.getName())
            && this.type.equals(company.getType())) return true;

        return false;
    }

}
