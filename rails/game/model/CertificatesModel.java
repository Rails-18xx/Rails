package rails.game.model;

import java.util.Iterator;

import com.google.common.collect.ImmutableList;

import rails.game.Player;
import rails.game.PublicCertificate;
import rails.game.PublicCompany;
import rails.game.state.Item;
import rails.game.state.Model;
import rails.game.state.Portfolio;
import rails.game.state.PortfolioMap;

/**
 * Model that contains and manages the certificates
 * TODO: It might improve performance to separate the large multimap into smaller ones per individual companies, but I doubt it
        // TODO: find out where the president model has to be linked
        // this.addObserver(company.getPresidentModel());
 * @author freystef
 */
public final class CertificatesModel extends Model {

    public static final String id = "CertificatesModel";
    
    // Certificates portfolio
    private final PortfolioMap<PublicCertificate> certificates = PortfolioMap.create();

    private CertificatesModel() {}
   
    /**
     * Creates an initialized CertificatesModel
     * id is identical to class name "CertificatesModel"
     */
    public static CertificatesModel create(Item parent) {
        CertificatesModel model = new CertificatesModel();
        model.init(parent, id);
        return model;
    }
    
    @Override
    public void init(Item parent, String id){
        super.init(parent, id);
        certificates.init(this, "Certificates");
    }
    
    public PortfolioMap<PublicCertificate> getPortfolio() {
        return certificates;
    }
    
    public void moveAll(CertificatesModel to) {
        Portfolio.moveAll(certificates, to.getPortfolio());
    }
    
    public int getShare(PublicCompany company) {
        int share = 0;
        for (PublicCertificate cert : certificates.getItems(company)) {
            share += cert.getShare();
        }
        return share;
    }

    public String getText(PublicCompany company) {
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
    public String toString() {
        return certificates.toString();
    }
    
    public ImmutableList<PublicCertificate> getCertificates() {
        return certificates.items();
    }
    
    public ImmutableList<PublicCertificate> getCertificates(PublicCompany company) {
        return certificates.getItems(company);
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
