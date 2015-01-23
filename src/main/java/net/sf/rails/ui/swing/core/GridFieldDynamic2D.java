package net.sf.rails.ui.swing.core;

import com.google.common.base.Preconditions;

import net.sf.rails.game.state.ColorModel;
import net.sf.rails.game.state.Item;
import net.sf.rails.game.state.Observable;

public class GridFieldDynamic2D extends GridField {
    
    private final Accessor2D<? extends Item,? extends Item> textAccessor;
    private final Class<? extends Item> itemTClass;
    private final Class<? extends Item> itemSClass;
    private Accessor2D<? extends Item,? extends Item> tooltipAccessor;
    private Accessor2D<? extends Item,? extends Item> colorAccessor;

    GridFieldDynamic2D(Accessor2D<? extends Item,? extends Item> text) {
        this.textAccessor = text;
        this.itemTClass = text.getItemTClass();
        this.itemSClass = text.getItemSClass();
    }
    
    GridFieldDynamic2D setTooltip(Accessor2D<? extends Item,? extends Item> tooltip) {
        Preconditions.checkArgument(tooltip.getItemTClass() == itemTClass  && tooltip.getItemTClass() == itemSClass,
                "Tooltip accessor not compatible with Text accesors");
        this.tooltipAccessor = tooltip;
        return this;
    }

    GridFieldDynamic2D setColor(Accessor2D<? extends Item,? extends Item> color) {
        Preconditions.checkArgument(color.getItemTClass() == itemTClass  && color.getItemTClass() == itemSClass,
                "Color accessor not compatible with Text accesors");
        this.colorAccessor = color;
        return this;
    }

    @Override
    TableField toTableField(Item rowItem, Item colItem) {
        Preconditions.checkArgument(rowItem != null && colItem != null, "Neither rowItem or colItem can be null");
        
        Class<? extends Item> rowClass = rowItem.getClass();
        Class<? extends Item> colClass = colItem.getClass();
        
        Preconditions.checkArgument(rowClass == itemTClass || colClass == itemTClass, "Either rowClass or colClass have to be of %s", itemTClass );
        Preconditions.checkArgument(rowClass == itemSClass || colClass == itemSClass, "Either rowClass or colClass have to be of %s", itemSClass );
        
        // preconditions check that both itemT and itemS classes are available, thus only ordering has still to be checked
        if (rowClass == itemTClass) {
            return buildTableField(rowItem, colItem);
        } else {
            return buildTableField(colItem, rowItem);
        }
    }
    
    private TableField buildTableField(Item itemT, Item itemS) {
        TableField.Builder builder = buildDefaults();
                
        if (textAccessor instanceof Accessor2D.AText) {
            String text = ((Accessor2D.AText<?,?>)textAccessor).get(itemT, itemS);
            builder.setText(text);
        }
        if (textAccessor instanceof Accessor2D.AObservable) {
            Observable text = ((Accessor2D.AObservable<?,?>)textAccessor).get(itemT, itemS);
            builder.setText(text);
        }
        
        if (tooltipAccessor instanceof Accessor2D.AText) {
            String tooltip = ((Accessor2D.AText<?,?>)tooltipAccessor).get(itemT, itemS);
            builder.setTooltip(tooltip);
        }
        if (tooltipAccessor instanceof Accessor2D.AObservable) {
            Observable tooltip = ((Accessor2D.AObservable<?,?>)tooltipAccessor).get(itemT, itemS);
            builder.setTooltip(tooltip);
        }

        if (colorAccessor instanceof Accessor2D.AColors) {
            GridColors colors = ((Accessor2D.AColors<?,?>)colorAccessor).get(itemT, itemS);
            builder.setColors(colors);
        }
        if (colorAccessor instanceof Accessor2D.AColorModel) {
            ColorModel colorModel = ((Accessor2D.AColorModel<?,?>)colorAccessor).get(itemT, itemS);
            builder.setColors(colorModel);
        }

        return builder.build();
    }

}
