/**
 *
 */
package net.sf.rails.sound;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

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

    private static class SoundFileBuffer {
        private final Map<String,byte[]> fileBuffer = new HashMap<>();
        public synchronized BufferedInputStream getFileInputStream(String fileName) {
            if (!fileBuffer.containsKey(fileName)) {
                try (InputStream fis = Files.newInputStream(Paths.get(fileName))) {
                    long length = new File(fileName).length();
                    byte[] fileContent = new byte[(int)length];
                    fis.read(fileContent);
                    fileBuffer.put(fileName, fileContent);
                } catch (Exception e) {return null;}
            }
            return new BufferedInputStream(new ByteArrayInputStream(fileBuffer.get(fileName)));
        }
    }

    private class PlayerThread extends Thread {
        protected String fileName;
        private PlayerThread priorThread;
        private boolean playingDone;
        public PlayerThread(String fileName) {
            this.fileName = fileName;
            priorThread = null;
            playingDone = false;
        }
        public void setPriorThread(PlayerThread priorThread) {
            this.priorThread = priorThread;
        }
        //returns once playing is done
        public synchronized void waitForPlayingDone() {
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
                notifyAll();
                playingDone = true;
            }
        }
        public void play() {
            try {
                Player player = new Player(soundFileBuffer.getFileInputStream(fileName));
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
                PortionPlayer player = new PortionPlayer(
                        soundFileBuffer.getFileInputStream(fileName));
                player.play(startPos, endPos);
                player.close();
            }
            catch (Exception e) {
                //if anything goes wrong, don't play anything
            }
        }
    }
    private static class PortionPlayer extends AdvancedPlayer {
        private BufferedInputStream bis;
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
        private boolean isStopped = false;
        private Player player = null;
        private LoopPlayerThread previousLoopPlayerThread = null;
        public LoopPlayerThread(String fileName) {
            super(fileName);
        }
        public void play() {
            try {
                //stop prior BGM music
                if (previousLoopPlayerThread != null) {
                    previousLoopPlayerThread.interrupt();
                }

                while (!isStopped) {
                    player = new Player(soundFileBuffer.getFileInputStream(fileName));
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
        public void setPreviousLoopPlayer(LoopPlayerThread previousLoopPlayerThread) {
            this.previousLoopPlayerThread = previousLoopPlayerThread;
        }
    }

    private PlayerThread lastSFXThread = null;

    private LoopPlayerThread lastBGMThread = null;

    private final SoundFileBuffer soundFileBuffer = new SoundFileBuffer();

    /**
     * atomic switching of the pointer to the last thread which played an sfx.
     * @param newThread Player thread for the new sfx
     * @return Player thread which was the last to play a sfx
     */
    private synchronized PlayerThread adjustLastSFXThread(PlayerThread newThread) {
        PlayerThread pt = lastSFXThread;
        lastSFXThread = newThread;
        return pt;
    }

    /**
     * atomic switching of the pointer to the last thread which played music.
     * @param newThread Player thread for the new music
     * @return Player thread which was the last to play music
     */
    private synchronized LoopPlayerThread adjustLastBGMThread(LoopPlayerThread newThread) {
        LoopPlayerThread pt = lastBGMThread;
        lastBGMThread = newThread;
        return pt;
    }

    private void playSFX(PlayerThread newPlayerThread,boolean playImmediately) {
        PlayerThread oldPlayerThread = adjustLastSFXThread(newPlayerThread);
        if (!playImmediately) {
            newPlayerThread.setPriorThread(oldPlayerThread);
        }
        newPlayerThread.start();
    }

    private void playSFX(String fileName, boolean playImmediately) {
        playSFX(new PlayerThread (fileName),playImmediately);
    }

    private void playSFX(String fileName, double playSoundProportion, boolean playImmediately) {
        playSFX(new PortionPlayerThread (fileName, 1 - playSoundProportion, 1)
                ,playImmediately);
    }

    /**
     * SFX played after prior SFX playing has been completed
     */
    public void playSFXByConfigKey(String configKey) {
        playSFX(SoundConfig.get(configKey),
                SoundConfig.KEYS_SFX_IMMEDIATE_PLAYING.contains(configKey));
    }
    public void playSFXByConfigKey(String configKey,String parameter) {
        playSFX(SoundConfig.get(configKey,parameter),
                SoundConfig.KEYS_SFX_IMMEDIATE_PLAYING.contains(configKey));
    }

    /**
     * The latter part of the sfx is played.
     * @param playSoundProportion The length of this part relatively to the overall sound duration.
     */
    public void playSFXByConfigKey(String configKey, double playSoundProportion) {
        playSFX(SoundConfig.get(configKey), playSoundProportion,
                SoundConfig.KEYS_SFX_IMMEDIATE_PLAYING.contains(configKey));
    }

    /**
     * Plays new background music and stops old BGM only after all currently playing
     * sfx are finished.
     */
    public void playBGM(String backgroundMusicFileName) {
        LoopPlayerThread newPlayerThread = new LoopPlayerThread(backgroundMusicFileName);
        LoopPlayerThread oldPlayerThread = adjustLastBGMThread(newPlayerThread);

        //interrupt old bgm when starting the new bgm
        newPlayerThread.setPreviousLoopPlayer(oldPlayerThread);

        //wait for playing new bgm until all sfx have finished playing
        newPlayerThread.setPriorThread(lastSFXThread);

        newPlayerThread.start();
    }

    public void playBGMByConfigKey(String configKey) {
        playBGM(SoundConfig.get(configKey));
    }

    public void stopBGM() {
        LoopPlayerThread oldPlayerThread = adjustLastBGMThread(null);
        if (oldPlayerThread != null) oldPlayerThread.interrupt();
    }

    public boolean isBGMPlaying() {
        return (lastBGMThread != null);
    }
}
