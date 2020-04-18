package net.sf.rails.util;

import java.awt.*;
import java.awt.desktop.OpenFilesEvent;
import java.awt.desktop.OpenFilesHandler;
import java.io.File;

import javax.swing.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.rails.common.ConfigManager;
import net.sf.rails.ui.swing.AutoSaveLoadDialog;
import net.sf.rails.ui.swing.GameSetupController;

public class RunGame {

    private static final Logger log = LoggerFactory.getLogger(RunGame.class);

    public static void main(String[] args) {
        final String[] fileName = { null };

        if (args != null && args.length > 0) {
            for (String arg : args) {
                System.out.println ("Arg: "+arg);
            }
            fileName[0] = args[0];
        }

        if ( Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.APP_OPEN_FILE)) {
            Desktop.getDesktop().setOpenFileHandler(e -> {
                for ( File file : e.getFiles() ) {
                    // open the file
                    fileName[0] = file.toString();
                    // break out as we only handle one game at a time...
                    break;
                }
            });
        }

        // Initialize configuration
        ConfigManager.initConfiguration(false);

        // currently we only start one game
        if ( fileName[0] != null ) {
            GameLoader.loadAndStartGame(new File(fileName[0]));
        } else {
            /* Start the rails.game selector, which will do all the rest. */
            GameSetupController.Builder setupBuilder = GameSetupController.builder();
            setupBuilder.start();
        }
    }
}
