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

    // Required
    public String googleId;

    // Optional
    public String shortName;
    public String email;
    public String institute;
    public String nationality;
    public String gender; // only accepts "male", "female" or "other"
    public String moreInfo;
    public String pictureKey;
    public Instant modifiedDate;

    StudentProfileAttributes(String googleId) {
        this.googleId = googleId;
        this.shortName = "";
        this.email = "";
        this.institute = "";
        this.nationality = "";
        this.gender = "other";
        this.moreInfo = "";
        this.pictureKey = "";
        this.modifiedDate = Instant.now();
    }

    public static StudentProfileAttributes valueOf(StudentProfile sp) {
        return builder(sp.getGoogleId())
                .withShortName(sp.getShortName())
                .withEmail(sp.getEmail())
                .withInstitute(sp.getInstitute())
                .withGender(sp.getGender())
                .withNationality(sp.getNationality())
                .withMoreInfo(sp.getMoreInfo())
                .withPictureKey(sp.getPictureKey().getKeyString())
                .withModifiedDate(sp.getModifiedDate())
                .build();
    }

    /**
     * Return new builder instance all string fields setted to {@code ""}
     * and with {@code gender = "other"}.
     */
    public static Builder builder(String googleId) {
        return new Builder(googleId);
    }

    public StudentProfileAttributes getCopy() {
        return builder(googleId)
                .withShortName(shortName)
                .withEmail(email)
                .withInstitute(institute)
                .withGender(gender)
                .withNationality(nationality)
                .withMoreInfo(moreInfo)
                .withPictureKey(pictureKey)
                .withModifiedDate(modifiedDate)
                .build();
    }

    // branch is not fully tested here: part of StudentCourseJoinAuthenticatedAction
    public String generateUpdateMessageForStudent() {
        if (isMultipleFieldsEmpty()) {
            return Const.StatusMessages.STUDENT_UPDATE_PROFILE;
        } else if (StringHelper.isEmpty(shortName)) {
            return Const.StatusMessages.STUDENT_UPDATE_PROFILE_SHORTNAME;
        } else if (StringHelper.isEmpty(email)) {
            return Const.StatusMessages.STUDENT_UPDATE_PROFILE_EMAIL;
        } else if (StringHelper.isEmpty(pictureKey)) {
            return Const.StatusMessages.STUDENT_UPDATE_PROFILE_PICTURE;
        } else if (StringHelper.isEmpty(moreInfo)) {
            return Const.StatusMessages.STUDENT_UPDATE_PROFILE_MOREINFO;
        } else if (StringHelper.isEmpty(nationality)) {
            return Const.StatusMessages.STUDENT_UPDATE_PROFILE_NATIONALITY;
        }
        return "";
    }

    private boolean isMultipleFieldsEmpty() {
        int numEmptyFields = StringHelper.countEmptyStrings(shortName, email, nationality, moreInfo, pictureKey);
        return numEmptyFields > 1;
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

        addNonEmptyError(validator.getInvalidityInfoForGender(gender), errors);

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
        return new StudentProfile(googleId, shortName, email, institute, nationality, gender,
                                  moreInfo, new BlobKey(this.pictureKey));
    }

    @Override
    public String getIdentificationString() {
        return this.googleId;
    }

    @Override
    public String getEntityTypeAsString() {
        return "StudentProfile";
    }

    @Override
    public String getBackupIdentifier() {
        return "Student profile modified";
    }

    @Override
    public String getJsonString() {
        return JsonUtils.toJson(this, StudentProfileAttributes.class);
    }

    @Override
    public void sanitizeForSaving() {
        this.googleId = SanitizationHelper.sanitizeGoogleId(this.googleId);
    }

    /**
     * A Builder class for {@link StudentProfileAttributes}.
     */
    public static class Builder {
        private static final String REQUIRED_FIELD_CANNOT_BE_NULL = "Required field cannot be null";

        private final StudentProfileAttributes profileAttributes;

        public Builder(String googleId) {
            Assumption.assertNotNull(REQUIRED_FIELD_CANNOT_BE_NULL, googleId);
            profileAttributes = new StudentProfileAttributes(googleId);
        }

        public Builder withShortName(String shortName) {
            if (shortName != null) {
                profileAttributes.shortName = SanitizationHelper.sanitizeName(shortName);
            }
            return this;
        }

        public Builder withEmail(String email) {
            if (email != null) {
                profileAttributes.email = SanitizationHelper.sanitizeEmail(email);
            }
            return this;
        }

        public Builder withInstitute(String institute) {
            if (institute != null) {
                profileAttributes.institute = SanitizationHelper.sanitizeTitle(institute);
            }
            return this;
        }

        public Builder withNationality(String nationality) {
            if (nationality != null) {
                profileAttributes.nationality = SanitizationHelper.sanitizeName(nationality);
            }
            return this;
        }

        public Builder withGender(String gender) {
            profileAttributes.gender = isGenderValid(gender) ? gender : "other";
            return this;
        }

        public Builder withMoreInfo(String moreInfo) {
            if (moreInfo != null) {
                profileAttributes.moreInfo = moreInfo;
            }
            return this;
        }

        public Builder withPictureKey(String pictureKey) {
            if (pictureKey != null) {
                profileAttributes.pictureKey = pictureKey;
            }
            return this;
        }

        public Builder withModifiedDate(Instant modifiedDate) {
            profileAttributes.modifiedDate = modifiedDate == null ? Instant.now() : modifiedDate;
            return this;
        }

        public StudentProfileAttributes build() {
            return profileAttributes;
        }

        private boolean isGenderValid(String gender) {
            return "male".equals(gender) || "female".equals(gender) || "other".equals(gender);
        }
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
     * Returns a {@link UpdateOptions.Builder} to build {@link UpdateOptions} for profile.
     */
    public static UpdateOptions.Builder updateOptionsBuilder(String googleId) {
        return new UpdateOptions.Builder(googleId);
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
        private UpdateOption<String> genderOption = UpdateOption.empty();
        private UpdateOption<String> moreInfoOption = UpdateOption.empty();
        private UpdateOption<String> pictureKeyOption = UpdateOption.empty();

        private UpdateOptions(String googleId) {
            Assumption.assertNotNull(Const.StatusCodes.UPDATE_OPTIONS_NULL_INPUT, googleId);

            this.googleId = googleId;
        }

        public String getGoogleId() {
            return googleId;
        }

        /**
         * Builder class to build {@link UpdateOptions}.
         */
        public static class Builder {
            private UpdateOptions updateOptions;

            private Builder(String googleId) {
                updateOptions = new UpdateOptions(googleId);
            }

            public Builder withShortName(String shortName) {
                Assumption.assertNotNull(Const.StatusCodes.UPDATE_OPTIONS_NULL_INPUT, shortName);

                updateOptions.shortNameOption = UpdateOption.of(shortName);
                return this;
            }

            public Builder withEmail(String email) {
                Assumption.assertNotNull(Const.StatusCodes.UPDATE_OPTIONS_NULL_INPUT, email);

                updateOptions.emailOption = UpdateOption.of(email);
                return this;
            }

            public Builder withInstitute(String institute) {
                Assumption.assertNotNull(Const.StatusCodes.UPDATE_OPTIONS_NULL_INPUT, institute);

                updateOptions.instituteOption = UpdateOption.of(institute);
                return this;
            }

            public Builder withNationality(String nationality) {
                Assumption.assertNotNull(Const.StatusCodes.UPDATE_OPTIONS_NULL_INPUT, nationality);

                updateOptions.nationalityOption = UpdateOption.of(nationality);
                return this;
            }

            public Builder withGender(String gender) {
                Assumption.assertNotNull(Const.StatusCodes.UPDATE_OPTIONS_NULL_INPUT, gender);

                updateOptions.genderOption = UpdateOption.of(gender);
                return this;
            }

            public Builder withMoreInfo(String moreInfo) {
                Assumption.assertNotNull(Const.StatusCodes.UPDATE_OPTIONS_NULL_INPUT, moreInfo);

                updateOptions.moreInfoOption = UpdateOption.of(moreInfo);
                return this;
            }

            public Builder withPictureKey(String pictureKey) {
                Assumption.assertNotNull(Const.StatusCodes.UPDATE_OPTIONS_NULL_INPUT, pictureKey);

                updateOptions.pictureKeyOption = UpdateOption.of(pictureKey);
                return this;
            }

            public UpdateOptions build() {
                return updateOptions;
            }

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

    }
}
