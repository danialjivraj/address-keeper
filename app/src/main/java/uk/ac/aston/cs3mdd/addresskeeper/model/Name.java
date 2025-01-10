package uk.ac.aston.cs3mdd.addresskeeper.model;

import java.io.Serializable;

public class Name implements Serializable {
    private String first;
    private String last;

    public Name() {
        this.first = "";
        this.last = "";
    }

    public String getFirst() {
        return first;
    }

    public void setFirst(String first) {
        this.first = first;
    }

    public String getLast() {
        return last;
    }

    public void setLast(String last) {
        this.last = last;
    }
}
