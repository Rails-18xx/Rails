package net.sf.rails.ui.swing.core;

import java.awt.Color;

import javax.swing.JComponent;
import javax.swing.JLabel;

import net.sf.rails.game.state.ColorModel;
import net.sf.rails.game.state.Observable;
import net.sf.rails.game.state.Observer;

public class TableField {

    private final JComponent component;
    
    private final Observer textObserver;
    private final Observer tooltipObserver;
    private final Observer colorObserver;
    
    private TableField(Builder builder) {
        
        this.component = builder.component;
        
        this.textObserver = builder.textObserver;
        this.tooltipObserver = builder.tooltipObserver;
        this.colorObserver = builder.colorObserver;
        
        // initialize text
        String initText = null;
        if (builder.text != null) {
            initText = builder.text;
        } else if (textObserver != null) {
            initText = textObserver.getObservable().toText();
        }
 
        if (initText != null) {
            if (component instanceof JLabel) {
                ((JLabel)component).setText(initText);
            }
        }
        
        // initialize tooltip
        String initTooltip = null;
        if (builder.tooltip != null) {
            initTooltip = builder.tooltip;
        } else if (tooltipObserver != null) {
            initTooltip = tooltipObserver.getObservable().toText();
        }
        
        if (initTooltip != null) {
            component.setToolTipText(initTooltip);
        }
        
        // initialize background color
        Color initBackgroundColor = null;
        if (builder.colors != null && builder.colors.background != null) {
            initBackgroundColor = builder.colors.background;
        } else if (colorObserver != null) {
            initBackgroundColor = ((ColorModel)colorObserver.getObservable()).getBackground();
        }

        if (initBackgroundColor != null) {
            component.setBackground(initBackgroundColor);
            component.setOpaque(true);
        }

        // initialize foreground color
        Color initForegroundColor = null;
        if (builder.colors != null && builder.colors.foreground != null) {
            initForegroundColor = builder.colors.foreground;
        } else if (colorObserver != null) {
            initForegroundColor = ((ColorModel)colorObserver.getObservable()).getForeground();
        }

        if (initForegroundColor != null) {
            component.setForeground(initForegroundColor);
        }
        
    }
    
    public JComponent getUI() {
        return component;
    }
    
    public void clearObservers() {
         if (textObserver != null) {
             textObserver.getObservable().removeObserver(textObserver);
         }
         if (tooltipObserver != null) {
             tooltipObserver.getObservable().removeObserver(tooltipObserver);
         }
         if (colorObserver != null) {
             colorObserver.getObservable().removeObserver(colorObserver);
         }
    }

    public static Builder builder(JComponent component) {
        return new Builder(component);
    }
    
    static class Builder {
        
        private final JComponent component;

        private String text;
        private String tooltip;
        private GridColors colors;
        
        private Observer textObserver;
        private Observer tooltipObserver;
        private Observer colorObserver;
        
        private Builder(JComponent component) {
            this.component = component;
        }
        
        TableField build() {
            return new TableField(this);
        }

        Builder setText(String text) {
            this.text = text;
            return this;
        }
        
        Builder setText(final Observable observable) {
            textObserver = new Observer() {

                @Override
                public void update(String text) {
                    if (component instanceof JLabel) {
                        ((JLabel)component).setText(text);
                    }
                }

                @Override
                public Observable getObservable() {
                    return observable;
                }
            };
            observable.addObserver(textObserver);
            return this;
        }
        
        Builder setTooltip(String tooltip) {
            this.tooltip = tooltip;
            return this;
        }

        Builder setTooltip(final Observable observable) {
            tooltipObserver = new Observer() {

                @Override
                public void update(String text) {
                    component.setToolTipText(text);
                }

                @Override
                public Observable getObservable() {
                    return observable;
                }
            };
            observable.addObserver(tooltipObserver);
            return this;
        }
        
        Builder setColors(GridColors colors) {
            this.colors = colors;
            return this;
        }
        
        Builder setColors(final ColorModel model) {
            colorObserver = new Observer() {

                @Override
                public void update(String text) {
                    component.setBackground(model.getBackground());
                    component.setForeground(model.getForeground());
                }

                @Override
                public Observable getObservable() {
                    return model;
                }
            };
            model.addObserver(colorObserver);
            return this;
        }
    }

    
}
