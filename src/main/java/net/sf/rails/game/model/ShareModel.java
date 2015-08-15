package net.sf.rails.game.model;

import net.sf.rails.game.PublicCompany;

/**
 * ShareModel for displaying the share percentages
 */
public final class ShareModel extends RailsModel {
    
    private final PublicCompany company;

    private ShareModel(CertificatesModel parent, PublicCompany company) {
        super(parent, "shareModel_" + company.getId());
        this.company = company;
        // have share model observe floatation status of company
        company.getFloatedModel().addModel(this);
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
