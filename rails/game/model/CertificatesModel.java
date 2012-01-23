package rails.game.model;

import java.util.Iterator;

import com.google.common.collect.ImmutableSet;

import rails.game.Player;
import rails.game.PublicCertificate;
import rails.game.PublicCompany;
import rails.game.state.HashMultimapState;
import rails.game.state.Item;

/**
 * Model that contains and manages the certificates
 * TODO: It might improve performance to separate the large multimap into smaller ones per individual companies, but I doubt it
        // TODO: find out where the president model has to be linked
        // this.addObserver(company.getPresidentModel());
 * @author freystef
 */
public final class CertificatesModel extends Model implements Storage<PublicCertificate> {

    /** Owned public company certificates by company */
    private final HashMultimapState<PublicCompany, PublicCertificate> certificates;

    /**
     * Certificates is initialized with a default id "CertificatesModel"
     */
    public CertificatesModel() {
        super("CertificatesModel");
        certificates = HashMultimapState.create("Certificates");
    }
   
    /**
     * Creates an initialized CertificatesModel
     */
    public static CertificatesModel create(Owner parent) {
        return new CertificatesModel().init(parent);
    }
    
    /** 
     * @param parent restricted to Owners
     */
    @Override
    public CertificatesModel init(Item parent){
        super.init(parent);
        if (parent instanceof Owner) {
            Owner owner = (Owner)parent;
            certificates.init(owner);
            certificates.addModel(this);
        } else {
            throw new IllegalArgumentException("CertificatesModel init() only works for Owners");
        }
        return this;
    }
    
    /**
     * @return restricted to Owner
     */
    @Override
    public Owner getParent() {
        return (Owner)super.getParent();
    }

    
    public int getShare(PublicCompany company) {
        int share = 0;
        for (PublicCertificate cert : certificates.get(company)) {
            share += cert.getShare();
        }
        return share;
    }

    public String getText(PublicCompany company) {
        int share = this.getShare(company);
        
        if (share == 0) return "";
        StringBuffer b = new StringBuffer();
        b.append(share).append("%");
        
        if (getOwner() instanceof Player
            && company.getPresident() == getOwner()) {
            b.append("P");
            if (!company.hasFloated()) b.append("U");
            b.append(company.getExtraShareMarks());
        }
        return b.toString();
    }
    
    @Override
    public String toString() {
        return certificates.toString();
    }
    
    public ImmutableSet<PublicCertificate> getCertificates() {
        return certificates.values();
    }
    
    public ImmutableSet<PublicCertificate> getCertificates(PublicCompany company) {
        return certificates.get(company);
    }
    
    public float getCertificateCount() {
        float number = 0;
        for (PublicCertificate cert:certificates) {
            PublicCompany company = cert.getCompany();
            if (!company.hasFloated() || !company.hasStockPrice()
                    || !cert.getCompany().getCurrentSpace().isNoCertLimit()) {
                number += cert.getCertificateCount();
            }
        }
        return number;
    }
    
    public boolean contains(PublicCertificate certificate) {
        // should be quicker than containsValue ?
        return certificates.containsEntry(certificate.getCompany(), certificate);
    }
    
    public boolean contains(PublicCompany company) {
        return certificates.containsKey(company);
    }
    
    // methods from Holder interface
    public Iterator<PublicCertificate> iterator() {
        return certificates.iterator();
    }

    public boolean addObject(PublicCertificate object) {
        return certificates.put(object.getCompany(), object);
    }

    public boolean removeObject(PublicCertificate object) {
        return certificates.remove(object.getCompany(), object);
    }

    public int size() {
        return certificates.size();
    }

    public boolean isEmpty() {
        return certificates.isEmpty();
    }

    public Owner getOwner() {
        return getOwner();
    }

}
