package com.notquests.paper.gui.item;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.Click;
import xyz.xenondevs.invui.item.AbstractScrollGuiBoundItem;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.ItemWrapper;

public class ScrollUpItem extends AbstractScrollGuiBoundItem {
    private final ItemWrapper itemWrapper;

    public ScrollUpItem(ItemWrapper itemWrapper) {
        this.itemWrapper = itemWrapper;
    }

    @Override
    public ItemProvider getItemProvider(@NotNull Player player) {
        return itemWrapper;
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull Click click) {
        var gui = getGui();
        if (gui != null && gui.getLine() > 0) {
            gui.setLine(gui.getLine() - 1);
        }
    }
}
