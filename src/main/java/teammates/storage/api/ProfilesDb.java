package teammates.storage.api;

import static com.googlecode.objectify.ObjectifyService.ofy;

import java.time.Instant;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.cmd.LoadType;
import com.googlecode.objectify.cmd.QueryKeys;

import teammates.common.datatransfer.attributes.StudentProfileAttributes;
import teammates.common.exception.InvalidParametersException;
import teammates.common.util.Assumption;
import teammates.common.util.Const;
import teammates.common.util.GoogleCloudStorageHelper;
import teammates.storage.entity.Account;
import teammates.storage.entity.StudentProfile;

/**
 * Handles CRUD operations for student profiles.
 *
 * @see StudentProfile
 * @see StudentProfileAttributes
 */
public class ProfilesDb extends EntitiesDb<StudentProfile, StudentProfileAttributes> {

    /**
     * Gets the student profile associated with {@code accountGoogleId}.
     *
     * @return null if the profile was not found
     */
    public StudentProfileAttributes getStudentProfile(String accountGoogleId) {
        return makeAttributesOrNull(getStudentProfileEntityFromDb(accountGoogleId));
    }

    /**
     * Updates/Creates the profile using {@link StudentProfileAttributes.UpdateOptions}.
     *
     * @return updated student profile
     * @throws InvalidParametersException if attributes to update are not valid
     */
    public StudentProfileAttributes updateOrCreateStudentProfile(StudentProfileAttributes.UpdateOptions updateOptions)
            throws InvalidParametersException {
        Assumption.assertNotNull(Const.StatusCodes.DBLEVEL_NULL_INPUT, updateOptions);

        StudentProfile studentProfile = getStudentProfileEntityFromDb(updateOptions.getGoogleId());
        if (studentProfile == null) {
            studentProfile = new StudentProfile(updateOptions.getGoogleId());
        }

        StudentProfileAttributes newAttributes = makeAttributes(studentProfile);
        newAttributes.update(updateOptions);

        newAttributes.sanitizeForSaving();
        if (!newAttributes.isValid()) {
            throw new InvalidParametersException(newAttributes.getInvalidityInfo());
        }

        if (hasNoNewChangesToProfile(newAttributes, studentProfile)) {
            return newAttributes;
        }

        studentProfile.setShortName(newAttributes.shortName);
        studentProfile.setEmail(newAttributes.email);
        studentProfile.setInstitute(newAttributes.institute);
        studentProfile.setNationality(newAttributes.nationality);
        studentProfile.setGender(newAttributes.gender.name().toLowerCase());
        studentProfile.setMoreInfo(newAttributes.moreInfo);
        studentProfile.setPictureKey(newAttributes.pictureKey);
        studentProfile.setModifiedDate(Instant.now());

        saveEntity(studentProfile);

        return makeAttributes(studentProfile);
    }

    private boolean hasNoNewChangesToProfile(StudentProfileAttributes newSpa, StudentProfile profileToUpdate) {
        StudentProfileAttributes newSpaCopy = newSpa.getCopy();
        StudentProfileAttributes existingProfile = StudentProfileAttributes.valueOf(profileToUpdate);

        newSpaCopy.modifiedDate = existingProfile.modifiedDate;
        return existingProfile.toString().equals(newSpaCopy.toString());
    }

    /**
     * Deletes the student profile associated with the {@code googleId}.
     *
     * <p>Fails silently if the student profile doesn't exist.</p>
     */
    public void deleteStudentProfile(String googleId) {
        StudentProfile sp = getStudentProfileEntityFromDb(googleId);
        if (sp == null) {
            return;
        }
        if (!sp.getPictureKey().equals("")) {
            deletePicture(sp.getPictureKey());
        }
        Key<Account> parentKey = Key.create(Account.class, googleId);
        Key<StudentProfile> profileKey = Key.create(parentKey, StudentProfile.class, googleId);
        deleteEntity(profileKey);
    }

    /**
     * Deletes picture associated with the {@code key}.
     *
     * <p>Fails silently if the {@code key} doesn't exist.</p>
     */
    public void deletePicture(String key) {
        GoogleCloudStorageHelper.deleteFile(key);
    }

    /**
     * Deletes the {@code pictureKey} of the profile with given {@code googleId} by setting it to an empty string.
     *
     * <p>Fails silently if the {@code studentProfile} doesn't exist.</p>
     */
    public void deletePictureKey(String googleId) {
        Assumption.assertNotNull(Const.StatusCodes.DBLEVEL_NULL_INPUT, googleId);
        StudentProfile studentProfile = getStudentProfileEntityFromDb(googleId);

        if (studentProfile != null) {
            studentProfile.setPictureKey("");
            studentProfile.setModifiedDate(Instant.now());
            saveEntity(studentProfile);
        }
    }

    //-------------------------------------------------------------------------------------------------------
    //-------------------------------------- Helper Functions -----------------------------------------------
    //-------------------------------------------------------------------------------------------------------

    /**
     * Gets the profile entity associated with the {@code googleId}.
     *
     * @return null if entity is not found
     */
    private StudentProfile getStudentProfileEntityFromDb(String googleId) {
        Key<Account> parentKey = Key.create(Account.class, googleId);
        Key<StudentProfile> childKey = Key.create(parentKey, StudentProfile.class, googleId);
        return ofy().load().key(childKey).now();
    }

    @Override
    protected LoadType<StudentProfile> load() {
        return ofy().load().type(StudentProfile.class);
    }

    @Override
    protected StudentProfile getEntity(StudentProfileAttributes attributes) {
        // this method is never used and is here only for future expansion and completeness
        return getStudentProfileEntityFromDb(attributes.googleId);
    }

    @Override
    protected QueryKeys<StudentProfile> getEntityQueryKeys(StudentProfileAttributes attributes) {
        Key<Account> parentKey = Key.create(Account.class, attributes.googleId);
        Key<StudentProfile> childKey = Key.create(parentKey, StudentProfile.class, attributes.googleId);
        return load().filterKey(childKey).keys();
    }

    @Override
    protected StudentProfileAttributes makeAttributes(StudentProfile entity) {
        Assumption.assertNotNull(Const.StatusCodes.DBLEVEL_NULL_INPUT, entity);

        return StudentProfileAttributes.valueOf(entity);
    }
}
