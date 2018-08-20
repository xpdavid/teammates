package teammates.test.cases.logic;

import org.testng.annotations.Test;

import com.google.appengine.api.blobstore.BlobKey;

import teammates.common.datatransfer.attributes.StudentProfileAttributes;
import teammates.logic.core.ProfilesLogic;

/**
 * SUT: {@link ProfilesLogic}.
 */
public class ProfilesLogicTest extends BaseLogicTest {

    private static final ProfilesLogic profilesLogic = ProfilesLogic.inst();

    @Override
    protected void prepareTestData() {
        // no test data used for this test
    }

    @Test
    public void testStudentProfileFunctions() throws Exception {

        // 4 functions are tested together as:
        //      => The functions are very simple (one-liners)
        //      => They are fundamentally related and easily tested together
        //      => It saves time during tests

        ______TS("get SP");
        StudentProfileAttributes expectedSpa = StudentProfileAttributes.builder("id")
                .withShortName("shortName")
                .withEmail("personal@email.com")
                .withInstitute("institute")
                .withNationality("American")
                .withGender("female")
                .withMoreInfo("moreInfo")
                .build();

        profilesLogic.updateOrCreateStudentProfile(expectedSpa);

        StudentProfileAttributes actualSpa = profilesLogic.getStudentProfile(expectedSpa.googleId);
        expectedSpa.modifiedDate = actualSpa.modifiedDate;
        assertEquals(expectedSpa.toString(), actualSpa.toString());

        ______TS("update SP");

        expectedSpa.pictureKey = "non-empty";
        profilesLogic.updateOrCreateStudentProfile(expectedSpa);

        actualSpa = profilesLogic.getStudentProfile(expectedSpa.googleId);
        expectedSpa.modifiedDate = actualSpa.modifiedDate;
        assertEquals(expectedSpa.toString(), actualSpa.toString());

        ______TS("update picture");

        expectedSpa.pictureKey = writeFileToGcs(expectedSpa.googleId, "src/test/resources/images/profile_pic.png");
        profilesLogic.updateStudentProfilePicture(expectedSpa.googleId, expectedSpa.pictureKey);
        actualSpa = profilesLogic.getStudentProfile(expectedSpa.googleId);
        expectedSpa.modifiedDate = actualSpa.modifiedDate;
        assertEquals(expectedSpa.toString(), actualSpa.toString());
    }

    @Test
    public void testDeleteStudentProfile() throws Exception {
        // more tests in ProfilesDbTest

        profilesLogic.updateOrCreateStudentProfile(
                StudentProfileAttributes.builder("sp.logic.test")
                        .withShortName("Test Name")
                        .withPictureKey(writeFileToGcs("sp.logic.test", "src/test/resources/images/profile_pic_default.png"))
                        .build()
        );
        // make sure we create an profile with picture key
        StudentProfileAttributes savedProfile = profilesLogic.getStudentProfile("sp.logic.test");
        assertNotNull(savedProfile);
        assertFalse(savedProfile.pictureKey.isEmpty());

        profilesLogic.deleteStudentProfile("sp.logic.test");
        // check that profile get deleted and picture get deleted
        verifyAbsentInDatastore(savedProfile);
        assertFalse(doesFileExistInGcs(new BlobKey(savedProfile.pictureKey)));
    }

    @Test
    public void testDeletePicture() throws Exception {
        String keyString = writeFileToGcs("accountsLogicTestid", "src/test/resources/images/profile_pic.png");
        BlobKey key = new BlobKey(keyString);
        profilesLogic.deletePicture(key);
        assertFalse(doesFileExistInGcs(key));
    }

}
