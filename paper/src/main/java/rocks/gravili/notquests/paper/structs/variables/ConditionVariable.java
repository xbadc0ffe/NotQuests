package rocks.gravili.notquests.paper.structs.variables;


import org.bukkit.entity.Player;
import redempt.crunch.CompiledExpression;
import redempt.crunch.Crunch;
import redempt.crunch.functional.EvaluationEnvironment;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.variables.StringVariableValueParser;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.ArrayList;
import java.util.List;

public class ConditionVariable extends Variable<Boolean> {

    private final EvaluationEnvironment env = new EvaluationEnvironment();
    Variable<?> cachedVariable = null;
    private CompiledExpression exp;
    private int variableCounter = 0;
    private Player playerToEvaluate = null;
    private QuestPlayer questPlayerToEvaluate = null;

    public ConditionVariable(NotQuests main) {
        super(main);

        addRequiredString(StringVariableValueParser.of("Conditions", null, (context, input) -> {
            ArrayList<String> suggestions = new ArrayList<>();
            for (String conditionIdentifier :
                    main.getConditionsYMLManager().getConditionsAndIdentifiers().keySet()) {
                if (input.endsWith(conditionIdentifier)) {
                    suggestions.add(input + "&");
                    suggestions.add(input + "|");
                } else {
                    suggestions.add(conditionIdentifier);
                }
            }
            return suggestions;
        }));
    }

    public final String getExpression() {
        return getRequiredStringValue("Conditions");
    }

    @Override
    public Boolean getValueInternally(QuestPlayer questPlayer, Object... objects) {
        this.playerToEvaluate = questPlayer.getPlayer();
        this.questPlayerToEvaluate = questPlayer;
        initializeExpressionAndCachedVariable();

        return exp.evaluate() >= 0.98d;
    }

    public final String getExpressionAndGenerateEnv(String expressions) {
        boolean foundOne = false;
        for (final String conditionIdentifier :
                main.getConditionsYMLManager().getConditionsAndIdentifiers().keySet()) {
            if (!expressions.contains(conditionIdentifier)) {
                continue;
            }
            foundOne = true;

            variableCounter++;
            expressions = expressions.replace(conditionIdentifier, "var" + variableCounter);
            env.addLazyVariable(
                    "var" + variableCounter,
                    () -> main.getConditionsYMLManager()
                            .getCondition(conditionIdentifier)
                            .check(questPlayerToEvaluate)
                            .fulfilled()
                            ? 1
                            : 0);
        }
        if (!foundOne) {
            return expressions;
        }

        return getExpressionAndGenerateEnv(expressions);
    }

    public void initializeExpressionAndCachedVariable() {
        if (exp == null) {
            String expression = getExpressionAndGenerateEnv(getExpression());
            exp = Crunch.compileExpression(expression, env);
        }
    }

    @Override
    public boolean setValueInternally(Boolean newValue, QuestPlayer questPlayer, Object... objects) {
        return false;
    }

    @Override
    public List<String> getPossibleValues(QuestPlayer questPlayer, Object... objects) {
        return null;
    }

    @Override
    public String getPlural() {
        return "Conditions";
    }

    @Override
    public String getSingular() {
        return "Condition";
    }
}
