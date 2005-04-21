/*
 * Created on 05-Mar-2005
 *
 * IG Adams
 */
package game;


/**
 * @author iadams
 *
 * Trivial implementation of CompanyI as proof-of concept.
 */
public class SimpleCompany implements CompanyI {

    /**
     * Constructor, just takes name and type information.
     * @param name the name of this company
     * @param type the company's type.
     */
    public SimpleCompany(String name, String type) {
        mName = name;
        mType = type;
    }

    /**
     * @see game.CompanyI#getName()
     */
    public String getName() {
        return mName;
    }

    /**
     * @see game.CompanyI#getType()
     */
    public String getType() {
        return mType;
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString(){
        return "Company called " + mName + ", of type " + mType;
    }

    private String mName;
    private String mType;

	/* (non-Javadoc)
	 * @see game.CompanyI#isClosed()
	 */
	public boolean isClosed() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see game.CompanyI#setClosed(boolean)
	 */
	public void setClosed(boolean b) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see game.CompanyI#getCertLimitCount()
	 */
	public int getCertLimitCount() {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see game.CompanyI#setCertLimitCount(int)
	 */
	public void setCertLimitCount(int i) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see game.CompanyI#init(java.lang.String, game.CompanyTypeI)
	 */
	public void init(String name, CompanyTypeI type) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see game.CompanyI#getTypeName()
	 */
	public String getTypeName() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see game.CompanyI#getValue()
	 */
	public int getValue() {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see game.CompanyI#setValue(int)
	 */
	public void setValue(int i) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see game.CompanyI#getCompanyNumber()
	 */
	public int getCompanyNumber() {
		// TODO Auto-generated method stub
		return 0;
	}
}
