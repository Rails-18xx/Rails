package net.sf.rails.util;

import java.awt.*;
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.rails.common.ConfigManager;
import net.sf.rails.game.OpenGamesManager;
import net.sf.rails.ui.swing.GameSetupController;

public class RunGame {

    private static final Logger log = LoggerFactory.getLogger(RunGame.class);

    public static void main(String[] args) {
        String fileName = null;
        AtomicBoolean hasStarted = new AtomicBoolean(false);

        if (args != null && args.length > 0) {
            for (String arg : args) {
                log.debug("found arg: {}", arg);
            }
            fileName = args[0];
        }

        // Initialize configuration
        ConfigManager.initConfiguration(false);

        if ( Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.APP_OPEN_FILE)) {
            Desktop.getDesktop().setOpenFileHandler(e -> {
                for ( File file : e.getFiles() ) {
                    // open the file
                    log.debug("received file open event for: {}", file);

                    if ( OpenGamesManager.getInstance().countOfOpenGames() > 0 ) {
                        // unable to currently open more than one game
                        log.warn("Unable to open more than one game");
                        break;
                    }
                    synchronized (hasStarted) {
                        if ( hasStarted.get() ) {
                            log.debug("hiding game setup controller");
                            GameSetupController.getInstance().prepareGameUIInit();
                        } else {
                            hasStarted.set(true);
                        }
                        log.debug("starting passed in game from: {}", file);
                        new Thread(() -> GameLoader.loadAndStartGame(file)).start();
                    }
                    // break out as we only handle one game at a time...
                    break;
                }
            });
        }

        try {
            // sleep for a tad to allow for any open file events to come through before we
            // display the setup controller otherwise it can display briefly and then is hidden
            Thread.sleep(200);
        }
        catch (InterruptedException e) {
            // ignored
        }
        synchronized (hasStarted) {
            if ( ! hasStarted.get() ) {
                if ( fileName != null ) {
                    GameLoader.loadAndStartGame(new File(fileName));
                } else {
                    /* Start the rails.game selector, which will do all the rest. */
                    log.debug("starting game setup controller");
                    GameSetupController.getInstance().show();
                }
                hasStarted.set(true);
            }
        }
    }
}
