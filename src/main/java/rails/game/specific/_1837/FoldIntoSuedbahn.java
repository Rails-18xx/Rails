package rails.game.specific._1837;


import java.util.List;

import net.sf.rails.game.Company;

import rails.game.action.FoldIntoNational;

public class FoldIntoSuedbahn extends FoldIntoNational {

    public static final long serialVersionUID = 1L;

    public FoldIntoSuedbahn(List<Company> companies) {
        super(companies);
    }

    public FoldIntoSuedbahn(Company company) {
        super(company);
    }

}