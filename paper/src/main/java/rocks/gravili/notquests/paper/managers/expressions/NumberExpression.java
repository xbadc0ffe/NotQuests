package rocks.gravili.notquests.paper.managers.expressions;

import org.bukkit.command.CommandSender;
import redempt.crunch.CompiledExpression;
import redempt.crunch.Crunch;
import redempt.crunch.functional.EvaluationEnvironment;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.variables.BooleanVariableValueParser;
import rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableValueParser;
import rocks.gravili.notquests.paper.commands.arguments.variables.StringVariableValueParser;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.variables.Variable;
import rocks.gravili.notquests.paper.structs.variables.VariableDataType;

/**
 * The NumberExpression generates a Compiled Expression based on an Expression String, which supports NotQuests variables.
 * The expression is compiled the NumberExpression object is created only once, and potential static results are chaches.
 * This ensures the highest performance - especially during runtime - in exchange for slightly slower load times.
 * <p>
 * //TODO: Support static/cached results for static/final variables which won't change (if there are such variables). Because right now, any present variable will make result not static. Such variables are very rare though.
 */
public class NumberExpression {
    private final NotQuests main;

    /**
     * Raw expression string
     */
    private final String expression;

    /**
     * Evaluation environment, to which the lazy variables, and how to lazily evaluate them, are added
     */
    private final EvaluationEnvironment evaluationEnvironment = new EvaluationEnvironment();

    /**
     * The expression gets compiled into a compiledExpression, which is a lot faster. With this, the result can be calculated (if the questplayer is updated)
     */
    private final CompiledExpression compiledExpression;

    /**
     * Amount of variables used in the expression string. If this is zero, the expression result is basically static/final and will be cached
     */
    private int variableCounter = 0;

    /**
     * Needed for compiling the evaluation, as well as calculating the result. This variable needs to be updated before calculating the expression result, if it's not static
     */
    private QuestPlayer questPlayerToEvaluate;

    /**
     * If the expression is always the same (= it has no variables which might be dynamic), the result will be cached here.
     */
    private double cachedStaticResult;

    /**
     * Determines if the expression is static or not. This is set to true if no variables are found (=> if variableCounter is 0)
     */
    private boolean resultStatic = false;


    public NumberExpression(final NotQuests main, final String expression) {
        this.main = main;
        this.expression = expression;


        //From here on, the Evaluation Environment, as well as the compiled expression will be initialized. If the expression is static,
        //The static result will be cached here as well
        final String modifiedExpression = getExpressionAndGenerateEnv(expression);
        compiledExpression = Crunch.compileExpression(modifiedExpression, evaluationEnvironment);

        if (variableCounter == 0) {
            cachedStaticResult = compiledExpression.evaluate();
            resultStatic = true;
        }
    }

    private NumberExpression(final NotQuests main, final double staticValue) {
        this.main = main;
        this.expression = "" + staticValue;

        cachedStaticResult = staticValue;
        resultStatic = true;

        compiledExpression = null;
    }

    public static NumberExpression ofStatic(final NotQuests main, final double staticValue) {
        return new NumberExpression(main, staticValue);
    }


    /**
     * @param questPlayer The QuestPlayer for which the variables present in the expression will be calculated
     * @return The final result of the expression. It either evaluates the pre-compiled expression, or returns a static, cached result.
     */
    public final double calculateValue(final QuestPlayer questPlayer) {
        if (isResultStatic()) {
            return cachedStaticResult;
        } else {
            this.questPlayerToEvaluate = questPlayer;
            return compiledExpression.evaluate();
        }
    }

    /**
     * @param questPlayer The QuestPlayer for which the variables present in the expression will be calculated
     * @return This returns true if the result of the calculateValue() double is bigger than 0.98. Otherwise, it returns false.
     */
    public final boolean calculateBooleanValue(final QuestPlayer questPlayer) {
        return calculateValue(questPlayer) >= 0.98d;
    }

    /**
     * @return if the result of the number expression is static/final (meaning it can be cached, as it never changes).
     * This is the case if, for example, the expression is a simple "1" or any math expression without any variables.
     */
    private boolean isResultStatic() {
        return resultStatic;
    }

    /**
     * @return the raw expression string which was used the create the Number Expression
     */
    public final String getRawExpression() {
        return expression;
    }

    /**
     * @param expressions current state of the expression (either raw or partially processed by getExpressionAndGenerateEnv)
     * @return replaced expression string with modified variable names which will be fed into the evaluationEnvironment
     */
    private String getExpressionAndGenerateEnv(String expressions) {
        boolean foundAVariable = false;

        for (String variableString : main.getVariablesManager().getVariableIdentifiers()) {
            if (!expressions.contains(variableString)) {
                continue;
            }
            final Variable<?> variable = main.getVariablesManager().getVariableFromString(variableString);
            if (variable == null || (variable.getVariableDataType() != VariableDataType.NUMBER && variable.getVariableDataType() != VariableDataType.BOOLEAN)) {
                main.getLogManager().debug("Null variable: <highlight>" + variableString);
                continue;
            }

            //Extra Arguments:
            if (expressions.contains(variableString + "(")) {
                foundAVariable = true;
                final String everythingAfterBracket = expressions.substring(expressions.indexOf(variableString + "(") + variableString.length() + 1);
                final String insideBracket = everythingAfterBracket.substring(0, everythingAfterBracket.indexOf(")"));
                main.getLogManager().debug("Inside Bracket: " + insideBracket);
                final String[] extraArguments = insideBracket.split(",");
                for (final String extraArgument : extraArguments) {
                    main.getLogManager().debug("Extra: " + extraArgument);
                    if (extraArgument.startsWith("--")) {
                        variable.addAdditionalBooleanArgument(extraArgument.replace("--", ""), new NumberExpression(main, "true"));
                        main.getLogManager().debug("AddBoolFlag: " + extraArgument.replace("--", ""));
                    } else {
                        final String[] split = extraArgument.split(":");
                        final String key = split[0];
                        final String value = split[1];
                        for (StringVariableValueParser<CommandSender> stringParser : variable.getRequiredStrings()) {
                            if (stringParser.getIdentifier().equalsIgnoreCase(key)) {
                                variable.addAdditionalStringArgument(key, value);
                                main.getLogManager().debug("AddString: " + key + " val: " + value);
                            }
                        }
                        for (NumberVariableValueParser<CommandSender> numberParser : variable.getRequiredNumbers()) {
                            if (numberParser.getIdentifier().equalsIgnoreCase(key)) {
                                variable.addAdditionalNumberArgument(key, new NumberExpression(main, value));
                                main.getLogManager().debug("AddNumb: " + key + " val: " + value);
                            }
                        }
                        for (BooleanVariableValueParser<CommandSender> booleanParser : variable.getRequiredBooleans()) {
                            if (booleanParser.getIdentifier().equalsIgnoreCase(key)) {
                                variable.addAdditionalBooleanArgument(key, new NumberExpression(main, value));
                                main.getLogManager().debug("AddBool: " + key + " val: " + value);
                            }

                        }
                    }
                }

                variableString = variableString + "(" + insideBracket + ")"; //For the replacing with the actual number below
            }


            final String newVariableName = "var" + ++variableCounter;
            expressions = expressions.replace(variableString, newVariableName);
            evaluationEnvironment.addLazyVariable(newVariableName, () -> {
                final Object valueObject = variable.getValue(questPlayerToEvaluate);
                if (valueObject instanceof final Number n) {
                    return n.doubleValue();
                } else if (valueObject instanceof final Boolean b) {
                    return b ? 1 : 0;
                }
                return 0;
            });
        }
        if (!foundAVariable) {
            return expressions;
        }

        return getExpressionAndGenerateEnv(expressions);
    }


}
