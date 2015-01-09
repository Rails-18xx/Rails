package net.sf.rails.ui.swing.core;

import javax.swing.JComponent;

import net.sf.rails.game.state.ColorModel;
import net.sf.rails.game.state.Item;
import net.sf.rails.game.state.Observable;

class GridFieldDynamic1D extends GridField {
    
    private Accessor1D<? extends Item> textAccessor;
    private Accessor1D<? extends Item> tooltipAccessor;
    private Accessor1D<? extends Item> colorAccessor;
    
    GridFieldDynamic1D(Accessor1D<? extends Item> text) {
        this.textAccessor = text;
    }

    GridFieldDynamic1D setTooltip(Accessor1D<? extends Item> tooltip) {
        this.tooltipAccessor = tooltip;
        return this;
    }

    GridFieldDynamic1D setColor(Accessor1D<? extends Item> color) {
        this.colorAccessor = color;
        return this;
    }

    TableField buildTableField(JComponent component, Item item) {
        TableField.Builder builder = TableField.builder(component);

        buildDefaults(builder);
        
        if (textAccessor instanceof Accessor1D.AText) {
            String text = ((Accessor1D.AText<?>)textAccessor).get(item);
            builder.setText(text);
        }
        if (textAccessor instanceof Accessor1D.AObservable) {
            Observable text = ((Accessor1D.AObservable<?>)textAccessor).get(item);
            builder.setText(text);
        }
        
        if (tooltipAccessor instanceof Accessor1D.AText) {
            String tooltip = ((Accessor1D.AText<?>)tooltipAccessor).get(item);
            builder.setTooltip(tooltip);
        }
        if (tooltipAccessor instanceof Accessor1D.AObservable) {
            Observable tooltip = ((Accessor1D.AObservable<?>)tooltipAccessor).get(item);
            builder.setTooltip(tooltip);
        }

        if (colorAccessor instanceof Accessor1D.AColors) {
            GridColors colors = ((Accessor1D.AColors<?>)colorAccessor).get(item);
            builder.setColors(colors);
        }
        if (colorAccessor instanceof Accessor1D.AColorModel) {
            ColorModel colorModel = ((Accessor1D.AColorModel<?>)colorAccessor).get(item);
            builder.setColors(colorModel);
        }

        return builder.build();
    }
}
