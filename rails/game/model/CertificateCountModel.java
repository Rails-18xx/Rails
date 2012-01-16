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
     * Creates a fully initialized CertificateCountModel
     */
    public static CertificateCountModel create(Portfolio parent){
        return new CertificateCountModel().init(parent);
    }

    /** 
     * @param parent restricted to Portfolio
     */
    @Override
    public CertificateCountModel init(Item parent){
        super.init(parent);
        if (parent instanceof Portfolio) {
            this.owner = (Portfolio)parent;
        } else {
            throw new IllegalArgumentException("CertificateCountModel init() only works for Portfolios");
        }
        return this;
    }

    /**
     * @return restricted to Portfolios
     */
    @Override
    public Portfolio getParent() {
        return (Portfolio)super.getParent();
    }

    @Override
    protected String getText() {
        return ("" + owner.getCertificateCount()).replaceFirst("\\.0", "").replaceFirst("\\.5", "\u00bd");
    }

}
