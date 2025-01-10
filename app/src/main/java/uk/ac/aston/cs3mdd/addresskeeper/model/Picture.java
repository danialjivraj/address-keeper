package uk.ac.aston.cs3mdd.addresskeeper.model;

import java.io.Serializable;

public class Picture implements Serializable {
    private String large;

    public Picture() {
        this.large = null;
    }

    public String getLarge() {
        return large;
    }

    public void setLarge(String large) {
        this.large = large;
    }
}
