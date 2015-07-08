package net.sf.rails.ui.swing.core;

import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

/**
 * GridFieldFormat stores format instructions (Borders, Colors etc.) for a GridTable
 */
public class GridFieldFormat {

    private final Border border;
    private final GridColors colors;
    private final int horizontalAligment;

    private GridFieldFormat(Border border, GridColors colors, int horizontalAlignment) {
        this.border = border;
        this.colors = colors;
        this.horizontalAligment = horizontalAlignment;
    }
    
    public Border getBorder() {
        return border;
    }
    
    public GridColors getColors() {
        return colors;
    }
    
    public int getHorizontalAlignment() {
        return horizontalAligment;
    }
    
    public static Builder builder() {
        return new Builder();
    }
     
    public static class Builder {
        
        private Border border = BorderFactory.createEmptyBorder(1, 2, 1, 2);
        private Color background = null;
        private Color foreground = Color.BLACK;
        private int horizontalAlignment = SwingConstants.CENTER;
        
        private Builder() {}
        
        public Builder setBorder(Border border) {
            this.border = border;
            return this;
        }

        public Builder setBackground(Color background) {
            this.background = background;
            return this;
        }

        public Builder setForeground(Color foreground) {
            this.foreground = foreground;
            return this;
        }
        
        public Builder setHorizontalAlignment(int horizontalAlignment) {
            this.horizontalAlignment = horizontalAlignment;
            return this;
        }
        
        public GridFieldFormat build() {
            GridColors colors = new GridColors(background, foreground);
            return new GridFieldFormat(border, colors, horizontalAlignment);
        }
    }
}
