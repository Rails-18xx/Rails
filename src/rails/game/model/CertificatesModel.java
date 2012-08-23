package rails.game.model;

import java.util.Iterator;

import com.google.common.collect.ImmutableSet;

import rails.game.Player;
import rails.game.PublicCertificate;
import rails.game.PublicCompany;
import rails.game.state.HashMapState;
import rails.game.state.Model;
import rails.game.state.Owner;
import rails.game.state.PortfolioMap;

/**
 * Model that contains and manages the certificates
 * TODO: It might improve performance to separate the large multimap into smaller ones per individual companies, but I doubt it
        // TODO: find out where the president model has to be linked
        // this.addModel(company.getPresidentModel());
 * @author freystef
 */
public class CertificatesModel extends Model {

    public final static String ID = "CertificatesModel";
    
    private final PortfolioMap<PublicCompany, PublicCertificate> certificates;
    
    private final HashMapState<PublicCompany, ShareModel> shareModels = HashMapState.create(this, "shareModels");

    private CertificatesModel(Owner parent) {
        super(parent, ID);
        certificates = PortfolioMap.create(parent, "certificates", PublicCertificate.class);
    }
    
    public static CertificatesModel create(Owner parent) {
        return new CertificatesModel(parent);
    }
    
    @Override
    public Owner getParent() {
        return (Owner)super.getParent();
    }
    
    public PortfolioMap<PublicCompany, PublicCertificate> getPortfolio() {
        return certificates;
    }
    
    public void moveAll(Owner newOwner) {
        certificates.moveAll(newOwner);
    }
    
    public ShareModel getShareModel(PublicCompany company) {
        if (shareModels.containsKey(company)) {
            return shareModels.get(company);
        } else {
            ShareModel model = ShareModel.create(this, company);
            shareModels.put(company, model);
            return model;
        }
    }
    
    int getShare(PublicCompany company) {
        int share = 0;
        for (PublicCertificate cert : certificates.items(company)) {
            share += cert.getShare();
        }
        return share;
    }

    String getText(PublicCompany company) {
        int share = this.getShare(company);
        
        if (share == 0) return "";
        StringBuffer b = new StringBuffer();
        b.append(share).append("%");
        
        if (getParent() instanceof Player
            && company.getPresident() == getParent()) {
            b.append("P");
            if (!company.hasFloated()) b.append("U");
            b.append(company.getExtraShareMarks());
        }
        return b.toString();
    }
    
    @Override
    public String toText() {
        return certificates.toString();
    }
    
    public ImmutableSet<PublicCertificate> getCertificates() {
        return certificates.items();
    }
    
    public ImmutableSet<PublicCertificate> getCertificates(PublicCompany company) {
        return certificates.items(company);
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
        return certificates.containsItem(certificate);
    }
    
    public boolean contains(PublicCompany company) {
        return certificates.containsKey(company);
    }
    
    public Iterator<PublicCertificate> iterator() {
        return certificates.iterator();
    }

    public boolean moveInto(PublicCertificate c) {
        return certificates.moveInto(c);
    }

    public int size() {
        return certificates.size();
    }

    public boolean isEmpty() {
        return certificates.isEmpty();
    }

}
