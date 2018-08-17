package teammates.storage.api;

import java.time.Instant;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.TimestampValue;
import com.google.cloud.datastore.Value;
import com.google.cloud.datastore.ValueType;
import com.googlecode.objectify.impl.translate.SimpleTranslatorFactory;


/**
 * Translates between `java.time.Instant` in entity class and `com.google.cloud.Timestamp` in Google Cloud Datastore.
 */
public class InstantTranslatorFactory extends SimpleTranslatorFactory<Instant, Timestamp> {
    public InstantTranslatorFactory() {
        super(Instant.class, ValueType.TIMESTAMP);
    }

    @Override
    protected Instant toPojo(final Value<Timestamp> value) {
        return value.get().toSqlTimestamp().toInstant();
    }

    @Override
    protected Value<Timestamp> toDatastore(final Instant value) {
        return TimestampValue.of(
                Timestamp.ofTimeSecondsAndNanos(
                        value.getEpochSecond(),
                        value.getNano()
                ));
    }
}