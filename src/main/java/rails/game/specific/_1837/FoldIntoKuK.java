package rails.game.specific._1837;

import java.util.List;

import net.sf.rails.game.Company;

import rails.game.action.FoldIntoNational;

public class FoldIntoKuK extends FoldIntoNational {

    public static final long serialVersionUID = 1L;

    public FoldIntoKuK(List<Company> companies) {
        super(companies);
    }

    public FoldIntoKuK(Company company) {
        super(company);
    }

}