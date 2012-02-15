/**
 * 
 */
package rails.ui.swing;

import java.awt.Rectangle;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;

/**
 * Splash window shown during setup of game UI components and game loading.
 * Provides for a progress bar, status bar, and ensures that frames only become visible
 * when initialization is complete.
 * 
 * @author Frederick Weld
 */
public class SplashWindow {

    public static String STEP_LOAD_GAME = "1";
    public static String STEP_INIT_UI = "2";
    public static String STEP_STOCK_CHART = "3";
    public static String STEP_REPORT_WINDOW = "4";
    public static String STEP_OR_INIT_DOCKING_FRAME = "5A";
    public static String STEP_OR_INIT_PANELS = "5B";
    public static String STEP_OR_INIT_TILES = "5C";
    public static String STEP_STATUS_WINDOW = "6";
    public static String STEP_INIT_NEW_GAME = "7";
    public static String STEP_CONFIG_WINDOW = "8";
    public static String STEP_INIT_SOUND = "9";
    public static String STEP_INIT_LOADED_GAME = "10";

    private static class StepDuration {
        int expDurationInMillis;
        String labelConfigKey;
        StepDuration(int expDurationInMillis,String labelConfigKey) {
            this.expDurationInMillis = expDurationInMillis;
            this.labelConfigKey = labelConfigKey;
        }
    }
    private static StepDuration[] stepDuration = {
            new StepDuration ( 0, "Start"), // used to facilitate array border handling
            new StepDuration ( 6000, STEP_LOAD_GAME ),
            new StepDuration ( 500, STEP_INIT_UI ),
            new StepDuration ( 230, STEP_STOCK_CHART ),
            new StepDuration ( 850, STEP_REPORT_WINDOW ),
            new StepDuration ( 2600, STEP_OR_INIT_DOCKING_FRAME ),
            new StepDuration ( 1650, STEP_OR_INIT_PANELS ),
            new StepDuration ( 7000, STEP_OR_INIT_TILES ),
            new StepDuration ( 400, STEP_STATUS_WINDOW ),
            new StepDuration ( 300, STEP_INIT_NEW_GAME ),
            new StepDuration ( 1200, STEP_CONFIG_WINDOW ),
            new StepDuration ( 200, STEP_INIT_SOUND ),
            new StepDuration ( 1000, STEP_INIT_LOADED_GAME ),
            new StepDuration ( 0, "End"), // used to facilitate array border handling
    };

    private int totalDuration = 0;
    private int[] cumulativeDuration;
    
    private Set<JFrame> framesRegisteredAsVisible = new HashSet<JFrame>();
    
    private JWindow myWin;

    //TODO remove temp label
    private JLabel tempL;

    public SplashWindow() {
        myWin = new JWindow();
        myWin.setBounds(new Rectangle(200,200,400,200));
        
        //TODO remove temp
        tempL = new JLabel("sghsghsghsfghws");
        myWin.getContentPane().add(tempL);
        tempL.setVisible(true);
        //TODO set up frame (incl. title, icons, bar, status text)
        myWin.setVisible(true);
        
        cumulativeDuration = new int[stepDuration.length];
        for (int i = 0 ; i < stepDuration.length ; i++) {
            totalDuration += stepDuration[i].expDurationInMillis;
            cumulativeDuration[i] = totalDuration;
        }
    }
    
    public void notifyOfStep(String stepLabelConfigKey) {
        myWin.toFront();
        for (int i = 0 ; i < stepDuration.length ; i++) {
            if (stepDuration[i].labelConfigKey.equals(stepLabelConfigKey)) {
                setCurrentStep(i);
            }
        }
    }

    private void setCurrentStep(int currentStep) {
        //everything until i-1 is done, as i has now begun
        double percentage = 100.0 * cumulativeDuration[currentStep-1] / totalDuration;
        tempL.setText("<html>" + percentage + "<br>" + stepDuration[currentStep].labelConfigKey + "</html>");
        //TODO update bar
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
    
    public void finalizeGameInit() {
        setCurrentStep(stepDuration.length - 1);
        
        //finally restore visibility of registered frames
        //only after EDT is ready (as window built-up could still be pending)
        SwingUtilities.invokeLater(new Thread() {
            @Override
            public void run() {
                for (JFrame frame : framesRegisteredAsVisible) {
                    frame.setVisible(true);
                }
            }
        });
        
        myWin.dispose();
        
    }

}
