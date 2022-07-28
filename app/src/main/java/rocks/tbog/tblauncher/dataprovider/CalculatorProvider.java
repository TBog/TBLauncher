package rocks.tbog.tblauncher.dataprovider;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayDeque;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import rocks.tbog.tblauncher.calculator.Calculator;
import rocks.tbog.tblauncher.calculator.Result;
import rocks.tbog.tblauncher.calculator.ShuntingYard;
import rocks.tbog.tblauncher.calculator.Tokenizer;
import rocks.tbog.tblauncher.entry.CalculatorEntry;
import rocks.tbog.tblauncher.searcher.ISearcher;


public class CalculatorProvider extends SimpleProvider<CalculatorEntry> {
    private final Pattern computableRegexp;
    // A regexp to detect plain numbers (including phone numbers)
    private final Pattern numberOnlyRegexp;
    private final NumberFormat LOCALIZED_NUMBER_FORMATTER = NumberFormat.getInstance();

    public CalculatorProvider() {
        //This should try to match as much as possible without going out of the expression,
        //even if the expression is not actually a computable operation.
        computableRegexp = Pattern.compile("^[\\-.,\\d+*×x/÷^'()]+$");
        numberOnlyRegexp = Pattern.compile("^\\+?[.,()\\d]+$");
    }

    @Override
    public void requestResults(String query, ISearcher searcher) {
        String spacelessQuery = query.replaceAll("\\s+", "");
        // Now create matcher object.
        Matcher m = computableRegexp.matcher(spacelessQuery);
        if (m.find()) {
            if (numberOnlyRegexp.matcher(spacelessQuery).find()) {
                return;
            }

            String operation = m.group();

            Result<ArrayDeque<Tokenizer.Token>> tokenized = Tokenizer.tokenize(operation);
            String readableResult;

            if (tokenized.syntacticalError) {
                return;
            } else if (tokenized.arithmeticalError) {
                return;
            } else {
                Result<ArrayDeque<Tokenizer.Token>> posfixed = ShuntingYard.infixToPostfix(tokenized.result);

                if (posfixed.syntacticalError) {
                    return;
                } else if (posfixed.arithmeticalError) {
                    return;
                } else {
                    Result<BigDecimal> result = Calculator.calculateExpression(posfixed.result);

                    if (result.syntacticalError) {
                        return;
                    } else if (result.arithmeticalError) {
                        return;
                    } else {
                        String localizedNumber = LOCALIZED_NUMBER_FORMATTER.format(result.result);
                        readableResult = " = " + localizedNumber;
                    }
                }
            }

            String queryProcessed = operation + readableResult;
            CalculatorEntry pojo = new CalculatorEntry(queryProcessed);
            pojo.setRelevance(pojo.normalizedName, null);

            pojo.boostRelevance(19);
            searcher.addResult(pojo);
        }
    }
}
