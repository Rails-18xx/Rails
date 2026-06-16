package net.sf.rails.game.specific._1870;

import net.sf.rails.game.GameManager;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.GameDef;
import rails.game.action.SetDividend;
import rails.game.action.PossibleAction;
import net.sf.rails.game.state.StringState;
import rails.game.action.GuiTargetedAction;
import net.sf.rails.game.state.Owner;

public class ConnectionRunRound_1870 extends OperatingRound_1870 {

    private final StringState connectionCompanyId = StringState.create(this, "connectionCompanyId");

    public ConnectionRunRound_1870(GameManager parent, String id) {
        super(parent, id);

        this.steps = new GameDef.OrStep[] {
                GameDef.OrStep.INITIAL,
                GameDef.OrStep.CALC_REVENUE,
                GameDef.OrStep.FINAL
        };
    }

    public void setConnectionCompany(PublicCompany company) {
        this.connectionCompanyId.set(company.getId());
    }

    public static class TargetedSetDividend extends SetDividend implements GuiTargetedAction {
        private static final long serialVersionUID = 1L;

        public TargetedSetDividend(net.sf.rails.game.RailsRoot root, int actualRevenue, int[] allowedRevenueActions) {
            super(root, actualRevenue, false, allowedRevenueActions);
        }

        @Override
        public Object clone() {
            int[] allowedRevenueActions = new int[] { SetDividend.PAYOUT, SetDividend.WITHHOLD };
            TargetedSetDividend clone = new TargetedSetDividend(getRoot(), this.getActualRevenue(),
                    allowedRevenueActions);
            clone.setActualCompanyTreasuryRevenue(this.getActualCompanyTreasuryRevenue());
            clone.setRevenueAllocation(this.getRevenueAllocation());
            return clone;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || !(o instanceof SetDividend))
                return false;
            SetDividend other = (SetDividend) o;

            // Relax the getClass() check to allow UI clones to safely match the engine's
            // original action
            return this.getCompany().equals(other.getCompany()) &&
                    this.getActualRevenue() == other.getActualRevenue() &&
                    this.getRevenueAllocation() == other.getRevenueAllocation();
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(getCompany(), getActualRevenue(), getRevenueAllocation());
        }

        @Override
        public String getGroupLabel() {
            return "CONNECTION RUN";
        }

        @Override
        public String getButtonLabel() {
            return "Execute Run";
        }

        @Override
        public Owner getTarget() {
            return getCompany();
        }

        @Override
        public Owner getActor() {
            return getCompany();
        }

        @Override
        public java.awt.Color getHighlightBackgroundColor() {
            return new java.awt.Color(255, 215, 0); // Gold
        }

        @Override
        public java.awt.Color getHighlightBorderColor() {
            return new java.awt.Color(218, 165, 32); // Dark Goldenrod
        }

        @Override
        public java.awt.Color getHighlightTextColor() {
            return java.awt.Color.BLACK;
        }

        @Override
        public java.awt.Color getButtonColor() {
            return new java.awt.Color(255, 215, 0); // Gold
        }

        @Override
        public int getHotkey() {
            return 0;
        }
    }

@Override
    public void start() {

        String compId = connectionCompanyId.value();
        if (compId != null && !compId.isEmpty()) {
            PublicCompany connectionCompany = getRoot().getCompanyManager().getPublicCompany(compId);
            if (connectionCompany != null) {
                operatingCompanies.clear();
                operatingCompanies.add(connectionCompany);
            }
        }
        super.start();

    }

    @Override
    protected void privatesPayOut() {
        // Prevent double-dipping: Private companies do NOT pay out again during a
        // connection run.
    }

    @Override
    protected void prepareRevenueAndDividendAction() {
        super.prepareRevenueAndDividendAction();

        SetDividend originalAction = null;
        for (PossibleAction pa : possibleActions.getList()) {
            if (pa instanceof SetDividend) {
                originalAction = (SetDividend) pa;
                break;
            }
        }

        if (originalAction != null) {
            possibleActions.remove(originalAction);

            // 1870 Rule: Connection runs do not allow Half Dividends
            int[] allowedRevenueActions = new int[] { SetDividend.PAYOUT, SetDividend.WITHHOLD };

            SetDividend modifiedAction = new TargetedSetDividend(getRoot(), originalAction.getActualRevenue(),
                    allowedRevenueActions);
            modifiedAction.setActualRevenue(originalAction.getActualRevenue());

            possibleActions.add(modifiedAction);
        } else {
            // Since the RUN_TRAINS step is bypassed, inject a default action
            int[] allowedRevenueActions = new int[] { SetDividend.PAYOUT, SetDividend.WITHHOLD };
            TargetedSetDividend modifiedAction = new TargetedSetDividend(getRoot(), 0, allowedRevenueActions);
            possibleActions.add(modifiedAction);
        }
    }

    @Override
    public boolean process(PossibleAction action) {
        if (action instanceof SetDividend) {

           
            boolean success = super.process(action);

            // CRITICAL FIX: The 1870 Connection Run consists solely of a single payout.
            // Bypassing the standard OR steps means the engine searches for a "Buy Train"
            // phase that doesn't exist and hangs. We explicitly finish the round right
            // here.
            if (success) {
                finishRound();
            }

            return success;
        }
        return super.process(action);
    }

    @Override
    public String getRoundName() {
        return "Connection Run";
    }


}