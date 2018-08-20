package teammates.logic.core;

import com.google.appengine.api.blobstore.BlobKey;

import teammates.common.datatransfer.attributes.StudentProfileAttributes;
import teammates.common.exception.InvalidParametersException;
import teammates.storage.api.ProfilesDb;

/**
 * Handles the logic related to student profiles.
 */
public final class ProfilesLogic {

    private static ProfilesLogic instance = new ProfilesLogic();

    private static final ProfilesDb profilesDb = new ProfilesDb();

    private ProfilesLogic() {
        // prevent initialization
    }

    public static ProfilesLogic inst() {
        return instance;
    }

    public StudentProfileAttributes getStudentProfile(String googleId) {
        return profilesDb.getStudentProfile(googleId);
    }

    public void updateOrCreateStudentProfile(StudentProfileAttributes newStudentProfileAttributes)
            throws InvalidParametersException {
        profilesDb.updateOrCreateStudentProfile(newStudentProfileAttributes);
    }

    public void deleteStudentProfile(String googleId) {
        profilesDb.deleteStudentProfile(googleId);
    }

    public void deletePicture(BlobKey key) {
        profilesDb.deletePicture(key);
    }

    public void updateStudentProfilePicture(String googleId, String newPictureKey) {
        profilesDb.updateStudentProfilePicture(googleId, newPictureKey);
    }

}
