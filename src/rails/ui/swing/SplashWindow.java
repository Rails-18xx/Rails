package rails.ui.swing;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;

import rails.common.LocalText;
import rails.common.parser.Config;
import rails.game.RailsRoot;

/**
 * Splash window shown during setup of game UI components and game loading.
 * Provides for a progress bar, status bar, and ensures that frames only become visible
 * when initialization is complete.
 * 
 * @author Frederick Weld
 */
public class SplashWindow {

    /**
     * in millisecs
     */
    private static long PROGRESS_UPDATE_INTERVAL = 200;

    private static String DUMMY_STEP_BEFORE_START = "-1";
    private static String DUMMY_STEP_START = "0";
    private static String DUMMY_STEP_END = "inf";

    public static String STEP_LOAD_GAME = "Splash.step.loadGame";
    public static String STEP_INIT_UI = "Splash.step.initUI";
    public static String STEP_STOCK_CHART = "Splash.step.stockChart";
    public static String STEP_REPORT_WINDOW = "Splash.step.reportWindow";
    public static String STEP_OR_INIT_DOCKING_FRAME = "Splash.step.or.initDockingFrame";
    public static String STEP_OR_INIT_PANELS = "Splash.step.or.initPanels";
    public static String STEP_OR_INIT_TILES = "Splash.step.or.initTiles";
    public static String STEP_OR_APPLY_DOCKING_FRAME = "Splash.step.or.applyDockingFrame";
    public static String STEP_STATUS_WINDOW = "Splash.step.statusWindow";
    public static String STEP_INIT_NEW_GAME = "Splash.step.initNewGame";
    public static String STEP_CONFIG_WINDOW = "Splash.step.configWindow";
    public static String STEP_INIT_SOUND = "Splash.step.initSound";
    public static String STEP_INIT_LOADED_GAME = "Splash.step.initLoadedGame";
    public static String STEP_FINALIZE = "Splash.step.finalize";
    
    private static List<String> STEP_GROUP_LOAD = Arrays.asList(new String[] {
            STEP_LOAD_GAME,
            STEP_INIT_LOADED_GAME
    });

    private static List<String> STEP_GROUP_DOCKING_LAYOUT = Arrays.asList(new String[] {
            STEP_OR_INIT_DOCKING_FRAME,
            STEP_OR_INIT_TILES,
            STEP_OR_APPLY_DOCKING_FRAME,
    });

    private static class StepDuration {
        long expectedDurationInMillis;
        String labelConfigKey;
        StepDuration(int expectedDurationInMillis,String labelConfigKey) {
            this.expectedDurationInMillis = expectedDurationInMillis;
            this.labelConfigKey = labelConfigKey;
        }
    }
    private static StepDuration[] stepDuration = {
            new StepDuration ( 0, DUMMY_STEP_BEFORE_START), // used to facilitate array border handling
            new StepDuration ( 0, DUMMY_STEP_START), // used to facilitate array border handling
            new StepDuration ( 6000, STEP_LOAD_GAME ),
            new StepDuration ( 500, STEP_INIT_UI ),
            new StepDuration ( 230, STEP_STOCK_CHART ),
            new StepDuration ( 850, STEP_REPORT_WINDOW ),
            new StepDuration ( 2600, STEP_OR_INIT_DOCKING_FRAME ),
            new StepDuration ( 1650, STEP_OR_INIT_PANELS ),
            new StepDuration ( 5000, STEP_OR_INIT_TILES ),
            new StepDuration ( 1000, STEP_OR_APPLY_DOCKING_FRAME ),
            new StepDuration ( 400, STEP_STATUS_WINDOW ),
            new StepDuration ( 300, STEP_INIT_NEW_GAME ),
            new StepDuration ( 1200, STEP_CONFIG_WINDOW ),
            new StepDuration ( 200, STEP_INIT_SOUND ),
            new StepDuration ( 1000, STEP_INIT_LOADED_GAME ),
            new StepDuration ( 1000, STEP_FINALIZE),
            new StepDuration ( 0, DUMMY_STEP_END), // used to facilitate array border handling
    };

    private long totalDuration = 0;
    private long[] cumulativeDuration = null;
    
    private Set<JFrame> framesRegisteredAsVisible = new HashSet<JFrame>();
    private List<JFrame> framesRegisteredToFront = new ArrayList<JFrame>();
    
    private static Dimension iconSize = new Dimension(90,78);
  
    private JWindow myWin = null;
    private JLabel leftIcon = null;
    private JLabel rightIcon = null;
    private JProgressBar progressBar = null;
    private JLabel stepLabel = null;

    private ProgressVisualizer progressVisualizer = null;

    private int currentStep = 1; //the start step

    public SplashWindow(boolean isLoad, String initDetailsText) {
        //quit directly when no visualization required
        //all visualization related attributes remain null then
        if ("no".equals(Config.get("splash.window.open"))) return;

        //calculate estimated duration for the respective steps
        cumulativeDuration = new long[stepDuration.length];
        boolean isDockingLayout = "yes".equals(Config.get("or.window.dockablePanels"));
        for (int i = 0 ; i < stepDuration.length ; i++) {
            //only consider step if relevant for this setup
            if ( (isLoad || !STEP_GROUP_LOAD.contains(stepDuration[i].labelConfigKey))
                    &&
                 (isDockingLayout || !STEP_GROUP_DOCKING_LAYOUT.contains(stepDuration[i].labelConfigKey)) ) {
                totalDuration += stepDuration[i].expectedDurationInMillis;
            }
            cumulativeDuration[i] = totalDuration;
        }

        //set up dynamic elements

        myWin = new JWindow();

        leftIcon = new JLabel();
        leftIcon.setPreferredSize(iconSize);
        rightIcon = new JLabel();
        rightIcon.setPreferredSize(iconSize);

        progressBar = new JProgressBar(0,(int)totalDuration);
        progressBar.setStringPainted(true);
        progressBar.setMinimum(0);

        stepLabel = new JLabel();
        stepLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        //set up static elements

        JLabel railsLabel = new JLabel("Rails " + RailsRoot.getFullVersion());
        railsLabel.setFont(railsLabel.getFont().deriveFont( 
                (float)2.0 * railsLabel.getFont().getSize()));
        railsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        String commandTextKey = isLoad ? "Splash.command.loadGame" : "Splash.command.newGame";
        JLabel commandLabel = new JLabel(
                LocalText.getText(commandTextKey,initDetailsText));
        commandLabel.setFont(commandLabel.getFont().deriveFont(Font.ITALIC));
        commandLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        //plug elements together and set up layout

        JPanel railsCommandPanel = new JPanel();
        railsCommandPanel.setLayout(new BoxLayout(railsCommandPanel, BoxLayout.Y_AXIS));
        railsCommandPanel.add(railsLabel);
        railsCommandPanel.add(commandLabel);
        
        JPanel idPanel = new JPanel();
        idPanel.setLayout(new BoxLayout(idPanel, BoxLayout.X_AXIS));
        idPanel.add(leftIcon);
        idPanel.add(railsCommandPanel);
        idPanel.add(rightIcon);
        
        JComponent contentPane = (JComponent)myWin.getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        contentPane.add(idPanel);
        contentPane.add(progressBar);
        contentPane.add(stepLabel);
        contentPane.setBorder(new CompoundBorder(new EtchedBorder(),new EmptyBorder(5,5,5,5)));
        
        //perform layout within the EDT
        //blocking call as further initialization requires the layout to be frozen
        try {
            SwingUtilities.invokeAndWait(new Thread() {
                @Override
                public void run() {
                    myWin.pack();
                    Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
                    myWin.setLocation(
                          (dim.width - myWin.getSize().width) / 2,
                          (dim.height - myWin.getSize().height) / 2
                    );
                    myWin.setVisible(true);
                }
            });
        } catch (Exception e) {}

        progressVisualizer = new ProgressVisualizer();
        progressVisualizer.setCurrentStep(currentStep);
        progressVisualizer.start();
    }
    
    public void notifyOfStep(String stepLabelConfigKey) {
        //ignore if no visualization requested
        if (myWin == null) return;
        
        for (int i = 0 ; i < stepDuration.length ; i++) {
            if (stepDuration[i].labelConfigKey.equals(stepLabelConfigKey)) {
                progressVisualizer.setCurrentStep(i);
            }
        }
    }

    /**
     * @param elapsedDuration Refers to a duration normalized based on the expected durations 
     * of the process steps.
     */
    synchronized private void visualizeProgress(long elapsedDuration, int currentStep) {
        //update current step (including description)
        if (currentStep != this.currentStep) {
            this.currentStep = currentStep;
            //only display step description for non-dummy steps
            if (stepDuration[currentStep].expectedDurationInMillis > 0) {
                stepLabel.setText(stepDuration[currentStep].labelConfigKey);
                enforeGUIUpdate(stepLabel);
            }
        }

        //show progress
        progressBar.setValue((int)elapsedDuration);
        enforeGUIUpdate(progressBar);

        //ensure visibility of window
        myWin.toFront();
    }

    /**
     * ensure that progress is updated even if EDT is very busy
     * CAUTION: paintImmediately is called outside of EDT
     *          works but not guideline-conform
     */
    private void enforeGUIUpdate(JComponent c) {
        c.paintImmediately(c.getBounds());
    }

    /**
     * Remembers that this frame is to be put to visible at the end of the splash process 
     */
    public void registerFrameForDeferredVisibility(JFrame frame,boolean setToVisible) {
        if (setToVisible) {
            framesRegisteredAsVisible.add(frame);
        } else {
            framesRegisteredAsVisible.remove(frame);
        }
    }
    
    /**
     * Remembers that this frame is to be put to front at the end of the splash process
     * Handles the list of to front requests in order to
     *  - apply all requests in a FIFO manner
     *  - ensure that each frame is only sent to the front once (at the latest registered time) 
     */
    public void registerFrameForDeferredToFront(JFrame frame) {
        framesRegisteredToFront.remove(frame);
        framesRegisteredToFront.add(frame);
    }
    
    public void finalizeGameInit() {
        notifyOfStep(STEP_FINALIZE);
        
        //finally restore visibility / toFront of registered frames
        //only after EDT is ready (as window built-up could still be pending)
        //block any frame disposal to the point in time when this is through
        try {
            SwingUtilities.invokeAndWait(new Thread() {
                @Override
                public void run() {
                    //visibility
                    for (JFrame frame : framesRegisteredAsVisible) {
                        frame.setVisible(true);
                    }
                    //to front
                    for (JFrame frame : framesRegisteredToFront) {
                        frame.toFront();
                    }
                }
            });
        } catch (Exception e) {}
        
        //clean up visualization only if it was requested
        if (myWin != null) {
            progressVisualizer.interrupt();
            myWin.dispose();
        }
    }

    private class ProgressVisualizer extends Thread {
        private long elapsedTime = 0;
        private int currentStep = 0;
        @Override
        public void run() {
            try {
                while (!isInterrupted()) {
                    visualizeProgress(elapsedTime, currentStep);
    
                    sleep(PROGRESS_UPDATE_INTERVAL);
    
                    //adjusted elapsed duration
                    synchronized (this) {
                        elapsedTime += PROGRESS_UPDATE_INTERVAL;
                        //elapsed duration must remain within the bounds of the estimated cumulative duration
                        //between the end of last step and the end of the current step
                        elapsedTime = Math.max ( 
                            cumulativeDuration[currentStep-1],
                            Math.min( elapsedTime, cumulativeDuration[currentStep] )
                        );
                    }
                }
            } catch (InterruptedException e) {}
        }

        synchronized private void setCurrentStep(int currentStep) {
            this.currentStep = currentStep;
            //System.out.println("Time: "+elapsedTime + " (Step: "+stepDuration[currentStep].labelConfigKey+")");
        }
    }
}