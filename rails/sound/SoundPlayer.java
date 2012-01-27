/**
 * 
 */
package rails.sound;

import java.io.BufferedInputStream;
import java.io.FileInputStream;

import javazoom.jl.player.Player;

/**
 * Handles play requests for music and sfx.
 * 
 * Some specific requirements:
 * - At most one SFX should be played at the same time (necessiting queuing sfx play requests)
 * 
 * @author Frederick Weld
 *
 */
public class SoundPlayer {

    private class PlayerThread extends Thread {
        String fileName;
        PlayerThread priorThread;
        boolean playingDone;
        public PlayerThread(String fileName) {
            this.fileName = fileName;
            priorThread = null;
            playingDone = false;
        }
        public void setPriorThread(PlayerThread priorThread) {
            this.priorThread = priorThread;
        }
        //returns once playing is done
        synchronized public void waitForPlayingDone() {
            if (!playingDone) {
                try {
                    wait();
                } catch (InterruptedException e) {}
            }
        }
        public void run() {
            //wait until prior thread has finished playing
            if (priorThread != null) priorThread.waitForPlayingDone();
            priorThread = null; //release handle
            try {
                FileInputStream fis = new FileInputStream(fileName);
                BufferedInputStream bis = new BufferedInputStream(fis);
                Player player = new Player(bis);
                player.play();
                player.close();
            }
            catch (Exception e) { 
                //if anything goes wrong, don't play anything
            }
            //wake the subsequent thread if there is one waiting
            synchronized (this) {
                notify();
                playingDone = true;
            }
        }
    }
    
    private PlayerThread lastSFXThread = null;
    
    /**
     * atomic switching of the pointer to the last thread which played an sfx.
     * @param newThread Player thread for the new sfx 
     * @return Player thread which was the last to play a sfx
     */
    synchronized private PlayerThread adjustLastSFXThread(PlayerThread newThread) {
        PlayerThread pt = lastSFXThread;
        lastSFXThread = newThread;
        return pt;
    }
    
    public void playSFX(String fileName) {
        PlayerThread newPlayerThread = new PlayerThread (fileName);
        PlayerThread oldPlayerThread = adjustLastSFXThread(newPlayerThread);
        newPlayerThread.setPriorThread(oldPlayerThread);
        newPlayerThread.start();
    }
    public void playSFXByConfigKey(String configKey) {
        playSFX(SoundConfig.get(configKey));
    }
    public void playSFXByConfigKey(String configKey,String parameter) {
        playSFX(SoundConfig.get(configKey,parameter));
    }
}
