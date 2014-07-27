package rails.game.specific._1837;

import java.util.List;

import net.sf.rails.game.Company;

import rails.game.action.FoldIntoNational;

public class FoldIntoHungary extends FoldIntoNational {

    public static final long serialVersionUID = 1L;

    public FoldIntoHungary(List<Company> companies) {
        super(companies);
    }

    public FoldIntoHungary(Company company) {
        super(company);
    }

}
