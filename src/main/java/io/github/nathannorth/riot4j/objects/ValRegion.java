package io.github.nathannorth.riot4j.objects;

/**
 * Val regions represent valid regions to execute api calls against
 */
public enum ValRegion {
    NORTH_AMERICA("NA"),
    BRAZIL("br"),
    EUROPE("eu"),
    LATIN_AMERICA("latam"),
    ASIA_PACIFIC("ap"),
    KOREA("kr");

    private final String value;
    ValRegion(String value) {
        this.value = value;
    }
    public String getValue() {
        return value;
    }
}
