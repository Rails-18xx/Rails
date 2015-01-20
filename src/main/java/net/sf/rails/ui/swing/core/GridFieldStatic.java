package net.sf.rails.ui.swing.core;

import javax.swing.JComponent;

import net.sf.rails.game.state.ColorModel;
import net.sf.rails.game.state.Item;
import net.sf.rails.game.state.Observable;

class GridFieldStatic extends GridField {

    private Observable textObservable;
    private Observable tooltipObservable;
    private ColorModel colorModel;
    
    GridFieldStatic(Observable text) {
        this.textObservable = text;
    }
    
    GridFieldStatic(String text) {
        setText(text);
    }
    
    GridFieldStatic setTooltip(Observable tooltip) {
        this.tooltipObservable = tooltip;
        return this;
    }
    
    GridFieldStatic setColorModel(ColorModel colorModel) {
        this.colorModel = colorModel;
        return this;
    }
    
    @Override
    TableField toTableField(JComponent component, Item rowItem, Item colItem) {
        return buildTableField(component);
    }
    
    private TableField buildTableField(JComponent component) {
        TableField.Builder builder = TableField.builder(component);
        return buildDefaults(builder)
                .setText(textObservable).setTooltip(tooltipObservable).setColors(colorModel)
                .build();
    }

    
}
