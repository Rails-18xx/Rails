package net.sf.rails.ui.swing;

import java.awt.Rectangle;
import java.io.*;
import java.util.*;

import javax.swing.JFrame;

import net.sf.rails.util.SystemOS;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class WindowSettings {

    private Map<String, Rectangle> settings = new HashMap<String, Rectangle>();
    private String filepath;
    private String defaultpath;
    private boolean defaultUsed = false;

    private static final String SETTINGS_FILENAME = "settings_xxxx.rails_ini";
    private static final String SETTINGS_FOLDER = "windowSettings";

    private static final Logger log =
        LoggerFactory.getLogger(WindowSettings.class);

    public WindowSettings (String gameName) {
        File directory = SystemOS.get().getConfigurationFolder(SETTINGS_FOLDER, true);
        defaultpath = directory + File.separator + SETTINGS_FILENAME;
        filepath = defaultpath.replace("xxxx", gameName);
    }

    private Rectangle rectangle (String windowName) {

        if (settings.containsKey(windowName)) {
            return settings.get(windowName);
        } else {
            Rectangle r = new Rectangle(-1, -1, -1, -1);
            settings.put(windowName, r);
            return r;
        }
    }

    public Rectangle getBounds (JFrame w) {
        return rectangle (w.getClass().getSimpleName());
    }

    public boolean isDefaultUsed() {
        return defaultUsed;
    }

    public void load () {
        FileReader file;
        try {
            file = new FileReader (filepath);
        } catch (FileNotFoundException e1) {
            try {
                file = new FileReader (defaultpath);
            } catch (FileNotFoundException e2) {
                return;
            }
            defaultUsed = true;
        }
        try (BufferedReader in = new BufferedReader (file)) {
            String line;
            String[] fields;
            int v;
            Rectangle r;
            while ((line = in.readLine()) != null) {
                fields = line.split("[\\.=]");
                if (fields.length < 3) continue;
                v = Integer.parseInt(fields[2]);
                r = rectangle(fields[0]);
                switch (fields[1].charAt(0)) {
                case 'X': r.x = v; break;
                case 'Y': r.y = v; break;
                case 'W': r.width = v; break;
                case 'H': r.height = v; break;
                }
            }
        } catch (Exception e) {
            log.error("Error while loading {}", filepath, e);
        }
    }

    public void set(JFrame window) {
        if (window != null) {
            // Save one window's settings
            String name = window.getClass().getSimpleName();
            Rectangle r = rectangle (name);
            r.x = window.getX();
            r.y = window.getY();
            r.width = window.getWidth();
            r.height = window.getHeight();
        }
    }

    public void save () {

        // Save all settings to file
        log.debug("Saving all window settings");
        try(PrintWriter out = new PrintWriter (new FileWriter (new File (filepath)))) {
            Rectangle r;
            Set<String> keys = new TreeSet<String> (settings.keySet());
            for (String name : keys) {
                r = settings.get(name);
                out.println(name+".X="+r.x);
                out.println(name+".Y="+r.y);
                out.println(name+".W="+r.width);
                out.println(name+".H="+r.height);
            }
        } catch (Exception e) {
            log.error ("Exception while saving window settings", e);
        }
    }


}

