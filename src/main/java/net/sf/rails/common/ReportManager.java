package net.sf.rails.common;

import net.sf.rails.game.RailsManager;
import net.sf.rails.game.RailsRoot;

/**
 * ReportManager controls the (non-action) communication with the client.
 * <p>
 * Specific task:
 * Handling of the ReportBuffer and the DisplayBuffer
 */
public class ReportManager extends RailsManager {

    private final DisplayBuffer displayBuffer = new DisplayBuffer(this, "displayBuffer");

    private final ReportBuffer reportBuffer = new ReportBuffer(this, "reportBuffer");

    public ReportManager(RailsRoot parent, String id) {
        super(parent, id);

        parent.getStateManager().getChangeStack().addChangeReporter(reportBuffer);
    }

    public DisplayBuffer getDisplayBuffer() {
        return displayBuffer;
    }

    public ReportBuffer getReportBuffer() {
        return reportBuffer;
    }
}
