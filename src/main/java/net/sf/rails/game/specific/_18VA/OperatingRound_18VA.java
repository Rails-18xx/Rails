package net.sf.rails.game.specific._18VA;

import net.sf.rails.game.*;
import net.sf.rails.game.special.SpecialBaseTokenLay;
import net.sf.rails.game.special.SpecialRight;
import net.sf.rails.game.specific._1826.PublicCompany_1826;
import rails.game.action.GrowCompany;
import rails.game.action.LayBaseToken;
import rails.game.action.UseSpecialProperty;

import java.util.ArrayList;
import java.util.List;

public class OperatingRound_18VA extends OperatingRound {

    /**
     * Constructed via Configure
     */
    public OperatingRound_18VA(GameManager parent, String id) {
        super(parent, id);

    }

    @Override
    protected void setGameSpecificPossibleActions() {

        PublicCompany_18VA company = (PublicCompany_18VA) getOperatingCompany();

        // From phase 3, 5-share companies may grow to 10-share companies
        if (company.getShareUnit() == 20
                && getRoot().getPhaseManager().hasReachedPhase("3")) {
            possibleActions.add(new GrowCompany(getRoot(), 10));
        }

        // Check if the operating company can use the extra train right
        List<SpecialRight> srs = company.getPortfolioModel()
                .getSpecialProperties(SpecialRight.class, false);
        if (srs != null && !srs.isEmpty()) {
            possibleActions.add(new UseSpecialProperty(srs.get(0)));
        }
    }

    @Override
    protected void setSpecialTokenLays() {

        /* Special-property base token lays */
        currentSpecialTokenLays.clear();

        PublicCompany company = operatingCompany.value();
        if (!company.canUseSpecialProperties()) return;
        List<MapHex> remainingLocations = new ArrayList<>();

        for (SpecialBaseTokenLay stl : getSpecialProperties(SpecialBaseTokenLay.class)) {
            // in 18VA, below settings must be true, but check anyway
            if (stl.isExtra() && stl.isCreate() && stl.isOffCity()) {

                // This STL is location specific. Check if there
                // isn't already a token of this company
                List<MapHex> locations = stl.getLocations();
                if (locations != null && !locations.isEmpty()) {
                    for (MapHex location : locations) {
                        if (location.hasTokenOfCompany(company)) {
                            continue;
                        }
                        remainingLocations.add(location);
                    }
                }
                LayBaseToken action = new LayBaseToken(getRoot(), stl);
                action.setType(LayBaseToken.NON_CITY);
                currentSpecialTokenLays.add(action);
            }
        }
    }

    public boolean layBaseToken(LayBaseToken action) {

        if (action.getType() == LayBaseToken.NON_CITY) {
            // Create an extra (virtual) token spot (in 18VA only 1 station per hex)
            action.getChosenHex().getStation(1).addVirtualBaseSlot();
            // Create an extra token
            //getOperatingCompany().getBaseTokensModel().addBaseToken(BaseToken.create(getOperatingCompany()), true);
            ((PublicCompany_18VA)getOperatingCompany()).addBaseToken();
        }

        return super.layBaseToken(action);
    }

    protected void newPhaseChecks() {
        Phase phase = Phase.getCurrent(this);
        String phaseId = phase.getId();

        if (phaseId.equals("5")) {
            // Convert all remaining 5-share companies to 10-share
            for (PublicCompany company : companyManager.getAllPublicCompanies()) {
                if (!company.isClosed() && company.getShareUnit() == 20) {
                    company.grow();
                }
            }
        }
    }

}
