package rails.game.model;

import rails.game.state.Item;


// TODO: Check what is required to get the update correctly
public final class CertificateCountModel extends Model {

    private Portfolio owner;
    
    /**
     * CertificateCountModel is initialized with a default id "CertificateCountModel"
     */
    public CertificateCountModel() {
        super("CertificateCountModel");
    }
  
    /**
     * Initialization of a CertficateCountModel only works for a Portfolio
     * @param owner the portfolio for which certificates are counted
     */
    public void init(Portfolio owner) {
        super.init(owner);
        this.owner = owner;
    }

    /** 
     * This method throws an IllegalArgumentException as CertificateCountModel works only for Portfolios
     */
    @Override
    public void init(Item parent){
        throw new IllegalArgumentException("CertificateCountModel init() only works for Portfolios");
    }

    @Override
    protected String getText() {
        return ("" + owner.getCertificateCount()).replaceFirst("\\.0", "").replaceFirst("\\.5", "\u00bd");
    }

}
