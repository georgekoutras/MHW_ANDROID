package gr.openit.smarthealthwatch;

public class AdviceRow {

    String title;
    String id;


    public AdviceRow(String id,String title) {
        this.id = id;
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public String getId() {
        return id;
    }
}
