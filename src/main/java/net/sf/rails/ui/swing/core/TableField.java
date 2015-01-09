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
    
    private TableField(JComponent component, String text, String tooltip, GridColors colors,
            Observer textObserver, Observer tooltipObserver, Observer colorObserver) {
        
        this.component = component;
        
        this.textObserver = textObserver;
        this.tooltipObserver = tooltipObserver;
        this.colorObserver = colorObserver;
        
        // initialize text
        String initText = null;
        if (text != null) {
            initText = text;
        } else if (textObserver != null) {
            initText = textObserver.getObservable().toText();
        }
 
        if (initText != null) {
            if (component instanceof JLabel) {
                ((JLabel)component).setText(text);
            }
        }
        
        // initialize tooltip
        String initTooltip = null;
        if (tooltip != null) {
            initTooltip = tooltip;
        } else if (tooltipObserver != null) {
            initTooltip = tooltipObserver.getObservable().toText();
        }
        
        if (initTooltip != null) {
            component.setToolTipText(initTooltip);
        }
        
        // initialize background color
        Color initBackgroundColor = null;
        if (colors != null && colors.background != null) {
            initBackgroundColor = colors.background;
        } else if (colorObserver != null) {
            initBackgroundColor = ((ColorModel)colorObserver.getObservable()).getBackground();
        }

        if (initBackgroundColor != null) {
            component.setBackground(initBackgroundColor);
        }

        // initialize foreground color
        Color initForegroundColor = null;
        if (colors != null && colors.foreground != null) {
            initForegroundColor = colors.foreground;
        } else if (colorObserver != null) {
            initForegroundColor = ((ColorModel)colorObserver.getObservable()).getForeground();
        }

        if (initForegroundColor != null) {
            component.setForeground(initForegroundColor);
        }
        
        // add observers
        if (textObserver != null) {
            textObserver.getObservable().addObserver(textObserver);
        }
        if (tooltipObserver != null) {
            tooltipObserver.getObservable().addObserver(tooltipObserver);
        }
        if (colorObserver != null) {
            colorObserver.getObservable().addObserver(colorObserver);
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
            return new TableField(component, text, tooltip, colors,
                    textObserver, tooltipObserver, colorObserver);
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
