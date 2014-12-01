package net.sf.rails.ui.swing.core;

import javax.swing.JComponent;
import javax.swing.JLabel;

import net.sf.rails.game.state.ColorModel;
import net.sf.rails.game.state.Observable;
import net.sf.rails.game.state.Observer;

public class FieldUI extends ItemUI {
    
    private final JComponent component;
    
    private final Observer text;
    private final Observer tooltip;
    private final Observer color;
    
    private FieldUI(ItemUI parent, String id, JComponent component, Observer text, Observer tooltip, Observer color) {
        super(parent, id);
        this.component = component;
        this.text = text;
        this.tooltip = tooltip;
        this.color = color;
        if (text != null) {
            text.getObservable().addObserver(text);
        }
        if (tooltip != null) {
            tooltip.getObservable().addObserver(tooltip);
        }
        if (color != null) {
            color.getObservable().addObserver(color);
        }
    }
    
    
    public JComponent getUI() {
        return component;
    }
    
    public void clearObservers() {
         if (text != null) {
             text.getObservable().removeObserver(text);
         }
         if (tooltip != null) {
             tooltip.getObservable().removeObserver(tooltip);
         }
         if (color != null) {
             color.getObservable().removeObserver(color);
         }
    }

    public static Builder buildLabel(ItemUI parent, String id) {
        return new Builder(parent, id, new JLabel());
    }
    
    public static class Builder {
        
        private final ItemUI parent;
        private final String id;
        
        private final JComponent component;

        private Observer textObserver;
        private Observer tooltipObserver;
        private Observer colorObserver;
        
        private Builder(ItemUI parent, String id, JComponent component) {
            this.parent = parent;
            this.id = id;
            this.component = component;
        }
        
        public FieldUI build() {
            return new FieldUI(parent, id, component, textObserver, tooltipObserver, colorObserver);
        }

        public void setTextObservable(final Observable observable) {
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

        public void setTooltipObservable(final Observable observable) {
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
