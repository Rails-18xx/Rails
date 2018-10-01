package net.sf.rails.javafx;

import java.awt.*;

public class ColorUtils {
    /**
     * Converts the given {@link Color color} object into a rgb string usable in javafx css
     *
     * @param color The color to be converted
     * @return The rgb string
     */
    public static String toRGBString(Color color) {
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();

        return "rgb(" + r + "," + g + ", " + b + ")";
    }

    /**
     * Converts an awt/swing {@link Color} object into a javafx {@link javafx.scene.paint.Color} object
     *
     * @param color The awt/swing color object
     * @return The javafx color object
     */
    public static javafx.scene.paint.Color toColor(Color color) {
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();

        int a = color.getAlpha();
        double opacity = a / 255.0;

        return javafx.scene.paint.Color.rgb(r, g, b, opacity);
    }
}
