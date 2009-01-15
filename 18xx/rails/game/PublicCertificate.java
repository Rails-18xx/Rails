/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/PublicCertificate.java,v 1.14 2009/01/15 20:53:28 evos Exp $ */
package rails.game;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import rails.game.move.MoveableHolderI;
import rails.game.move.ObjectMove;
import rails.util.LocalText;

public class PublicCertificate implements PublicCertificateI, Cloneable {

    /** From which public company is this a certificate */
    protected PublicCompanyI company;
    /**
     * Share percentage represented by this certificate
     */
    protected int shares;
    /** President's certificate? */
    protected boolean president;
    /** Availability at the start of the game */
    protected boolean initiallyAvailable;
    /** Current holder of the certificate */
    protected Portfolio portfolio;

    /** A key identifying the certificate's unique type */
    protected String certTypeId;

    /** A key identifying the certificate's unique ID */
    protected String certId;

    /** A map alllowing to find certificates by unique id */
    protected static Map<String, PublicCertificateI> certMap =
            new HashMap<String, PublicCertificateI>();

    protected static Logger log =
            Logger.getLogger(PublicCertificate.class.getPackage().getName());

    public PublicCertificate(int shares) {
        this(shares, false, true);
    }

    public PublicCertificate(int shares, boolean president) {
        this(shares, president, true);
    }

    public PublicCertificate(int shares, boolean president, boolean available) {
        this.shares = shares;
        this.president = president;
        this.initiallyAvailable = available;
    }

    public void setUniqueId(String name, int index) {
        certId = name + "-" + index;
        certMap.put(certId, this);
    }

    public String getUniqueId() {
        return certId;
    }

    public static PublicCertificateI getByUniqueId(String certId) {
        return certMap.get(certId);
    }

    public void moveTo(MoveableHolderI newHolder) {
        new ObjectMove(this, portfolio, newHolder);
    }

    /**
     * @return Portfolio this certificate belongs to.
     */
    public Portfolio getPortfolio() {
        return portfolio;
    }

    public MoveableHolderI getHolder() {
        return portfolio;
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
    public String getName() {
        int share = getShare();
        if (share == 100) {
            /* Applies to shareless minors: just name the company */
            return company.getName();
        } else if (president) {
            return LocalText.getText("PRES_CERT_NAME",
                    company.getName(),
                    getShare() );
        } else {
            return LocalText.getText("CERT_NAME",
                    company.getName(),
                    getShare());
        }
    }

    /**
     * @param b
     */
    public boolean isInitiallyAvailable() {
        return initiallyAvailable;
    }

    /**
     * @param portfolio
     */
    public void setPortfolio(Portfolio portfolio) {
        this.portfolio = portfolio;
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
    public PublicCompanyI getCompany() {
        return company;
    }

    /**
     * @param companyI
     */
    public void setCompany(PublicCompanyI companyI) {
        company = companyI;
        certTypeId = company.getName() + "_" + getShare() + "%";
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
            log.fatal("Cannot clone certificate:", e);
            return null;
        }
    }

    public PublicCertificateI copy() {
        return (PublicCertificateI) this.clone();
    }

    /**
     * Two certificates are "equal" if they both belong to the same company,
     * represent the same share percentage, and are not a president share.
     *
     * @param cert Public company certificate to compare with.
     * @return True if the certs are "equal" in the defined sense.
     */
    public boolean equals(PublicCertificateI cert) {
        return (cert != null && getCompany() == cert.getCompany()
                && isPresidentShare() == cert.isPresidentShare() && getShares() == cert.getShares());
    }

    @Override
    public String toString() {
        return "PublicCertificate: " + getName();
    }
}
