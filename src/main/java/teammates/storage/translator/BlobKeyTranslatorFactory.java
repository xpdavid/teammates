package teammates.storage.translator;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.cloud.datastore.StringValue;
import com.google.cloud.datastore.Value;
import com.google.cloud.datastore.ValueType;
import com.googlecode.objectify.impl.translate.SimpleTranslatorFactory;

/**
 * Translates between `com.google.appengine.api.blobstore.BlobKey` in entity class and `String` in Google Cloud Datastore.
 */
public class BlobKeyTranslatorFactory extends SimpleTranslatorFactory<BlobKey, String> {
    public BlobKeyTranslatorFactory() {
        super(BlobKey.class, ValueType.STRING);
    }

    @Override
    protected BlobKey toPojo(final Value<String> blobKeyStr) {
        return new BlobKey(blobKeyStr.get());
    }

    @Override
    protected Value<String> toDatastore(final BlobKey blobKey) {
        return StringValue.of(blobKey.getKeyString());
    }
}
