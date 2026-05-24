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
        // Add variable to store the optional move limit
        int moveLimit = -1;
        AtomicBoolean hasStarted = new AtomicBoolean(false);

        if (args != null && args.length > 0) {
            String inputArg = args[0];
            // Parse optional second argument for move limit
            if (args.length > 1) {
                try {
                    moveLimit = Integer.parseInt(args[1]);
                    log.info("Command line move limit found: {}", moveLimit);
                } catch (NumberFormatException e) {
                    log.warn("Invalid move limit argument ignored: {}", args[1]);
                }
            }

            // Auto-Expand File Names ---
            // Checks for the file in Root, Save, Old_Games, Data.
            // Handles inputs with or without ".rails" extension.
            File file = new File(inputArg);
            if (file.exists()) {
                fileName = inputArg;
            } else {
                String nameWithExt = inputArg.endsWith(".rails") ? inputArg : inputArg + ".rails";
                
                // Search order: Root -> save -> old_games -> data
                String[] candidates = {
                    nameWithExt,                  // Check Root folder first
                    "save/" + nameWithExt,
                    "old_games/" + nameWithExt,
                    "data/" + nameWithExt
                };
                
                for (String path : candidates) {
                    File candidate = new File(path);
                    if (candidate.exists()) {
                        fileName = candidate.getAbsolutePath();
                        break;
                    }
                }
                
                // If still not found, pass the input through (loader will likely fail/prompt)
                if (fileName == null) fileName = inputArg;
            }
        }

        // Initialize configuration
        ConfigManager.initConfiguration(false);

        if ( Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.APP_OPEN_FILE)) {
            Desktop.getDesktop().setOpenFileHandler(e -> {
                for ( File file : e.getFiles() ) {
                    // open the file

                    if ( OpenGamesManager.getInstance().countOfOpenGames() > 0 ) {
                        // unable to currently open more than one game
                        break;
                    }
                    synchronized (hasStarted) {
                        if ( hasStarted.get() ) {
                            GameSetupController.getInstance().prepareGameUIInit();
                        } else {
                            hasStarted.set(true);
                        }
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
                    if (moveLimit > 0) {
                        GameLoader.loadAndStartGame(new File(fileName), moveLimit);
                    } else {
                        GameLoader.loadAndStartGame(new File(fileName));
                    }
                                } else {
                    /* Start the rails.game selector, which will do all the rest. */
                    GameSetupController.getInstance().show();
                }
                hasStarted.set(true);
            }
        }
        // Ensure UI settings, bounds, and scaling are saved on application exit
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutdown hook triggered: Flushing window settings to disk...");
            if (net.sf.rails.ui.swing.GameUIManager.getInstance() != null) {
                net.sf.rails.ui.swing.GameUIManager.getInstance().shutdown();
            }
        }));
    }
}