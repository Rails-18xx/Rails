package rails.ui.swing;

import java.awt.Rectangle;
import java.io.*;
import java.util.*;

import javax.swing.JFrame;

import org.apache.log4j.Logger;

import rails.util.Config;

public class WindowSettings {

    private Map<String, Rectangle> settings = new HashMap<String, Rectangle>();
    private String filepath;
    private String defaultpath;
    private boolean defaultUsed = false;

    private static final String settingsfilename = "settings_xxxx.rails_ini";

    protected static Logger log =
        Logger.getLogger(WindowSettings.class.getPackage().getName());

    public WindowSettings (String gameName) {
        String directory = System.getProperty("settings.directory");
        if (directory == null) directory = Config.get("save.directory");
        defaultpath = directory + File.separator + settingsfilename;
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
        BufferedReader in;
        try {
            in = new BufferedReader (file);
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
            in.close();
        } catch (Exception e) {
            log.error ("Error while loading "+filepath, e);
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
        return;
    }

    public void save () {

        // Save all settings to file
        log.debug("Saving all window settings");
        try {
            PrintWriter out = new PrintWriter (new FileWriter (new File (filepath)));
            Rectangle r;
            Set<String> keys = new TreeSet<String> (settings.keySet());
            for (String name : keys) {
                r = settings.get(name);
                out.println(name+".X="+r.x);
                out.println(name+".Y="+r.y);
                out.println(name+".W="+r.width);
                out.println(name+".H="+r.height);
            }
            out.close();
        } catch (Exception e) {
            log.error ("Exception while saving window settings", e);
        }

    }


}

