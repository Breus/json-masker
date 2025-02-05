package notdefaultpackage;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.encoder.EncoderBase;
import dev.blaauwendraad.masker.json.JsonMasker;
import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;
import dev.blaauwendraad.masker.json.config.KeyMaskingConfig;

import java.util.Set;

public class MaskingEncoder extends EncoderBase<ILoggingEvent> {

    private JsonMaskingConfig.Builder builder = JsonMaskingConfig.builder();
    private JsonMasker jsonMasker;

    private Encoder<ILoggingEvent> delegate;

    public void setDelegate(Encoder<ILoggingEvent> delegate) {
        this.delegate = delegate;
    }

    public void setMaskKeys(String maskKeys) {
        builder.maskKeys(Set.of(maskKeys.split(",")));
    }

    public void addMaskKey(KeyConfig keyConfig) {
        builder.maskKeys(keyConfig.key, KeyMaskingConfig.builder().maskStringsWith(keyConfig.maskStringsWith).build());
    }

    public void setMaskJsonPaths(String maskJsonPaths) {
        builder.maskJsonPaths(Set.of(maskJsonPaths.split(",")));
    }

    public void setAllowKeys(String allowKeys) {
        builder.allowKeys(Set.of(allowKeys.split(",")));
    }

    public void setAllowJsonPaths(String allowJsonPaths) {
        builder.allowJsonPaths(Set.of(allowJsonPaths.split(",")));
    }

    public void setCaseSensitiveKeys(boolean value) {
        if (value) {
            builder.caseSensitiveTargetKeys();
        }
    }

    public void setMaskStringsWith(String value) {
        builder.maskStringsWith(value);
    }

    public void setMaskNumbersWith(int value) {
        builder.maskNumbersWith(value);
    }

    public void setMaskNumbersWithString(String value) {
        builder.maskNumbersWith(value);
    }

    @Override
    public void start() {
        super.start();

        jsonMasker = JsonMasker.getMasker(builder.build());
    }

    @Override
    public byte[] headerBytes() {
        return delegate.headerBytes();
    }

    @Override
    public byte[] encode(ILoggingEvent event) {
        var bytes = delegate.encode(event);
        return jsonMasker.mask(bytes);
    }

    @Override
    public byte[] footerBytes() {
        return delegate.footerBytes();
    }

    public static class KeyConfig {
        private String key;
        private String maskStringsWith;

        public void setKey(String key) {
            this.key = key;
        }

        public void setMaskStringsWith(String value) {
            this.maskStringsWith = value;
        }
    }
}
