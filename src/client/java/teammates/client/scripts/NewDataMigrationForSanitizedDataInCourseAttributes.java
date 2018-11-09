package teammates.client.scripts;

import java.io.IOException;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.cmd.Query;

import teammates.common.util.SanitizationHelper;
import teammates.storage.entity.Course;

/**
 * Script to desanitize content of {@link Course} if it is sanitized.
 */
public class NewDataMigrationForSanitizedDataInCourseAttributes
        extends DataMigrationWithCheckpointForEntities<Course> {

    public NewDataMigrationForSanitizedDataInCourseAttributes() {
        numberOfScannedKey.set(0L);
        numberOfAffectedEntities.set(0L);
        numberOfUpdatedEntities.set(0L);
    }

    public static void main(String[] args) throws IOException {
        NewDataMigrationForSanitizedDataInCourseAttributes migrator =
                new NewDataMigrationForSanitizedDataInCourseAttributes();
        migrator.doOperationRemotely();
    }

    @Override
    protected Query<Course> getFilterQuery() {
        return ofy().load().type(Course.class);
    }

    @Override
    protected boolean isPreview() {
        return true;
    }

    @Override
    protected String getLastPositionOfCursor() {
        return "";
    }

    @Override
    protected int getCursorInformationPrintCycle() {
        return 100;
    }

    @Override
    protected boolean isMigrationNeeded(Key<Course> key) throws Exception {
        Course course = ofy().load().key(key).now();

        return SanitizationHelper.isSanitizedHtml(course.getName());
    }

    @Override
    protected void migrateEntity(Key<Course> key) throws Exception {
        Course course = ofy().load().key(key).now();

        course.setName(SanitizationHelper.desanitizeIfHtmlSanitized(course.getName()));

        ofy().save().entity(course).now();
    }
}
