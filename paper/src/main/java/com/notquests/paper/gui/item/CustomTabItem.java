package com.notquests.paper.gui.item;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.Click;
import xyz.xenondevs.invui.item.AbstractTabGuiBoundItem;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.ItemWrapper;

import java.util.List;

public class CustomTabItem extends AbstractTabGuiBoundItem {

    private final int tab;
    private final List<ItemStack> itemStacks;
    private final Component newTitle;
    public CustomTabItem(int tab, Component newTitle, List<ItemStack> itemStacks) {
        this.tab = tab;
        this.itemStacks = itemStacks;
        this.newTitle = newTitle;
    }

    @Override
    public ItemProvider getItemProvider(@NotNull Player player) {
        var activeTabItem = itemStacks.get(0);
        var inactiveTabItem = itemStacks.get(0);
        if (itemStacks.size() > 1) {
            inactiveTabItem = itemStacks.get(1);
        }
        var gui = getGui();
        if (gui != null && gui.getTab() == tab) {
            return new ItemWrapper(activeTabItem);
        } else {
            return new ItemWrapper(inactiveTabItem);
        }
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull Click click) {
        var gui = getGui();
        if (gui != null) {
            gui.setTab(tab);
            if (newTitle != null) {
                var windows = gui.getWindows();
                windows.stream()
                        .filter(w -> w.getViewer().equals(player))
                        .findFirst()
                        .ifPresent(w -> w.setTitle(newTitle));
            }
        }
    }
}
