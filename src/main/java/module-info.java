module dev.blaauwendraad.masker.json {
    requires java.base;
    // https://github.com/ben-manes/caffeine/issues/535#issuecomment-854879514
    requires static org.jspecify;

    exports dev.blaauwendraad.masker.json;
    exports dev.blaauwendraad.masker.json.config;
    exports dev.blaauwendraad.masker.json.path;
}