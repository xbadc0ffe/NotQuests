package com.notquests.paper.variables;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import com.notquests.paper.NotQuests;
import com.notquests.paper.commands.arguments.variables.BooleanVariableValueParser;
import com.notquests.paper.commands.arguments.variables.NumberVariableValueParser;
import com.notquests.paper.commands.arguments.variables.StringVariableValueParser;
import com.notquests.paper.commands.framework.NQFlag;
import com.notquests.paper.managers.expressions.NumberExpression;
import com.notquests.paper.builtin.objectives.ConditionWatch;
import com.notquests.paper.builtin.objectives.NumberVariable;
import com.notquests.paper.structs.ActiveObjective;
import com.notquests.paper.structs.ActiveQuest;
import com.notquests.paper.structs.QuestPlayer;
import com.notquests.paper.conditions.*;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class Variable<T> {
    protected final NotQuests main;
    private final ArrayList<StringVariableValueParser<CommandSender>> requiredStrings;
    private final ArrayList<NumberVariableValueParser<CommandSender>> requiredNumbers;
    private final ArrayList<BooleanVariableValueParser<CommandSender>> requiredBooleans;
    private final ArrayList<NQFlag> requiredBooleanFlags;

    private final ArrayList<String> setOnlyRequiredValues = new ArrayList<>(); //TODO: Implement
    private final ArrayList<String> getOnlyRequiredValues = new ArrayList<>(); //TODO: Implement
    private final VariableDataType variableDataType;
    private HashMap<String, String> additionalStringArguments;
    private HashMap<String, NumberExpression> additionalNumberArguments;
    private HashMap<String, NumberExpression> additionalBooleanArguments;
    private boolean canSetValue = false;


    public Variable(final NotQuests main){
        this.main = main;
        requiredStrings = new ArrayList<>();
        requiredNumbers = new ArrayList<>();
        requiredBooleans = new ArrayList<>();
        requiredBooleanFlags = new ArrayList<>();
        additionalStringArguments = new HashMap<>();
        additionalNumberArguments = new HashMap<>();
        additionalBooleanArguments = new HashMap<>();


        Class<T> typeOf = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        if(typeOf == String.class || typeOf == Character.class){
            variableDataType = VariableDataType.STRING;
        }else if(typeOf == Boolean.class){
            variableDataType = VariableDataType.BOOLEAN;
        }else if(typeOf == String[].class){
            variableDataType = VariableDataType.LIST;
        }else if(typeOf == ItemStack[].class){
            variableDataType = VariableDataType.ITEMSTACKLIST;
        }else if(typeOf == ArrayList.class){
            main.getLogManager().warn("Error: ArrayList variables are not supported yet. Using LIST variable...");
            variableDataType = VariableDataType.LIST;
        }else{
            variableDataType = VariableDataType.NUMBER;
        }
    }

    public final ArrayList<String> getSetOnlyRequiredValues() {
        return setOnlyRequiredValues;
    }

    public final ArrayList<String> getGetOnlyRequiredValues() {
        return getOnlyRequiredValues;
    }

    public final HashMap<String, String> getAdditionalStringArguments() {
        return additionalStringArguments;
    }

    public final HashMap<String, NumberExpression> getAdditionalBooleanArguments() {
        return additionalBooleanArguments;
    }

    public void addSetOnlyRequiredValue(final String value) {
        setOnlyRequiredValues.add(value);
    }

    public void addGetOnlyRequiredValue(final String value) {
        getOnlyRequiredValues.add(value);
    }

    public final VariableDataType getVariableDataType(){
        return variableDataType;
    }

    public final boolean isCanSetValue(){
        return canSetValue;
    }

    protected void setCanSetValue(final boolean canSetValue){
        this.canSetValue = canSetValue;
    }

    protected void addRequiredString(final StringVariableValueParser<CommandSender> stringArgument) {
        requiredStrings.add(stringArgument);
    }

    protected void addRequiredNumber(final NumberVariableValueParser<CommandSender> numberVariableValueArgument){
        requiredNumbers.add(numberVariableValueArgument);
    }

    protected void addRequiredBoolean(final BooleanVariableValueParser<CommandSender> booleanArgument){
        requiredBooleans.add(booleanArgument);
    }

    protected void addRequiredBooleanFlag(final NQFlag commandFlag){
        requiredBooleanFlags.add(commandFlag);
    }

    public final ArrayList<StringVariableValueParser<CommandSender>> getRequiredStrings(){
        return requiredStrings;
    }

    public final ArrayList<NumberVariableValueParser<CommandSender>> getRequiredNumbers(){
        return requiredNumbers;
    }

    public final ArrayList<BooleanVariableValueParser<CommandSender>> getRequiredBooleans(){
        return requiredBooleans;
    }

    public final ArrayList<NQFlag> getRequiredBooleanFlags() {
        return requiredBooleanFlags;
    }

    public final HashMap<String, NumberExpression> getAdditionalNumberArguments() {
        return additionalNumberArguments;
    }

    protected final String getRequiredStringValue(final String key) {
        return additionalStringArguments.getOrDefault(key, "");
    }

    public final T getValue(final QuestPlayer questPlayer, final Object... objects){
        if(Bukkit.isPrimaryThread()){
            return getValueInternally(questPlayer, objects);
        }else {
            main.getLogManager().severe("Trying to get a variable value from a non-primary thread! This is may not work. Please report this to the developer!");
            T toReturn = null;
            try {
                toReturn = getValueInternally(questPlayer, objects);
            }catch (Exception e){
                e.printStackTrace();
            }
            return toReturn;
        }
    }
    public abstract T getValueInternally(final QuestPlayer questPlayer, final Object... objects);

    public final boolean setValue(final T newValue, final QuestPlayer questPlayer, final Object... objects) {
        if (!isCanSetValue()) {
            return false;
        }

        boolean result;
        if(Bukkit.isPrimaryThread()){
            result = setValueInternally(newValue, questPlayer, objects);
        }else {
            main.getLogManager().severe("Trying to set a variable value from a non-primary thread! This is may not work. Please report this to the developer!");
            try {
                result = setValueInternally(newValue, questPlayer, objects);
            }catch (Exception e){
                e.printStackTrace();
                return false;
            }
        }


        if (questPlayer != null) {
            if(questPlayer.isHasActiveConditionObjectives() || questPlayer.isHasActiveVariableObjectives()){
                for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                    for (final ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
                        if (ConditionWatch.isConditionWatch(activeObjective.getObjective())) {
                            if (!activeObjective.isUnlocked()) {
                                continue;
                            }
                            if(ConditionWatch.watchedVariableName(activeObjective.getObjective()).equalsIgnoreCase(getVariableType())){
                                ConditionWatch.updateProgressIfFulfilled(activeObjective, questPlayer);
                            }
                        } else if (NumberVariable.isNumberVariable(activeObjective.getObjective())) {
                            if (!activeObjective.isUnlocked() || getVariableDataType() != VariableDataType.NUMBER) {
                                continue;
                            }

                            if(NumberVariable.variableName(activeObjective.getObjective()).equalsIgnoreCase(getVariableType())){
                                //double newValueDouble = (double) newValue;
                                NumberVariable.updateProgress(main, activeObjective);
                            }
                        }
                    }
                    activeQuest.removeCompletedObjectives(true);
                }
                questPlayer.removeCompletedQuests();
            }
        }


        return result;

    }

    public abstract boolean setValueInternally(final T newValue, final QuestPlayer questPlayer, final Object... objects);

    public abstract List<String> getPossibleValues(final QuestPlayer questPlayer, final Object... objects);

    public final String getVariableType() {
        return main.getVariableCatalog().getVariableType(this);
    }

    public abstract String getPlural();

    public abstract String getSingular();

    protected final double getRequiredNumberValue(final String key, final QuestPlayer questPlayer) {
        return additionalNumberArguments.get(key).calculateValue(questPlayer);
    }

    protected final boolean getRequiredBooleanValue(final String key, final QuestPlayer questPlayer) {
        final NumberExpression numberExpression = additionalBooleanArguments.getOrDefault(key, null);
        if (numberExpression != null) {
            return numberExpression.calculateBooleanValue(questPlayer);
        } else {
            return false;
        }
    }

    public void addAdditionalBooleanArgument(final String key, final NumberExpression value) {
        additionalBooleanArguments.put(key, value);
    }

    public void addAdditionalNumberArgument(final String key, final NumberExpression value) {
        additionalNumberArguments.put(key, value);
    }

    public void addAdditionalStringArgument(final String key, final String value) {
        additionalStringArguments.put(key, value);
    }

    public void setAdditionalStringArguments(final HashMap<String, String> additionalStringArguments) {
        this.additionalStringArguments = additionalStringArguments;
    }

    public void setAdditionalNumberArguments(final HashMap<String, NumberExpression> additionalNumberArguments) {
        this.additionalNumberArguments = additionalNumberArguments;
    }

    public void setAdditionalBooleanArguments(final HashMap<String, NumberExpression> additionalBooleanArguments) {
        this.additionalBooleanArguments = additionalBooleanArguments;
    }

}
