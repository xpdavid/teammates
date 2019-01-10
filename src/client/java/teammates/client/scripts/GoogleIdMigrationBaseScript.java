package teammates.client.scripts;

import java.util.List;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.tools.cloudstorage.GcsFilename;
import com.google.appengine.tools.cloudstorage.GcsService;
import com.google.appengine.tools.cloudstorage.GcsServiceFactory;
import com.google.appengine.tools.cloudstorage.RetryParams;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.cmd.Query;

import teammates.common.util.Config;
import teammates.storage.entity.Account;
import teammates.storage.entity.CourseStudent;
import teammates.storage.entity.Instructor;
import teammates.storage.entity.StudentProfile;
import teammates.test.driver.TestProperties;

/**
 * Base script to migrate a googleId to a new googleId.
 *
 * <p>The change of googleId is not atomic due to the constrains imposed by the Datastore.
 *
 * @see GoogleIdMigrationBaseScript#shouldUseTransaction()
 */
public abstract class GoogleIdMigrationBaseScript extends DataMigrationEntitiesBaseScript<Account> {

    @Override
    protected Query<Account> getFilterQuery() {
        return ofy().load().type(Account.class);
    }

    @Override
    protected int getCursorInformationPrintCycle() {
        return 100;
    }

    @Override
    protected boolean shouldUseTransaction() {
        // queries for student & instructor entities are not supported by the Datastore
        return false;
    }

    @Override
    protected boolean isMigrationNeeded(Key<Account> key) throws Exception {
        Account account = ofy().load().key(key).now();

        if (!isMigrationOfGoogleIdNeeded(account)) {
            return false;
        }

        String newGoogleId = generateNewGoogleId(account);
        println(String.format("Going to migrate account with googleId %s to new googleId %s",
                account.getGoogleId(), newGoogleId));

        return true;
    }

    @Override
    protected void migrateEntity(Key<Account> oldAccountKey) throws Exception {
        Account oldAccount = ofy().load().key(oldAccountKey).now();
        String oldGoogleId = oldAccount.getGoogleId();
        String newGoogleId = generateNewGoogleId(oldAccount);

        Key<StudentProfile> oldStudentProfileKey = Key.create(oldAccountKey, StudentProfile.class, oldGoogleId);
        StudentProfile oldStudentProfile = ofy().load().key(oldStudentProfileKey).now();

        List<CourseStudent> oldStudents = ofy().load().type(CourseStudent.class)
                .filter("googleId =", oldGoogleId).list();

        List<Instructor> oldInstructors = ofy().load().type(Instructor.class)
                .filter("googleId =", oldGoogleId).list();

        // update students and instructors

        if (!oldStudents.isEmpty()) {
            oldStudents.forEach(student -> student.setGoogleId(newGoogleId));
            ofy().save().entities(oldStudents);
        }

        if (!oldInstructors.isEmpty()) {
            oldInstructors.forEach(instructor -> instructor.setGoogleId(newGoogleId));
            ofy().save().entities(oldInstructors);
        }

        // recreate account and student profile

        oldAccount.setGoogleId(newGoogleId);
        if (ofy().load().type(Account.class).id(newGoogleId) == null) {
            ofy().save().entity(oldAccount).now();
        } else {
            println(String.format("Skip creation of new account as account (%s) already exists", newGoogleId));
        }
        ofy().delete().type(Account.class).id(oldGoogleId).now();

        if (oldStudentProfile != null) {
            BlobKey oldPictureKey = oldStudentProfile.getPictureKey();

            if (!TestProperties.isDevServer()) {
                try {
                    GcsFilename oldGcsFilename = new GcsFilename(Config.PRODUCTION_GCS_BUCKETNAME, oldGoogleId);
                    GcsFilename newGcsFilename = new GcsFilename(Config.PRODUCTION_GCS_BUCKETNAME, newGoogleId);
                    GcsService gcsService = GcsServiceFactory.createGcsService(RetryParams.getDefaultInstance());
                    gcsService.copy(oldGcsFilename, newGcsFilename);
                    gcsService.delete(oldGcsFilename);
                    BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
                    oldPictureKey = blobstoreService.createGsBlobKey(
                            String.format("/gs/%s/%s", Config.PRODUCTION_GCS_BUCKETNAME, newGoogleId));
                } catch (Exception e) {
                    println("Profile picture not exist or error during copy: " + e.getMessage());
                }
            }

            oldStudentProfile.setGoogleId(newGoogleId);
            oldStudentProfile.setPictureKey(oldPictureKey);
            ofy().save().entity(oldStudentProfile).now();
            ofy().delete().keys(oldStudentProfileKey).now();
        }

        println(String.format("Complete migration for account with googleId %s. The new googleId is %s",
                oldGoogleId, newGoogleId));
    }

    /**
     * Checks whether the googleId of the {@code account} is needed to be migrated or not.
     */
    protected abstract boolean isMigrationOfGoogleIdNeeded(Account account);

    /**
     * Generates a new googleId based on the {@code oldAccount}.
     */
    protected abstract String generateNewGoogleId(Account oldAccount);
}
