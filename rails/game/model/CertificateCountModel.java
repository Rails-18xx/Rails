package rails.game.model;

import rails.game.state.Item;
import rails.game.state.Model;

public final class CertificateCountModel extends Model {

    private CertificateCountModel() {}
  
    /**
     * Creates a initialized CertificateCountModel
     */
    public static CertificateCountModel create(PortfolioModel parent){
        CertificateCountModel model =  new CertificateCountModel();
        model.init(parent, "CertificateCountModel");
        return model;
    }

    /** 
     * @param parent restricted to Portfolio
     */
    @Override
    public void init(Item parent, String id){
        super.checkedInit(parent, id, PortfolioModel.class);
        super.init(parent, id);
        // lets certificate count model update on portfolio changes
        getParent().addModel(this);
    }

    /**
     * @return restricted to PortfolioModel
     */
    @Override
    public PortfolioModel getParent() {
        return (PortfolioModel)super.getParent();
    }

    @Override
    public String toString() {
        return ("" + getParent().getCertificateCount()).replaceFirst("\\.0", "").replaceFirst("\\.5", "\u00bd");
    }

}
