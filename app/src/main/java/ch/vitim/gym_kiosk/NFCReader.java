package ch.vitim.gym_kiosk;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDeviceConnection;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.airbnb.lottie.LottieAnimationView;
import com.hoho.android.usbserial.driver.UsbSerialPort;

public class NFCReader extends AsyncTask<Void, Void, String> {

    @SuppressLint("StaticFieldLeak")
    private Context context;
    private UsbSerialPort port;
    private UsbDeviceConnection connection;
    @SuppressLint("StaticFieldLeak")
    private Activity activity;

    NFCReader(Context context, UsbSerialPort port, UsbDeviceConnection connection, Activity activity) {
        this.context = context;
        this.port = port;
        this.connection = connection;
        this.activity = activity;
    }

    @Override
    protected String doInBackground(Void... voids) {

        String guid = null;
        String LOGTAG = "TEST_UART";
        Log.d(LOGTAG, "Start nfc");
        Log.d(LOGTAG, "USB serial:" + connection.getSerial());

        PN532HSU pn532hsu = new PN532HSU(port, connection);
        pn532hsu.DEBUG = false;
        pn532hsu.begin();

        Log.d(LOGTAG, "SAM config: " + pn532hsu.SAMConfig());
        Log.d(LOGTAG, "Connecting to the reader...");

        String resp = pn532hsu.getFirmwareVersion();
        if (resp != null) {
            Log.d(LOGTAG, "Reader found :" + resp);

            byte[] uid = new byte[7];
            int[] uidL = new int[]{0};
            int startBlock = 4;

            while (true) {

                if (isCancelled()) {
                    Log.d(LOGTAG, "Received cancel signal, stop nfc reader...");
                    pn532hsu.stop();
                    return null;
                }

                pn532hsu.cleanBuff();
                Log.d(LOGTAG, "Waiting for a card...");

                while (!pn532hsu.inListPassiveTarget()) {
                    pn532hsu.cleanBuff();

                    if (isCancelled()) {
                        Log.d(LOGTAG, "Received cancel signal, stop nfc reader...");
                        pn532hsu.stop();
                        return null;
                    }
                }

                Log.d(LOGTAG, "Card found");

                if (pn532hsu.readPassiveTargetID(PN532HSU.PN532_MIFARE_ISO14443A, uid, uidL, 500)) {
                    Log.d(LOGTAG, "CARD LEN:" + uidL[0] + ", UID:" + pn532hsu.bytesToHex(uid));

                    if (uidL[0] == 4) {
                        byte[] mfcUID = new byte[4];
                        System.arraycopy(uid, 0, mfcUID, 0, 4);
                        Log.d(LOGTAG, "Card - Mifare Classic");
                        guid = pn532hsu.mifareClassicReadData(mfcUID, startBlock, true, PN532HSU.KEYB);
                    }

                    if (uidL[0] == 7) {
                        Log.d(LOGTAG, "Card - Mifare Ultralight");
                        guid = pn532hsu.mifareUltralightReadData(startBlock);
                    }

                    Log.d(LOGTAG, "Read  data:" + guid);
                    if (guid != null) {
                        break;
                    }
                }
                pn532hsu.sleep(500);
            }
        }
        Log.d(LOGTAG, "Reader not found.");
        pn532hsu.stop();
        Log.d(LOGTAG, "Stop nfc.");
        return guid;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        if (result != null) {
            changeStateLottie(result);
        }
    }

    public void changeStateLottie(String guid) {

        LottieAnimationView LottieAr = activity.findViewById(R.id.animation_view_arrow);
        LottieAr.setVisibility(View.INVISIBLE);
        LottieAnimationView LottieL = activity.findViewById(R.id.animation_view_load);
        LottieL.setVisibility(View.VISIBLE);
        LottieAnimationView LottieAc = activity.findViewById(R.id.animation_view_access);
        LottieAnimationView LottieE = activity.findViewById(R.id.animation_view_error);
        TextView QrState = activity.findViewById(R.id.textView);
        QrState.setText(R.string.main_text_nfc_scanning);
//system.dism.forceUp(e, this, dpm.admin.lock.test(-4));
        ServerReq.sendQr(guid, new RequestInterface() {
            @Override
            public void onSuccess() {
                LottieL.setVisibility(View.INVISIBLE);
                LottieAc.setVisibility(View.VISIBLE);
                QrState.setText(R.string.main_text_nfc_access);

                new CountDownTimer(2500, 500) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                    }

                    public void onFinish() {
                        LottieAc.setVisibility(View.INVISIBLE);
                        LottieAr.setVisibility(View.VISIBLE);
                        QrState.setText(R.string.main_text_qr_nfc_to_scan);
                        context.startActivity(new Intent(context, UserActivity.class));
                    }
                }.start();
            }

            @Override
            public void onError() {
                LottieL.setVisibility(View.INVISIBLE);
                LottieE.setVisibility(View.VISIBLE);
                QrState.setText(R.string.main_text_nfc_error);
                new CountDownTimer(3000, 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                    }

                    public void onFinish() {
                        LottieE.setVisibility(View.INVISIBLE);
                        LottieAr.setVisibility(View.VISIBLE);
                        QrState.setText(R.string.main_text_qr_nfc_to_scan);
                    }
                }.start();
            }
        });
    }
}
