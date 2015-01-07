package net.sf.rails.ui.swing.core;

import java.awt.Color;

import javax.swing.JComponent;
import javax.swing.JLabel;

import net.sf.rails.game.state.ColorModel;
import net.sf.rails.game.state.Observable;
import net.sf.rails.game.state.Observer;

public class TableField extends ItemUI {

    private final JComponent component;
    
    private final Observer textObserver;
    private final Observer tooltipObserver;
    private final Observer colorObserver;
    
    private TableField(ItemUI parent, String id, JComponent component, String text, String tooltip, 
            Color backgroundColor, Color foregroundColor,
            Observer textObserver, Observer tooltipObserver, Observer colorObserver) {
        super(parent, id);
        
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
        if (backgroundColor != null) {
            initBackgroundColor = backgroundColor;
        } else if (colorObserver != null) {
            initBackgroundColor = ((ColorModel)colorObserver.getObservable()).getBackground();
        }

        if (initBackgroundColor != null) {
            component.setBackground(initBackgroundColor);
        }
        
        // initialize foreground color
        Color initForegroundColor = null;
        if (foregroundColor != null) {
            initForegroundColor = foregroundColor;
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

    public static Builder buildLabel(ItemUI parent, String id) {
        return new Builder(parent, id, new JLabel());
    }
    
    public static class Builder {
        
        private final ItemUI parent;
        private final String id;
        
        private final JComponent component;

        private String text;
        private String tooltip;
        private Color backgroundColor;
        private Color foregroundColor;
        
        private Observer textObserver;
        private Observer tooltipObserver;
        private Observer colorObserver;
        
        private Builder(ItemUI parent, String id, JComponent component) {
            this.parent = parent;
            this.id = id;
            this.component = component;
        }
        
        public TableField build() {
            return new TableField(parent, id, component, text, tooltip, backgroundColor, foregroundColor,
                    textObserver, tooltipObserver, colorObserver);
        }

        public void setText(String text) {
            this.text = text;
        }
        
        public void setText(final Observable observable) {
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
        }
        
        public void setTooltip(String tooltip) {
            this.tooltip = tooltip;
        }

        public void setTooltip(final Observable observable) {
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
        }
        
        public void setBackgroundColor(Color color) {
            this.backgroundColor = color;
        }
        
        public void setForegroundColor(Color color) {
            this.foregroundColor = color;
        }

        public void setColorModel(final ColorModel model) {
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
        }
    }

    
}
