package gr.openit.smarthealthwatch;

import org.json.JSONObject;

public class MessageRow {

    String sender;
    String id;
    String date;
    Boolean read;
    JSONObject data;
    Boolean isMessage;
    public MessageRow(String id,String sender, String date, Boolean read, JSONObject data, Boolean isMessage) {
        this.id = id;
        this.sender = sender;
        this.date = date;
        this.read = read;
        this.data = data;
        this.isMessage = isMessage;
    }

    public String getSender() {
        return sender;
    }

    public String getId() {
        return id;
    }

    public String getDate() { return  date; }

    public Boolean getRead() { return read; }

    public JSONObject getData() { return data; }

    public boolean isMessage() { return isMessage; }
}
