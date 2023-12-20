package io.github.givimad.whisperjni;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * The {@link WhisperGrammar} class represents a native whisper.cpp parsed grammar.
 * <p>
 * You need to dispose the native memory for its instances by calling {@link #close}
 *
 * @author Miguel Álvarez Díez - Initial contribution
 */
public class WhisperGrammar extends WhisperJNI.WhisperJNIPointer {
    private final WhisperJNI whisper;
    private final String grammarText;

    /**
     * Internal context constructor
     *
     * @param whisper library instance
     * @param ref     native pointer identifier
     */
    protected WhisperGrammar(WhisperJNI whisper, int ref, String text) {
        super(ref);
        this.whisper = whisper;
        this.grammarText = text;
    }

    @Override
    public void close() {
        whisper.free(this);
    }


    /**
     * Java implementation of a GBNF grammar validator.
     * Asserts the provided grammar is valid to use with whisper.cpp.
     * Meaning it must contain a root expression with termination
     * which sub-expressions can be resolved.
     *
     * @param grammar path to a GBNF grammar.
     * @throws ParseException if grammar is invalid.
     */
    public static void assertValidGrammar(Path grammar) throws ParseException, IOException {
        if (!Files.exists(grammar)) {
            throw new ParseException("Grammar file does not exists.", 0);
        }
        assertValidGrammar(Files.readString(grammar));
    }

    /**
     * Java implementation of a whisper.cpp grammar validator.
     * Asserts the provided grammar is valid to use with whisper.cpp.
     * Meaning it must contain a root expression with termination
     * which subexpressions can be resolved.
     *
     * @param grammarText GBNF grammar text.
     * @throws ParseException if grammar is invalid.
     */
    public static void assertValidGrammar(String grammarText) throws ParseException {
        if (grammarText.isBlank()) {
            throw new ParseException("Empty grammar.", 0);
        }
        Map<String, String> expressions = parseExpressionsText(grammarText);
        var rootExpression = expressions.get("root");
        if (rootExpression == null) {
            throw new IllegalArgumentException("Missing root expression.");
        }
        assertValidExpression(expressions, rootExpression, new ArrayList<>(), new HashSet<>(), true);
    }

    /**
     * Parse and map expressions text by name.
     *
     * @param gbnfGrammar A GBNF grammar.
     * @return map of expressions text by name.
     * @throws ParseException if unable to parse unique expressions.
     */
    private static Map<String, String> parseExpressionsText(String gbnfGrammar) throws ParseException {
        String currentExpressionName = "";
        StringBuilder currentExpression = new StringBuilder();
        HashMap<String, String> expressions = new HashMap<>();
        String assignSign = "::=";
        String[] split = gbnfGrammar.split("\n");
        for (int i = 0; i < split.length; i++) {
            String line = split[i];
            boolean isLast = i == split.length - 1;
            if (line.isBlank()) {
                // Skip empty lines
                continue;
            }
            if (line.trim().startsWith("#")) {
                // Skip comments
                continue;
            }
            boolean start = line.contains(assignSign);
            if (!start && currentExpressionName.isEmpty()) {
                throw new ParseException("Grammar should start with an expression", 0);
            }
            if (start) {
                if (!currentExpressionName.isBlank()) {
                    expressions.put(currentExpressionName, currentExpression.toString());
                }
                String[] parts = line.split(assignSign);
                currentExpressionName = parts[0].trim();
                if (currentExpressionName.isEmpty()) {
                    throw new ParseException("Missed expression name: " + line, 0);
                }
                currentExpression = new StringBuilder(parts[1].trim());
                if (expressions.containsKey(currentExpressionName)) {
                    throw new ParseException("Duplicated expression: " + currentExpressionName, 0);
                }
                continue;
            }
            currentExpression.append(" ").append(line.trim());
            if (isLast) {
                String expression = currentExpression.toString();
                if (expression.isBlank()) {
                    throw new ParseException("Missed expression value for: " + currentExpressionName, 0);
                }
                expressions.put(currentExpressionName, expression);
            }
        }
        if (!expressions.containsKey(currentExpressionName)) {
            String expression = currentExpression.toString();
            if (expression.isBlank()) {
                throw new ParseException("Missed expression value for: " + currentExpressionName, 0);
            }
            expressions.put(currentExpressionName, expression);
        }
        return expressions;
    }

    /**
     * Asserts that the provided text is a correct GBNF expression that can be resolved with valid subexpressions.
     * This method calls itself recursively in order to detect missing or cyclic subexpressions.
     *
     * @param expressions       map of available expressions.
     * @param expressionText    GBNF expression.
     * @param parentExpressions List to prevent cyclic use of expressions.
     * @param validExpressions  Store expressions already validated.
     * @param shouldTerminate   Validate expression ends with termination char.
     * @throws ParseException if expression is invalid.
     */
    private static void assertValidExpression(Map<String, String> expressions, String expressionText,
                                              ArrayList<String> parentExpressions, HashSet<String> validExpressions, boolean shouldTerminate)
            throws ParseException {
        // System.err.println("SEGMENT: " + expressionText);
        boolean onText = false;
        boolean onRegex = false;
        int startRegexIndex = 0;
        int onGroup = 0;
        StringBuilder groupExpression = new StringBuilder();
        String[] tokens = expressionText.trim().split("\\s+");
        // check cyclic recursion
        if (tokens.length == 1 && expressions.containsKey(tokens[0])) {
            if (parentExpressions.contains(tokens[0])) {
                throw new ParseException("Cyclic resolution of expression: " + tokens[0], 0);
            }
            parentExpressions = new ArrayList<>(parentExpressions);
            parentExpressions.add(tokens[0]);
        }
        for (int i = 0; i < tokens.length; i++) {
            var token = tokens[i];
            var isLast = i == tokens.length - 1;
            if (token.isBlank()) {
                continue;
            }
            if (!onText && !onRegex) {
                boolean isGroupStart = token.startsWith("(");
                boolean isOptionalGroupEnd = token.endsWith(")?");
                boolean isGroupEnd = token.endsWith(")") || isOptionalGroupEnd;
                if (isGroupStart) {
                    // System.err.println("CHECK GROUP START - " + token);
                    int index = 0;
                    while (token.substring(index).startsWith("(")){
                        onGroup += 1;
                        index ++;
                    }
                }
                if (isGroupEnd) {
                    // System.err.println("CHECK GROUP END - " + token);
                    int index = token.length();
                    String tmpToken = token.substring(0, index);
                    while (tmpToken.endsWith(")")|| tmpToken.endsWith(")?")){
                        onGroup -= 1;
                        index -= tmpToken.endsWith("?") ? 2: 1;
                        tmpToken = token.substring(0, index);
                    }
                    if (onGroup < 0) {
                        throw new ParseException("Missing group open", 0);
                    }
                    if (token.length() > 1) {
                        groupExpression.append(" ").append(token, isGroupStart ? 1 : 0, token.length() - (isOptionalGroupEnd ? 2 : 1));
                    }
                    assertValidExpression(expressions, groupExpression.toString(), parentExpressions, validExpressions,
                            isLast && shouldTerminate);
                    groupExpression = new StringBuilder();
                    continue;
                } else if (onGroup > 0) {
                    if (isGroupStart) {
                        groupExpression.append(token.substring(1));
                    } else {
                        groupExpression.append(" ").append(token);
                    }
                    continue;
                }
            }
            if (!onText && !onRegex && token.startsWith("\"")) {
                onText = true;
                // System.err.println("CHECK TEXT START - " + token);
                if (token.length() == 1) {
                    continue;
                }
            }
            if (!onRegex && onGroup == 0 && (token.endsWith("\"") || token.endsWith("\"?"))) {
                // System.err.println("CHECK TEXT STOP - " + token);
                if (!onText) {
                    throw new ParseException("Missing string open on segment: " + expressionText, 0);
                }
                onText = false;
                continue;
            }
            if (onText) {
                if (isLast && shouldTerminate && !token.endsWith(".")) {
                    throw new ParseException("Root expression resolution should end with a dot.", 0);
                }
                continue;
            }
            if (!onRegex && onGroup == 0 && token.startsWith("[")) {
                // System.err.println("CHECK REGEX START");
                onRegex = true;
                startRegexIndex = i;
                if (token.length() == 1) {
                    continue;
                }
            }
            if (onGroup == 0
                    && (token.endsWith("]") || token.endsWith("]?") || token.endsWith("]+") || token.endsWith("]*"))) {
                if (!onRegex) {
                    throw new ParseException("Missing regex open on segment: ", 0);
                }
                // System.err.println("CHECK REGEX END");
                onRegex = false;
                if (isLast && shouldTerminate) {
                    throw new ParseException("Root expression resolution should end with a dot.", 0);
                }
                String regexExpression = String.join(" ", Arrays.copyOfRange(tokens, startRegexIndex, i+1));
                try {
                    // System.err.println("CHECK REGEX - " + regexExpression);
                    String regexText = regexExpression.substring(1, regexExpression.lastIndexOf("]"));
                    Pattern.compile(regexText);
                } catch (PatternSyntaxException e) {
                    throw new ParseException("Invalid regex expression: " + regexExpression, 0);
                }
                continue;
            }
            if (onRegex) {
                continue;
            }
            if (token.equals("|")) {
                assertValidExpression(expressions, String.join(" ", Arrays.copyOfRange(tokens, i + 1, tokens.length)),
                        parentExpressions, validExpressions, shouldTerminate);
                break;
            }
            String subExpression = token;
            if (subExpression.endsWith("?")) {
                subExpression = subExpression.substring(0, subExpression.length() - 1);
            }
            var subExpressionValue = expressions.get(subExpression);
            if (subExpressionValue == null) {
                throw new ParseException("Unable to resolve expression: " + subExpression, 0);
            }
            if ((!isLast || !shouldTerminate) && validExpressions.contains(subExpression)) {
                // System.err.println("CHECK ALREADY RESOLVED - " + token);
                continue;
            }
            parentExpressions = new ArrayList<>(parentExpressions);
            parentExpressions.add(subExpression);
            assertValidExpression(expressions, subExpressionValue, parentExpressions, validExpressions,
                    isLast && shouldTerminate);
            if (!isLast || !shouldTerminate) {
                validExpressions.add(subExpression);
            }
        }
        if (onText) {
            throw new ParseException("Unclosed text at: " + expressionText, 0);
        }
        if (onRegex) {
            throw new ParseException("Unclosed regex at: " + expressionText, 0);
        }
        if (onGroup > 1) {
            throw new ParseException("Unclosed group at: " + expressionText, 0);
        }
    }
}
