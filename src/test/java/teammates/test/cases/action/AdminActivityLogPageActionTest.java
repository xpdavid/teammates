package teammates.test.cases.action;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.testng.annotations.Test;

import com.google.appengine.api.log.dev.LocalLogService;
import com.google.appengine.tools.development.testing.LocalLogServiceTestConfig;
import com.google.gson.reflect.TypeToken;

import teammates.common.util.ActivityLogEntry;
import teammates.common.util.Const;
import teammates.common.util.JsonUtils;
import teammates.common.util.TimeHelper;
import teammates.test.driver.FileHelper;
import teammates.test.driver.TestProperties;
import teammates.ui.controller.AdminActivityLogPageAction;
import teammates.ui.controller.ShowPageResult;
import teammates.ui.pagedata.AdminActivityLogPageData;

public class AdminActivityLogPageActionTest extends BaseActionTest {
    
    private static final String TYPICAL_LOG_MESSAGE = "/typicalLogMessage.json";
    
    private static final int LOG_MESSAGE_TODAY_INDEX = 0;
    private static final int LOG_MESSAGE_YESTERDAY_INDEX = 1;
    private static final int LOG_MESSAGE_TWO_DAYS_AGO_INDEX = 2;
    
    private LocalLogService localLogService;
    private List<List<String>> logMessages;

    @Override
    protected String getActionUri() {
        return Const.ActionURIs.ADMIN_ACTIVITY_LOG_PAGE;
    }
    
    @Override
    protected void prepareTestData() {
        localLogService = LocalLogServiceTestConfig.getLocalLogService();
        logMessages = loadLogMessages();
        removeAndRestoreLogMessage();
    }
    
    @Override
    @Test
    public void testExecuteAndPostProcess() {
        gaeSimulation.loginAsAdmin("admin");
        
        testInvalidQuery();
        testShowTestingDataAndExcludedUri();
        testFilters();
        testFiltersCombination();
        testLogMessageInDifferentVersions();
    }

    private void testInvalidQuery() {
        int[][] expected = new int[][]{{0, 1, 3, 4, 5}};
        String query = "unknown";
        verifyActionResult(expected, "filterQuery", query);
        query = "";
        verifyActionResult(expected, "filterQuery", query);
        query = "info";
        verifyActionResult(expected, "filterQuery", query);
        query = "info:";
        verifyActionResult(expected, "filterQuery", query);
        query = "request:servlet3 unknown_connector role:Student";
        verifyActionResult(expected, "filterQuery", query);
        query = "unknown:servlet3 | role:Student";
        verifyActionResult(expected, "filterQuery", query);
               
        expected = new int[][]{{0, 1, 2, 3, 4, 5, 6}};
        query = "information:unkown";
        verifyActionResult(expected, "filterQuery", query, "testdata", "true");
    }

    private void testShowTestingDataAndExcludedUri() {
        ______TS("Test default configuration: no test data, no excluded uri");
        int[][] expected = new int[][]{{0, 1, 3, 4, 5}};
        verifyActionResult(expected);
        
        ______TS("Test configuration: show test data, no excluded uri");
        expected = new int[][]{{0, 1, 2, 3, 4, 5, 6}};
        verifyActionResult(expected, "testdata", "true");
        
        ______TS("Test configuration: no test data, show excluded uri");
        expected = new int[][]{{0, 1, 3, 4, 5, 7, 8, 9}};
        verifyActionResult(expected, "all", "true");
        
        ______TS("Test configuration: show test data, show excluded uri");
        expected = new int[][]{{0, 1, 2, 3, 4, 5, 6, 7, 8, 9}};
        verifyActionResult(expected, "testdata", "true", "all", "true");
    }

    private void testFilters() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy");
        sdf.setTimeZone(TimeZone.getTimeZone(Const.SystemParams.ADMIN_TIME_ZONE));
        
        // from
        int[][] expected = new int[][]{{0, 1, 3, 4, 5}, {0, 1, 2}};
        Date yesterday = TimeHelper.getDateOffsetToCurrentTime(-1);
        String query = String.format("from:%s", sdf.format(yesterday));
        verifyActionResult(expected, "filterQuery", query);
        
        // to
        expected = new int[][]{{}, {}, {0, 1}};
        Date twoDaysAgo = TimeHelper.getDateOffsetToCurrentTime(-2);
        query = String.format("to:%s", sdf.format(twoDaysAgo));
        verifyActionResult(expected, "filterQuery", query);
        
        // from-to
        expected = new int[][]{{}, {0, 1, 2}, {0, 1}};
        query = String.format("from:%s and to:%s", sdf.format(twoDaysAgo), sdf.format(yesterday));
        verifyActionResult(expected, "filterQuery", query);
        
        Date today = TimeHelper.getDateOffsetToCurrentTime(0);
        expected = new int[][]{{0, 1, 3, 4, 5}, {0, 1, 2}, {0, 1}};
        query = String.format("from:%s | to:%s", sdf.format(twoDaysAgo), sdf.format(today));
        verifyActionResult(expected, "filterQuery", query);
        
        // person: name
        query = "person:Name1";
        expected = new int[][]{{0, 1, 3}};
        verifyActionResult(expected, "filterQuery", query);
        
        // person: googleId
        query = String.format("person:id1@google.com | to:%s", sdf.format(yesterday));
        expected = new int[][]{{}, {0, 1}};
        verifyActionResult(expected, "filterQuery", query);
        
        // person: email
        query = String.format("person:email2@email.com | from:%s | to:%s",
                              sdf.format(twoDaysAgo), sdf.format(yesterday));
        expected = new int[][]{{}, {2}, {0, 1}};
        verifyActionResult(expected, "filterQuery", query);
        
        // role
        query = "role:Admin | person:id1@google.com.sg";
        expected = new int[][]{{1}};
        verifyActionResult(expected, "filterQuery", query);
        
        // request
        query = "request:servlet3 | role:Student";
        expected = new int[][]{{4}};
        verifyActionResult(expected, "filterQuery", query);
        
        // response
        query = "response:action1 | request:servlet1";
        expected = new int[][]{{0}};
        verifyActionResult(expected, "filterQuery", query);
        
        // time
        query = "time:50";
        expected = new int[][]{{4, 5}};
        verifyActionResult(expected, "filterQuery", query);
        
        // info
        query = "info:keyword1";
        expected = new int[][]{{0, 3, 5}};
        verifyActionResult(expected, "filterQuery", query);
        
        query = String.format("info:keyword2 | from:%s", sdf.format(yesterday));
        expected = new int[][]{{0, 1, 4}, {0, 1, 2}};
        verifyActionResult(expected, "filterQuery", query);
        
        // id
        expected = new int[][]{{2}};
        query = "id:id02";
        verifyActionResult(expected, "testdata", "true", "filterQuery", query);
    }

    private void testFiltersCombination() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy");
        sdf.setTimeZone(TimeZone.getTimeZone(Const.SystemParams.ADMIN_TIME_ZONE));
        Date today = TimeHelper.getDateOffsetToCurrentTime(0);
        Date twoDaysAgo = TimeHelper.getDateOffsetToCurrentTime(-2);
        
        int[][] expected = new int[][]{{0, 3, 5, 7, 8, 9}};
        String query = "info:keyword1";
        verifyActionResult(expected, "filterQuery", query, "all", "true");
        
        expected = new int[][]{{0, 1, 3, 7, 8, 9}, {0, 1, 4}, {3}};
        query = String.format("person:Name1 | from:%s and to:%s", sdf.format(twoDaysAgo), sdf.format(today));
        verifyActionResult(expected, "filterQuery", query, "all", "true");
        
        expected = new int[][]{{0, 1, 2}};
        query = "role:Admin";
        verifyActionResult(expected, "filterQuery", query, "testdata", "true");
        
        expected = new int[][]{{2, 4, 5, 6, 7}, {1, 3, 4}, {0, 1, 2}};
        query = String.format("time:50 | from:%s and to:%s", sdf.format(twoDaysAgo), sdf.format(today));
        verifyActionResult(expected, "filterQuery", query, "testdata", "true", "all", "true");
    }

    private void testLogMessageInDifferentVersions() {
        // version query is controlled by GAE itself
        // so there is no need to write comprehensive test case for it
        
        int[][] expected = new int[][]{{}};
        String query = "version:2";
        verifyActionResult(expected, "filterQuery", query);
        
        expected = new int[][]{{0, 1, 3, 4, 5}};
        query = "version:2, 1";
        verifyActionResult(expected, "filterQuery", query);
    }

    private void verifyActionResult(int[][] expectedLogs, String... params) {
        AdminActivityLogPageAction action = getAction(params);
        ShowPageResult result = getShowPageResult(action);
        AdminActivityLogPageData page = (AdminActivityLogPageData) result.data;
        List<ActivityLogEntry> actualLogs = page.getLogs();
        List<String> expectedMsgs = generateExpectedMsgFrom(expectedLogs);
        
        assertEquals(expectedMsgs.size(), actualLogs.size());
        for (int i = 0; i < expectedMsgs.size(); i++) {
            String actualMsg = actualLogs.get(i).generateLogMessage();
            actualMsg = actualMsg.replace("<mark>", "").replace("</mark>", "");
            assertTrue("expected: " + expectedMsgs.get(i) + "to contain:" + actualMsg,
                       expectedMsgs.get(i).contains(actualMsg));
        }
    }
    
    private List<String> generateExpectedMsgFrom(int[][] expectedLogs) {
        List<String> result = new ArrayList<String>();
        for (int i = 0; i < expectedLogs.length; i++) {
            for (int j = 0; j < expectedLogs[i].length; j++) {
                result.add(logMessages.get(i).get(expectedLogs[i][j]));
            }
        }
        return result;
    }
    
    private void removeAndRestoreLogMessage() {
        localLogService.clear();
        
        Date twoDaysAgo = TimeHelper.getDateOffsetToCurrentTime(-2);
        insertLogMessagesAtTime(logMessages.get(LOG_MESSAGE_TWO_DAYS_AGO_INDEX), twoDaysAgo.getTime());
        Date yesterday = TimeHelper.getDateOffsetToCurrentTime(-1);
        insertLogMessagesAtTime(logMessages.get(LOG_MESSAGE_YESTERDAY_INDEX), yesterday.getTime());
        Date today = TimeHelper.getDateOffsetToCurrentTime(0);
        insertLogMessagesAtTime(logMessages.get(LOG_MESSAGE_TODAY_INDEX), today.getTime());
        
    
    }
    
    private void insertLogMessagesAtTime(List<String> msgList, long timeMillis) {
        createDummyRequestInfoAtTime(timeMillis);
        
        int levelInfo = 1;
        long offset = 0;
        // bug might be introduced when the time is 00:00 AM.
        // but this situation is really rare and can be solved by re-running the test case
        for (String msg : msgList) {
            localLogService.addAppLogLine(String.valueOf(timeMillis),
                                          timeMillis * 1000 - offset, levelInfo, msg);
            offset--;
        }
    }
    
    private void createDummyRequestInfoAtTime(long timeMillis) {
        String dummyStr = "TEST";
        String defaultVersion = "1";
        localLogService.addRequestInfo(dummyStr, defaultVersion, String.valueOf(timeMillis), dummyStr,
                                       dummyStr, timeMillis * 1000, timeMillis * 1000, dummyStr,
                                       dummyStr, dummyStr, dummyStr, true, 200, dummyStr);
    }
    
    private List<List<String>> loadLogMessages() {
        try {
            String pathToJsonFile = TestProperties.TEST_DATA_FOLDER + TYPICAL_LOG_MESSAGE;
            String jsonString = FileHelper.readFile(pathToJsonFile);
            Type listType = new TypeToken<List<List<String>>>(){}.getType();
            
            return JsonUtils.fromJson(jsonString, listType);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected AdminActivityLogPageAction getAction(String... params) {
        return (AdminActivityLogPageAction) gaeSimulation.getActionObject(getActionUri(), params);
    }
    
}
