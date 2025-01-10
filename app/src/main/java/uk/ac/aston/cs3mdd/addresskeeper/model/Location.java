package uk.ac.aston.cs3mdd.addresskeeper.model;

import java.io.Serializable;

public class Location implements Serializable {
    private Coordinates coordinates;

    public void setCoordinates(Coordinates coordinates) {
        this.coordinates = coordinates;
    }

    public Coordinates getCoordinates() {
        return coordinates;
    }
}
