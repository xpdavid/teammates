package teammates.test.cases.util;

import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Test;

import teammates.common.util.Const;
import teammates.common.util.FieldValidator;
import teammates.common.util.StringHelper;
import teammates.test.cases.BaseTestCase;

public class StringHelperTest extends BaseTestCase {

    @Test
    public void testGenerateStringOfLength() {

        assertEquals(5, StringHelper.generateStringOfLength(5).length());
        assertEquals(0, StringHelper.generateStringOfLength(0).length());
    }

    @Test
    public void testIsWhiteSpace() {

        assertTrue(StringHelper.isWhiteSpace(""));
        assertTrue(StringHelper.isWhiteSpace("       "));
        assertTrue(StringHelper.isWhiteSpace("\t\n\t"));
        assertTrue(StringHelper.isWhiteSpace(Const.EOL));
        assertTrue(StringHelper.isWhiteSpace(Const.EOL + "   "));
    }

    @Test
    public void testIsMatching() {
        assertTrue(StringHelper.isMatching("\u00E0", "à"));
        assertTrue(StringHelper.isMatching("\u0061\u0300", "à"));
        assertFalse(StringHelper.isMatching("Héllo", "Hello"));
    }

    @Test
    public void testIsAnyMatching() {
        //this method is used in header row processing in StudentAttributesFactory: locateColumnIndexes
        //so use this to test the various header field regex expressions here

        List<String> regexList = FieldValidator.REGEX_COLUMN_NAME;
        String[] stringsToMatch = {"names", "name", " name ", " names ", "student name", "students names",
                                   "student names", "students name", "full name", "full names", "full   names",
                                   "student full names", "students full    names", "Names", "NAMES", "Full Names",
                                   "FULL NAMES", "Full Name", "Student Full Name", "Name"};
        verifyRegexMatch(stringsToMatch, regexList, true);

        stringsToMatch = new String[]{"namess", "nam", "student", "full"};
        verifyRegexMatch(stringsToMatch, regexList, false);

        regexList = FieldValidator.REGEX_COLUMN_SECTION;
        stringsToMatch = new String[]{"section", "sections", "sect", "sec", "course sections", "courses sections",
                                      "course section", "course sections", "course sec", "courses sec", "Section",
                                      "SECTIONS", "Sect", "Sec", "Course Section", "Course Sections"};
        verifyRegexMatch(stringsToMatch, regexList, true);

        stringsToMatch = new String[]{"secc", "Section 1", "Course 1"};
        verifyRegexMatch(stringsToMatch, regexList, false);

        regexList = FieldValidator.REGEX_COLUMN_TEAM;
        stringsToMatch = new String[]{"team", "teams", "Team", "TEAMS", "group", "Group",
                                      "Groups", "GROUPS", "student teams", "students teams ", "student team",
                                      "students team", "STUDENT TEAM", "Student Teams ", "Student groups",
                                      "Student Groups", "student   groups", "student   teams", "Course Teams",
                                      "courses teams", "course   team", "courses team", "COURSE TEAM"};
        verifyRegexMatch(stringsToMatch, regexList, true);

        stringsToMatch = new String[]{"tea", "Team 1", "Group 1"};
        verifyRegexMatch(stringsToMatch, regexList, false);

        regexList = FieldValidator.REGEX_COLUMN_EMAIL;
        stringsToMatch = new String[]{"email", "emails", " email ", " Email ", " Emails", "EMAILS", "EMAIL",
                                      "mail", "Mail", "MAIL", "MAILS", "E-mail", "E-MAILS", "E-mail", "E-mails",
                                      "e mails", "E mails", "E  mail", "E MAIL", "E MAILS", "Email address",
                                      "email addresses", "EMAIL addresses", "email   addresses", "E-mail addresses",
                                      "E-mail  addresses", "Contact", "CONTACT", "contacts"};
        verifyRegexMatch(stringsToMatch, regexList, true);

        stringsToMatch = new String[]{"emai", "test@gmail.com", "address1"};
        verifyRegexMatch(stringsToMatch, regexList, false);

        regexList = FieldValidator.REGEX_COLUMN_COMMENT;
        stringsToMatch = new String[]{"comment", "Comment", "COMMENT", "comments", "Comments", " COMMENTS ",
                                      "note", "Note", "NOTE", "notes", "Notes", "  NOTES  "};
        verifyRegexMatch(stringsToMatch, regexList, true);

        stringsToMatch = new String[]{"this is a comment", "this is a note", "one comment, one note"};
        verifyRegexMatch(stringsToMatch, regexList, false);

    }

    @Test
    public void testToString() {
        ArrayList<String> strings = new ArrayList<String>();
        assertEquals("", StringHelper.toString(strings, ""));
        assertEquals("", StringHelper.toString(strings, "<br>"));

        strings.add("aaa");
        assertEquals("aaa", StringHelper.toString(strings, ""));
        assertEquals("aaa", StringHelper.toString(strings, "\n"));
        assertEquals("aaa", StringHelper.toString(strings, "<br>"));

        strings.add("bbb");
        assertEquals("aaabbb", StringHelper.toString(strings, ""));
        assertEquals("aaa\nbbb", StringHelper.toString(strings, "\n"));
        assertEquals("aaa<br>bbb", StringHelper.toString(strings, "<br>"));

        ArrayList<Integer> ints = new ArrayList<Integer>();
        ints.add(1);
        ints.add(44);
        assertEquals("1\n44", StringHelper.toString(ints, "\n"));
    }

    @Test
    public void testKeyEncryption() {
        String msg = "Test decryption";
        String decrptedMsg;

        decrptedMsg = StringHelper.decrypt(StringHelper.encrypt(msg));
        assertEquals(msg, decrptedMsg);
    }

    @Test
    public void testSplitName() {

        String fullName = "singleWord";
        String[] splitName = StringHelper.splitName(fullName);

        assertEquals(splitName[0], "");
        assertEquals(splitName[1], "singleWord");

        fullName = "";
        splitName = StringHelper.splitName(fullName);

        assertEquals(splitName[0], "");
        assertEquals(splitName[1], "");

        splitName = StringHelper.splitName(null);
        assertEquals(0, splitName.length);

        fullName = "two words";
        splitName = StringHelper.splitName(fullName);

        assertEquals(splitName[0], "two");
        assertEquals(splitName[1], "words");

        fullName = "now three words";
        splitName = StringHelper.splitName(fullName);

        assertEquals(splitName[0], "now three");
        assertEquals(splitName[1], "words");

        fullName = "what if four words";
        splitName = StringHelper.splitName(fullName);

        assertEquals(splitName[0], "what if four");
        assertEquals(splitName[1], "words");

        fullName = "first name firstName {last Name}";
        splitName = StringHelper.splitName(fullName);

        assertEquals(splitName[0], "first name firstName");
        assertEquals(splitName[1], "last Name");

    }

    @Test
    public void testRemoveExtraSpace() {

        assertEquals(null, StringHelper.removeExtraSpace((String) null));

        String str = "";
        assertEquals("", StringHelper.removeExtraSpace(str));

        str = "a    a";
        assertEquals("a a", StringHelper.removeExtraSpace(str));

        str = "  a    a   ";
        assertEquals("a a", StringHelper.removeExtraSpace(str));

        str = "    ";
        assertEquals("", StringHelper.removeExtraSpace(str));

        str = " a      b       c       d      ";
        assertEquals("a b c d", StringHelper.removeExtraSpace(str));
    }

    @Test
    public void testReplaceIllegalChars() {
        String regex = "[a-zA-Z0-9_.$-]+";

        assertEquals(null, StringHelper.replaceIllegalChars(null, regex, '_'));

        String str = "";
        assertEquals("", StringHelper.replaceIllegalChars(str, regex, '_'));

        str = "abc";
        assertEquals("abc", StringHelper.replaceIllegalChars(str, regex, '_'));

        str = "illegal!?Chars+1";
        assertEquals("illegal__Chars_1", StringHelper.replaceIllegalChars(str, regex, '_'));
        assertEquals("illegal..Chars.1", StringHelper.replaceIllegalChars(str, regex, '.'));
    }

    @Test
    public void testCountEmptyStrings() {
        String empty = "";
        String whitespace = " ";
        String nul = null;
        String nonEmpty = "non-empty";
        assertEquals(1, StringHelper.countEmptyStrings(empty));
        assertEquals(0, StringHelper.countEmptyStrings(whitespace));
        assertEquals(1, StringHelper.countEmptyStrings(nul));
        assertEquals(0, StringHelper.countEmptyStrings(nonEmpty));
        assertEquals(2, StringHelper.countEmptyStrings(empty, whitespace, nul, nonEmpty));
    }

    @Test
    public void testConvertToEmptyStringIfNull() {
        String empty = "";
        String whitespace = " ";
        String nonEmpty = "non-empty";
        assertEquals("", StringHelper.convertToEmptyStringIfNull(null));
        assertEquals("non-empty", StringHelper.convertToEmptyStringIfNull(nonEmpty));
        assertEquals("", StringHelper.convertToEmptyStringIfNull(empty));
        assertEquals(" ", StringHelper.convertToEmptyStringIfNull(whitespace));
    }

    @Test
    public void testTruncate() {
        assertEquals("1234567...", StringHelper.truncate("1234567890xxxx", 10));
        assertEquals("1234567890", StringHelper.truncate("1234567890", 10));
        assertEquals("123456789", StringHelper.truncate("123456789", 10));
    }

    @Test
    public void testTruncateHead() {
        assertEquals("1234567890", StringHelper.truncateHead("xxxx1234567890", 10));
        assertEquals("1234567890", StringHelper.truncateHead("1234567890", 10));
        assertEquals("123456789", StringHelper.truncateHead("123456789", 10));
        assertEquals("567890", StringHelper.truncateHead("1234567890", 6));
    }

    @Test
    public void testRemoveEnclosingSquareBrackets() {
        // typical case
        assertEquals("test1, test2", StringHelper.removeEnclosingSquareBrackets("[test1, test2]"));

        // input multiple square brackets, expected outermost brackets removed
        assertEquals("[ \"test\" ]", StringHelper.removeEnclosingSquareBrackets("[[ \"test\" ]]"));

        // input nested square brackets, expected outermost brackets removed
        assertEquals("test1, [], ] test2",
                     StringHelper.removeEnclosingSquareBrackets("[test1, [], ] test2]"));

        // input no square brackets, expected same input string
        assertEquals("test", StringHelper.removeEnclosingSquareBrackets("test"));
        assertEquals("  test  ", StringHelper.removeEnclosingSquareBrackets("  test  "));

        // input unmatched brackets, expected same input string
        assertEquals("[test", StringHelper.removeEnclosingSquareBrackets("[test"));
        assertEquals("(test]", StringHelper.removeEnclosingSquareBrackets("(test]"));

        // input empty string, expected empty string
        assertEquals("", StringHelper.removeEnclosingSquareBrackets(""));

        // input null, expected null
        assertEquals(null, StringHelper.removeEnclosingSquareBrackets(null));
    }

    private void verifyRegexMatch(String[] stringsToMatch, List<String> regexList, boolean expectedResult) {
        for (String str : stringsToMatch) {
            assertEquals(expectedResult, StringHelper.isAnyMatching(str, regexList));
        }
    }

    @Test
    public void testCsvToHtmlTable() {
        String csvText = "ColHeader1, ColHeader2, ColHeader3, ColHeader4" + Const.EOL
                         + "\"Data 1-1\", \"Data 1\"\"2\", \"Data 1,3\", \"Data 1\"\"\"\"4\"" + Const.EOL
                         + "Data 2-1, Data 2-2, Data 2-3, \"Data 2-4\"\"\"" + Const.EOL
                         + "Data 3-1, Data 3-2, Data 3-3, Data 3-4" + Const.EOL;
        String htmlText = StringHelper.csvToHtmlTable(csvText);
        String expectedHtmlText = "<table class=\"table table-bordered table-striped table-condensed\">"
                                      + "<tr>"
                                          + "<td>ColHeader1</td>"
                                          + "<td> ColHeader2</td>"
                                          + "<td> ColHeader3</td>"
                                          + "<td>ColHeader4</td>"
                                      + "</tr>"
                                      + "<tr>"
                                          + "<td>Data 1-1</td>"
                                          + "<td> Data 1&quot;2</td>"
                                          + "<td> Data 1,3</td>"
                                          + "<td>Data 1&quot;&quot;4</td>"
                                      + "</tr>"
                                      + "<tr>"
                                          + "<td>Data 2-1</td>"
                                          + "<td> Data 2-2</td>"
                                          + "<td> Data 2-3</td>"
                                          + "<td>Data 2-4&quot;</td>"
                                      + "</tr>"
                                      + "<tr>"
                                          + "<td>Data 3-1</td>"
                                          + "<td> Data 3-2</td>"
                                          + "<td> Data 3-3</td>"
                                          + "<td>Data 3-4</td>"
                                      + "</tr>"
                                  + "</table>";
        assertEquals(expectedHtmlText, htmlText);
    }

    @Test
    public void testRemoveNonAscii() {
        assertEquals("Hello world!", StringHelper.removeNonAscii("Hello world!"));

        assertEquals("", StringHelper.removeNonAscii("©¡¢â"));

        assertEquals("Coevaluacin Prctica (Part 1)",
                     StringHelper.removeNonAscii("Coevaluación Práctica (Part 1)"));
    }

    @Test
    public void testJoin() {
        assertEquals("", StringHelper.join("", new String[] {}));
        assertEquals("", StringHelper.join(",", new String[] {}));
        assertEquals("", StringHelper.join("||", new String[] {}));

        assertEquals("test", StringHelper.join("", new String[] {"test"}));
        assertEquals("test", StringHelper.join(",", new String[] {"test"}));
        assertEquals("test", StringHelper.join("||", new String[] {"test"}));
        assertEquals("testdata", StringHelper.join("", new String[] {"test", "data"}));

        assertEquals("test,data", StringHelper.join(",", new String[] {"test", "data"}));
        assertEquals("test||data", StringHelper.join("||", new String[] {"test", "data"}));
        assertEquals("test|||data|||testdata",
                StringHelper.join("|||", new String[] {"test", "data", "testdata"}));
    }
}
