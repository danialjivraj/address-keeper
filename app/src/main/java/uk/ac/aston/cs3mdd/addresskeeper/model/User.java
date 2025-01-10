package uk.ac.aston.cs3mdd.addresskeeper.model;

import java.io.Serializable;
import java.util.UUID;

public class User implements Serializable {
    private String id;
    private Name name;
    private Picture picture;
    private Location location;

    public User() {
        this.id = UUID.randomUUID().toString();
        this.name = new Name();
        this.picture = new Picture();
        this.location = new Location();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Name getName() {
        return name;
    }

    public void setName(Name name) {
        this.name = name;
    }

    public Picture getPicture() {
        return picture;
    }

    public void setPicture(Picture picture) {
        this.picture = picture;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (name != null) {
            sb.append(name.getFirst() != null ? name.getFirst() : "");
            sb.append(" ");
            sb.append(name.getLast() != null ? name.getLast() : "");
        } else {
            sb.append("Unnamed Entity");
        }
        return sb.toString();
    }
}
