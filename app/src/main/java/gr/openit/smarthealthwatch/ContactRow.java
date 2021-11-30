package gr.openit.smarthealthwatch;

public class ContactRow {

    String name;
    String id;


    public ContactRow(String id,String name) {
        this.id = id;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }
}
