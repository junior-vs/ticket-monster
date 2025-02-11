package br.studio.ticketmonster.venue.persistence.entity;

public enum VenueTypeEnum {

    //'auditório', 'estádio', 'sala de conferência', 'teatro', 'outro'

    AUDITORIUM("auditorium"),
    STADIUM("stadium"),
    CONFERENCE_ROOM("conference room"),
    THEATER("theater"),
    OTHER("other");

    private final String type;

    VenueTypeEnum(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
    
    public static VenueTypeEnum fromString(String type) {
        for (VenueTypeEnum venueType : VenueTypeEnum.values()) {
            if (venueType.type.equalsIgnoreCase(type)) {
                return venueType;
            }
        }
        throw new IllegalArgumentException("No constant with type " + type + " found");
    }
}
