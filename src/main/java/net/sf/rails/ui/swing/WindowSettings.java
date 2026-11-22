package net.sf.rails.ui.swing;

import java.awt.Rectangle;
import java.io.*;
import java.util.*;

import javax.swing.JFrame;

import net.sf.rails.util.SystemOS;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WindowSettings {

    private final Map<String, Rectangle> settings = new HashMap<>();
    private final Map<String, String> properties = new HashMap<>();
    private final String filepath;
    private final String defaultpath;
    private boolean defaultUsed = false;

    private static final String SETTINGS_FILENAME = "settings_xxxx.rails_ini";
    private static final String SETTINGS_FOLDER = "windowSettings";

    private static final Logger log = LoggerFactory.getLogger(WindowSettings.class);

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

    public void setProperty(String key, String value) {
        properties.put(key, value);
    }

    public String getProperty(String key) {
        return properties.get(key);
    }
    public double getDoubleProperty(String key, double defaultValue) {
        String val = properties.get(key);
        if (val != null) {
            try { return Double.parseDouble(val); } catch (NumberFormatException e) {}
        }
        return defaultValue;
    }

    public void setDoubleProperty(String key, double value) {
        properties.put(key, String.valueOf(value));
    }

 
    public boolean isDefaultUsed() {
        return defaultUsed;
    }

    public void load () {
        FileReader file;
        try {
            // log.info("WindowSettings: Attempting to load settings from '{}'", filepath);
            file = new FileReader (filepath);
        } catch (FileNotFoundException e1) {
            // log.warn("WindowSettings: User settings file not found at '{}'. Attempting default.", filepath);
            try {
                file = new FileReader (defaultpath);
            } catch (FileNotFoundException e2) {
                log.warn("WindowSettings: Default settings file not found at '{}'. Aborting load.", defaultpath);
                return;
            }
            defaultUsed = true;
        }
        try (BufferedReader in = new BufferedReader (file)) {
            String line;
            while ((line = in.readLine()) != null) {
                // Split on the first '=' only to separate key and value
                String[] parts = line.split("=", 2);
                if (parts.length < 2) {
                    continue;
                }
                
                String key = parts[0].trim();
                String value = parts[1].trim();

                // Check if this is a window dimension (Ends in .X, .Y, .W, .H)
                if (key.endsWith(".X") || key.endsWith(".Y") || key.endsWith(".W") || key.endsWith(".H")) {
                    try {
                        String windowName = key.substring(0, key.length() - 2);
                        char dimension = key.charAt(key.length() - 1);
                        int v = Integer.parseInt(value);
                        Rectangle r = rectangle(windowName);
                        
                        switch (dimension) {
                            case 'X': r.x = v; break;
                            case 'Y': r.y = v; break;
                            case 'W': r.width = v; break;
                            case 'H': r.height = v; break;
                        }
                    } catch (Exception e) {
                        log.warn("Failed to parse window setting line: {}", line);
                    }
                } else {
                    // It is a generic property (like font.ui.scale)
                    properties.put(key, value);
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
            
            // Save generic properties
            Set<String> propKeys = new TreeSet<>(properties.keySet());
  

            for (String key : propKeys) {
                String val = properties.get(key);
                out.println(key + "=" + val);
            }
            
            out.flush(); // Force write to disk


        } catch (Exception e) {
            log.error ("Exception while saving window settings", e);
            e.printStackTrace();
        }
    }
}