package com.notquests.paper.gui.item;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.jetbrains.annotations.NotNull;
import com.notquests.paper.NotQuests;
import com.notquests.paper.gui.GuiContext;
import com.notquests.paper.gui.icon.Button;
import com.notquests.paper.gui.property.types.ListIconProperty;
import com.notquests.paper.structs.QuestPlayer;
import com.notquests.paper.actions.Action;
import com.notquests.paper.conditions.Condition;
import xyz.xenondevs.invui.Click;
import xyz.xenondevs.invui.item.AbstractItem;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.ItemWrapper;

import java.util.ArrayList;
import java.util.List;

public class ActionItem extends AbstractItem {

    private final NotQuests notQuests;
    private final Button icon;
    private final GuiContext guiContext;
    private final ItemWrapper itemWrapper;

    public ActionItem(NotQuests notQuests, ItemWrapper itemWrapper, Button icon, GuiContext guiContext) {
        this.notQuests = notQuests;
        this.itemWrapper = itemWrapper;
        this.icon = icon;
        this.guiContext = guiContext;
    }



    @Override
    public ItemProvider getItemProvider(Player viewer) {
        return itemWrapper;
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull Click click) {

        if (notQuests.getConversationManager() == null) {
            return;
        }

        // get possible actions and replace all placeholders
        var actionProperty = icon.getIconProperty("actions");
        if (actionProperty.isEmpty() || !(actionProperty.get().getValue() instanceof ListIconProperty listActionProperty)) {
            return;
        }
        var replacedActionStrings = replaceWithPlaceholders(listActionProperty.value(), player, guiContext);

        // get possible conditions and replace all placeholders
        var conditionProperty = icon.getIconProperty("conditions");

        var questPlayer = notQuests.getQuestPlayerManager().getActiveQuestPlayer(player.getUniqueId());
        var actions = notQuests.getConversationManager().parseActionString(replacedActionStrings);

        List<Condition> conditions = null;

        if (conditionProperty.isPresent() && actionProperty.get().getValue() instanceof ListIconProperty listConditionProperty) {
            var replacedConditionStrings = replaceWithPlaceholders(listConditionProperty.value(), player, guiContext);
            conditions = notQuests.getConversationManager().parseConditionsString(replacedConditionStrings);
        }

        executeActionsWithConditions(actions, conditions, questPlayer);
    }

    public void executeActionsWithConditions(List<Action> actions, List<Condition> conditions, QuestPlayer questPlayer) {
        actions.forEach(action -> {
            if (conditions != null) {
                conditions.forEach(condition -> action.addCondition(condition, false, null, null));
            }
            notQuests.getActionRunner().executeActionWithConditions(action, questPlayer, questPlayer.getPlayer(), true);
        });
    }

    public List<String> replaceWithPlaceholders(List<String> toReplace, Player player, GuiContext guiContext) {
        var replacedStrings = new ArrayList<String>();

        toReplace.forEach(actionString -> {
            var replacedString = actionString;

            var languageManager = notQuests.getLanguageManager();
            if (!notQuests.getConfiguration().supportPlaceholderAPIInTranslationStrings || !notQuests.getIntegrationsManager().isPlaceholderAPIEnabled() || player == null) {
                replacedString = languageManager.applySpecial(
                        languageManager.applyInternalPlaceholders(replacedString, player, guiContext.getAsObjectArray())
                );
            } else {
                replacedString = languageManager.applySpecial(PlaceholderAPI.setPlaceholders(
                        player, languageManager.applyInternalPlaceholders(
                                replacedString, player, guiContext.getAsObjectArray()
                        ))
                );
            }
            replacedStrings.add(replacedString);
        });
        return replacedStrings;
    }
}
