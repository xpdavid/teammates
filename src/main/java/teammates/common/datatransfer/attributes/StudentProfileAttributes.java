package teammates.common.datatransfer.attributes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.google.appengine.api.blobstore.BlobKey;

import teammates.common.util.Assumption;
import teammates.common.util.Const;
import teammates.common.util.FieldValidator;
import teammates.common.util.JsonUtils;
import teammates.common.util.SanitizationHelper;
import teammates.common.util.StringHelper;
import teammates.storage.entity.StudentProfile;

/**
 * The data transfer object for StudentProfile entities.
 */
public class StudentProfileAttributes extends EntityAttributes<StudentProfile> {

    private static final String STUDENT_PROFILE_BACKUP_LOG_MSG = "Recently modified student profile::";

    // Required
    public String googleId;

    // Optional
    public String shortName;
    public String email;
    public String institute;
    public String nationality;
    public Gender gender;
    public String moreInfo;
    public String pictureKey;
    public Instant modifiedDate;

    StudentProfileAttributes(String googleId) {
        this.googleId = googleId;
        this.shortName = "";
        this.email = "";
        this.institute = "";
        this.nationality = "";
        this.gender = Gender.OTHER;
        this.moreInfo = "";
        this.pictureKey = "";
        this.modifiedDate = Instant.now();
    }

    public static StudentProfileAttributes valueOf(StudentProfile sp) {
        StudentProfileAttributes studentProfileAttributes = new StudentProfileAttributes(sp.getGoogleId());

        if (sp.getShortName() != null) {
            studentProfileAttributes.shortName = sp.getShortName();
        }
        if (sp.getEmail() != null) {
            studentProfileAttributes.email = sp.getEmail();
        }
        if (sp.getInstitute() != null) {
            studentProfileAttributes.institute = sp.getInstitute();
        }
        studentProfileAttributes.gender = Gender.getGenderEnumValue(sp.getGender());
        if (sp.getNationality() != null) {
            studentProfileAttributes.nationality = sp.getNationality();
        }
        if (sp.getMoreInfo() != null) {
            studentProfileAttributes.moreInfo = sp.getMoreInfo();
        }
        if (sp.getPictureKey() != null) {
            studentProfileAttributes.pictureKey = sp.getPictureKey().getKeyString();
        }
        if (sp.getModifiedDate() != null) {
            studentProfileAttributes.modifiedDate = sp.getModifiedDate();
        }

        return studentProfileAttributes;
    }

    /**
     * Return a builder for {@link StudentProfileAttributes}.
     */
    public static Builder builder(String googleId) {
        return new Builder(googleId);
    }

    public StudentProfileAttributes getCopy() {
        StudentProfileAttributes studentProfileAttributes = new StudentProfileAttributes(googleId);

        studentProfileAttributes.shortName = shortName;
        studentProfileAttributes.email = email;
        studentProfileAttributes.institute = institute;
        studentProfileAttributes.gender = gender;
        studentProfileAttributes.nationality = nationality;
        studentProfileAttributes.moreInfo = moreInfo;
        studentProfileAttributes.pictureKey = pictureKey;
        studentProfileAttributes.modifiedDate = modifiedDate;

        return studentProfileAttributes;
    }

    public String getGoogleId() {
        return googleId;
    }

    public String getShortName() {
        return shortName;
    }

    public String getEmail() {
        return email;
    }

    public String getInstitute() {
        return institute;
    }

    public String getNationality() {
        return nationality;
    }

    public Gender getGender() {
        return gender;
    }

    public String getMoreInfo() {
        return moreInfo;
    }

    public String getPictureKey() {
        return pictureKey;
    }

    public Instant getModifiedDate() {
        return modifiedDate;
    }

    @Override
    public List<String> getInvalidityInfo() {
        FieldValidator validator = new FieldValidator();
        List<String> errors = new ArrayList<>();

        addNonEmptyError(validator.getInvalidityInfoForGoogleId(googleId), errors);

        // accept empty string values as it means the user has not specified anything yet.

        if (!StringHelper.isEmpty(shortName)) {
            addNonEmptyError(validator.getInvalidityInfoForPersonName(shortName), errors);
        }

        if (!StringHelper.isEmpty(email)) {
            addNonEmptyError(validator.getInvalidityInfoForEmail(email), errors);
        }

        if (!StringHelper.isEmpty(institute)) {
            addNonEmptyError(validator.getInvalidityInfoForInstituteName(institute), errors);
        }

        if (!StringHelper.isEmpty(nationality)) {
            addNonEmptyError(validator.getInvalidityInfoForNationality(nationality), errors);
        }

        Assumption.assertNotNull(gender);

        Assumption.assertNotNull(this.pictureKey);

        // No validation for modified date as it is determined by the system.
        // No validation for More Info. It will properly sanitized.

        return errors;
    }

    @Override
    public String toString() {
        return JsonUtils.toJson(this, StudentProfileAttributes.class);
    }

    @Override
    public StudentProfile toEntity() {
        return new StudentProfile(googleId, shortName, email, institute, nationality, gender.name().toLowerCase(),
                                  moreInfo, new BlobKey(this.pictureKey));
    }

    @Override
    public String getBackupIdentifier() {
        return STUDENT_PROFILE_BACKUP_LOG_MSG + googleId;
    }

    @Override
    public void sanitizeForSaving() {
        this.googleId = SanitizationHelper.sanitizeGoogleId(this.googleId);
    }

    /**
     * Updates with {@link UpdateOptions}.
     */
    public void update(UpdateOptions updateOptions) {
        updateOptions.shortNameOption.ifPresent(s -> shortName = s);
        updateOptions.emailOption.ifPresent(s -> email = s);
        updateOptions.instituteOption.ifPresent(s -> institute = s);
        updateOptions.nationalityOption.ifPresent(s -> nationality = s);
        updateOptions.genderOption.ifPresent(s -> gender = s);
        updateOptions.moreInfoOption.ifPresent(s -> moreInfo = s);
        updateOptions.pictureKeyOption.ifPresent(s -> pictureKey = s);
    }

    /**
     * Returns a {@link UpdateOptions.Builder} to build {@link UpdateOptions} for a profile.
     */
    public static UpdateOptions.Builder updateOptionsBuilder(String googleId) {
        return new UpdateOptions.Builder(googleId);
    }

    /**
     * A builder class for {@link StudentProfileAttributes}.
     */
    public static class Builder extends BasicBuilder<StudentProfileAttributes, Builder> {
        private final StudentProfileAttributes profileAttributes;

        private Builder(String googleId) {
            super(new UpdateOptions(googleId));
            thisBuilder = this;

            profileAttributes = new StudentProfileAttributes(googleId);
        }

        @Override
        public StudentProfileAttributes build() {
            profileAttributes.update(updateOptions);

            return profileAttributes;
        }
    }

    /**
     * Represents the gender of a student.
     */
    public enum Gender {
        MALE,
        FEMALE,
        OTHER;

        /**
         * Returns the Gender enum value corresponding to {@code gender}, or OTHER by default.
         */
        public static Gender getGenderEnumValue(String gender) {
            if (gender == null) {
                return Gender.OTHER;
            }
            try {
                return Gender.valueOf(gender.toUpperCase());
            } catch (IllegalArgumentException e) {
                return Gender.OTHER;
            }
        }
    }

    /**
     * Helper class to specific the fields to update in {@link StudentProfileAttributes}.
     */
    public static class UpdateOptions {
        private String googleId;

        private UpdateOption<String> shortNameOption = UpdateOption.empty();
        private UpdateOption<String> emailOption = UpdateOption.empty();
        private UpdateOption<String> instituteOption = UpdateOption.empty();
        private UpdateOption<String> nationalityOption = UpdateOption.empty();
        private UpdateOption<Gender> genderOption = UpdateOption.empty();
        private UpdateOption<String> moreInfoOption = UpdateOption.empty();
        private UpdateOption<String> pictureKeyOption = UpdateOption.empty();

        private UpdateOptions(String googleId) {
            Assumption.assertNotNull(Const.StatusCodes.NULL_PARAMETER, googleId);

            this.googleId = googleId;
        }

        public String getGoogleId() {
            return googleId;
        }

        @Override
        public String toString() {
            return "StudentAttributes.UpdateOptions ["
                    + "googleId = " + googleId
                    + ", shortName = " + shortNameOption
                    + ", email = " + emailOption
                    + ", institute = " + instituteOption
                    + ", nationality = " + nationalityOption
                    + ", gender = " + genderOption
                    + ", moreInfo = " + moreInfoOption
                    + "]";
        }

        /**
         * Builder class to build {@link UpdateOptions}.
         */
        public static class Builder extends BasicBuilder<UpdateOptions, Builder> {

            private Builder(String googleId) {
                super(new UpdateOptions(googleId));
                thisBuilder = this;
            }

            @Override
            public UpdateOptions build() {
                return updateOptions;
            }
        }

    }

    /**
     * Basic builder to build {@link StudentProfileAttributes} related classes.
     *
     * @param <T> type to be built
     * @param <B> type of the builder
     */
    private abstract static class BasicBuilder<T, B extends BasicBuilder<T, B>> {

        protected UpdateOptions updateOptions;
        protected B thisBuilder;

        protected BasicBuilder(UpdateOptions updateOptions) {
            this.updateOptions = updateOptions;
        }

        public B withShortName(String shortName) {
            Assumption.assertNotNull(Const.StatusCodes.NULL_PARAMETER, shortName);

            updateOptions.shortNameOption = UpdateOption.of(shortName);
            return thisBuilder;
        }

        public B withEmail(String email) {
            Assumption.assertNotNull(Const.StatusCodes.NULL_PARAMETER, email);

            updateOptions.emailOption = UpdateOption.of(email);
            return thisBuilder;
        }

        public B withInstitute(String institute) {
            Assumption.assertNotNull(Const.StatusCodes.NULL_PARAMETER, institute);

            updateOptions.instituteOption = UpdateOption.of(institute);
            return thisBuilder;
        }

        public B withNationality(String nationality) {
            Assumption.assertNotNull(Const.StatusCodes.NULL_PARAMETER, nationality);

            updateOptions.nationalityOption = UpdateOption.of(nationality);
            return thisBuilder;
        }

        public B withGender(Gender gender) {
            Assumption.assertNotNull(Const.StatusCodes.NULL_PARAMETER, gender);

            updateOptions.genderOption = UpdateOption.of(gender);
            return thisBuilder;
        }

        public B withMoreInfo(String moreInfo) {
            Assumption.assertNotNull(Const.StatusCodes.NULL_PARAMETER, moreInfo);

            updateOptions.moreInfoOption = UpdateOption.of(moreInfo);
            return thisBuilder;
        }

        public B withPictureKey(String pictureKey) {
            Assumption.assertNotNull(Const.StatusCodes.NULL_PARAMETER, pictureKey);

            updateOptions.pictureKeyOption = UpdateOption.of(pictureKey);
            return thisBuilder;
        }

        public abstract T build();

    }
}
