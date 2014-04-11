package net.sf.rails.game;

import java.util.HashMap;
import java.util.Map;

import net.sf.rails.common.LocalText;
import net.sf.rails.game.model.CertificatesModel;
import net.sf.rails.game.state.IntegerState;
import net.sf.rails.game.state.Ownable;
import net.sf.rails.game.state.Typable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ComparisonChain;


public class PublicCertificate extends RailsOwnableItem<PublicCertificate> implements Certificate, Cloneable, Typable<PublicCompany> {

    /** From which public company is this a certificate */
    protected PublicCompany company;
    /**
     * Share percentage represented by this certificate
     */
    protected IntegerState shares = IntegerState.create(this, "shares");
    /** President's certificate? */
    protected boolean president;
    // FIXME: If this is changable, it should be a state variable, otherwise UNDO problems
    /** Count against certificate limits */
    protected float certificateCount = 1.0f;
    
    /** Availability at the start of the game */
    protected boolean initiallyAvailable;

    /** A key identifying the certificate's unique type */
    protected String certTypeId;

    /** A key identifying the certificate's unique ID */
    protected String certId;
    
    // FIMXE: 
    /** Index within company (to be maintained in the IPO) */
    protected int indexInCompany;

    /** A map allowing to find certificates by unique id */
    // FIXME: Remove static map, replace by other location mechanisms
    protected static Map<String, PublicCertificate> certMap =
            new HashMap<String, PublicCertificate>();

    
    protected static Logger log =
            LoggerFactory.getLogger(PublicCertificate.class);

    // TODO: Rewrite constructors
    // TODO: Should every certificate have its own id and be registered with the parent?
    public PublicCertificate(RailsItem parent, String id, int shares, boolean president, 
            boolean available, float certificateCount, int index) {
        super(parent, id, PublicCertificate.class);
        this.shares.set(shares);
        this.president = president;
        this.initiallyAvailable = available;
        this.certificateCount = certificateCount;
        this.indexInCompany = index;
    }

// TODO: Can be removed, as
//    most likely this does not work, as it duplicates ids
//    public PublicCertificate(PublicCertificate oldCert) {
//        super(oldCert.getParent(), oldCert.getId(), PublicCertificate.class);
//        this.shares = oldCert.getShares();
//        this.president = oldCert.isPresidentShare();
//        this.initiallyAvailable = oldCert.isInitiallyAvailable();
//        this.certificateCount = oldCert.getCertificateCount();
//        this.indexInCompany = oldCert.getIndexInCompany();
//    }

    @Override
    public RailsItem getParent(){
        return (RailsItem)super.getParent();
    }
    
    @Override
    public RailsRoot getRoot() {
        return (RailsRoot)super.getRoot();
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
        return (Integer) shares.value();
    }

    /**
     * Get the percentage of ownership that this certificate represents. This is
     * equal to the number of shares * the share unit.
     *
     * @return The share percentage.
     */
    public int getShare() {
        return ((Integer) shares.value()) * company.getShareUnit();
    }

    /**
     * Get the name of a certificate. The name is derived from the company name
     * and the share percentage of this certificate. If it is a 100% share (as
     * occurs with e.g. 1835 minors), only the company name is given. If it is a
     * president's share, that fact is mentioned.
     * FIXME: This was renamed from getID(), this will cause some display errors
     */
    public String getName() {
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

    // Typable interface
    public PublicCompany getType() {
        return company;
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
     * Compare is based on 
     * A) Presidency (presidency comes first in natural ordering)
     * B) Number of Shares (more shares means come first)
     * C) Id of CertificateType
     * D) Id of Certificate
     */
    // FIXME: Use this comparator (including the other criterias below to display, the one here only for 
    // default sorting, otherwise Portfolios (TreeMaps) might be confused
    @Override
    public int compareTo(Ownable other) {
        if (other instanceof PublicCertificate) {
            PublicCertificate otherCert = (PublicCertificate)other;
            // sort by the criteria defined above
            return ComparisonChain.start()
//                    .compare(otherCert.isPresidentShare(), this.isPresidentShare())
//                    .compare(otherCert.getShares(), this.getShares())
//                    .compare(this.getType().getId(), otherCert.getType().getId())
                    .compare(this.getId(), otherCert.getId())
                    .result();
        } else {
            return super.compareTo(other);
        }
    }

    public void setShares(int numShares) {
       this.shares.set(numShares);
        
    }

    // Certificate Interface
    public float getCertificateCount() {
        return certificateCount;
    }

    @Deprecated
    public void setCertificateCount(float certificateCount) {
        this.certificateCount = certificateCount;
    }
    
    /**
     * Two certificates are "equal" if they both belong to the same company,
     * represent the same share percentage, and are not a president share.
     *
     * @param cert Public company certificate to compare with.
     * @return True if the certs are "equal" in the defined sense.
     * 
     * FIXME: This cannot work as long as HashCode is not defined accordingly
     */
//    public boolean equals(PublicCertificate cert) {
//        return (cert != null && getCompany() == cert.getCompany()
//                && isPresidentShare() == cert.isPresidentShare() && getShares() == cert.getShares());
//    }

    // TODO: Check if this was required somewhere
//    @Override
//    public String toString() {
//        return "PublicCertificate: " + getId();
//    }


}
