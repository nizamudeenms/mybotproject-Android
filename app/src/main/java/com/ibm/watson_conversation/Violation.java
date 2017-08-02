package com.ibm.watson_conversation;

/**
 * Created by nizamudeenms on 06/06/17.
 */

public class Violation {
    private String ID = "id";
    private String DATE = "date";
    private String TIME = "time";
    private String DESC = "desc";
    private String PLACE = "place";
    private String POINTS = "points";
    private String AMOUNT = "amount";

    public Violation() {
    }

    public String getID() {
        return ID;
    }

    public void setID(String ID) {
        this.ID = ID;
    }

    public String getDATE() {
        return DATE;
    }

    public void setDATE(String DATE) {
        this.DATE = DATE;
    }

    public String getTIME() {
        return TIME;
    }

    public void setTIME(String TIME) {
        this.TIME = TIME;
    }

    public String getDESC() {
        return DESC;
    }

    public void setDESC(String DESC) {
        this.DESC = DESC;
    }

    public String getPLACE() {
        return PLACE;
    }

    public void setPLACE(String PLACE) {
        this.PLACE = PLACE;
    }

    public String getPOINTS() {
        return POINTS;
    }

    public void setPOINTS(String POINTS) {
        this.POINTS = POINTS;
    }

    public String getAMOUNT() {
        return AMOUNT;
    }

    public void setAMOUNT(String AMOUNT) {
        this.AMOUNT = AMOUNT;
    }
}
