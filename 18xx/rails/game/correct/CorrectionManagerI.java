package rails.game.correct;

import java.util.List;

/**
 * CorrectionManagerI is the interface for the specific CorrectionManagers
 * Corrections use the (abstract) factory pattern.
 * @author freystef
 *
 */

public interface CorrectionManagerI {
    
    public CorrectionType getCorrectionType();

    public boolean isActive();
    
    public List<CorrectionAction> createCorrections();
    
    public boolean executeCorrection(CorrectionAction action);
    
}
