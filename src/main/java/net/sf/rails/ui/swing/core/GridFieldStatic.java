package net.sf.rails.ui.swing.core;

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
    TableField toTableField(Item rowItem, Item colItem) {
        return buildTableField();
    }
    
    private TableField buildTableField() {
        TableField.Builder builder = buildDefaults();
        
        if (textObservable != null) {
            builder.setText(textObservable);
        }
        if (tooltipObservable != null) {
            builder.setTooltip(tooltipObservable);
        }
        if (colorModel != null) {
            builder.setColors(colorModel);
        }
        return builder.build();
    }

    
}
