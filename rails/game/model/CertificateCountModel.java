package rails.game.model;

import rails.game.Portfolio;
import rails.game.PublicCertificateI;
import rails.game.PublicCompanyI;

public final class CertificateCountModel extends AbstractModel<String> {

    public CertificateCountModel(Portfolio owner) {
        super(owner, "CertificateCountModel");
    }

    public String getData() {
        return ("" + owner.getPortfolio().getCertificateCount()).replaceFirst("\\.0", "").replaceFirst("\\.5", "\u00bd");
    }






}
