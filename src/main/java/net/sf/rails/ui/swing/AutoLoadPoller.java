package net.sf.rails.ui.swing;

import java.io.*;
import java.util.Calendar;

import javax.swing.SwingUtilities;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rails.game.action.GameAction;


public class AutoLoadPoller extends Thread {

    private final GameUIManager guiMgr;
    private String saveDirectory;
    private String savePrefix;
    private String ownPostfix;
    private int pollingInterval;
    private int pollingStatus;

    private boolean pollingActive = false;

    private final String lastSavedFilenameFilepath;
    private String lastSavedFilename;

    public static final int OFF = 0;
    public static final int ON = 1;
    public static final int SUSPENDED = 2;

    private static final Logger log = LoggerFactory.getLogger(AutoLoadPoller.class);

    public AutoLoadPoller (GameUIManager guiMgr, String saveDirectory, String savePrefix, String lastSavedFilename,
            String ownPostfix, int status, int pollingInterval) {
        this.guiMgr = guiMgr;
        this.saveDirectory = saveDirectory;
        this.savePrefix = savePrefix;
        this.lastSavedFilename = StringUtils.defaultString(lastSavedFilename, "");
        this.ownPostfix = ownPostfix;
        this.pollingStatus = status;
        this.pollingInterval = pollingInterval;

        lastSavedFilenameFilepath = saveDirectory + "/" + savePrefix + "." + GameUIManager.DEFAULT_SAVE_POLLING_EXTENSION;

        log.debug("Poller own postfix: {}", ownPostfix);
        log.debug("Poller last-filename path: {}", lastSavedFilenameFilepath);
    }

    @Override
    public void run () {
        log.info ("AutoLoadPoller started");

        int currentPollInterval = 1;
        int secs, sleepTime;

        while ( true ) {
            secs = Calendar.getInstance().get(Calendar.SECOND);
            try {
                sleepTime = 1000 * (currentPollInterval - secs%currentPollInterval);
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                continue;
            }

            if (pollingActive && pollingStatus == ON) {
                try (BufferedReader in = new BufferedReader (new FileReader (lastSavedFilenameFilepath))){
                    String currentFilename = in.readLine();
                    String fileSize = in.readLine();
                    in.close();
                    log.trace("Read filename {}; last seen filename {}", currentFilename, lastSavedFilename);


                    if (!lastSavedFilename.equals(currentFilename)) {
                        File currFile = new File(saveDirectory+"/"+currentFilename);
                        if ( ! currFile.exists() ) {
                            log.debug("Saved file {} missing, waiting", currFile);
                            currentPollInterval = 1;
                            continue;
                        }

                        if ( fileSize != null ) {
                            long fileSizeNum = Long.parseLong(fileSize);
                            if ( currFile.getTotalSpace() != fileSizeNum ) {
                                // file size doesn't match, could be it the process of being written
                                // or due to network errors, might be zero length so lets ignore it
                                log.debug("file goes not match expected size {} (expected {})", currFile.getTotalSpace(), fileSizeNum);
                                currentPollInterval = 1;
                                continue;
                            }
                        }
                        lastSavedFilename = currentFilename;

                        final GameAction reload = new GameAction(guiMgr.getRoot(), GameAction.Mode.RELOAD);
                        reload.setFilepath(saveDirectory+"/"+currentFilename);

                        // The GUI must be accessed on the event dispatch thread only.
                        SwingUtilities.invokeLater (() -> guiMgr.processAction(reload));
                    }
                } catch (IOException e) {
                    log.error("Exception whilst polling {}", lastSavedFilenameFilepath, e);
                }

            } else {
                log.trace("Polling status={} active={}", pollingStatus, pollingActive);
            }
            currentPollInterval = pollingInterval;
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
        log.debug("AutoLoad polling set to {}", pollingActive);
    }

    public String getLastSavedFilename() {
        return lastSavedFilename;
    }

    public void setLastSavedFilename(String lastSavedFilename) {
        this.lastSavedFilename = lastSavedFilename;
    }


}
