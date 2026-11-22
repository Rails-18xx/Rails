package net.sf.rails.game.specific._1817;

import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.financial.PublicCertificate;

public class ShortCertificate extends PublicCertificate {
    private static final long serialVersionUID = 1L;

    public ShortCertificate(PublicCompany company, int index) {
        // index ensures a unique ID (e.g., "PRR_SHORT_1") for sorting
        super(company, company.getId() + "_SHORT_" + index, 1, false, false, 0f, index);
        setCompany(company); // Required for PublicCertificate.compareTo()
    }


    @Override
    public PublicCompany getCompany() {
        // Fallback: If the protected 'company' field is not yet set (during construction), 
        // retrieve the parent which the constructor passed as the company.
        PublicCompany c = super.getCompany();
        if (c == null && getParent() instanceof PublicCompany) {
            return (PublicCompany) getParent();
        }
        return c;
    }

    @Override
    public String getUniqueId() {
        return getId();
    }
    
    @Override
    public String toString() {
        PublicCompany c = getCompany();
        // Null-safe ID check to prevent crashes during state-manager initialization
        return "Short " + (c != null ? c.getId() : "??") + " (" + getId() + ")";
    }

}