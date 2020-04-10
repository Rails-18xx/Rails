package net.sf.rails.game.model;

import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.financial.RailsMoneyOwner;
import net.sf.rails.game.state.BooleanState;
import net.sf.rails.game.state.Currency;
import net.sf.rails.game.state.Purse;

/**
 * A MoneyModel with a wallet inside
 */
public class PurseMoneyModel extends MoneyModel {

    private final Purse purse;

    private final BooleanState initialised;

    private PurseMoneyModel(RailsMoneyOwner parent, String id, Boolean init, Currency currency) {
        super(parent, id, currency);

        this.purse = Purse.create(parent, "purse", currency);
        this.purse.addModel(this);

        this.initialised = new BooleanState(this, "initialised", init);
    }

    public static PurseMoneyModel create(Bank parent, String id, Boolean init, Currency currency) {
        return new PurseMoneyModel(parent, id, init, currency);
    }

    public static PurseMoneyModel create(RailsMoneyOwner parent, String id, Boolean init) {
        Currency currency = parent.getRoot().getBank().getCurrency();
        return new PurseMoneyModel(parent, id, init, currency);
    }

    @Override
    public RailsMoneyOwner getParent() {
        return (RailsMoneyOwner) super.getParent();
    }

    public Purse getPurse() {
        return purse;
    }

    // MoneyModel abstracts
    @Override
    public int value() {
        return purse.value();
    }

    @Override
    public boolean initialised() {
        return initialised.value();
    }

}
