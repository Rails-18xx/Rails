package rails.game.model;

import rails.game.PublicCompany;
import rails.game.state.Model;

/**
 * ShareModel for displaying the share percentages
 */
public final class ShareModel extends Model {
    
    private final PublicCompany company;

    private ShareModel(CertificatesModel parent, PublicCompany company) {
        super(parent, "shareModel_" + company.getId());
        this.company = company;
    }

    public static ShareModel create(CertificatesModel certModel, PublicCompany company) {
        ShareModel model = new ShareModel(certModel, company);
        certModel.addModel(model);
        return model;
    }
    
    @Override
    public CertificatesModel getParent() {
        return (CertificatesModel)super.getParent();
    }
    
    @Override
    public String toText() {
        return getParent().toText(company);
    }
    
}
