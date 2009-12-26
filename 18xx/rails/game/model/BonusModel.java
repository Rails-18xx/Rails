/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/model/BonusModel.java,v 1.2 2009/12/26 12:46:52 evos Exp $*/
package rails.game.model;

import java.util.List;

import rails.game.Bank;
import rails.game.Bonus;
import rails.game.state.StringState;

public class BonusModel extends StringState {

    private List<Bonus> bonuses;

    public BonusModel(String name) {
        super(name, "");
    }

	public void set(List<Bonus> bonuses) {

        this.bonuses = bonuses;

    }

    @Override
	public String getText() {

    	if (bonuses == null || bonuses.isEmpty()) return "";

    	StringBuffer b = new StringBuffer("<html><center>");

    	for (Bonus bonus : bonuses) {
    		if (b.length() > 14) {
    			b.append("<br>");
    		}
    		b.append(bonus.getIdForView()).append("+").append(Bank.format(bonus.getValue()));
    	}

    	return b.toString();
    }

}
