package rails.game;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rails.common.LocalText;
import rails.game.model.CertificatesModel;
import rails.game.state.Item;
import rails.game.state.OwnableItem;

public class PublicCertificate extends OwnableItem<PublicCertificate> implements Certificate, Cloneable {

    /** From which public company is this a certificate */
    protected PublicCompany company;
    /**
     * Share percentage represented by this certificate
     */
    protected int shares;
    /** President's certificate? */
    protected boolean president;
    /** Count against certificate limits */
    protected float certificateCount = 1.0f;
    
    /** Availability at the start of the game */
    protected boolean initiallyAvailable;

    /** A key identifying the certificate's unique type */
    protected String certTypeId;

    /** A key identifying the certificate's unique ID */
    protected String certId;
    
    /** Index within company (to be maintained in the IPO) */
    protected int indexInCompany;

    /** A map allowing to find certificates by unique id */
    // FIXME: Remove static map
    protected static Map<String, PublicCertificate> certMap =
            new HashMap<String, PublicCertificate>();

    
    protected static Logger log =
            LoggerFactory.getLogger(PublicCertificate.class.getPackage().getName());

    // TODO: Rewrite constructors
    // TODO: Should every certificate have its own id and be registered with the parent?
    public PublicCertificate(Item parent, String id, int shares, boolean president, 
            boolean available, float certificateCount, int index) {
        super(parent, id);
        this.shares = shares;
        this.president = president;
        this.initiallyAvailable = available;
        this.certificateCount = certificateCount;
        this.indexInCompany = index;
    }

    // FIXME: Check if this does work, most likely not, as it duplicates IDs
    public PublicCertificate(PublicCertificate oldCert) {
        super(oldCert.getParent(), oldCert.getId());
        this.shares = oldCert.getShares();
        this.president = oldCert.isPresidentShare();
        this.initiallyAvailable = oldCert.isInitiallyAvailable();
        this.certificateCount = oldCert.getCertificateCount();
        this.indexInCompany = oldCert.getIndexInCompany();
    }

    /** Set the certificate's unique ID, for use in deserializing */
    public void setUniqueId(String name, int index) {
        certId = name + "-" + index;
        certMap.put(certId, this);
    }

    /** Set the certificate's unique ID */
    public String getUniqueId() {
        return certId;
    }
    
    public int getIndexInCompany() {
        return indexInCompany;
    }

    public static PublicCertificate getByUniqueId(String certId) {
        return certMap.get(certId);
    }

    
    // FIXME: There is no guarantee that the parent of a certificate portfolio is a portfolioModel
    // Replace that by something that works
    public CertificatesModel getHolder() {
        //return getPortfolio().getParent().getShareModel(company);
        return null;
    }

    /**
     * @return if this is a president's share
     */
    public boolean isPresidentShare() {
        return president;
    }

    /**
     * Get the number of shares that this certificate represents.
     *
     * @return The number of shares.
     */
    public int getShares() {
        return shares;
    }

    /**
     * Get the percentage of ownership that this certificate represents. This is
     * equal to the number of shares * the share unit.
     *
     * @return The share percentage.
     */
    public int getShare() {
        return shares * company.getShareUnit();
    }

    /**
     * Get the name of a certificate. The name is derived from the company name
     * and the share percentage of this certificate. If it is a 100% share (as
     * occurs with e.g. 1835 minors), only the company name is given. If it is a
     * president's share, that fact is mentioned.
     */
    public String getId() {
        int share = getShare();
        if (share == 100) {
            /* Applies to shareless minors: just name the company */
            return company.getId();
        } else if (president) {
            return LocalText.getText("PRES_CERT_NAME",
                    company.getId(),
                    getShare() );
        } else {
            return LocalText.getText("CERT_NAME",
                    company.getId(),
                    getShare());
        }
    }

    public void setInitiallyAvailable(boolean initiallyAvailable) {
        this.initiallyAvailable = initiallyAvailable;
    }

    /**
     * @param b
     */
    public boolean isInitiallyAvailable() {
        return initiallyAvailable;
    }
    
    public float getCertificateCount() {
        return certificateCount;
    }

    public void setCertificateCount(float certificateCount) {
        this.certificateCount = certificateCount;
    }

    /**
     * @param b
     */
    public void setPresident(boolean b) {
        president = b;
    }

    /**
     * @return
     */
    public PublicCompany getCompany() {
        return company;
    }

    /**
     * @param companyI
     */
    public void setCompany(PublicCompany companyI) {
        company = companyI;
        certTypeId = company.getId() + "_" + getShare() + "%";
        if (president) certTypeId += "_P";
    }

    public String getTypeId() {
        return certTypeId;
    }

    @Override
    protected Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            log.error("Cannot clone certificate:", e);
            return null;
        }
    }

    public PublicCertificate copy() {
        return (PublicCertificate) this.clone();
    }

    /**
     * Two certificates are "equal" if they both belong to the same company,
     * represent the same share percentage, and are not a president share.
     *
     * @param cert Public company certificate to compare with.
     * @return True if the certs are "equal" in the defined sense.
     */
    public boolean equals(PublicCertificate cert) {
        return (cert != null && getCompany() == cert.getCompany()
                && isPresidentShare() == cert.isPresidentShare() && getShares() == cert.getShares());
    }

    @Override
    public String toString() {
        return "PublicCertificate: " + getId();
    }


}
