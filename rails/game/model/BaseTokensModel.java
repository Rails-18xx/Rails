/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/model/BaseTokensModel.java,v 1.4 2008/06/04 19:00:37 evos Exp $*/
package rails.game.model;

import rails.game.PublicCompanyI;
import rails.game.state.State;

/**
 * A model presenting the number of tokens
 */

public class BaseTokensModel extends AbstractModel<String> {

    private PublicCompanyI company;

    public BaseTokensModel(PublicCompanyI company, State allTokenState, State freeTokenState) {
        super(company, "baseTokensModel");
        this.company = company;
        allTokenState.addModel(this);
        freeTokenState.addModel(this);
    }

    public String getData() {
        int allTokens = company.getNumberOfBaseTokens();
        int freeTokens = company.getNumberOfFreeBaseTokens();
        if (allTokens == 0) {
            return "";
        } else if (freeTokens == 0) {
            return "-/" + allTokens;
        } else {
            return freeTokens + "/" + allTokens;
        }
    }

}
