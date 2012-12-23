/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/ORWindow.java,v 1.36 2010/06/24 21:48:08 stefanfrey Exp $*/
package rails.ui.swing;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

import rails.common.Config;
import rails.common.GuiDef;
import rails.common.LocalText;
import rails.game.GameManager;
import rails.game.OperatingRound;
import rails.game.action.*;
import rails.ui.swing.elements.DockingFrame;

/**
 * This Window displays the available operations that may be performed during an
 * Operating Round. This window also contains the Game Map.
 */
public class ORWindow extends DockingFrame implements ActionPerformer {
    private static final long serialVersionUID = 1L;
    protected GameUIManager gameUIManager;
    protected ORUIManager orUIManager;
    protected MapPanel mapPanel;
    protected ORPanel orPanel;
    protected UpgradesPanel upgradePanel;
    protected MessagePanel messagePanel;

    protected Rectangle lastBounds;

    protected PossibleActions possibleActions = PossibleActions.getInstance();

    List<LayTile> allowedTileLays = new ArrayList<LayTile>();
    List<LayToken> allowedTokenLays = new ArrayList<LayToken>();

    protected static Logger log =
            Logger.getLogger(ORWindow.class.getPackage().getName());

    public ORWindow(GameUIManager gameUIManager,SplashWindow splashWindow) {
        super( "yes".equals(Config.get("or.window.dockablePanels")) , splashWindow );
        this.gameUIManager = gameUIManager;

        splashWindow.notifyOfStep(SplashWindow.STEP_OR_INIT_PANELS);
        
        String orUIManagerClassName = gameUIManager.getClassName(GuiDef.ClassName.OR_UI_MANAGER);
        try {
            Class<? extends ORUIManager> orUIManagerClass =
                Class.forName(orUIManagerClassName).asSubclass(ORUIManager.class);
            log.debug("Class is "+orUIManagerClass.getName());
            orUIManager = orUIManagerClass.newInstance();
        } catch (Exception e) {
            log.fatal("Cannot instantiate class " + orUIManagerClassName, e);
            System.exit(1);
        }
        gameUIManager.setORUIManager(orUIManager);
        orUIManager.setGameUIManager(gameUIManager);

        messagePanel = new MessagePanel();
        JScrollPane messagePanelSlider = new JScrollPane(messagePanel);
        messagePanel.setParentSlider(messagePanelSlider);

        upgradePanel = new UpgradesPanel(orUIManager,isDockingFrameworkEnabled());
        addMouseListener(upgradePanel);

        mapPanel = new MapPanel(gameUIManager);

        orPanel = new ORPanel(this, orUIManager);

        //create docking / conventional layout
        if (isDockingFrameworkEnabled()) {

            //set up the button panel (which is separated from its OR panel parent)
            //adding upgrade panel buttons on top
            orPanel.addToButtonPanel(upgradePanel.getButtons(),0);
            
            //initialize remaining tile panel as it is no optional part in the docking layout
            splashWindow.notifyOfStep(SplashWindow.STEP_OR_INIT_TILES);
            RemainingTilesWindow rtw = new RemainingTilesWindow(this);
            orUIManager.setRemainingTilesWindows(rtw);
            JScrollPane remainingTilesPanelSlider = rtw.getScrollPane();
            
            //generate layout
            splashWindow.notifyOfStep(SplashWindow.STEP_OR_APPLY_DOCKING_FRAME);
            addDockable ( messagePanelSlider, 
                    "Dockable.orWindow.messagePanel",
                    0, 0, 100, 10, DockableProperty.closeable);
            addDockable ( upgradePanel, 
                    "Dockable.orWindow.upgradePanel",
                    0, 10, 20, 70, DockableProperty.standard);
            addDockable ( mapPanel, 
                    "Dockable.orWindow.mapPanel",
                    20, 10, 80, 70, DockableProperty.standard);
            addDockable ( remainingTilesPanelSlider, 
                    "Dockable.orWindow.remainingTilesPanel",
                    100, 0, 120, 100, DockableProperty.initially_hidden);
            addDockable ( orPanel, 
                    "Dockable.orWindow.orPanel",
                    0, 80, 100, 15, DockableProperty.standard);
            addDockable ( orPanel.getButtonPanel(), 
                    "Dockable.orWindow.buttonPanel",
                    0, 95, 100, 5, DockableProperty.standard);
            deployDockables();

            //take over or panel's menu bar as the frame menu bar
            JMenuBar menuBar = orPanel.getMenuBar();
            addDockingFrameMenu(menuBar);
            setJMenuBar( menuBar );
            
        } else {
            // CONVENTIONAL LAYOUT
            
            getContentPane().setLayout(new BorderLayout());
            getContentPane().add(messagePanelSlider, BorderLayout.NORTH);
            getContentPane().add(mapPanel, BorderLayout.CENTER);
            getContentPane().add(upgradePanel, BorderLayout.WEST);
            getContentPane().add(orPanel, BorderLayout.SOUTH);
            
        }

        orUIManager.init(this);

        setTitle(LocalText.getText("MapWindowTitle"));
        setLocation(10, 10);
        setVisible(false);

        // make map and upgrade panels invisible for noMapMode
        if (gameUIManager.getGameParameterAsBoolean(GuiDef.Parm.NO_MAP_MODE)) {
            mapPanel.setVisible(false);
            upgradePanel.setVisible(false);
            setSize(800, 500);
        } else
            setSize(800, 600);

        log.debug("OrWindow: MapPanel size = " + mapPanel.getSize());
        log.debug("OrWindow size = " + this.getSize());

        final JFrame frame = this;
        final GameUIManager guiMgr = gameUIManager;
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveLayout();
                StatusWindow.uncheckMenuItemBox(StatusWindow.MAP_CMD);
                if (!isDockingFrameworkEnabled()) {
                    frame.dispose();
                } else {
                    setVisible(false);
                }
            }
        });
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

        getGameUIManager().reportWindow.updateLog();

        //call pack and size init within the swing EDT
        //(as this frame is not inited within the EDT)
        //no standard call to gameUIManager's packAndApplySizing possible (due to docking switch)
        SwingUtilities.invokeLater(new Thread() {
            public void run() {
                //rearrange layout only if no docking framework active
                if (!isDockingFrameworkEnabled()) {
                    pack();
                } else {
                    setSize(new Dimension(600,500));
                }

                WindowSettings ws = getGameUIManager().getWindowSettings();
                Rectangle bounds = ws.getBounds(ORWindow.this);
                if (bounds.x != -1 && bounds.y != -1) setLocation(bounds.getLocation());
                if (bounds.width != -1 && bounds.height != -1) setSize(bounds.getSize());
                ws.set(frame);

                if (isDockingFrameworkEnabled()) {
                    initLayout();
                }
            }            
        });

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

    public MessagePanel getMessagePanel() {
        return messagePanel;
    }

    public boolean process(PossibleAction action) {

        // Add the actor for safety checking in the server
        if (action != null) action.setPlayerName(orPanel.getORPlayer());
        // Process the action
        boolean result = gameUIManager.processAction(action);
        // Display any error message
        //displayServerMessage();

        return result;
    }

    // Not yet used
    public boolean processImmediateAction() {
        return true;
    }

    public void displayORUIMessage(String message) {
        if (message != null) {
            JOptionPane.showMessageDialog(this, message);
        }
    }

    public void repaintORPanel() {
        //rearrange layout only if no docking framework active
        if (!isDockingFrameworkEnabled()) {
            orPanel.revalidate();
        }
    }

    public void activate(OperatingRound or) {
        GameManager gameManager = (GameManager) gameUIManager.getGameManager();
        String numORs = gameManager.getNumOfORs ();

        orPanel.recreate(or);
        setTitle(LocalText.getText("MapWindowORTitle",
                gameManager.getORId(),
                String.valueOf(gameManager.getRelativeORNumber()),
                numORs ));
        
        //rearrange layout only if no docking framework active
        if (!isDockingFrameworkEnabled()) {
            pack();
            if (lastBounds != null) {
                Rectangle newBounds = getBounds();
                lastBounds.width = newBounds.width;
                setBounds (lastBounds);
            }
        }
        
        setVisible(true);
        requestFocus();
    }

    public void updateStatus(boolean myTurn) {
        // Safety check. Do nothing if this method is called outside Operating Rounds,
        // for instance when a token is exchanged during a Stock Round.
        if (!(gameUIManager.getCurrentRound() instanceof OperatingRound)) return;

        orUIManager.updateStatus(myTurn);
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
        messagePanel.setMessage("");
        setTitle(LocalText.getText("MapWindowTitle"));
    }

    protected String getLayoutFileName() {
        return getClass().getSimpleName() + "_" 
                + gameUIManager.getGameManager().getGameName() ;
    }

}
