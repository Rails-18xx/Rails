package net.sf.rails.game;

import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.financial.BankPortfolio;
import net.sf.rails.game.financial.PublicCertificate;
import net.sf.rails.game.model.PortfolioModel;
import net.sf.rails.game.state.BooleanState;
import net.sf.rails.game.state.Portfolio;
import net.sf.rails.util.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReleaseRule {

    BooleanState done;
    String soldString, releaseString;
    boolean soldStartPacket = false;
    Map<PublicCompany, Integer> soldPercPerCompany = new HashMap<>();
    Map<PublicCompany, Boolean> statusPerCompany = new HashMap<>();
    List<PublicCompany> companiesToRelease = new ArrayList<>();

    CompanyManager companyManager;
    GameManager gameManager;
    PortfolioModel ipo, unavailable;

    public ReleaseRule(CompanyManager companyManager, String id,
                       String soldString, String releaseString) {
        this.soldString = soldString;
        this.releaseString = releaseString;

        this.companyManager = companyManager;
        this.gameManager = companyManager.gameManager;
        Bank bank = gameManager.getRoot().getBank();
        this.ipo = bank.getIpo().getPortfolioModel();
        this.unavailable = bank.getUnavailable().getPortfolioModel();

        done = new BooleanState (companyManager, "RR_"+id);

        if (soldString.equalsIgnoreCase("StartPacket")) {
            soldStartPacket = true;
        } else {
            for (String cNameAndPerc : soldString.split(",")) {
                String[] parts = cNameAndPerc.split(":");
                PublicCompany company = companyManager.getPublicCompany(parts[0]);
                int percSold = (parts.length > 1 ? Integer.parseInt(parts[1]) : 100);
                soldPercPerCompany.put(company, percSold);
                statusPerCompany.put(company, false);
            }
        }
        for (String cName : releaseString.split(",")) {
            PublicCompany company = companyManager.getPublicCompany(cName);
            companiesToRelease.add(company);
        }
    }

    private boolean ifSold(PublicCompany company, int percSold) {
        if (percSold == 0) {
            return ipo.getShare(company) == 0;
        } else {
            return ipo.getShare(company) <= 100 - percSold;
        }
    }

    private void releaseCompanyShares(PublicCompany company) {
        int share;
        int totalShare = 0;
        String reportText = null;

        List<PublicCertificate> certsToMove = new ArrayList<>();
        for (PublicCertificate cert : unavailable.getCertificates(company)) {
            if (cert.isInitiallyAvailable()) {
                certsToMove.add(cert);
                share = cert.getShare();
                totalShare += share;
            }
        }
        Portfolio.moveAll(certsToMove, ipo.getParent());
        company.setBuyable(true);

        if (totalShare == 100) {
            reportText = LocalText.getText("SharesReleased",
                    "All", company.getId());
        } else if (totalShare > 0){
            reportText = LocalText.getText("SharesReleased",
                    totalShare + "%", company.getId());
        }
        if (reportText != null) ReportBuffer.add(gameManager, reportText);

    }

    public boolean isDone() {
        if (!done.value()) {
            if (soldStartPacket) {
                if (!gameManager.getStartPacket().areAllSold()) {
                    return false;
                }
            } else {
                for (PublicCompany company : soldPercPerCompany.keySet()) {
                    if (!statusPerCompany.get(company)) {
                        if (ifSold(company, soldPercPerCompany.get(company))) {
                            statusPerCompany.put(company, true);
                        } else {
                            return false;
                        }
                    }
                }
            }
            done.set (true);
            for (PublicCompany company : companiesToRelease) {
                releaseCompanyShares(company);
            }
        }
        return done.value();
    }
}
