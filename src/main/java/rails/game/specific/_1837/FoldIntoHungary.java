package rails.game.specific._1837;

import java.util.List;

import net.sf.rails.game.Company;

import net.sf.rails.game.RailsRoot;
import rails.game.action.FoldIntoNational;

public class FoldIntoHungary extends FoldIntoNational {

    public static final long serialVersionUID = 1L;

    public FoldIntoHungary(RailsRoot root, List<Company> companies) {
        super(root, companies);
    }

    public FoldIntoHungary(RailsRoot root, Company company) {
        super(company);
    }

}
