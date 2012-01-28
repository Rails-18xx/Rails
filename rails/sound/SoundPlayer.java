/**
 * 
 */
package rails.sound;

import java.io.BufferedInputStream;
import java.io.FileInputStream;

import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;
import javazoom.jl.player.advanced.AdvancedPlayer;

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

            play();
            
            //wake the subsequent thread if there is one waiting
            synchronized (this) {
                notify();
                playingDone = true;
            }
        }
        public void play() {
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
        }
    }
    private class PortionPlayerThread extends PlayerThread {
        private double startPos;
        private double endPos;
        /**
         * @param startPos Relative offset [0..1] - first mp3 frame played
         * @param endPos Relative offset [0..1] - last mp3 frame played
         */
        public PortionPlayerThread(String fileName, double startPos, double endPos) {
            super(fileName);
            this.startPos = startPos;
            this.endPos = endPos;
        }
        @Override
        public void play() {
            try {
                FileInputStream fis = new FileInputStream(fileName);
                BufferedInputStream bis = new BufferedInputStream(fis);
                PortionPlayer player = new PortionPlayer(bis);
                player.play(startPos, endPos);
                player.close();
            }
            catch (Exception e) { 
                //if anything goes wrong, don't play anything
            }
        }
    }
    private class PortionPlayer extends AdvancedPlayer {
        BufferedInputStream bis;
        public PortionPlayer(BufferedInputStream bis) throws JavaLayerException {
            super(bis);
            this.bis = bis;
        }
        /**
         * @param startPos Relative offset [0..1] - first mp3 frame played
         * @param endPos Relative offset [0..1] - last mp3 frame played
         */
        public void play(double startPos, double endPos) {
            try {
                //first get number of frames
                bis.mark(Integer.MAX_VALUE);
                int frameNum = 0;
                while (skipFrame()) {
                    frameNum++;
                }
                int startFrame = (int)Math.floor(startPos * frameNum);
                int endFrame = (int)Math.ceil(endPos * frameNum);
                if (startFrame < endFrame) {
                    bis.reset();
                    play(startFrame, endFrame);
                }
            } catch (Exception e) { 
                //if anything goes wrong, don't play anything
            }
        }
    }
    private class LoopPlayerThread extends PlayerThread {
        boolean isStopped = false;
        Player player = null;
        public LoopPlayerThread(String fileName) {
            super(fileName);
        }
        public void play() {
            try {
                while (!isStopped) {
                    FileInputStream fis = new FileInputStream(fileName);
                    BufferedInputStream bis = new BufferedInputStream(fis);
                    player = new Player(bis);
                    player.play();
                    player.close();
                }
            }
            catch (Exception e) { 
                //if anything goes wrong, don't play anything
            }
        }
        @Override
        public void interrupt() {
            super.interrupt();
            isStopped = true;
            if (player!=null) player.close();
        }
    }
    
    private PlayerThread lastSFXThread = null;
    
    private LoopPlayerThread lastBGMThread = null;
    
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
    
    /**
     * atomic switching of the pointer to the last thread which played music.
     * @param newThread Player thread for the new music
     * @return Player thread which was the last to play music
     */
    synchronized private LoopPlayerThread adjustLastBGMThread(LoopPlayerThread newThread) {
        LoopPlayerThread pt = lastBGMThread;
        lastBGMThread = newThread;
        return pt;
    }
    
    private void playSFX(PlayerThread newPlayerThread) {
        PlayerThread oldPlayerThread = adjustLastSFXThread(newPlayerThread);
        newPlayerThread.setPriorThread(oldPlayerThread);
        newPlayerThread.start();
    }
    
    public void playSFX(String fileName) {
        playSFX(new PlayerThread (fileName));
    }
    public void playSFXByConfigKey(String configKey) {
        playSFX(SoundConfig.get(configKey));
    }
    public void playSFXByConfigKey(String configKey,String parameter) {
        playSFX(SoundConfig.get(configKey,parameter));
    }

    public void playSFX(String fileName, double playSoundProportion) {
        playSFX(new PortionPlayerThread (fileName, 1 - playSoundProportion, 1));
    }
    
    /**
     * The latter part of the sfx is played.
     * @param playSoundProportion The length of this part relatively to the overall sound duration.
     */
    public void playSFXByConfigKey(String configKey, double playSoundProportion) {
        playSFX(SoundConfig.get(configKey), playSoundProportion);
    }

    public void playBGM(String backgroundMusicFileName) {
        LoopPlayerThread newPlayerThread = new LoopPlayerThread(backgroundMusicFileName);
        LoopPlayerThread oldPlayerThread = adjustLastBGMThread(newPlayerThread);
        if (oldPlayerThread != null) oldPlayerThread.interrupt();
        newPlayerThread.start();
    }
    
    public void playBGMByConfigKey(String configKey) {
        playBGM(SoundConfig.get(configKey));
    }
}
