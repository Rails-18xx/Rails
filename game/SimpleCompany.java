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

    public CompanyType getType()
    {
       return coType;
    }
    
    /**
     * @see game.CompanyI#getType()
     */
    public String getTypeName() {
        return mType;
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString(){
        return "Company called " + mName + ", of type " + mType;
    }
    
    public int getValue()
    {
       return value;
    }
    
    public void setValue(int v)
    {
       value = v;
    }
    
    public void setCertLimitCount(int c)
    {
       certLimitCount = c;
    }
    
    public int getCertLimitCount()
    {
       return certLimitCount;
    }
    
    public boolean isClosed()
    {
       return isClosed;
    }
    
    public void setClosed(boolean c)
    {
       isClosed = c;
    }
    
    public int getCompanyNumber()
    {
       return companyNumber;
    }
    
    public void init(String n, CompanyType t)
    {
       mName = n;
       coType = t;
    }

    private String mName;
    private String mType;
    private CompanyType coType;
    private int value;
    private int certLimitCount;
    private int companyNumber;
    private boolean isClosed;
}
