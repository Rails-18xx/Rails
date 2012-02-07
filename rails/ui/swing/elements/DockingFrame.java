package rails.ui.swing.elements;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Locale;

import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import rails.common.LocalText;
import rails.common.parser.Config;

import bibliothek.gui.DockController;
import bibliothek.gui.DockStation;
import bibliothek.gui.Dockable;
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
import bibliothek.gui.dock.common.menu.CThemeMenuPiece;
import bibliothek.gui.dock.common.mode.ExtendedMode;
import bibliothek.gui.dock.common.theme.ThemeMap;
import bibliothek.gui.dock.event.DockStationAdapter;
import bibliothek.gui.dock.facile.menu.RootMenuPiece;
import bibliothek.gui.dock.facile.menu.SubmenuPiece;
import bibliothek.gui.dock.station.LayoutLocked;

import org.apache.log4j.Logger;

/**
 * Superclass for all application frames that want to use the docking
 * framework for managing its panels.
 * 
 * All references to the docking framework are private by purpose. This
 * enforces that any sub-class must not deal with any framework related
 * issues (this superclass acts as a facade to the framework).
 * 
 * @author Frederick Weld
 *
 */
public abstract class DockingFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    private static final String layoutDirectoryName = "DockableLayout";
    private static final String layoutFileSuffix = "_layout.rails_ini";
    private static final String layoutName_initial = "InitialLayout";
    private static final String layoutName_current = "CurrentLayout";
    private static final String defaultTheme = ThemeMap.KEY_BASIC_THEME;
    private static Logger log = Logger.getLogger(DockingFrame.class.getPackage().getName());

    private boolean isDockingFrameworkEnabled;
    private CControl control = null;
    private CGrid gridLayout = null;
    
    /**
     * Decision whether docking framework should be activated for a frame
     * has to be done at the beginning as later switching is not supported
     */
    protected DockingFrame(boolean isDockingFrameworkEnabled) {
        this.isDockingFrameworkEnabled = isDockingFrameworkEnabled;
        if (!isDockingFrameworkEnabled) return;
        
        //init the ccontrol
        control = new CControl( this );
        control.setTheme(defaultTheme);
        add( control.getContentArea() );
        if ("en_us".equalsIgnoreCase(Config.get("locale"))) {
            //hard setting to default in case of US as this is DockingFrames default language
            //don't use Locale constant as it is en_US (case sensitive)
            control.setLanguage(new Locale(""));
        }
        
        //init the grid layout
        gridLayout = new CGrid( control );

        //ensure that externalized dockables get a split station as parent
        //necessary, otherwise externalized dockables cannot be docked together
        alwaysAddStationsToExternalizedDockables(control);
        

    }
    
    public boolean isDockingFrameworkEnabled() {
        return isDockingFrameworkEnabled;
    }
    
    /**
     * Registers a component that is to become a dockable.
     * The dockable is only deployed to the frame if deployDockables is called.
     */
    protected void addDockable(JComponent c, String dockableTitle, int x, int y, int width, int height) {
        DefaultSingleCDockable d = new DefaultSingleCDockable( 
                dockableTitle, dockableTitle );
        d.add( c, BorderLayout.CENTER );
        d.setCloseable( false );
        gridLayout.add( x, y, width, height, d );
    }
    
    /**
     * Deploys to the frame all dockables that have been added before
     */
    protected void deployDockables() {
        control.getContentArea().deploy( gridLayout );
    }

    /**
     * Creates a generic layout menu and adds it to the specified menu bar
     */
    protected void addDockingFrameMenu(JMenuBar menuBar) {
        RootMenuPiece layoutMenu = new RootMenuPiece(
                LocalText.getText("DockingFrame.menu.layout"), 
                false);
        layoutMenu.add( new SubmenuPiece( 
                LocalText.getText("DockingFrame.menu.layout.theme"), 
                false, 
                new CThemeMenuPiece( control )
        ));
        layoutMenu.getMenu().addSeparator();
        JMenuItem resetMenuItem = new JMenuItem (
                LocalText.getText("DockingFrame.menu.layout.reset"));
        resetMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                control.load(layoutName_initial);
                control.setTheme(defaultTheme);
            }
        });
        layoutMenu.getMenu().add(resetMenuItem);
        JMenuItem applyFromMenuItem = new JMenuItem (
                LocalText.getText("DockingFrame.menu.layout.applyFrom"));
        applyFromMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                loadLayoutUserDefined();
            }
        });
        layoutMenu.getMenu().add(applyFromMenuItem);

        //deploy menu
        menuBar.add( layoutMenu.getMenu() );
    }
    
    /**
     * May only be called once the docking frame's layout has been constructed.
     * Remembers that layout as the initial one.
     * Loads a former layout if that was persisted in a prior session. 
     */
    protected void initLayout() {
        control.save(layoutName_initial);
        loadLayout(getLayoutFile(),true);
    }

    /**
     * @return The file name which is to be used for storing the layout upon exit
     * and restoring prior layout when entering a session.
     */
    abstract protected String getLayoutFileName();
    
    /**
     * get layout directory (and ensure that it is available)
     */
    private File getLayoutDirectory() {
        try {
            File layoutDirectory = new File(Config.get("save.directory"),layoutDirectoryName);
            if (!layoutDirectory.isDirectory()) {
                layoutDirectory.mkdirs();
            }
            return layoutDirectory;
        }
        catch (Exception e) {
            //return no valid file if anything goes wrong
            return null;
        }
    }

    private File getLayoutFile() {
        File layoutFile = new File(getLayoutDirectory(), 
               getLayoutFileName() + layoutFileSuffix );
        return layoutFile;
    }

    public void saveLayout() {
        if (!isDockingFrameworkEnabled) return;

        File layoutFile = getLayoutFile();
        try {
            control.save(layoutName_current);
            control.writeXML(layoutFile);
            log.info("Layout saved to " + layoutFile.getName());
        } catch (Exception e) {
            log.error("Layout could not be saved to " + layoutFile.getName());
            return;
        }
    }
    
    /**
     * @param isTentative If true, then method only tries to load specified layout
     * but would not produce any error popup.
     */
    private void loadLayout(File layoutFile, boolean isTentative) {
        if (!isDockingFrameworkEnabled) return;
        
        try {
            control.readXML(layoutFile);
            control.load(layoutName_current);
            log.info("Layout loaded from " + layoutFile.getName());
        } catch (Exception e) {
            if (!isTentative) {
                JOptionPane.showMessageDialog(this,
                            "Unable to load layout from " + layoutFile.getName());
            }
            log.error("Layout could not be loaded from " + layoutFile.getName());
            return;
        }
        
        //ensure that all dockables that are externalized according to layout
        //information don't have the default maximize button (as it won't work
        //for the adjusted externalization setup)
        for (int i = 0 ; i < control.getCDockableCount() ; i++ ) {
            CDockable d = control.getCDockable(i);
            if (d instanceof DefaultCDockable) {
                DefaultCDockable dd = (DefaultCDockable)d;
                if (ExtendedMode.EXTERNALIZED.equals(d.getExtendedMode())) {
                    dd.putAction( CDockable.ACTION_KEY_MAXIMIZE, CBlank.BLANK );
                } else {
                    dd.putAction( CDockable.ACTION_KEY_MAXIMIZE, null );
                }
            }
        }
    }

    /**
     * Lets user choose a layout in a file chooser popup and then loads/applies it
     */
    private void loadLayoutUserDefined() {
        JFileChooser jfc = new JFileChooser();
        jfc.setCurrentDirectory(getLayoutDirectory());
        if (jfc.showOpenDialog(getContentPane()) != JFileChooser.APPROVE_OPTION) return; // cancel pressed
        loadLayout(jfc.getSelectedFile(),false);
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