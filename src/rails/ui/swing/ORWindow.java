/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/ORWindow.java,v 1.36 2010/06/24 21:48:08 stefanfrey Exp $*/
package rails.ui.swing;

import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bibliothek.gui.DockController;
import bibliothek.gui.Dockable;
import bibliothek.gui.DockStation;
import bibliothek.gui.dock.ScreenDockStation;
import bibliothek.gui.dock.SplitDockStation;
import bibliothek.gui.dock.common.CControl;
import bibliothek.gui.dock.common.CGrid;
import bibliothek.gui.dock.common.CStation;
import bibliothek.gui.dock.common.DefaultSingleCDockable;
import bibliothek.gui.dock.common.action.predefined.CBlank;
import bibliothek.gui.dock.common.event.CDockableStateListener;
import bibliothek.gui.dock.common.intern.CDockable;
import bibliothek.gui.dock.common.intern.DefaultCDockable;
import bibliothek.gui.dock.common.intern.ui.CSingleParentRemover;
import bibliothek.gui.dock.common.mode.ExtendedMode;
import bibliothek.gui.dock.common.theme.ThemeMap;
import bibliothek.gui.dock.event.DockStationAdapter;
import bibliothek.gui.dock.station.LayoutLocked;

import rails.common.GuiDef;
import rails.common.LocalText;
import rails.common.parser.Config;
import rails.game.GameManager;
import rails.game.OperatingRound;
import rails.game.action.*;

/**
 * This Window displays the available operations that may be performed during an
 * Operating Round. This window also contains the Game Map.
 */
public class ORWindow extends JFrame implements ActionPerformer {
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

    CControl orWindowControl = null;

    private static final String layoutFolderName = "DockableLayout";
    private static final String layoutFileSuffix = "_layout.rails_ini";
    
    protected static Logger log =
            LoggerFactory.getLogger(ORWindow.class);

    public ORWindow(GameUIManager gameUIManager) {
        super();
        this.gameUIManager = gameUIManager;

        String orUIManagerClassName = gameUIManager.getClassName(GuiDef.ClassName.OR_UI_MANAGER);
        try {
            Class<? extends ORUIManager> orUIManagerClass =
                Class.forName(orUIManagerClassName).asSubclass(ORUIManager.class);
            log.debug("Class is "+orUIManagerClass.getName());
            orUIManager = orUIManagerClass.newInstance();
        } catch (Exception e) {
            log.error("Cannot instantiate class " + orUIManagerClassName, e);
            System.exit(1);
        }
        gameUIManager.setORUIManager(orUIManager);
        orUIManager.setGameUIManager(gameUIManager);

        messagePanel = new MessagePanel();
        JScrollPane slider = new JScrollPane(messagePanel);
        messagePanel.setParentSlider(slider);

        upgradePanel = new UpgradesPanel(orUIManager);
        addMouseListener(upgradePanel);

        mapPanel = new MapPanel(gameUIManager);

        orPanel = new ORPanel(this, orUIManager);
        
        //create docking / conventional layout depending config
        if (isDockablePanelsEnabled()) {
            //DOCKABLE LAYOUT

            //build the docking layout
            orWindowControl = new CControl( this );
            orWindowControl.setTheme( ThemeMap.KEY_SMOOTH_THEME );
            add( orWindowControl.getContentArea() );
            CGrid orWindowLayout = new CGrid( orWindowControl );
            
            //ensure that externalized dockables get a split station as parent
            //necessary, otherwise externalized dockables cannot be docked together
            alwaysAddStationsToExternalizedDockables(orWindowControl);
            
            //set docks tooltip language
            if ("en_us".equalsIgnoreCase(Config.get("locale"))) {
                //hard setting to default in case of US as this is DockingFrames default language
                //don't use Locale constant as it is en_US (case sensitive)
                orWindowControl.setLanguage(new Locale(""));
            }

            //add message panel
            String dockableName = LocalText.getText("DockableTitle.orWindow.messagePanel");
            DefaultSingleCDockable singleDockable = new DefaultSingleCDockable( dockableName, dockableName );
            singleDockable.add( slider, BorderLayout.CENTER );
            singleDockable.setCloseable( false );
            orWindowLayout.add( 0, 0, 100, 10, singleDockable );
            
            //add upgrade panel
            dockableName = LocalText.getText("DockableTitle.orWindow.upgradePanel");
            singleDockable = new DefaultSingleCDockable( dockableName, dockableName );
            singleDockable.add( upgradePanel, BorderLayout.CENTER );
            singleDockable.setCloseable( false );
            orWindowLayout.add( 0, 10, 20, 70, singleDockable );
    
            //add map panel
            dockableName = LocalText.getText("DockableTitle.orWindow.mapPanel");
            singleDockable = new DefaultSingleCDockable( dockableName, dockableName );
            singleDockable.add( mapPanel, BorderLayout.CENTER );
            singleDockable.setCloseable( false );
            orWindowLayout.add( 20, 10, 80, 70, singleDockable );
    
            //add or panel
            dockableName = LocalText.getText("DockableTitle.orWindow.orPanel");
            singleDockable = new DefaultSingleCDockable( dockableName, dockableName );
            singleDockable.add( orPanel, BorderLayout.CENTER );
            singleDockable.setCloseable( false );
            orWindowLayout.add( 0, 80, 100, 15, singleDockable );
    
            //add button panel of or panel
            dockableName = LocalText.getText("DockableTitle.orWindow.buttonPanel");
            singleDockable = new DefaultSingleCDockable( dockableName, dockableName );
            singleDockable.add( orPanel.getButtonPanel(), BorderLayout.CENTER );
            singleDockable.setCloseable( false );
            orWindowLayout.add( 0, 95, 100, 5, singleDockable );
            
            orWindowControl.getContentArea().deploy( orWindowLayout );
            
        } else {
            // CONVENTIONAL LAYOUT
            
            getContentPane().setLayout(new BorderLayout());
            getContentPane().add(slider, BorderLayout.NORTH);
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
                saveDockableLayout();
                StatusWindow.uncheckMenuItemBox(StatusWindow.MAP_CMD);
                frame.dispose();
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

        //rearrange layout only if no docking framework active
        if (!isDockablePanelsEnabled()) {
            pack();
        }

        WindowSettings ws = gameUIManager.getWindowSettings();
        Rectangle bounds = ws.getBounds(this);
        if (bounds.x != -1 && bounds.y != -1) setLocation(bounds.getLocation());
        if (bounds.width != -1 && bounds.height != -1) setSize(bounds.getSize());
        ws.set(frame);

        gameUIManager.reportWindow.updateLog();
        
        //dockable panes: restore former layout (depending on game variant)
        loadDockableLayout();
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
        if (!isDockablePanelsEnabled()) {
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
        if (!isDockablePanelsEnabled()) {
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
    
    public boolean isDockablePanelsEnabled() {
        return "yes".equals(Config.get("or.window.dockablePanels"));
    }
    
    
    private String getLayoutName() {
        return getClass().getSimpleName() + "_" 
                + gameUIManager.getGameManager().getGameName() ;
    }
    
    private File getLayoutFile() {
        try {
            //get layout folder (and ensure that it is available)
            File layoutFolder = new File(Config.get("save.directory"),layoutFolderName);
            if (!layoutFolder.isDirectory()) {
                layoutFolder.mkdirs();
            }
            File layoutFile = new File(layoutFolder, 
                    getLayoutName() + layoutFileSuffix );
            return layoutFile;
        } catch (Exception e) {
            //return no valid file if anything goes wrong
            return null;
        }
    }

    public void saveDockableLayout() {
        if (!isDockablePanelsEnabled()) return;

        File layoutFile = getLayoutFile();
        if (layoutFile != null) {
            try {
                orWindowControl.save(getLayoutName());
                orWindowControl.writeXML(layoutFile);
            } catch (Exception e) {} //skip in case of issue
        }
    }
    
    private void loadDockableLayout() {
        if (!isDockablePanelsEnabled()) return;
        
        File layoutFile = getLayoutFile();
        if (layoutFile != null) {
            try {
                orWindowControl.readXML(layoutFile);
                orWindowControl.load(getLayoutName());
            } catch (Exception e) {} //skip if layout not found
        }
        
        //ensure that all dockables that are externalized according to layout
        //information don't have the deault maximize button (as it won't work
        //for the adjusted externalization setup)
        for (int i = 0 ; i < orWindowControl.getCDockableCount() ; i++ ) {
            CDockable d = orWindowControl.getCDockable(i);
            if (d instanceof DefaultCDockable) {
                DefaultCDockable dd = (DefaultCDockable)d;
                if (ExtendedMode.EXTERNALIZED.equals(d.getExtendedMode())) {
                    dd.putAction( CDockable.ACTION_KEY_MAXIMIZE, CBlank.BLANK );
                }
            }
        }
    }

    /**
     * The behavior of the specified CControl is altered by the following:
     * If a dockable is detached / externalized, it would normally put directly
     * under the ScreenDockStation - thus inhibiting any docking to/from this
     * dockable. This is changed such that a split station (that would allow for
     * that) is put in between the ScreenDockStation and the Dockable. 
     */
    private void alwaysAddStationsToExternalizedDockables(CControl cc) {

        // access the DockStation which shows our detached (externalized) items
        CStation<?> screen = (CStation<?>) 
                cc.getStation( CControl.EXTERNALIZED_STATION_ID );
        
        // remove the standard maximize action when externalizing
        // and adds it back when unexternalizing
        // (as maximize won't work for the adjusted externalization setup)
        cc.addStateListener( new CDockableStateListener() {
            public void visibilityChanged( CDockable cd ){
                // ignore
            }
     
            public void extendedModeChanged( CDockable cd, ExtendedMode mode ){
                if( cd instanceof DefaultCDockable ) {
                    DefaultCDockable dockable = (DefaultCDockable) cd;
                    if( mode.equals( ExtendedMode.EXTERNALIZED ) ) {
                        dockable.putAction( CDockable.ACTION_KEY_MAXIMIZE, CBlank.BLANK );
                    }
                    else {
                        dockable.putAction( CDockable.ACTION_KEY_MAXIMIZE, null );
                    }
                }
            }
        });
        
        // if a Dockable is added to that station...
        screen.getStation().addDockStationListener( new ScreenDockStationListener());
 
        // make sure a SplitDockStation with one child and a parent 
        // that is a ScreenDockStation does not get removed
        cc.intern().getController().setSingleParentRemover( 
                new CSingleParentRemover( cc ){
            @Override
            protected boolean shouldTest( DockStation station ){
                if( station instanceof SplitDockStation ) {
                    SplitDockStation split = (SplitDockStation) station;
                    if( split.getDockParent() instanceof ScreenDockStation ) {
                        // but we want to remove the station if it does 
                        // not have any children at all
                        return split.getDockableCount() == 0;
                    }
                }
                return super.shouldTest( station );
            }
        } );
    }
    
    @LayoutLocked(locked = false)
    private class ScreenDockStationListener extends DockStationAdapter {
        public void dockableAdded( DockStation station, final Dockable dockable ){
            // ... and the new child is not a SplitDockStation ...
            if( !(dockable instanceof SplitDockStation) ) {
                SwingUtilities.invokeLater( new Runnable(){
                    public void run(){
                        checkAndReplace( dockable );
                    }
                } );
            }
        }
        private void checkAndReplace( Dockable dockable ){
            DockStation station = dockable.getDockParent();
            if( !(station instanceof ScreenDockStation) ) {
                // cancel
                return;
            }
     
            // .. then we just insert a SplitDockStation
            SplitDockStation split = new SplitDockStation();
            DockController controller = station.getController();
     
            try {
                // disable events while rearranging our layout
                controller.freezeLayout();
     
                station.replace( dockable, split );
                split.drop( dockable );
            }
            finally {
                // and enable events after we finished
                controller.meltLayout();
            }
        }
    }
}
