package net.sf.rails.game.specific._1826;

import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.game.BaseToken;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.RailsItem;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.financial.BankPortfolio;
import net.sf.rails.game.financial.PublicCertificate;
import net.sf.rails.game.model.PortfolioOwner;
import net.sf.rails.game.special.ExtraTrainRight;
import net.sf.rails.game.state.Model;
import net.sf.rails.game.state.Observer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PublicCompany_1826 extends PublicCompany {

    private static final Logger log = LoggerFactory.getLogger(PublicCompany_1826.class);

    public PublicCompany_1826(RailsItem parent, String id) {
        super (parent, id, true);
    }

    public PublicCompany_1826(RailsItem parent, String id, boolean hasStockPrice) {
        super(parent, id, hasStockPrice);
    }

    public void finishConfiguration(RailsRoot root)
            throws ConfigurationException {

        super.finishConfiguration(root);

        // 5-share companies have an initial share unit of 20%
        if (isPotentialFiveShareCompany()) {
            shareUnit.set(20);
            for (PublicCertificate cert : certificates) {

            }
        }
    }

    private boolean isPotentialFiveShareCompany() {
        return getType().getId().equals("Public") && !getId().equals("Belg");
    }

    @Override
    public void setFloated() {

        int extraTokens = 0;

        if (isPotentialFiveShareCompany()) {
            if (shareUnit.value() == 10) {  // Floating as a 10-share company
                // 4 tokens
                extraTokens++;
                if (getRoot().getPhaseManager().hasReachedPhase("10H")) {
                    // 5 tokens
                    extraTokens++;
                }
            }
        }

        for (int i = 0; i < extraTokens; i++) {
            baseTokens.addBaseToken(BaseToken.create(this), false);
        }
        numberOfBaseTokens += extraTokens;

        super.setFloated();
    }

    /** Convert company from a 5-share to a 10-share company */
    /* TODO: the numbers here should become configurable */
    public boolean grow (int newNumberOfShares) {  // argument not used

        if (getShareUnit() == 20 && hasReachedDestination()) {

            shareUnit.set(10);

            BankPortfolio reserved = getRoot().getBank().getUnavailable();
            Set<PublicCertificate> last5Shares = reserved.getPortfolioModel().getCertificates(this);
            for (PublicCertificate cert : last5Shares) {
                cert.moveTo(this);
            }

            ReportBuffer.add(this, LocalText.getText("CompanyHasGrown",
                    this, newNumberOfShares));

            trainLimit.setTo(List.of(4,3,2));

            // For some reason the shareUnit change does not update
            // the percentages shown in the GameStatus window.
            // E.g. 60% should become 30%, etc.
            // There must be a nicer way to accomplish that,
            // but for now the below code works.
            Set<Model> modelsToUpdate = new HashSet<>();
            PortfolioOwner owner;
            Model model;
            for (PublicCertificate cert : getCertificates()) {
                owner = (PortfolioOwner) cert.getOwner();
                model = owner.getPortfolioModel().getShareModel(this);
                if (!modelsToUpdate.contains(model)) modelsToUpdate.add(model);
            }
            for (Model m : modelsToUpdate) {
                for (Observer obs : m.getObservers()) {
                    obs.update(m.toText());
                }
            }
        }
        return true;
    }

    @Override
    protected int getTrainLimit(int index) {

        int limit = super.getTrainLimit(index);
        if (rightsModel == null || rightsModel.isEmpty()) return limit;

        ExtraTrainRight etr = rightsModel.getRightType(ExtraTrainRight.class);
        if (etr != null) limit += etr.getExtraTrains();

        return limit;
    }


}