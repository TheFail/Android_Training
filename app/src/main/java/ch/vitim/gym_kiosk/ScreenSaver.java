package ch.vitim.gym_kiosk;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.Layout;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

import static android.service.autofill.Validators.and;

/** from MainActivity
final SweetAlertDialog pDialog = new SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#A5DC86"));
        pDialog.setTitleText("QR-code checking.\n Please wait...");
        pDialog.setCancelable(false);
        pDialog.setCanceledOnTouchOutside(false);
        pDialog.show();
        time = findViewById(R.id.viewTime);
        arrows = findViewById(R.id.arrows);
        time.setVisibility(View.INVISIBLE);
        arrows.setVisibility(View.INVISIBLE);

        final Intent UserI = new Intent(this, UserActivity.class);
        String qr=QrReader.qrString;
        ServerReq.sendQr(qr, new RequestInterface() {
            @Override
            public void onSuccess() {
                pDialog.hideConfirmButton();
                pDialog.changeAlertType(SweetAlertDialog.SUCCESS_TYPE);
                pDialog.setTitleText("QR-code check success.");
                new CountDownTimer(1500, 500) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        Log.e("Timer","seconds remaining: " + millisUntilFinished / 500);
                    }
                    public void onFinish() {
                        pDialog.dismiss();
                        time.setVisibility(View.VISIBLE);
                        arrows.setVisibility(View.VISIBLE);
                        startActivity(UserI);
                    }
                }.start();
            }
            @Override
            public void onError() {
                pDialog.changeAlertType(SweetAlertDialog.ERROR_TYPE);
                pDialog.setTitleText("QR-code check error.\n Try again.");
                new CountDownTimer(5000, 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        pDialog.setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                            @Override
                            public void onClick(SweetAlertDialog sweetAlertDialog) {
                                pDialog.dismiss();
                                time.setVisibility(View.VISIBLE);
                                arrows.setVisibility(View.VISIBLE);
                            }
                        });
                        Log.e("Timer","seconds remaining: " + millisUntilFinished / 1000);
                    }
                    public void onFinish() {
                        pDialog.dismiss();
                        time.setVisibility(View.VISIBLE);
                        arrows.setVisibility(View.VISIBLE);
                    }
                }.start();

            }
        });*/




/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class ScreenSaver extends AppCompatActivity implements SurfaceHolder.Callback, MediaPlayer.OnPreparedListener
{
    private MediaPlayer mediaPlayer;
    private SurfaceHolder vidHolder;
    private SurfaceView vidSurface;
    EditText qrReader;
    public Intent main ;


    @RequiresApi(api = Build.VERSION_CODES.O)
    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.my_dream);
        main = new Intent(this, MainActivity.class);
        getWindow().setSoftInputMode(

                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN

        );
        QrReader.qrString=null;
        qrReader = (EditText) findViewById(R.id.qrReader);
        qrReader.setText(null);
        qrReader.requestFocus();
        qrReader.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence cs, int arg1, int arg2, int arg3) {
                QrReader.qrString=String.valueOf(qrReader.getText());
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
            }

            @Override
            public void afterTextChanged(Editable arg0) {

                qrReader.setOnKeyListener(new View.OnKeyListener() {
                    int i=0;
                    @Override
                    public boolean onKey(View v, int keyCode, KeyEvent event) {
                        if (keyCode==66 && i==1){
                            QrReader.qrString=String.valueOf(qrReader.getText());
                            finish();
                            try {
                                if (mediaPlayer.isPlaying()) {
                                    mediaPlayer.stop();
                                }
                            }catch (Exception  e){
                                System.err.println(e);
                            }
                            Toast.makeText(ScreenSaver.this, ("Qr-Code read\n"+QrReader.qrString), Toast.LENGTH_SHORT).show();
                            Log.e("DD", QrReader.qrString);
                            QrReader.writed=true;
                            startActivity(main);
                            return true;
                        }else if (keyCode==66 && i !=1){
                            i+=1;
                            return false;
                        }else {
                            return false;
                        }
                    }
                });
            }

        });
        SurfaceView vi = findViewById(R.id.surfView);
        vi.setOnTouchListener ( new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                finish();
                try {
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.stop();
                    }
                }catch (Exception  e){
                    System.err.println(e);
                }
                QrReader.writed=false;
                startActivity(main);
                return false;
            }
        });
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        View decorView = getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                    hideSystemUI();
                }
            }
        });
        Log.e("QR", String.valueOf(qrReader.getText()));

    }


    @Override
    public void onPrepared(MediaPlayer mp) {
        mediaPlayer.start();
    }

    @SuppressLint("ClickableViewAccessibility")
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        vidSurface = (SurfaceView) findViewById(R.id.surfView);
        vidHolder = vidSurface.getHolder();
        vidHolder.addCallback(this);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        try {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
        }catch (Exception  e){
            System.err.println(e);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        try {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
        }catch (Exception  e){
            System.err.println(e);
        }
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        try {
            mediaPlayer.stop();
        }catch (Exception e){
            System.err.println(e);
        }
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            @SuppressLint("SdCardPath") String FILE_PATH = "/storage/self/primary/.gym_kiosk/screen.m4v";
            File file = new File(FILE_PATH);
            if (file.exists()) {
                mediaPlayer = MediaPlayer.create(this, Uri.fromFile(file));
            } else {
                mediaPlayer = MediaPlayer.create(this,R.raw.screen);
            }
            mediaPlayer.setLooping(true);
            mediaPlayer.setDisplay(vidHolder);
            mediaPlayer.setOnPreparedListener(this);
            mediaPlayer.prepare();
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    private void hideSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                SurfaceView.SYSTEM_UI_FLAG_IMMERSIVE
                        | SurfaceView.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | SurfaceView.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | SurfaceView.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | SurfaceView.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | SurfaceView.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (! hasFocus) {
            hideSystemUI();
            Intent closeDialog = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            sendBroadcast(closeDialog);
        }
    }
}
