package rails.game.model;

import rails.game.Player;
import rails.game.Portfolio;
import rails.game.PublicCertificateI;
import rails.game.PublicCompanyI;

/**
 * Model that contains and manages the certificates of a public company
 * @author freystef
 */
public final class CertificatesModel extends HolderModel<PublicCertificateI> {

    private final Portfolio portfolio;
    private final PublicCompanyI company;

    public CertificatesModel(Portfolio portfolio, PublicCompanyI company) {
        super(portfolio, "Certificates_" + company.getId());
        this.portfolio = portfolio;
        this.company = company;
        // add companies president model as observer
        this.addView(company.getPresidentModel());
    }

    public int getShare() {
        int share = 0;
        for (PublicCertificateI cert : this.viewList()) {
            share += cert.getShare();
        }
        return share;
    }

    public String getData() {
        int share = this.getShare();
        
        if (share == 0) return "";
        StringBuffer b = new StringBuffer();
        b.append(share).append("%");
        if (portfolio.getOwner() instanceof Player
            && company.getPresident() == portfolio.getOwner()) {
            b.append("P");
            if (!company.hasFloated()) b.append("U");
            b.append(company.getExtraShareMarks());
        }
        return b.toString();
    }

}
