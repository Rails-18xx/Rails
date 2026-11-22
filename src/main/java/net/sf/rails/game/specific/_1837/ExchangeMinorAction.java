package net.sf.rails.game.specific._1837;

import java.awt.Color;
import java.io.IOException;
import java.io.ObjectInputStream;

import com.google.common.base.Objects;

import net.sf.rails.game.CompanyManager;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.state.Owner;
import rails.game.action.GuiTargetedAction;
import rails.game.action.PossibleAction;

/**
 * Generic action for exchanging a minor for a share in a major.
 * Modeled strictly after StartPrussian.java to ensure correct UI visualization.
 */
public class ExchangeMinorAction extends PossibleAction implements GuiTargetedAction {

    private static final long serialVersionUID = 1L;
    
    // Transient references to the objects
    private transient PublicCompany minor;
    private transient PublicCompany targetMajor;
    
    // Persisted IDs for reconstruction
    private String minorId;
    private String targetMajorId;
    private boolean isFormation;

    public ExchangeMinorAction(PublicCompany minor, PublicCompany targetMajor, boolean isFormation) {
        super(minor.getRoot());
        this.minor = minor;
        this.targetMajor = targetMajor;
        this.minorId = minor.getId();
        this.targetMajorId = targetMajor.getId();
        this.isFormation = isFormation;
    }

    public PublicCompany getMinor() {
        if (minor == null) resolveCompanies();
        return minor;
    }

    /**
     * Required by ORPanel to display the acting player's name in the header.
     */
    @Override
    public String getPlayerName() {
        PublicCompany m = getMinor();
        if (m != null && m.getPresident() != null) {
            return m.getPresident().getName();
        }
        return "";
    }
    
    public PublicCompany getTargetMajor() {
        if (targetMajor == null) resolveCompanies();
        return targetMajor;
    }
    
    public boolean isFormation() {
        return isFormation;
    }

    @Override
    public Owner getActor() {
        // Lazy Load: Ensure minor is not null before returning.
        // This fixes the "Active Company (Reflect): None" issue in ORPanel
        // where serialization timing left 'minor' as null.
        if (minor == null) {
            resolveCompanies();
        }
        return minor;
    }
    
    // Explicitly implement getTarget to match DiscardTrain pattern
    @Override
    public Object getTarget() {
        return getActor();
    }

    @Override
    public String getGroupLabel() {
        return "Exchange " + minorId;
    }

    @Override
    public String getButtonLabel() {
        if (isFormation) {
            return "Form " + targetMajorId + " (" + minorId + ")";
        }
        return "Exchange " + minorId + " for " + targetMajorId;
    }


    @Override
    public Color getButtonColor() {
        return new Color(152, 251, 152); // PaleGreen
    }

    @Override
    public Color getHighlightBackgroundColor() {
        return new Color(152, 251, 152); // PaleGreen
    }

    @Override
    public Color getHighlightBorderColor() {
        return new Color(34, 139, 34); // ForestGreen
    }

    @Override
    public Color getHighlightTextColor() {
        return Color.BLACK; 
    }

    // --- Serialization & Equality ---

    @Override
    public boolean equalsAs(PossibleAction pa, boolean asOption) {
        if (pa == this) return true;
        if (!super.equalsAs(pa, asOption)) return false;
        ExchangeMinorAction other = (ExchangeMinorAction) pa;
        return Objects.equal(this.minorId, other.minorId)
            && Objects.equal(this.targetMajorId, other.targetMajorId)
            && this.isFormation == other.isFormation;
    }
    
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        // Attempt immediate resolution, but don't fail if root isn't ready
        resolveCompanies();
    }
    
    /**
     * Helper to restore transient fields from IDs.
     * Safe to call multiple times.
     */
    private void resolveCompanies() {
        if (minor == null && minorId != null) {
            CompanyManager cm = getCompanyManager();
            if (cm != null) {
                this.minor = cm.getPublicCompany(minorId);
            }
        }
        if (targetMajor == null && targetMajorId != null) {
            CompanyManager cm = getCompanyManager();
            if (cm != null) {
                this.targetMajor = cm.getPublicCompany(targetMajorId);
            }
        }
    }
    
    @Override
    public String toString() {
        return String.format("ExchangeMinorAction [minor=%s, target=%s, formation=%s]", 
                minorId, targetMajorId, isFormation);
    }
}