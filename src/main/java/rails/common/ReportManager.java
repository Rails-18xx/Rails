package rails.common;

import rails.game.RailsManager;
import rails.game.RailsRoot;

/**
 * ReportManager controls the (non-action) communication with the client. 
 * 
 * Specific task:
 * Handling of the ReportBuffer and the DisplayBuffer
 */
public class ReportManager extends RailsManager {

    private final DisplayBuffer displayBuffer = DisplayBuffer.create(this, "displayBuffer");
    private final ReportBuffer reportBuffer = ReportBuffer.create(this, "reportBuffer");
    
    private ReportManager(RailsRoot parent, String id) {
        super(parent, id);
        parent.getStateManager().getChangeStack().addChangeReporter(reportBuffer);
    }

    public static ReportManager create(RailsRoot parent, String id) {
        return new ReportManager(parent, id);
    }
    
    public DisplayBuffer getDisplayBuffer() {
        return displayBuffer;
    }
    
    public ReportBuffer getReportBuffer() {
        return reportBuffer;
    }
    
}
