package net.sf.rails.common;

import lombok.Getter;
import net.sf.rails.game.RailsManager;
import net.sf.rails.game.RailsRoot;

/**
 * ReportManager controls the (non-action) communication with the client. 
 * 
 * Specific task:
 * Handling of the ReportBuffer and the DisplayBuffer
 */
public class ReportManager extends RailsManager {

    @Getter
    private final DisplayBuffer displayBuffer = DisplayBuffer.create(this, "displayBuffer");

    @Getter
    private final ReportBuffer reportBuffer = ReportBuffer.create(this, "reportBuffer");
    
    public ReportManager(RailsRoot parent, String id) {
        super(parent, id);

        parent.getStateManager().getChangeStack().addChangeReporter(reportBuffer);
    }
}
