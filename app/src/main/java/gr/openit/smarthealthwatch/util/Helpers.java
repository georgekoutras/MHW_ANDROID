package gr.openit.smarthealthwatch.util;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Helpers {

    public static final List<String> HOURS = new ArrayList<>( Arrays.asList("Ώρα", "00", "01", "02", "03", "04", "05", "06",
            "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17",
            "18", "19", "20", "21", "22", "23" ));

    public static final List<String> MINUTES =new ArrayList<>( Arrays.asList("Λεπτά", "00", "05", "10", "15", "20", "25", "30",
            "35", "40", "45", "50", "55" ));

    public static final List<String> GLUCE_TYPES =new ArrayList<>( Arrays.asList("Πρωινή", "Προγευματική", "Μεταγευματική"));

    public static final List<String> OCCUPATION_LIST =new ArrayList<>( Arrays.asList("Ιατρός", "Φροντιστής","Συγγενής"));


    public static void hideKeyboard(Activity activity) {
        InputMethodManager inputManager = (InputMethodManager) activity
                .getSystemService(Context.INPUT_METHOD_SERVICE);

        // check if no view has focus:
        View currentFocusedView = activity.getCurrentFocus();
        if (currentFocusedView != null) {
            inputManager.hideSoftInputFromWindow(currentFocusedView.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    public static JSONArray sortJsonArray(JSONArray array) {
        List<JSONObject> jsons = new ArrayList<JSONObject>();
        try {
            for (int i = 0; i < array.length(); i++) {
                jsons.add(array.getJSONObject(i));
            }
            Collections.sort(jsons, new Comparator<JSONObject>() {
                @Override
                public int compare(JSONObject lhs, JSONObject rhs) {
                    String lid = null;
                    String rid = null;
                    try {
                        lid = lhs.getString("timeStamp");
                        rid = rhs.getString("timeStamp");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    return rid.compareTo(lid);

                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return new JSONArray(jsons);
    }
}
