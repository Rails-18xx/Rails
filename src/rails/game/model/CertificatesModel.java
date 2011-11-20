package rails.game.model;

import java.util.HashMap;
import java.util.Iterator;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Maps;

import rails.game.Player;
import rails.game.PublicCertificate;
import rails.game.PublicCompany;
import rails.game.state.Model;
import rails.game.state.Owner;
import rails.game.state.PortfolioMap;

/**
 * Model that contains and manages the certificates
 * TODO: It might improve performance to separate the large multimap into smaller ones per individual companies, but I doubt it
        // TODO: find out where the president model has to be linked
        // this.addModel(company.getPresidentModel());
 */
public class CertificatesModel extends Model implements Iterable<PublicCertificate> {

    public final static String ID = "CertificatesModel";
    
    private final PortfolioMap<PublicCompany, PublicCertificate> certificates;
    
    private final HashMap<PublicCompany, ShareModel> shareModels = Maps.newHashMap();
    
    private final HashMap<PublicCompany, ShareDetailsModel> shareDetailsModels = Maps.newHashMap();

    private CertificatesModel(Owner parent) {
        super(parent, ID);
        // certificates have the Owner as parent directly
        certificates = PortfolioMap.create(parent, "certificates", PublicCertificate.class);
        // so make this model updating
        certificates.addModel(this);
        
    }
    
    public static CertificatesModel create(Owner parent) {
        return new CertificatesModel(parent);
    }
    
    @Override
    public Owner getParent() {
        return (Owner)super.getParent();
    }
   
    void initShareModels(Iterable<PublicCompany> companies) {
        // create shareModels
        for (PublicCompany company:companies) {
            ShareModel model = ShareModel.create(this, company);
            shareModels.put(company, model);
            ShareDetailsModel modelDetails = ShareDetailsModel.create(this, company);
            shareDetailsModels.put(company, modelDetails);
        }
    }
    
    ShareModel getShareModel(PublicCompany company) {
        return shareModels.get(company);
    }

    ShareDetailsModel getShareDetailsModel(PublicCompany company) {
        return shareDetailsModels.get(company);
    }
    
    float getCertificateCount() {
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
    boolean contains(PublicCompany company) {
        return certificates.containsKey(company);
    }
    
    ImmutableSortedSet<PublicCertificate> getCertificates(
            PublicCompany company) {
        return certificates.items(company);
    }
    
    ImmutableMultimap<String, PublicCertificate> getCertificatesByType(PublicCompany company) {
        ImmutableMultimap.Builder<String, PublicCertificate> certs =  ImmutableMultimap.builder();
        for (PublicCertificate c:certificates.items(company)) {
            certs.put(c.getTypeId(), c);
        }
        return certs.build();
    }
    
    /** Return an array of length 5 that contains:<br>
     * - in element [0]: 1 if there is a presidency, 0 if not (but see below);<br>
     * - in element [1]: number of non-president certificates of size 1 (number of share units);<br>
     * - in element [2]: number of non-president certificates of size 2;<br>
     * - etc.
     * @param compName Company name
     * @param includePresident True if the president certificate must also be included in the other counts.
     * @return integer array
     */
    int[] getCertificateTypeCounts(PublicCompany company, boolean includePresident) {
        int[] uniqueCertCounts = new int[5];
        for (PublicCertificate cert : getCertificates(company)) {
            if (!cert.isPresidentShare() || includePresident) {
                ++uniqueCertCounts[cert.getShares()];
            }
            if (cert.isPresidentShare()) uniqueCertCounts[0] = 1;
        }
        return uniqueCertCounts;
    }
    
    PortfolioMap<PublicCompany, PublicCertificate> getPortfolio() {
        return certificates;
    }

    public Iterator<PublicCertificate> iterator() {
        return certificates.iterator();
    }

    int getShare(PublicCompany company) {
        int share = 0;
        for (PublicCertificate cert : certificates.items(company)) {
            share += cert.getShare();
        }
        return share;
    }
    
    String toText(PublicCompany company) {
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

}
