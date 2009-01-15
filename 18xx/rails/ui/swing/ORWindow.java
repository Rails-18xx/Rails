/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/ORWindow.java,v 1.22 2009/01/15 20:53:28 evos Exp $*/
package rails.ui.swing;

import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.apache.log4j.Logger;

import rails.common.Defs;
import rails.game.*;
import rails.game.action.*;
import rails.util.LocalText;

/**
 * This Window displays the available operations that may be performed during an
 * Operating Round. This window also contains the Game Map.
 */
public class ORWindow extends JFrame implements ActionPerformer {
    private static final long serialVersionUID = 1L;
    public GameUIManager gameUIManager;
    private ORUIManager orUIManager;
    private MapPanel mapPanel;
    private ORPanel orPanel;
    private UpgradesPanel upgradePanel;
    private MessagePanel messagePanel;

    private Rectangle lastBounds;

    protected PossibleActions possibleActions = PossibleActions.getInstance();

    List<LayTile> allowedTileLays = new ArrayList<LayTile>();
    List<LayToken> allowedTokenLays = new ArrayList<LayToken>();

    protected static Logger log =
            Logger.getLogger(ORWindow.class.getPackage().getName());

    public ORWindow(GameUIManager gameUIManager) {
        super();
        this.gameUIManager = gameUIManager;

        String orUIManagerClassName = gameUIManager.getClassName(Defs.ClassName.OR_UI_MANAGER);
        try {
            Class<? extends ORUIManager> orUIManagerClass =
                Class.forName(orUIManagerClassName).asSubclass(ORUIManager.class);
            log.debug("Class is "+orUIManagerClass.getName());
            orUIManager = orUIManagerClass.newInstance();
        } catch (Exception e) {
            log.fatal("Cannot instantiate class " + orUIManagerClassName, e);
            System.exit(1);
        }

        getContentPane().setLayout(new BorderLayout());

        messagePanel = new MessagePanel();
        getContentPane().add(messagePanel, BorderLayout.NORTH);

        mapPanel = new MapPanel(orUIManager);
        getContentPane().add(mapPanel, BorderLayout.CENTER);

        upgradePanel = new UpgradesPanel(orUIManager);
        getContentPane().add(upgradePanel, BorderLayout.WEST);
        addMouseListener(upgradePanel);

        orPanel = new ORPanel(this, orUIManager);
        getContentPane().add(orPanel, BorderLayout.SOUTH);

        orUIManager.init(this);

        setTitle(LocalText.getText("MapWindowTitle"));
        setLocation(10, 10);
        setVisible(false);
        setSize(800, 600);

        final JFrame frame = this;
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                StatusWindow.uncheckMenuItemBox(StatusWindow.MAP_CMD);
                frame.dispose();
            }
        });

        gameUIManager.reportWindow.addLog();
    }

    public ORUIManager getORUIManager() {
        return orUIManager;
    }

    public GameUIManager getGameUIManager() {
        return gameUIManager;
    }

    public MapPanel getMapPanel() {
        return mapPanel;
    }

    public ORPanel getORPanel() {
        return orPanel;
    }

    public UpgradesPanel getUpgradePanel() {
        return upgradePanel;
    }

    public ORUIManager getOrUIManager() {
        return orUIManager;
    }

    public MessagePanel getMessagePanel() {
        return messagePanel;
    }

    public boolean process(PossibleAction action) {

        // Add the actor for safety checking in the server
        action.setPlayerName(orPanel.getORPlayer());
        // Process the action
        boolean result = gameUIManager.processOnServer(action);
        // Display any error message
        displayServerMessage();

        return result;
    }

    // Not yet used
    public boolean processImmediateAction() {
        return true;
    }

    public void displayServerMessage() {
        String[] message = DisplayBuffer.get();
        if (message != null) {
            JOptionPane.showMessageDialog(this, message);
        }
    }

    public void displayORUIMessage(String message) {
        if (message != null) {
            JOptionPane.showMessageDialog(this, message);
        }
    }

    public void repaintORPanel() {
        orPanel.revalidate();
    }

    public void activate(OperatingRound or) {
		GameManager gameManager = (GameManager) gameUIManager.getGameManager();
		String compositeORNumber = gameManager.getCompositeORNumber ();
		String numORs = gameManager.getNumOfORs ();

        orPanel.recreate(or);
        setTitle(LocalText.getText("MapWindowORTitle",
								   compositeORNumber,
								   numORs ));
        pack();
        if (lastBounds != null) {
            Rectangle newBounds = getBounds();
            lastBounds.width = newBounds.width;
            setBounds (lastBounds);
        }
        setVisible(true);
        requestFocus();
    }

    public void updateStatus() {
        orUIManager.updateStatus();
        requestFocus();
    }

    /**
     * Round-end settings
     *
     */
    public void finish() {
        lastBounds = getBounds();
        orPanel.finish();
        upgradePanel.finish();
        setTitle(LocalText.getText("MapWindowTitle"));
    }
}
