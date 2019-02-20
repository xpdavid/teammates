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

    private static final String ACCOUNT_BACKUP_LOG_MSG = "Recently modified account::";

    public String googleId;
    public String name;
    public boolean isInstructor;
    public String email;
    public String institute;
    public Instant createdAt;

    AccountAttributes(String googleId) {
        this.googleId = googleId;
    }

    public static AccountAttributes valueOf(Account a) {
        AccountAttributes accountAttributes = new AccountAttributes(a.getGoogleId());

        accountAttributes.name = a.getName();
        accountAttributes.isInstructor = a.isInstructor();
        accountAttributes.email = a.getEmail();
        accountAttributes.institute = a.getInstitute();
        accountAttributes.createdAt = a.getCreatedAt();

        return accountAttributes;
    }

    /**
     * Returns a builder for {@link AccountAttributes}.
     */
    public static Builder builder(String googleId) {
        return new Builder(googleId);
    }

    /**
     * Gets a deep copy of this object.
     */
    public AccountAttributes getCopy() {
        AccountAttributes accountAttributes = new AccountAttributes(this.googleId);

        accountAttributes.name = this.name;
        accountAttributes.isInstructor = this.isInstructor;
        accountAttributes.email = this.email;
        accountAttributes.institute = this.institute;
        accountAttributes.createdAt = this.createdAt;

        return accountAttributes;
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

    public Instant getCreatedAt() {
        return createdAt;
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
    public String getBackupIdentifier() {
        return ACCOUNT_BACKUP_LOG_MSG + getGoogleId();
    }

    @Override
    public void sanitizeForSaving() {
        this.googleId = SanitizationHelper.sanitizeGoogleId(googleId);
        this.name = SanitizationHelper.sanitizeName(name);
        this.email = SanitizationHelper.sanitizeEmail(email);
        this.institute = SanitizationHelper.sanitizeTitle(institute);
    }

    public boolean isUserRegistered() {
        return googleId != null && !googleId.isEmpty();
    }

    /**
     * Updates with {@link UpdateOptions}.
     */
    public void update(UpdateOptions updateOptions) {
        updateOptions.isInstructorOption.ifPresent(s -> isInstructor = s);
    }

    /**
     * Returns a {@link UpdateOptions.Builder} to build {@link UpdateOptions} for an account.
     */
    public static UpdateOptions.Builder updateOptionsBuilder(String googleId) {
        return new UpdateOptions.Builder(googleId);
    }

    /**
     * A builder class for {@link AccountAttributes}.
     */
    public static class Builder extends BasicBuilder<AccountAttributes, Builder> {

        private AccountAttributes accountAttributes;

        private Builder(String googleId) {
            super(new UpdateOptions(googleId));
            thisBuilder = this;

            accountAttributes = new AccountAttributes(googleId);
        }

        public Builder withName(String name) {
            Assumption.assertNotNull(Const.StatusCodes.NULL_PARAMETER, name);

            accountAttributes.name = name;
            return this;
        }

        public Builder withEmail(String email) {
            Assumption.assertNotNull(Const.StatusCodes.NULL_PARAMETER, email);

            accountAttributes.email = email;
            return this;
        }

        public Builder withInstitute(String institute) {
            Assumption.assertNotNull(Const.StatusCodes.NULL_PARAMETER, institute);

            accountAttributes.institute = institute;
            return this;
        }

        @Override
        public AccountAttributes build() {
            accountAttributes.update(updateOptions);

            return accountAttributes;
        }
    }

    /**
     * Helper class to specific the fields to update in {@link AccountAttributes}.
     */
    public static class UpdateOptions {
        private String googleId;

        private UpdateOption<Boolean> isInstructorOption = UpdateOption.empty();

        private UpdateOptions(String googleId) {
            Assumption.assertNotNull(Const.StatusCodes.NULL_PARAMETER, googleId);

            this.googleId = googleId;
        }

        public String getGoogleId() {
            return googleId;
        }

        @Override
        public String toString() {
            return "AccountAttributes.UpdateOptions ["
                    + "googleId = " + googleId
                    + ", isInstructor = " + isInstructorOption
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
     * Basic builder to build {@link AccountAttributes} related classes.
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

        public B withIsInstructor(boolean isInstructor) {
            updateOptions.isInstructorOption = UpdateOption.of(isInstructor);
            return thisBuilder;
        }

        public abstract T build();
    }

}
