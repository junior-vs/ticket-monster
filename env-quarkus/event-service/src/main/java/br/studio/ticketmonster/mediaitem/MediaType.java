package br.studio.ticketmonster.mediaitem;

public enum MediaType {

    IMAGE("Image", true),
    VIDEO("video", false),
    AUDIO("audio", false),
    TEXT("text", true);

    private String description;
    private boolean cacheable;

    private MediaType(String description, boolean cacheable) {
        this.description = description;
        this.cacheable = cacheable;
    }

    public String getDescription() {
        return description;
    }

    public boolean isCacheable() {
        return cacheable;
    }

}
