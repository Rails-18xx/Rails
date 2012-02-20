package rails.ui.swing;

import java.awt.Rectangle;
import java.awt.event.*;

import javax.swing.JFrame;

import rails.common.Config;
import rails.common.LocalText;

public abstract class AbstractReportWindow extends JFrame {
    private static final long serialVersionUID = 1L;

    protected GameUIManager gameUIManager;

    // can be set to false, than it cannot be closed
    protected boolean closeable = true;

    public AbstractReportWindow (GameUIManager gameUIManager) {
        this.gameUIManager = gameUIManager;
    }

    public void init() {
        setSize(400, 400);
        setLocation(600, 400);
        setTitle(LocalText.getText("GameReportTitle"));

        final JFrame frame = this;
        this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (!closeable) return;
                StatusWindow.uncheckMenuItemBox(StatusWindow.REPORT_CMD);
                frame.dispose();
            }
        });
        final GameUIManager guiMgr = gameUIManager;
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent e) {
                guiMgr.getWindowSettings().set(frame);
            }
            @Override
            public void componentResized(ComponentEvent e) {
                guiMgr.getWindowSettings().set(frame);
            }
        });

        gameUIManager.packAndApplySizing(this);

        gameUIManager.setMeVisible(this,
                "yes".equalsIgnoreCase(Config.get("report.window.open")));
    }

    public abstract void updateLog();

    public abstract void scrollDown();

}