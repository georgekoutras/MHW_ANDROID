/*
 * Copyright (c) 2017 Garmin International. All Rights Reserved.
 * <p></p>
 * This software is the confidential and proprietary information of
 * Garmin International.
 * You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement
 * you entered into with Garmin International.
 * <p></p>
 * Garmin International MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF THE
 * SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, OR NON-INFRINGEMENT. Garmin International SHALL NOT BE LIABLE FOR ANY DAMAGES
 * SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING
 * THIS SOFTWARE OR ITS DERIVATIVES.
 * <p></p>
 * Created by johnsongar on 3/8/2017.
 */
package gr.openit.smarthealthwatch.ui;

import android.util.Log;
import android.view.MenuItem;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import gr.openit.smarthealthwatch.GarminHealthServiceReceiver;
import com.garmin.health.GarminHealth;

public abstract class BaseGarminHealthActivity extends UnityPlayerActivity {
    private static final String TAG = BaseGarminHealthActivity.class.getSimpleName();

    private static GarminHealthServiceReceiver mReceiver = new GarminHealthServiceReceiver();

    @Override
    protected void onStart() {
        if (GarminHealth.isInitialized()) {
            GarminHealth.restart();
        }
        Log.i("garminActivity","started");

        registerReceiver(mReceiver, GarminHealthServiceReceiver.INTENT_FILTER);

        super.onStart();
    }

    @Override
    protected void onStop() {
        if (GarminHealth.isInitialized()) {
            //GarminHealth.stop();
            GarminHealth.restart();

        }
        Log.i("garminActivity","stopped");
        unregisterReceiver(mReceiver);

        super.onStop();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
