package rails.game.model;

import com.google.common.collect.Multimap;

import rails.game.PublicCertificate;
import rails.game.PublicCompany;
import rails.game.state.Model;

/**
 * Model for displaying the share details (used for ToolTips)
*/
public final class ShareDetailsModel extends RailsModel {
        
    private final PublicCompany company;

    private ShareDetailsModel(CertificatesModel parent, PublicCompany company) {
        super(parent, "shareDetailsModel_" + company.getId());
        this.company = company;
    }

    public static ShareDetailsModel create(CertificatesModel certModel, PublicCompany company) {
        ShareDetailsModel model = new ShareDetailsModel(certModel, company);
        certModel.addModel(model);
        return model;
    }
    
    @Override
    public CertificatesModel getParent() {
        return (CertificatesModel)super.getParent();
    }
    
    @Override
    public String toText() {
        Multimap<String, PublicCertificate> certs = getParent().getCertificatesByType(company);
        if (certs.isEmpty()) return null;

        StringBuilder text = new StringBuilder();
        for (String certType : certs.keySet()) {
            if (text.length() > 0) text.append("<br>");
            // parse certType
            // TODO: Create true CertificateTypes
            String[] items = certType.split("_");
            String type = items[1] + (items.length > 2 && items[2].contains("P") ? "P" : "");
            text.append(type).append(" x ").append(certs.get(certType).size());
        }
        return "<html>" + text.toString() + "</html>";
    }

}
