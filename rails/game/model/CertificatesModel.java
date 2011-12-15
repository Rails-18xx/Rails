package rails.game.model;

import java.util.Iterator;

import com.google.common.collect.ImmutableSet;

import rails.game.Player;
import rails.game.PublicCertificate;
import rails.game.PublicCompany;
import rails.game.state.HashMultimapState;

/**
 * Model that contains and manages the certificates
 * TODO: It might improve performance to separate the large multimap into smaller ones per individual companies, but I doubt it
        // TODO: find out where the president model has to be linked
        // this.addObserver(company.getPresidentModel());
 * @author freystef
 */
public final class CertificatesModel extends AbstractModel<String> implements Storage<PublicCertificate> {

    /** Owned public company certificates by company */
    private final HashMultimapState<PublicCompany, PublicCertificate> certificates;

    public CertificatesModel(Owner owner) {
        super(owner, "CertificatesModel");
        certificates = new HashMultimapState<PublicCompany, PublicCertificate>(owner, "Certificates");
        certificates.addObserver(this);
    }

    public int getShare(PublicCompany company) {
        int share = 0;
        for (PublicCertificate cert : certificates.get(company)) {
            share += cert.getShare();
        }
        return share;
    }

    public String getData(PublicCompany company) {
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
