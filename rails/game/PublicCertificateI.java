/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/PublicCertificateI.java,v 1.12 2010/06/17 21:35:54 evos Exp $ */
package rails.game;

import rails.game.move.Moveable;

public interface PublicCertificateI extends Certificate, Moveable {

    /** Set the certificate's unique ID, for use in deserializing */
    public void setUniqueId(String name, int index);

    /** Set the certificate's unique ID */
    public String getUniqueId();
    
    public int getIndexInCompany();

    /**
     * @return if this certificate is a president's share
     */
    public boolean isPresidentShare();

    /**
     * Get the number of shares that this certificate represents.
     *
     * @return The number of shares.
     */
    public int getShares();

    /**
     * Get the percentage of ownership that this certificate represents. This is
     * equal to the number of shares * the share unit.
     *
     * @return The share percentage.
     */
    public int getShare();

    /**
     * Get the current price of this certificate.
     *
     * @return The current certificate price.
     */
    //public int getCertificatePrice();

    public void setInitiallyAvailable(boolean initiallyAvailable);
    /**
     * @param b
     */
    public boolean isInitiallyAvailable();

    public float getCertificateCount();

    public void setCertificateCount(float certificateCount);

    /**
     * @param portfolio
     */
    public void setPortfolio(Portfolio portfolio);

    /**
     * @param b
     */
    public void setPresident(boolean b);

    /**
     * @return
     */
    public PublicCompanyI getCompany();

    /**
     * @param companyI
     */
    public void setCompany(PublicCompanyI companyI);

    public String getTypeId();

    /** Clone this certificate */
    public PublicCertificateI copy();

    /**
     * Compare certificates
     *
     * @param cert Another publoc certificate.
     * @return TRUE if the certificates are of the same company and represent
     * the same number of shares.
     */
    public boolean equals(PublicCertificateI cert);

}
