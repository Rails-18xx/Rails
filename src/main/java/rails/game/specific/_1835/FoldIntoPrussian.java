package rails.game.specific._1835;

import java.util.List;

import rails.game.action.FoldIntoNational;
import net.sf.rails.game.Company;

/**
 * Rails 2.0: Updated equals and toString methods
*/
public class FoldIntoPrussian extends FoldIntoNational {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public FoldIntoPrussian(List<Company> companies) {
       super(companies);
    }

    public FoldIntoPrussian(Company company) {
        super(company);
    }
}

 