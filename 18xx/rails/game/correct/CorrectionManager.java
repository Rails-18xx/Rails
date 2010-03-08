package rails.game.correct;

import rails.game.GameManager;
import java.util.List;

/**
 * CorrectionManager is the interface for the specific CorrectionManagers
 * Corrections use the (abstract) factory pattern.
 * @author freystef
 *
 */

public interface CorrectionManager {
    
    public boolean isActive();
    
    public List<CorrectionAction> createCorrections();
    
    public boolean executeCorrection(CorrectionAction action);
    
}
