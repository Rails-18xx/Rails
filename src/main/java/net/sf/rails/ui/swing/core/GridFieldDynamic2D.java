package net.sf.rails.ui.swing.core;

import javax.swing.JComponent;

import net.sf.rails.game.state.Item;

public class GridFieldDynamic2D extends GridField {
    
    private Accessor2D<? extends Item,? extends Item> textAccessor;
    private Accessor2D<? extends Item,? extends Item> tooltipAccessor;
    private Accessor2D<? extends Item,? extends Item> colorAccessor;

    GridFieldDynamic2D(Accessor2D<? extends Item,? extends Item> text) {
        this.textAccessor = text;
    }
    
    GridFieldDynamic2D setTooltip(Accessor2D<? extends Item,? extends Item> tooltip) {
        this.tooltipAccessor = tooltip;
        return this;
    }

    GridFieldDynamic2D setColor(Accessor2D<? extends Item,? extends Item> color) {
        this.colorAccessor = color;
        return this;
    }

    @Override
    TableField toTableField(JComponent component, Item rowItem, Item colItem) {
        // TODO Auto-generated method stub
        return null;
    }


}
