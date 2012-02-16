package rails.ui.swing;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;

import rails.common.parser.Config;

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

    public static String STEP_LOAD_GAME = "1";
    public static String STEP_INIT_UI = "2";
    public static String STEP_STOCK_CHART = "3";
    public static String STEP_REPORT_WINDOW = "4";
    public static String STEP_OR_INIT_DOCKING_FRAME = "5A";
    public static String STEP_OR_INIT_PANELS = "5B";
    public static String STEP_OR_INIT_TILES = "5C";
    public static String STEP_OR_APPLY_DOCKING_FRAME = "5D";
    public static String STEP_STATUS_WINDOW = "6";
    public static String STEP_INIT_NEW_GAME = "7";
    public static String STEP_CONFIG_WINDOW = "8";
    public static String STEP_INIT_SOUND = "9";
    public static String STEP_INIT_LOADED_GAME = "10";
    public static String STEP_FINALIZE = "11";
    
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
    private long[] cumulativeDuration;
    
    private Set<JFrame> framesRegisteredAsVisible = new HashSet<JFrame>();
    private List<JFrame> framesRegisteredToFront = new ArrayList<JFrame>();
    
    private JWindow myWin;
    private ProgressVisualizer progressVisualizer;

    //TODO remove temp label
    private JLabel tempL;

    private int currentStep = 1; //the start step

    public SplashWindow(boolean isLoad) {
        myWin = new JWindow();
        myWin.setBounds(new Rectangle(200,200,400,200));
        
        //TODO remove temp
        tempL = new JLabel("");
        myWin.getContentPane().add(tempL);
        tempL.setVisible(true);
        //TODO set up frame (incl. title, icons, bar, status text)
        myWin.setVisible(true);
        
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

        progressVisualizer = new ProgressVisualizer();
        progressVisualizer.setCurrentStep(currentStep);
        progressVisualizer.start();
    }
    
    public void notifyOfStep(String stepLabelConfigKey) {
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
                //TODO
            }
        }

        //show progress
        double percentage = 100.0 * elapsedDuration / totalDuration;
        tempL.setText("<html>" + percentage + "<br>" + stepDuration[currentStep].labelConfigKey + "</html>");

        //ensure that progress is updated even if EDT is very busy
        //CAUTION: paintImmediately is called outside of EDT
        //         works but not guideline-conform
        tempL.paintImmediately(tempL.getBounds());
        
        //ensure visibility of window
        myWin.toFront();
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
        
        progressVisualizer.interrupt();

        myWin.dispose();
        
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