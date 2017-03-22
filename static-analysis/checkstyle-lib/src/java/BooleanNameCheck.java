package teammates.naming;

import java.util.Arrays;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

public class BooleanNameCheck extends AbstractCheck {

    public static final String MSG_NAMING = "Boolean variable name should start with %s";

    public static final String MSG_NEGATION = "Avoid boolean variables that represent the negation of a thing. %s";
    
    public static final String[] DEFAULT_PREFIXES = new String[] {"is", "has", "can", "should"};

    private String[] allowedPrefixes;

    public BooleanNameCheck() {
        allowedPrefixes = DEFAULT_PREFIXES;
    }

    public void setAllowedPrefixes(String[] values) {
        if (values == null) {
            return;
        }
        allowedPrefixes = values;
    }

    @Override
    public int[] getDefaultTokens() {
        return getAcceptableTokens();
    }

    @Override
    public int[] getAcceptableTokens() {
        return new int[] {TokenTypes.VARIABLE_DEF};
    }

    @Override
    public int[] getRequiredTokens() {
        return getAcceptableTokens();
    }

    @Override
    public void visitToken(DetailAST ast) {
        if (!isBooleanVariableDefinition(ast)) {
            return;
        }

        String variableName = getVariableName(ast);
        for (String allowedPrefix : allowedPrefixes) {
            if (variableName.startsWith(allowedPrefix)) {
                if (isNameNegationOfThing(variableName, allowedPrefix)) {
                    log(ast.getLineNo(), ast.getColumnNo(),
                            String.format(MSG_NEGATION, variableName), ast.getText());
                }
                return;
            }
        }

        log(ast.getLineNo(), ast.getColumnNo(),
                String.format(MSG_NAMING, Arrays.toString(allowedPrefixes)), ast.getText());
    }

    private boolean isBooleanVariableDefinition(DetailAST ast) {
        DetailAST variableType = ast.findFirstToken(TokenTypes.TYPE);
        return variableType.findFirstToken(TokenTypes.LITERAL_BOOLEAN) != null;
    }

    private String getVariableName(DetailAST ast) {
        return ast.findFirstToken(TokenTypes.IDENT).getText();
    }
    
    private boolean isNameNegationOfThing(String variableName, String prefix) {
        return variableName.replace(prefix, "").toLowerCase().startsWith("not");
    }

}
