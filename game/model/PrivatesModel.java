/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/model/Attic/PrivatesModel.java,v 1.1 2006/01/05 22:09:34 evos Exp $
 * 
 * Created on 11-Dec-2005
 * Change Log:
 */
package game.model;

import java.util.Iterator;

import game.Portfolio;
import game.PrivateCompanyI;

/**
 * @author Erik Vos
 */
public class PrivatesModel extends ModelObject {

    private Portfolio portfolio;
    
    public static final int SPACE = 0;
    public static final int BREAK = 1;
    
    public PrivatesModel (Portfolio portfolio) {
        this.portfolio = portfolio;
    }
    
    public ModelObject option (int i) {
        if (i == BREAK || i == SPACE) option = i;
        return this;
    }
    
    public String toString() {

		StringBuffer buf = new StringBuffer("<html>");
		Iterator it = portfolio.getPrivateCompanies().iterator();
		PrivateCompanyI priv;
		while (it.hasNext())
		{
			priv = (PrivateCompanyI) it.next();
			if (buf.length() > 6)
				buf.append(option == BREAK ? "<br>" : "&nbsp;");
			buf.append(priv.getName());
		}
		if (buf.length() > 6)
		{
			buf.append("</html>");
		}
		return buf.toString();

    }


}
