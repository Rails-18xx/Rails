/**
 * 
 */
package rails.ui.swing;

import java.awt.Container;
import java.awt.Rectangle;
import java.awt.Window;
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
    public static String STEP_OR_WINDOW = "5";
    public static String STEP_STATUS_WINDOW = "6";
    public static String STEP_INIT_NEW_GAME = "7";
    public static String STEP_CONFIG_WINDOW = "8";
    public static String STEP_INIT_SOUND = "9";
    public static String STEP_INIT_LOADED_GAME = "10";

    private static class StepWeight {
        int weight;
        String labelConfigKey;
        StepWeight(int weight,String labelConfigKey) {
            this.weight = weight;
            this.labelConfigKey = labelConfigKey;
        }
    }
    private static StepWeight[] stepWeights = {
            new StepWeight ( 0, "Start"), // used to facilitate array border handling
            new StepWeight ( 2, STEP_LOAD_GAME ),
            new StepWeight ( 1, STEP_INIT_UI ),
            new StepWeight ( 1, STEP_STOCK_CHART ),
            new StepWeight ( 1, STEP_REPORT_WINDOW ),
            new StepWeight ( 4, STEP_OR_WINDOW ),
            new StepWeight ( 2, STEP_STATUS_WINDOW ),
            new StepWeight ( 1, STEP_INIT_NEW_GAME ),
            new StepWeight ( 2, STEP_CONFIG_WINDOW ),
            new StepWeight ( 1, STEP_INIT_SOUND ),
            new StepWeight ( 1, STEP_INIT_LOADED_GAME ),
            new StepWeight ( 0, "End"), // used to facilitate array border handling
    };

    private int totalWeight = 0;
    private int[] cumulativeWeight;
    
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
        
        cumulativeWeight = new int[stepWeights.length];
        for (int i = 0 ; i < stepWeights.length ; i++) {
            totalWeight += stepWeights[i].weight;
            cumulativeWeight[i] = totalWeight;
        }
    }
    
    public void notifyOfStep(String stepLabelConfigKey) {
        myWin.toFront();
        for (int i = 0 ; i < stepWeights.length ; i++) {
            if (stepWeights[i].labelConfigKey.equals(stepLabelConfigKey)) {
                setCurrentStep(i);
            }
        }
    }

    private void setCurrentStep(int currentStep) {
        //everything until i-1 is done, as i has now begun
        double percentage = 100.0 * cumulativeWeight[currentStep-1] / totalWeight;
        tempL.setText("<html>" + percentage + "<br>" + stepWeights[currentStep].labelConfigKey + "</html>");
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
        setCurrentStep(stepWeights.length - 1);
        
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
