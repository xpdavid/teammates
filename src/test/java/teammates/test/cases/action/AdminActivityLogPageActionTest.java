package teammates.test.cases.action;

import org.testng.annotations.Test;

import com.google.appengine.api.log.LogQuery;
import com.google.appengine.api.log.LogService;
import com.google.appengine.api.log.LogServiceFactory;
import com.google.appengine.api.log.RequestLogs;
import com.google.appengine.api.log.dev.LocalLogService;
import com.google.appengine.tools.development.testing.LocalLogServiceTestConfig;

import teammates.common.util.Const;
import teammates.ui.controller.AdminActivityLogPageAction;
import teammates.ui.controller.ShowPageResult;

public class AdminActivityLogPageActionTest extends BaseActionTest {

    @Override
    protected String getActionUri() {
        return Const.ActionURIs.ADMIN_ACTIVITY_LOG_PAGE;
    }
    
    @Override
    protected void prepareTestData() {
        // no test data used in this test
    }
    
    @Override
    @Test
    public void testExecuteAndPostProcess() {
//        gaeSimulation.loginAsAdmin("admin");
//        AdminActivityLogPageAction action = getAction();
//        ShowPageResult result = getShowPageResult(action);
//        System.out.println(result.getStatusMessage());
        
        LocalLogService localLogService = LocalLogServiceTestConfig.getLocalLogService();
        localLogService.addRequestInfo("test", "1", "123", "test", "test",
                                        System.nanoTime() / 1000, System.nanoTime() / 1000, "test", "test", "test",
                                        "test", true, 200, "test");
        localLogService.addAppLogLine("123", 3333L, 1, "test");
        
        LogService logService = LogServiceFactory.getLogService();
        LogQuery query = LogQuery.Builder.withDefaults();
        query.includeAppLogs(true);
        for (RequestLogs requestLog : logService.fetch(query)) {
            System.out.println(requestLog.getCombined());
            System.out.println(requestLog.getAppLogLines());
        }
        //TODO: implement this
    }

    @Override
    protected AdminActivityLogPageAction getAction(String... params) {
        return (AdminActivityLogPageAction) gaeSimulation.getActionObject(getActionUri(), params);
    }
    
}
