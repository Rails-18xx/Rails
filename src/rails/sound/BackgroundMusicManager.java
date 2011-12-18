/**
 * 
 */
package rails.sound;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.util.*;

import javazoom.jl.player.Player;
import org.apache.log4j.Logger;

import rails.common.parser.Config;

/**
 * This is a static class as there should never be two
 * background musics playing at the same time.
 * 
 * This class is notified of:
 * - phase changes (eg., 2-train -> 3-train)
 * - round changes (eg., SR -> OR)
 * 
 * Based on this information, the appropriate background music (mp3) is played 
 * in the background. 
 */
public class BackgroundMusicManager {
    private static class Context {
        public int round;
        public String phaseName;
        public Context(int round, String phaseName) {
            this.round = round;
            this.phaseName = phaseName;
        }
        public boolean equals(Object o) {
            if (!(o instanceof Context)) return false;
            Context c = (Context)o;
            return ((round == c.round) && (phaseName.equals(c.phaseName)));
        }
        public int hashCode() {
            return round+phaseName.hashCode();
        }
        public Context clone() {
            return new Context(round,phaseName);
        }
    }
    private static final int ROUND_UNDEFINED = -1;
    private static final int ROUND_STOCK = 0;
    private static final int ROUND_OPERATING = 1;
    private static final String PHASENAME_DEFAULT = "";
    private static Map<Context,String> contextToMusicFileMapping = new HashMap<Context,String>();
    private static boolean isDisabled = true;
    private static boolean isMute = false;
    private static boolean isPlaying = false;
    private static String currentMusicFileName;
    private static Thread playingThread;
    private static Context context = new Context(ROUND_UNDEFINED,PHASENAME_DEFAULT);
    private static Logger log = Logger.getLogger(BackgroundMusicManager.class.getPackage().getName());

    private static void setContextToMusicFileMapping (String config,Context defaultContext) {
        if (config == null || config.equals("")) return;
        String[] assignments = config.split(",");
        for ( int i = 0 ; i < assignments.length ; i++ ) {
            String[] assignment = assignments[i].split("=");
            Context c = defaultContext.clone();
            if (assignment.length == 1) {
                //default assignment (meaning, phase-independent)
                contextToMusicFileMapping.put(c, assignment[0]);
            }
            else if (assignment.length == 2) {
                //phase-dependent assignment
                c.phaseName = assignment[0];
                contextToMusicFileMapping.put(c, assignment[1]);
            }
        }
    }
    public static void init() {
        String enablement = Config.get("sound.backgroundMusic");
        if (enablement != null && enablement.equals("enabled")) {
            isDisabled = false;
            setContextToMusicFileMapping(
                    Config.get("sound.backgroundMusic.stockRound"),
                    new Context(ROUND_STOCK,PHASENAME_DEFAULT)
            );
            setContextToMusicFileMapping(
                    Config.get("sound.backgroundMusic.operatingRound"),
                    new Context(ROUND_OPERATING,PHASENAME_DEFAULT)
            );
            playNewMusic();
        } else {
            isDisabled = true;
        }
        
    }
    public static void setPhase(String name) {
        if (!context.phaseName.equals(name)) {
            context.phaseName = name;
            playNewMusic();
        }
    }
    public static void notifyOfStockRoundStart() {
        if (context.round != ROUND_STOCK) {
            context.round = ROUND_STOCK;
            playNewMusic();
        }
    }
    public static void notifyOfOperatingRoundStart() {
        if (context.round != ROUND_OPERATING) {
            context.round = ROUND_OPERATING;
            playNewMusic();
        }
    }
    public static void mute() {
        isMute = true;
        stopMusic();
    }
    public static void unMute() {
        if (!isDisabled) {
            isMute = false;
            playNewMusic();
        }
    }
    
    private static void playNewMusic() {
        if (!isMute) {
            if (isPlaying) stopMusic();
            if (contextToMusicFileMapping != null) {
                String newMusicFileName = (String)contextToMusicFileMapping.get(context);
                if (newMusicFileName == null) {
                    //try phase-defaulting if nothing was found
                    newMusicFileName = (String)contextToMusicFileMapping.get(new Context(context.round,PHASENAME_DEFAULT));
                }
                //only restart/change the music if a new music file is to be played
                if (newMusicFileName != null && !newMusicFileName.equals(currentMusicFileName)) {
                    currentMusicFileName = newMusicFileName;
                // run music playing in new thread to play in background
                    playingThread = new Thread() {
                        Player player;
                        public void run() {
                            try {
                                while (!Thread.interrupted()) {
                                    FileInputStream fis = new FileInputStream(currentMusicFileName);
                                    BufferedInputStream bis = new BufferedInputStream(fis);
                                    player = new Player(bis);
                                    log.info("Now playing: "+currentMusicFileName);
                                    player.play();
                                }
                            }
                            catch (Exception e) { 
                                //if anything goes wrong, don't play anything
                                log.error(e);
                            }
                        }
                        public void interrupt() {
                            super.interrupt();
                            if (player!=null) player.close();
                        }
                    };
                    playingThread.start();
                    isPlaying = true;
                }
            }
        }
    }
    private static void stopMusic() {
        if (isPlaying) {
            playingThread.interrupt();
            isPlaying = false;
        }
    }
}
