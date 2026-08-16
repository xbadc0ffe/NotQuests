package com.notquests.paper.gui.icon;

import net.kyori.adventure.text.Component;
import org.apache.logging.log4j.core.util.Integers;
import com.notquests.paper.NotQuests;
import com.notquests.paper.gui.GuiContext;
import com.notquests.paper.gui.item.*;
import com.notquests.paper.gui.property.IconProperty;
import com.notquests.paper.gui.property.types.StringIconProperty;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemWrapper;

import java.util.*;
import java.util.stream.Collectors;

public class Button {
    private final ButtonType type;
    private final Set<IconProperty> iconProperties;
    private final Set<String> itemKeys;
    private final List<Icon> icons;

    public Button(
            ButtonType type,
            Set<IconProperty> iconProperties,
            Set<String> itemKeys
    ) {
        this.type = type;
        this.iconProperties = iconProperties;
        this.itemKeys = itemKeys;
        this.icons = new ArrayList<>();
    }

    public void registerIcons(Map<String, Icon> possibleItems) {
        possibleItems.forEach((key, item) -> {
            if (itemKeys.contains(key)) {
                icons.add(item);
            }
        });
    }

    public Item buildItem(NotQuests notQuests, GuiContext guiContext) {
        switch (type) {
            case FORWARD -> {
                var itemStackInWrapper = new ItemWrapper(ItemHelper.assembleItemStack(icons.get(0), notQuests,  guiContext));
                return new PageForwardItem(itemStackInWrapper);
            }
            case BACK -> {
                var itemStackInWrapper = new ItemWrapper(ItemHelper.assembleItemStack(icons.get(0), notQuests, guiContext));
                return new PageBackItem(itemStackInWrapper);
            }
            case UP -> {
                var itemStackInWrapper = new ItemWrapper(ItemHelper.assembleItemStack(icons.get(0), notQuests,  guiContext));
                return new ScrollUpItem(itemStackInWrapper);
            }
            case DOWN -> {
                var itemStackInWrapper = new ItemWrapper(ItemHelper.assembleItemStack(icons.get(0), notQuests,  guiContext));
                return new ScrollDownItem(itemStackInWrapper);
            }
            case PAGED -> {

            }
            case TAB -> {
                var itemStackInWrapper = new ItemWrapper(ItemHelper.assembleItemStack(icons.get(0), notQuests, guiContext));
                var tabIndexPropertyOpt = iconProperties.stream().filter(iconProperty -> iconProperty.getKey().equals("tabindex")).findFirst();
                if (tabIndexPropertyOpt.isEmpty()) {
                    return Item.simple(itemStackInWrapper);
                }
                if (!(tabIndexPropertyOpt.get().getValue() instanceof  StringIconProperty stringIconProperty)) {
                    return Item.simple(itemStackInWrapper);
                }
                var newTitleProperty = iconProperties.stream().filter(iconProperty -> iconProperty.getKey().equals("tabtitle")).findFirst();
                Component newTitle = null;
                if (newTitleProperty.isPresent() && newTitleProperty.get().getValue() instanceof StringIconProperty stringTitleProperty) {
                    newTitle = notQuests.getLanguageManager().getComponent(
                            stringTitleProperty.value(), guiContext.getPlayer(), guiContext.getAsObjectArray()
                    );
                }
                return new CustomTabItem(
                        Integers.parseInt(stringIconProperty.value()),
                        newTitle,
                        icons.stream().map(icon -> ItemHelper.assembleItemStack(
                                icon, notQuests, guiContext
                        )).collect(Collectors.toList()));
            }
            case ACTION -> {
                var itemStackInWrapper = new ItemWrapper(ItemHelper.assembleItemStack(icons.get(0), notQuests, guiContext));
                return new ActionItem(notQuests, itemStackInWrapper, this, guiContext);
            }
            default -> {
                var itemStackInWrapper = new ItemWrapper(ItemHelper.assembleItemStack(icons.get(0), notQuests, guiContext));
                return Item.simple(itemStackInWrapper);
            }
        }
        var itemStackInWrapper = new ItemWrapper(ItemHelper.assembleItemStack(icons.get(0), notQuests, guiContext));
        return Item.simple(itemStackInWrapper);
    }

    public ButtonType getType() {
        return type;
    }

    public Set<IconProperty> getIconProperties() {
        return iconProperties;
    }
    public Optional<IconProperty> getIconProperty(String key) {
        return iconProperties.stream().filter(iconProperty -> iconProperty.getKey().equals(key)).findFirst();
    }

    public List<Icon> getItems() {
        return icons;
    }
}
