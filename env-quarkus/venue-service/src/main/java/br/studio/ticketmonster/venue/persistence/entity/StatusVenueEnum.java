package br.studio.ticketmonster.venue.persistence.entity;

public enum StatusVenueEnum {

    ACTIVE("Active"), 
    INACTIVE("Inactive"), 
    CREATED("Created"), 
    MAINTENANCE("Maintenance"), 
    CLOSED("Closed");

    private String value;

    StatusVenueEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static StatusVenueEnum fromValue(String value) {
        for (StatusVenueEnum status : StatusVenueEnum.values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        return null;
    }

}
