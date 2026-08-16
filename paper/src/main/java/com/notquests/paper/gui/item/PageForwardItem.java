package com.notquests.paper.gui.item;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.Click;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.item.AbstractPagedGuiBoundItem;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.ItemWrapper;

public class PageForwardItem extends AbstractPagedGuiBoundItem {
    private final ItemWrapper itemWrapper;

    public PageForwardItem(ItemWrapper itemWrapper) {
        this.itemWrapper = itemWrapper;
    }

    @Override
    public ItemProvider getItemProvider(@NotNull Player player) {
        return itemWrapper;
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull Click click) {
        var gui = getGui();
        if (gui != null && gui.getPage() < gui.getPageCount() - 1) {
            gui.setPage(gui.getPage() + 1);
        }
    }
}
