package net.sf.rails.ui.swing.core;

import com.google.common.base.Preconditions;

import net.sf.rails.game.state.ColorModel;
import net.sf.rails.game.state.Item;
import net.sf.rails.game.state.Observable;

class GridFieldDynamic1D extends GridField {

    private final Accessor1D<? extends Item> textAccessor;
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
    
    @Override
    TableField toTableField(Item rowItem, Item colItem) {
        Preconditions.checkArgument(rowItem == null ^ colItem == null, "Expected to be either rowItem or colItem to be null");
        
        Item item = (rowItem != null) ? rowItem : colItem;
        Preconditions.checkArgument(item.getClass() == textAccessor.getItemClass(), 
                "Expected item class to equal accessor class. However item class is %s, accessor class is %s",
                item.getClass(), textAccessor.getItemClass());
        
        return buildTableField( item);
    }
        

    private TableField buildTableField(Item item) {
        TableField.Builder builder = buildDefaults();
        
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
