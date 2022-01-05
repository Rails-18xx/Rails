/**
 * 
 */
package net.sf.rails.game.specific._1844;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.rails.game.RailsItem;
import net.sf.rails.game.RailsOwnableItem;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.financial.Certificate;
import net.sf.rails.game.state.Context;
import net.sf.rails.game.state.IntegerState;
import net.sf.rails.game.state.Ownable;
import net.sf.rails.game.state.Owner;
import net.sf.rails.game.state.Typable;

/**
 * @author martin
 * As PublicCertificate is hardwired to PublicCompany we need this Class 
 * for the Certificates of a Holding Company.
 * In 1844 the certificates for the Tunnel and Mountain Railway Company 
 * respectivly hold special Properties
 * How do we transfer/configure those specialproperties ? DO we configure the Company 
 * with special properties or each certificate ?
 */
public class HoldingCompanyCertificate extends RailsOwnableItem<HoldingCompanyCertificate> implements Certificate, Cloneable, Typable<HoldingCompany> {

    /** From which public company is this a certificate */
    protected HoldingCompany company;
    /**
     * Share percentage represented by this certificate
     */
    protected IntegerState shares = IntegerState.create(this, "shares");

    // For 1844 its not changeable
    /** Count against certificate limits */
    protected float certificateCount = 1.0f;
    
    /** Availability at the start of the game */
    protected boolean initiallyAvailable;

    /** A key identifying the certificate's unique ID */
    protected String certId;
    
    /** Index within company (to be maintained in the IPO) */
    protected int indexInCompany;

    /** A map allowing to find certificates by unique id */
    // FIXME: Remove static map, replace by other location mechanisms
    protected static Map<String, HoldingCompanyCertificate> certMap =
            new HashMap<String, HoldingCompanyCertificate>();

    
    protected static Logger log =
            LoggerFactory.getLogger(HoldingCompanyCertificate.class);

    // TODO: Rewrite constructors
    // TODO: Should every certificate have its own id and be registered with the parent?
    public HoldingCompanyCertificate(RailsItem parent, String id, int shares,  
            boolean available, float certificateCount, int index) {
        super(parent, id, HoldingCompanyCertificate.class);
        this.shares.set(shares);
        this.initiallyAvailable = available;
        this.certificateCount = certificateCount;
        this.indexInCompany = index;
    }

    @Override
    public RailsItem getParent(){
        return (RailsItem)super.getParent();
    }

    @Override
    public Owner getOwner() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getId() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Context getContext() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RailsRoot getRoot() {
        return (RailsRoot)super.getRoot();
    }

    @Override
    public String getURI() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getFullURI() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String toText() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int compareTo(Ownable o) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public float getCertificateCount() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void setCertificateCount(float certificateCount) {
        // TODO Auto-generated method stub
        
    }

    public void setUniqueId(String id, int i) {
        // TODO Auto-generated method stub
        
    }

    public boolean isInitiallyAvailable() {
        // TODO Auto-generated method stub
        return false;
    }

    public void setInitiallyAvailable(boolean b) {
        // TODO Auto-generated method stub
        
    }

    public int getShares() {
        // TODO Auto-generated method stub
        return 0;
    }

    public void setCompany(HoldingCompany holdingCompany) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public HoldingCompany getType() {
        // TODO Auto-generated method stub
        return null;
    }

}
