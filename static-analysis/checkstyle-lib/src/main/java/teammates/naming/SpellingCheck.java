package teammates.naming;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.ExternalResourceHolder;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

public class SpellingCheck extends AbstractCheck implements ExternalResourceHolder {

    public static final String CAMEL_CASE_SPLITER_REGEX = "(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])";

    public static final String MSG = "%s is not a word according to the built-in dictionary";

    public final Set<String> wordDict = new HashSet<String>();

    public final Set<String> additionalWordDict = new HashSet<String>();

    private String dictFile;

    private String additionalDictFile;

    public void setDictFile(String file) throws CheckstyleException {
        if (file == null) {
            throw new CheckstyleException(
                    "property 'dictFile' is missing or invalid in module "
                            + getConfiguration().getName());
        }

        dictFile = file;
    }

    public void setAdditionalDictFile(String file) throws CheckstyleException {
        if (file == null) {
            throw new CheckstyleException(
                    "property 'additionalDictFile' is missing or invalid in module "
                            + getConfiguration().getName());
        }

        additionalDictFile = file;
    }

    @Override
    public void beginTree(DetailAST rootAST) {
        readWordDict();
        readAdditionalDict();
    }

    private void readWordDict() {
        try {
            File input = new File(dictFile);
            Scanner sc = new Scanner(input);
            while (sc.hasNextLine()) {
                wordDict.add(sc.nextLine());
            }
            sc.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void readAdditionalDict() {
        if (additionalDictFile == null) {
            return;
        }

        try {
            File input = new File(additionalDictFile);
            Scanner sc = new Scanner(input);
            while (sc.hasNextLine()) {
                wordDict.add(sc.nextLine());
            }
            sc.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public int[] getDefaultTokens() {
        return getAcceptableTokens();
    }

    @Override
    public int[] getAcceptableTokens() {
        return new int[] {TokenTypes.VARIABLE_DEF, TokenTypes.METHOD_DEF};
    }

    @Override
    public int[] getRequiredTokens() {
        return getAcceptableTokens();
    }

    @Override
    public void visitToken(DetailAST ast) {
        String name = getName(ast);

        String[] wordsWithoutUnderscore = name.split("_");
        for (String segment : wordsWithoutUnderscore) {
            String[] words = segment.split(CAMEL_CASE_SPLITER_REGEX);
            for(String word : words) {
                if (!isNameValid(word.toLowerCase())) {
                    log(ast.getLineNo(), ast.getColumnNo(),
                            String.format(MSG, word), ast.getText());
                }
            }
        }
    }

    private boolean isNameValid(String name) {
        return wordDict.contains(name) || additionalWordDict.contains(name);
    }

    private String getName(DetailAST ast) {
        return ast.findFirstToken(TokenTypes.IDENT).getText();
    }

    @Override
    public Set<String> getExternalResourceLocations() {
        Set<String> set = new HashSet<String>();
        if (additionalDictFile != null) {
            set.add(additionalDictFile);
        }
        set.add(dictFile);
        return set;
    }

}
