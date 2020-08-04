package ch.vitim.gym_kiosk;

import android.app.DownloadManager;
import android.content.Intent;
import android.os.Handler;
import android.util.JsonReader;
import android.util.Log;

import org.json.JSONObject;

import java.util.Random;

public class ServerReq {

    static String[] types={"treadmill","exercise_bike","barbell","ab_bench"};
    public static String training_apparatus = types[0];
    public static void sendQr(final String jsonstring, final RequestInterface requestInterface){
        Runnable runnable = new Runnable() {
            public void run() {
                Log.i("Tag", "Runnable running!");
                Random r = new Random();
                int i1 = r.nextInt(10);


                Log.wtf("Tag", jsonstring+"      "+ i1 + "");
                if (i1<=9 ) {
                    Log.wtf("Tag", "RonSuccess");
                    requestInterface.onSuccess();
                }else {
                    Log.wtf("Tag", "onError");
                    requestInterface.onError();
                }
            }
        };

        Handler handler = new android.os.Handler();
        handler.postDelayed(runnable, 3*1000);
    }
}
//https://next.json-generator.com/api/json/get/NkivyJfVu