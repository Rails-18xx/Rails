package rails.game.model;

import rails.game.state.Item;
import rails.game.state.Model;

public final class CertificateCountModel extends Model {

    private PortfolioModel owner;
    
    private CertificateCountModel() {}
  
    /**
     * Creates a fully initialized CertificateCountModel
     */
    public static CertificateCountModel create(PortfolioModel parent){
        return new CertificateCountModel().init(parent, "CertificateCountModel");
    }

    /** 
     * @param parent restricted to Portfolio
     */
    @Override
    public CertificateCountModel init(Item parent, String id){
        if ((parent instanceof PortfolioModel)) {
            throw new IllegalArgumentException("CertificateCountModel init() only works for Portfolios");
        }
        super.init(parent, id);
        this.owner = (PortfolioModel)parent;
        // lets certificate count model update on portfolio changes
        owner.addModel(this);
        
        return this;
    }

    /**
     * @return restricted to Portfolios
     */
    @Override
    public PortfolioModel getParent() {
        return (PortfolioModel)super.getParent();
    }

    @Override
    public String toString() {
        return ("" + owner.getCertificateCount()).replaceFirst("\\.0", "").replaceFirst("\\.5", "\u00bd");
    }

}
