package gr.openit.smarthealthwatch.util;

import com.google.gson.JsonArray;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

public class User {

    private int id;
    private String firstName, lastName, email, phone;
    private Set<String> monitorTypes;
    private Integer year;
    private JSONArray thresholds;
    public User(int id,  String firstName, String lastName, String email, Set<String> monitorTypes, Integer year, String phone) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.monitorTypes = monitorTypes;
        this.year = year;
        this.phone = phone;
    }

    public User(int id, String firstName, String lastName, String email, Set<String> monitorTypes) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.monitorTypes = monitorTypes;
    }

    public User(int id, String firstName, String lastName, String email, Set<String> monitorTypes, Integer year) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.monitorTypes = monitorTypes;
        this.year = year;
    }

    public User(int id,  String firstName, String lastName, String email, Set<String> monitorTypes, String phone) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.monitorTypes = monitorTypes;
        this.phone = phone;
    }

    public int getId() {
        return this.id;
    }

    public String getFirstName() {
        return this.firstName;
    }

    public String getLastName() {
        return this.lastName;
    }

    public String getEmail() {
        return this.email;
    }

    public Set<String> getMonitorTypes() { return this.monitorTypes;}

    public JSONArray getThresholds(){ return this.thresholds; }

    public void setThresholds(JSONArray thresholds){
        this.thresholds = thresholds;
    }

    public String getPhone() { return this.phone; }

    public Integer getBirthYear() { return this.year; }

    public void setFirstName(String firstName){ firstName = firstName;}

    public void setLastName(String lastName){ lastName = lastName;}

    public void setYear(Integer birthYear){ this.year = birthYear; }

    public void setPhone(String phone){ this.phone = phone; }

    public void setMonitorTypes(ArrayList<String> monitorTypes) { this.monitorTypes = new HashSet<String>(monitorTypes); }
}
