package net.sf.rails.game.specific._1837;

import net.sf.rails.game.GameManager;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.financial.PublicCertificate;
import rails.game.action.NullAction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KKFormationRound extends NationalFormationRound {
    private static final Logger log = LoggerFactory.getLogger(KKFormationRound.class);

    public KKFormationRound(GameManager parent, String id) {
        super(parent, id);
    }



    
    @Override
    protected void processExchange(PublicCompany minor, PublicCompany major, ExchangeMinorAction action) {
        log.info("1837_KK_NFR: Processing exchange for " + minor.getId());


        if ("K1".equals(minor.getId())) {
            net.sf.rails.game.Player owner = minor.getPresident();
            if (owner != null) {
                // Remove K1 certificates
                for (Object obj : new java.util.ArrayList<>(owner.getPortfolioModel().getCertificates())) {
                    if (obj instanceof PublicCertificate && ((PublicCertificate)obj).getCompany().equals(minor)) {
                        ((PublicCertificate)obj).moveTo(net.sf.rails.game.financial.Bank.getUnavailable(gameManager.getRoot()));
                    }
                }
                // Explicitly assign the 20% KK President share to the K1 owner
                for (PublicCertificate pc : major.getCertificates()) {
                    if (pc.isPresidentShare() && !(pc.getOwner() instanceof net.sf.rails.game.Player)) {
                        pc.moveTo(owner);
                        log.info("1837_KK_NFR: Assigned KK President Share to " + owner.getName());
                        break;
                    }
                }
            }
        }


        


        // Execute asset transfer and standard individual exchange
        Merger1837.mergeMinor(gameManager, minor, major);

      
    }

@Override
    public boolean setPossibleActions() {
        boolean b = super.setPossibleActions();
        
        // Ensure the "No" button is always a standard NullAction that ORPanel can validate.
        NullAction passAction = new NullAction(gameManager.getRoot(), rails.game.action.NullAction.Mode.DONE);
        passAction.setLabel("No / Keep Minor");
        passAction.setButtonLabel("No / Keep Minor");
        possibleActions.add(passAction);
        
        return b;
    }

}