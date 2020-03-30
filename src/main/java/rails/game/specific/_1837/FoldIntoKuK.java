package rails.game.specific._1837;

import java.util.List;

import net.sf.rails.game.Company;

import net.sf.rails.game.RailsRoot;
import rails.game.action.FoldIntoNational;

public class FoldIntoKuK extends FoldIntoNational {

    public static final long serialVersionUID = 1L;

    public FoldIntoKuK(RailsRoot root, List<Company> companies) {
        super(root, companies);
    }

    public FoldIntoKuK(RailsRoot root, Company company) {
        super(company);
    }

}
