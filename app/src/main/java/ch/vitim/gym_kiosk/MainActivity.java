package ch.vitim.gym_kiosk;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.airbnb.lottie.LottieAnimationView;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.File;
import java.io.IOException;
import java.util.List;


public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback
       // , MediaPlayer.OnPreparedListener
{

    public Handler handler;
    public Runnable r;
    public EditText qrReader;
    private LockGUI lockGUI = new LockGUI(this);
    private MediaPlayer mediaPlayer;
    private SurfaceHolder vidHolder;
    private SurfaceView vidSurface;
    LinearLayout time;
    LinearLayout arrows;
    LottieAnimationView LottieAr;
    LottieAnimationView LottieE;
    LottieAnimationView LottieAc;
    LottieAnimationView LottieL;
    TextView QrState;
    TextClock clock;

    private int scanQrCount = 0;
    private UsbManager manager;
    private List<UsbSerialDriver> availableDrivers;

    private NFCReader nfcReader;
    String LOGTAG = "TEST_UART";

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            Runtime.getRuntime().exec("dpm set-device-owner ch.vitim.gym_kiosk/.DevAdminReceiver");
        } catch (IOException e) {
            Log.e("Access", "aaaa");
        }
        if (RootUtil.isDeviceRooted()) {
            lockGUI.initStartTaskMode(this);
            Log.e("Access", "Locked");
        } else Log.e("Access", "Unlocked");
        setContentView(R.layout.activity_main);
        time = findViewById(R.id.viewTime);
        arrows = findViewById(R.id.arrows);
        time.setVisibility(View.VISIBLE);
        arrows.setVisibility(View.VISIBLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        handler = new Handler();
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
        );
        qrReader = (EditText) findViewById(R.id.editText);
        qrReader.setShowSoftInputOnFocus(false);
        qrReader.requestFocus();
        qrReader.setShowSoftInputOnFocus(false);


        qrReader.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                scanQrCount++;
                Log.d(LOGTAG, "QRcode read: " + textView.getText());
                if (scanQrCount == 1) {
                    QrReader.qrString = String.valueOf(qrReader.getText());
                    changeStateLottie();
                }
                textView.setText("");
                return true;
            }
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
        }
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        Log.e("Access", "Files"));
            }
        } else {
            // Permission has already been granted
        }

        //Search usb serial port for nfc reader
        manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
        runNfcAsyncTask();
    }


    public void runNfcAsyncTask() {

        if (!availableDrivers.isEmpty()) {
            UsbSerialDriver driver = availableDrivers.get(0);
            UsbDeviceConnection connection = manager.openDevice(driver.getDevice());
            if (connection != null) {

                if (nfcReader == null || nfcReader.getStatus().toString().equals("FINISHED")) {
                    nfcReader = new NFCReader(MainActivity.this, driver.getPorts().get(0), connection, this);
                }

                Log.d(LOGTAG, "STATUS:" + nfcReader.getStatus());
                if (nfcReader != null && nfcReader.getStatus().toString().equals("PENDING")) {
                    nfcReader.execute();
                }

            } else {
                Log.d(LOGTAG, "runNfcAsyncTask(): connection null");
            }
        } else {
            Log.d(LOGTAG, "runNfcAsyncTask(): availableDrivers empty");
        }
    }


    public void changeStateLottie() {

        final Intent UserI = new Intent(this, UserActivity.class);
        String qr = QrReader.qrString;
        LottieAr = findViewById(R.id.animation_view_arrow);
        LottieAr.setVisibility(View.INVISIBLE);
        LottieL = findViewById(R.id.animation_view_load);
        LottieL.setVisibility(View.VISIBLE);
        LottieAc = findViewById(R.id.animation_view_access);
        LottieE = findViewById(R.id.animation_view_error);
//        LottieAr.setAnimation(R.raw.load);
        QrState = findViewById(R.id.textView);
        QrState.setText(R.string.main_text_qr_scanning);
        ServerReq.sendQr(qr, new RequestInterface() {
            @Override
            public void onSuccess() {

                if (nfcReader != null && nfcReader.getStatus().toString().equals("RUNNING")) {
                    nfcReader.cancel(false);
                }
                LottieL.setVisibility(View.INVISIBLE);
                LottieAc.setVisibility(View.VISIBLE);
                QrState.setText(R.string.main_text_qr_access);
                new CountDownTimer(2500, 500) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                    }
                    public void onFinish() {
                        LottieAc.setVisibility(View.INVISIBLE);
                        LottieAr.setVisibility(View.VISIBLE);
                        QrReader.qrString = "";
                        QrReader.writed = false;
                        QrState.setText(R.string.main_text_qr_nfc_to_scan);
                        startActivity(UserI);
                        scanQrCount = 0;
                    }
                }.start();
            }

            @Override
            public void onError() {
                LottieL.setVisibility(View.INVISIBLE);
                LottieE.setVisibility(View.VISIBLE);
                QrState.setText(R.string.main_text_qr_error);
                new CountDownTimer(3000, 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                    }
                    public void onFinish() {
                        LottieE.setVisibility(View.INVISIBLE);
                        LottieAr.setVisibility(View.VISIBLE);
                        QrReader.qrString = "";
                        QrReader.writed = false;
                        qrReader.setFreezesText(false);
                        QrState.setText(R.string.main_text_qr_nfc_to_scan);
                        scanQrCount = 0;
                    }
                }.start();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.e("QRw34", QrReader.qrString);
        if ((QrReader.qrString != "" && QrReader.writed)) {
            changeStateLottie();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        qrReader = (EditText) findViewById(R.id.editText);
        qrReader.setText(null);
//        try {
//            if (mediaPlayer.isPlaying()) {
//                mediaPlayer.stop();
//            }
//        } catch (Exception e) {
//            System.err.println(e);
//        }

        try {
            nfcReader.cancel(false);
        } catch (RuntimeException e) {
            Log.e(LOGTAG, "onStop() error stop nfc reader");
        }
    }

    public void onDestroy() {
        super.onDestroy();
//        try {
//            if (mediaPlayer.isPlaying()) {
//                mediaPlayer.stop();
//            }
//        } catch (Exception e) {
//            System.err.println(e);
//        }
//
        try {
            nfcReader.cancel(false);
        } catch (RuntimeException e) {
            Log.e(LOGTAG, "onDestroy() error stop nfc reader");
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        qrReader = (EditText) findViewById(R.id.editText);
        qrReader.setText(QrReader.qrString);
        Log.e("FF", QrReader.qrString);
        vidSurface = (SurfaceView) findViewById(R.id.surfView);
        vidHolder = vidSurface.getHolder();
        vidHolder.addCallback(this);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        qrReader = (EditText) findViewById(R.id.editText);
        qrReader.setText(null);
//        try {
//            mediaPlayer.stop();
//        } catch (Exception e) {
//            System.err.println(e);
//        }
    }


    @Override
    public void onResume() {
        super.onResume();
        lockGUI.hideSystemUI(this);
        if (RootUtil.isDeviceRooted()) {
            if (lockGUI.dpm.isLockTaskPermitted(this.getPackageName())) {
                this.startLockTask();
                Intent intent = new Intent(this, MainActivity.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    lockGUI.onLockTaskModeEntering(this, intent);
                }
            } else {
                // Because the package isn't whitelisted, calling startLockTask() here
                // would put the activity into screen pinning mode.
            }
        }

        // Start NFC
        runNfcAsyncTask();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (!hasFocus) {
            lockGUI.hideSystemUI(this);
            Intent closeDialog = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            sendBroadcast(closeDialog);
        }
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
//        try {
//            @SuppressLint("SdCardPath") String FILE_PATH = "/storage/self/primary/.gym_kiosk/screen.m4v";
//            File file = new File(FILE_PATH);
//            if (file.exists()) {
//                mediaPlayer = MediaPlayer.create(this, Uri.fromFile(file));
//            } else {
//                mediaPlayer = MediaPlayer.create(this, R.raw.screen);
//            }
//            mediaPlayer.setLooping(true);
//            mediaPlayer.setDisplay(vidHolder);
//            mediaPlayer.setOnPreparedListener(this);
//            //mediaPlayer.prepare();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

//    @Override
//    public void onPrepared(MediaPlayer mp) {
//        mediaPlayer.start();
//    }
}


