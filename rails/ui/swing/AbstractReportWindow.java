package rails.ui.swing;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

import rails.util.Config;
import rails.util.LocalText;

public abstract class AbstractReportWindow extends JFrame {
    private static final long serialVersionUID = 1L;
    
    // can be set to false, than it cannot be closed
    protected boolean closeable = true;

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
        
        setVisible("yes".equalsIgnoreCase(Config.get("report.window.open")));
    }
    
    public abstract void updateLog();
    
    public abstract void scrollDown();

}