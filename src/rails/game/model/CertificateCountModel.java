package rails.game.model;

import static com.google.common.base.Preconditions.checkArgument;

import rails.game.state.Item;
import rails.game.state.Model;

public final class CertificateCountModel extends Model {
    
    public final static String ID = "CertificateCountModel"; 

    private CertificateCountModel() {}
  
    /**
     * Creates a non-initialized CertificateCountModel
     */
    public static CertificateCountModel create(){
        return new CertificateCountModel();
    }

    /** 
     * @param parent restricted to PortfolioModel
     * @param id restricted to static field ID
     */
    @Override
    public void init(Item parent, String id){
        checkArgument(id.equals(ID), "id of CertificateCoutModel must equal " + ID);
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
