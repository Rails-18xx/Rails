package rails.ui.swing;

import java.io.*;
import java.util.Calendar;

import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

import rails.game.action.GameAction;

public class AutoLoadPoller extends Thread {
    
    private GameUIManager guiMgr;
    private String saveDirectory;
    private String savePrefix;
    private String ownPostfix;
    private int pollingInterval;
    private int pollingStatus;
    
    private boolean pollingActive = false;
    
    private String lastSavedFilenameFilepath;
    private String lastSavedFilename = "";
    
    public static final int OFF = 0;
    public static final int ON = 1;
    public static final int SUSPENDED = 2;

    protected static Logger log =
        Logger.getLogger(AutoLoadPoller.class.getPackage().getName());

    public AutoLoadPoller (GameUIManager guiMgr, String saveDirectory, String savePrefix, String ownPostfix,
            int status, int pollingInterval) {
 
        this.guiMgr = guiMgr;
        this.saveDirectory = saveDirectory;
        this.savePrefix = savePrefix;
        this.ownPostfix = ownPostfix;
        this.pollingStatus = status;
        this.pollingInterval = pollingInterval;
        
        lastSavedFilenameFilepath = saveDirectory + "/" + savePrefix + ".last_rails";
        
        log.debug("Poller own postfix: "+ownPostfix);
        log.debug("Poller last-filename path: "+lastSavedFilenameFilepath);
        
    }

    @Override
    public void run () {

        log.info ("AutoLoadPoller started");

        int secs, sleepTime;
        String currentFilename;

        for (;;) {

            log.debug ("Polling cycle, status="+pollingStatus+" active="+pollingActive);
            // Process
            if (pollingActive && pollingStatus == ON) {
                log.debug("Polling...");
                try {
                    BufferedReader in = new BufferedReader (new FileReader (lastSavedFilenameFilepath));
                    currentFilename = in.readLine();
                    in.close();
                    log.debug("Read filename "+currentFilename+"; last saved filename "+lastSavedFilename);
                    
                    if (!lastSavedFilename.equals(currentFilename)) {
                        final GameAction reload = new GameAction (GameAction.RELOAD);
                        reload.setFilepath(saveDirectory+"/"+currentFilename);
                        lastSavedFilename = currentFilename;
                        
                        // The GUI must be accessed on the event dispatch thread only.
                        SwingUtilities.invokeLater (new Runnable() {
                            public void run() {
                                guiMgr.processAction(reload);
                            }
                        });

                    }
                    
                } catch (IOException e) {
                    log.error("Exception whilst polling "+lastSavedFilenameFilepath, e);
                }
            }
            
            
            
            secs = Calendar.getInstance().get(Calendar.SECOND);
            try {
                sleepTime = 1000 * (pollingInterval - secs%pollingInterval);
                sleep (sleepTime);
            } catch (InterruptedException e) {
                continue;
            }
        }
        // This thread never exits
    }

    public String getSaveDirectory() {
        return saveDirectory;
    }

    public void setSaveDirectory(String saveDirectory) {
        this.saveDirectory = saveDirectory;
    }

    public String getSavePrefix() {
        return savePrefix;
    }

    public void setSavePrefix(String savePrefix) {
        this.savePrefix = savePrefix;
    }

    public String getOwnPostfix() {
        return ownPostfix;
    }

    public void setOwnPostfix(String ownPostfix) {
        this.ownPostfix = ownPostfix;
    }

    public int getPollingInterval() {
        return pollingInterval;
    }

    public void setPollingInterval(int pollingInterval) {
        this.pollingInterval = pollingInterval;
    }

    public int getStatus() {
        return pollingStatus;
    }

    public void setStatus(int status) {
        this.pollingStatus = status;
    }

    public boolean isActive() {
        return pollingActive;
    }

    public void setActive(boolean pollingActive) {
        this.pollingActive = pollingActive;
        log.debug("AutoLoad polling set to "+pollingActive);
    }

    public String getLastSavedFilename() {
        return lastSavedFilename;
    }

    public void setLastSavedFilename(String lastSavedFilename) {
        this.lastSavedFilename = lastSavedFilename;
    }

    
}
