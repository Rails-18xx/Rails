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
}
