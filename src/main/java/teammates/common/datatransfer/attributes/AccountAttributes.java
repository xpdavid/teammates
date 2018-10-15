package teammates.common.datatransfer.attributes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import teammates.common.util.Assumption;
import teammates.common.util.Const;
import teammates.common.util.FieldValidator;
import teammates.common.util.JsonUtils;
import teammates.common.util.SanitizationHelper;
import teammates.common.util.StringHelper;
import teammates.storage.entity.Account;

/**
 * A data transfer object for Account entities.
 */
public class AccountAttributes extends EntityAttributes<Account> {

    // Note: be careful when changing these variables as their names are used in *.json files.

    public String googleId;
    public String name;
    public boolean isInstructor;
    public String email;
    public String institute;
    public Instant createdAt;

    AccountAttributes() {
        // Empty constructor for builder to construct object
    }

    public static AccountAttributes valueOf(Account a) {
        return builder()
                .withGoogleId(a.getGoogleId())
                .withName(a.getName())
                .withIsInstructor(a.isInstructor())
                .withInstitute(a.getInstitute())
                .withEmail(a.getEmail())
                .withCreatedAt(a.getCreatedAt())
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * A Builder class for {@link AccountAttributes}.
     */
    public static class Builder {
        private AccountAttributes accountAttributes;

        public Builder() {
            accountAttributes = new AccountAttributes();
        }

        public Builder withCreatedAt(Instant createdAt) {
            accountAttributes.createdAt = createdAt;
            return this;
        }

        public Builder withGoogleId(String googleId) {
            accountAttributes.googleId = googleId;
            return this;
        }

        public Builder withName(String name) {
            accountAttributes.name = name;
            return this;
        }

        public Builder withIsInstructor(boolean isInstructor) {
            accountAttributes.isInstructor = isInstructor;
            return this;
        }

        public Builder withEmail(String email) {
            accountAttributes.email = email;
            return this;
        }

        public Builder withInstitute(String institute) {
            accountAttributes.institute = institute;
            return this;
        }

        public AccountAttributes build() {
            accountAttributes.googleId = SanitizationHelper.sanitizeGoogleId(accountAttributes.googleId);
            accountAttributes.name = SanitizationHelper.sanitizeName(accountAttributes.name);
            accountAttributes.email = SanitizationHelper.sanitizeEmail(accountAttributes.email);
            accountAttributes.institute = SanitizationHelper.sanitizeTitle(accountAttributes.institute);

            return accountAttributes;
        }

    }

    /**
     * Gets a deep copy of this object.
     */
    public AccountAttributes getCopy() {
        return AccountAttributes.builder()
                .withGoogleId(googleId)
                .withName(name)
                .withEmail(email)
                .withInstitute(institute)
                .withIsInstructor(isInstructor)
                .build();
    }

    public boolean isInstructor() {
        return isInstructor;
    }

    public String getGoogleId() {
        return googleId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getTruncatedGoogleId() {
        return StringHelper.truncateLongId(googleId);
    }

    public String getInstitute() {
        return institute;
    }

    @Override
    public List<String> getInvalidityInfo() {
        FieldValidator validator = new FieldValidator();
        List<String> errors = new ArrayList<>();

        addNonEmptyError(validator.getInvalidityInfoForPersonName(name), errors);

        addNonEmptyError(validator.getInvalidityInfoForGoogleId(googleId), errors);

        addNonEmptyError(validator.getInvalidityInfoForEmail(email), errors);

        addNonEmptyError(validator.getInvalidityInfoForInstituteName(institute), errors);

        // No validation for isInstructor and createdAt fields.

        return errors;
    }

    @Override
    public Account toEntity() {
        return new Account(googleId, name, isInstructor, email, institute);
    }

    @Override
    public String toString() {
        return JsonUtils.toJson(this, AccountAttributes.class);
    }

    @Override
    public String getIdentificationString() {
        return this.googleId;
    }

    @Override
    public String getEntityTypeAsString() {
        return "Account";
    }

    @Override
    public String getBackupIdentifier() {
        return "Account";
    }

    @Override
    public String getJsonString() {
        return JsonUtils.toJson(this, AccountAttributes.class);
    }

    @Override
    public void sanitizeForSaving() {
        this.googleId = SanitizationHelper.sanitizeForHtml(googleId);
        this.name = SanitizationHelper.sanitizeForHtml(name);
        this.institute = SanitizationHelper.sanitizeForHtml(institute);
    }

    public boolean isUserRegistered() {
        return googleId != null && !googleId.isEmpty();
    }

    /**
     * Updates with {@link UpdateOptions}.
     */
    public void update(UpdateOptions updateOptions) {
        updateOptions.isInstructorOptional.ifPresent(s -> isInstructor = s);
    }

    /**
     * Returns a {@link UpdateOptions.Builder} to build {@link UpdateOptions} for account.
     */
    public static UpdateOptions.Builder updateOptionsBuilder(String googleId) {
        return new UpdateOptions.Builder(googleId);
    }

    /**
     * Helper class to specific the fields to update in {@link AccountAttributes}.
     */
    public static class UpdateOptions {
        private String googleId;

        private UpdateOption<Boolean> isInstructorOptional = UpdateOption.empty();

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

            public Builder withIsInstructor(boolean isInstructor) {
                updateOptions.isInstructorOptional = UpdateOption.of(isInstructor);
                return this;
            }

            public UpdateOptions build() {
                return updateOptions;
            }

        }

        @Override
        public String toString() {
            return "AccountAttributes.UpdateOptions ["
                    + "googleId = " + googleId
                    + ", isInstructor = " + isInstructorOptional
                    + "]";
        }

    }

}
