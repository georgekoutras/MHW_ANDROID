package gr.openit.smarthealthwatch;

public class MeasurementRow {

    String time;
    String data;
    String id;
    String data_extra;
    public MeasurementRow(String id,String time, String data) {
        this.id = id;
        this.time=time;
        this.data=data;
    }

    public MeasurementRow(String id,String time, String data, String data_extra) {
        this.id = id;
        this.time=time;
        this.data=data;
        this.data_extra = data_extra;
    }

    public String getTime() {
        return time;
    }

    public String getData() {
        return data;
    }

    public String getExtraData(){
        return data_extra;
    }

    public String getId() {
        return id;
    }
}
