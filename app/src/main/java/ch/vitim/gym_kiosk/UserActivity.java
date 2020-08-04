package ch.vitim.gym_kiosk;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.viewpager2.widget.ViewPager2;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Entity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Property;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.material.tabs.TabItem;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.os.Handler;

import com.google.android.material.tabs.TabLayout;

import static ch.vitim.gym_kiosk.ServerReq.training_apparatus;

public class UserActivity extends AppCompatActivity {

    Button Logout;
    Intent Main;
    JSONObject JSONob;
    TextView UserName;
    TextView UserTrainer;
    TextView UserGroup;
    String UserPicture;
    TextView taskDescr;
    TextView PrevTrainings;
    TextView DescrTreinings;
    LinearLayout TasksContainer;
    LinearLayout UserContainer;
    ConstraintLayout Parent;
    //    LinearLayout PrevLinear;
    ListView PrevLinear;
    ArrayAdapter<JSONProperties> adapter;
    public ArrayList<JSONProperties> resultProperties = new ArrayList<>();
    TabLayout tabLayout;
    ViewPager2 viewPager;
    TabLayout.Tab tab1;
    TabItem tab2;


    TextView timer;
    Button start, pause, reset;
    long MillisecondTime, StartTime, TimeBuff, UpdateTime = 0L;
    Handler handler;
    int Seconds, Minutes, MilliSeconds;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        float scale = this.getResources().getDisplayMetrics().density;
        setContentView(R.layout.activity_user);
        tabLayout = findViewById(R.id.tabs);
        viewPager = findViewById(R.id.mainTabs);

        viewPager.setAdapter(createCardAdapter());
        new TabLayoutMediator(tabLayout, viewPager,
                new TabLayoutMediator.TabConfigurationStrategy() {
                    @Override public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {
                        tab.setText("Tab " + (position + 1));
                    }
                }).attach();

        Main = new Intent(this, MainActivity.class);
        getWindow().setSoftInputMode(

                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN

        );
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


        /*{

            timer = (TextView) findViewById(R.id.timer);
            start = (Button) findViewById(R.id.training_starter);
            pause = (Button) findViewById(R.id.training_pause);
            reset = (Button) findViewById(R.id.training_stop);

            handler = new Handler();

            start.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    StartTime = SystemClock.uptimeMillis();
                    handler.postDelayed(runnable, 0);
                    Log.e("clock", String.valueOf(StartTime));
                    reset.setEnabled(false);
                    start.setEnabled(false);
                }
            });

            pause.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    TimeBuff += MillisecondTime;

                    handler.removeCallbacks(runnable);

                    reset.setEnabled(true);
                    start.setEnabled(true);
                }
            });

            reset.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    MillisecondTime = 0L;
                    StartTime = 0L;
                    TimeBuff = 0L;
                    UpdateTime = 0L;
                    Seconds = 0;
                    Minutes = 0;
                    MilliSeconds = 0;

                    start.setEnabled(true);
                    reset.setEnabled(false);
                    pause.setEnabled(false);
                }
            });
        }*/
        /*PrevLinear = findViewById(R.id.prev_linear);
        PrevTrainings =findViewById(R.id.previous_trainings);
        PrevExpand = findViewById(R.id.prev_expand);
        DescrTreinings = findViewById(R.id.task_description_button);
        DescrExpand = findViewById(R.id.descr_expand);
        PrevTrainings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PrevExpand.toggle();
                DescrExpand.toggle();
            }
        });
        DescrTreinings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PrevExpand.toggle();
                DescrExpand.toggle();
            }
        });*/
        // TODO now training task

        Logout = findViewById(R.id.Logout);
        Logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                startActivity(Main);
                finish();
            }
        });
        JSONob = createJSONFromFile(R.raw.backend_receve);
    }
// Json
    private ViewPagerAdapter createCardAdapter() {
        ViewPagerAdapter adapter = new ViewPagerAdapter(this);
        return adapter;
    }


    private JSONObject createJSONFromFile(int fileID) {

        JSONObject result = null;

        try {
            // Read file into string builder
            InputStream inputStream = this.getResources().openRawResource(fileID);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder builder = new StringBuilder();

            for (String line = null; (line = reader.readLine()) != null; ) {
                builder.append(line).append("\n");
            }

            // Parse into JSONObject
            String resultStr = builder.toString();
            JSONTokener tokener = new JSONTokener(resultStr);
            result = new JSONObject(tokener);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return result;
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
        }
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
        if (!hasFocus) {
            hideSystemUI();
            Intent closeDialog = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            sendBroadcast(closeDialog);
        }
        /*{
            Parent = findViewById(R.id.Parent);
            TasksContainer = findViewById(R.id.user_tasks_vert);
            UserContainer = findViewById(R.id.user_data);
            PrevTrainings =findViewById(R.id.previous_trainings);
            PrevExpand = findViewById(R.id.prev_expand);
            DescrTreinings = findViewById(R.id.task_description_button);
            DescrExpand = findViewById(R.id.descr_expand);
            DisplayMetrics displayMetrics = this.getResources().getDisplayMetrics();
            float dpHeight = displayMetrics.heightPixels / displayMetrics.density;
            float scale = this.getResources().getDisplayMetrics().density;
            float contSIze = dpHeight;
            int userDataSize = UserContainer.getHeight();
            ViewGroup.LayoutParams params = (ViewGroup.LayoutParams) DescrExpand.getLayoutParams();
            params.height=(int) ((contSIze-userDataSize+(16*scale))* scale + 0.5f);
            DescrExpand.setLayoutParams(params);
            JSONLoad();
        }*/
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        JSONLoad();
    }

    protected void JSONLoad() {
        try {
            float scale = this.getResources().getDisplayMetrics().density;
            UserName = findViewById(R.id.userName);
            UserName.setText(JSONob.getString("name"));
            UserTrainer = findViewById(R.id.userTrainer);
            UserTrainer.setText(JSONob.getString("trainer"));
            UserGroup = findViewById(R.id.userGroup);
            UserGroup.setText(JSONob.getString("group"));
            UserPicture = JSONob.getString("picture");
            try {
                new DownloadImageTask((ImageView) findViewById(R.id.userPicture)).execute(UserPicture);
            } catch (Exception e) {
                e.printStackTrace();
            }
            taskDescr = findViewById(R.id.task_descr);
            JSONObject type = JSONob.getJSONObject("type");
            JSONObject task = type.getJSONObject(training_apparatus);
            JSONArray date = task.getJSONArray("date");
            int datesCount = date.length();
            int counterReader = 0;
            int curDateNumb = datesCount;
            ArrayList<String> DatesList = new ArrayList<>();
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
            String curDate = sdf.format(new Date());
            String Descr;
            Descr = "Your today training task is: Running on " + training_apparatus + " 2.0" + "km" + "\n" + "with speed more than 7km/h";
            taskDescr.setText(Descr);
            while (counterReader < datesCount) {
                DatesList.add(date.getJSONObject(counterReader).getString("date"));


                counterReader++;
                /*if (counterReader<curDateNumb){
//                    *
//                     * previous loading method
//                     * Creating prev_line TableRow dynamically

                    {
                        TableRow table = new TableRow(this);
                        PrevLinear.addView(table);
                        table.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                        table.setBackground(getDrawable(R.drawable.border_background_buttons));
                        table.setGravity(Gravity.CENTER_VERTICAL);
                        table.setPadding(8,8,8,8);
                        {
                            LinearLayout prev_text = new LinearLayout(this);
                            table.addView(prev_text);
                            LinearLayout.LayoutParams params_prev_text = (LinearLayout.LayoutParams) prev_text.getLayoutParams();
                            params_prev_text.width = (int) (250*scale+0.5f);
                            params_prev_text.height= ViewGroup.LayoutParams.WRAP_CONTENT;
                            prev_text.setLayoutParams(params_prev_text);
                            //                        prev_text.setLayoutParams(new LinearLayout.LayoutParams((int) (250*scale+0.5f),LinearLayout.LayoutParams.WRAP_CONTENT));
                            prev_text.setOrientation(LinearLayout.VERTICAL);


                        }

                        {
                            TextView prev_text= new TextView(this);
                            table.addView(prev_text);
                            ViewGroup.LayoutParams params_text = (ViewGroup.LayoutParams) prev_text.getLayoutParams();
                            params_text.height=ViewGroup.LayoutParams.WRAP_CONTENT;
                            params_text.width=(int) (250*scale+0.5f);
                            prev_text.setLayoutParams(params_text);
    //                        prev_text.setLayoutParams(new ViewGroup.LayoutParams(((int) (250*scale+0.5f)), ViewGroup.LayoutParams.WRAP_CONTENT));
                            prev_text.setTextSize(24);
                            prev_text.setHint("ok");
                            String row = "Date: " +date.getJSONObject(counterReader).getString("date")+";"+"\n"+"Result: "+date.getJSONObject(counterReader).getString("result")+
                                    ";";
                            prev_text.setText(row);
                        }
                        if(date.getJSONObject(counterReader).getString("result").equals("complete"))
                        {
                            LineChart prev_line = new LineChart(this);
                            table.addView(prev_line);
                            ViewGroup.LayoutParams params_line = (ViewGroup.LayoutParams) prev_line.getLayoutParams();
                            //                        ViewGroup.LayoutParams params_line = new ViewGroup.LayoutParams(230, 120);
                            params_line.width=(int) (450*scale+0.5f);
                            params_line.height=(int) (120*scale+0.5f);
                            prev_line.setLayoutParams(params_line);
                            //                        prev_line.setLayoutParams(new ViewGroup.LayoutParams(450,120));
                            JSONObject date_graph = date.getJSONObject(counterReader).getJSONObject("graph");
                            int date_graph_length = date_graph.getJSONArray("x").length();
                            int counter_graph =0;
                            JSONArray dataObjectsX = date_graph.getJSONArray("x");
                            JSONArray dataObjectsY = date_graph.getJSONArray("y");
                            //                        Log.e("Data","wwwww");
                            List<Entry> entries = new ArrayList<Entry>();
                            while (counter_graph<date_graph_length){
                                entries.add(new Entry(dataObjectsX.getInt(counter_graph), dataObjectsY.getInt(counter_graph)));
                                //                            Log.e("Data","x="+dataObjectsX.getInt(counter_graph)+"; y="+dataObjectsY.getInt(counter_graph));
                                counter_graph++;
                            }
                            LineDataSet dataSet = new LineDataSet(entries, "Speed");
                            LineData lineData = new LineData(dataSet);
                            prev_line.setData(lineData);
                            prev_line.invalidate();
                        }else {
                            TextView prev_line_text =  new TextView(this);
                            table.addView(prev_line_text);
                            ViewGroup.LayoutParams params_line_text = (ViewGroup.LayoutParams) prev_line_text.getLayoutParams();
                            params_line_text.width=(int) (450*scale+0.5f);
                            params_line_text.height= ViewGroup.LayoutParams.WRAP_CONTENT;
                            prev_line_text.setLayoutParams(params_line_text);
                            prev_line_text.setTextSize(32);
                            prev_line_text.setText("No Data!!!");
                        }
                    }
                    {
                        JSONObject date_graph = date.getJSONObject(counterReader).getJSONObject("graph");
                        int date_graph_length = date_graph.getJSONArray("x").length();
                        int counter_graph =0;
                        JSONArray dataObjectsX = date_graph.getJSONArray("x");
                        JSONArray dataObjectsY = date_graph.getJSONArray("y");
                        //                        Log.e("Data","wwwww");
                        List<Entry> entries = new ArrayList<Entry>();
                        while (counter_graph<date_graph_length){
                            entries.add(new Entry(Float.valueOf(dataObjectsX.getString(counter_graph)), Float.valueOf(dataObjectsY.getString(counter_graph))));
                            //                            Log.e("Data","x="+dataObjectsX.getInt(counter_graph)+"; y="+dataObjectsY.getInt(counter_graph));
                            counter_graph++;
                        }
                        LineDataSet dataSet = new LineDataSet(entries, "Speed");
                        LineData lineData = new LineData(dataSet);
                        resultProperties.add(new JSONProperties(date.getJSONObject(counterReader).getString("date"),date.getJSONObject(counterReader).getString("result"),lineData));
                    }
                }*/
            }
            /*ListView listView = (ListView) findViewById(R.id.prev_linear);
            adapter = new propertyArrayAdapter(this, 0, resultProperties);
            listView.setAdapter(adapter);*/

        } catch (final JSONException e) {
            Log.e("TAG", "Json parsing error: " + e.getMessage());
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(),
                            "Json parsing error: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    public Runnable runnable = new Runnable() {

        public void run() {

            MillisecondTime = SystemClock.uptimeMillis() - StartTime;

            UpdateTime = TimeBuff + MillisecondTime;

            Seconds = (int) (UpdateTime / 1000);

            Minutes = Seconds / 60;

            Seconds = Seconds % 60;

            MilliSeconds = (int) (UpdateTime % 1000);

            timer.setText(String.format("%02d:%02d:%03d", Minutes, Seconds, MilliSeconds));

//            timer.setText("" + Minutes + ":"
//                    + String.format("%02d", Seconds) + ":"
//                    + String.format("%03d", MilliSeconds));

            handler.postDelayed(runnable, 0);
        }

    };

}

/*class propertyArrayAdapter extends ArrayAdapter<JSONProperties> {
    private Context context;
    private List<JSONProperties> resultProperties;

    //constructor, call on creation
    public propertyArrayAdapter(Context context, int resource, ArrayList<JSONProperties> objects) {
        super(context, resource, objects);

        this.context = context;
        this.resultProperties = objects;
    }

    //called when rendering the list
    public View getView(int position, View convertView, ViewGroup parent) {

        //get the property we are displaying
        JSONProperties property = resultProperties.get(position);

        //get the inflater and inflate the XML layout for each item
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.user_data_prev_results, null);

        TextView dateView = (TextView) view.findViewById(R.id.prev_data_date);
        TextView resultView = (TextView) view.findViewById(R.id.prev_data_result);
        LineChart prevLine = (LineChart) view.findViewById(R.id.prev_linear);

        //set price and rental attributes
        //price.setText("$" + String.valueOf(property.getPrice()));

        dateView.setText(property.getDate());
        resultView.setText(property.getResult());

        if(property.getResult().equals("complete"))
        {
            prevLine.setData(property.getLineData());
            prevLine.setTouchEnabled(false);
            prevLine.setDragEnabled(false);
            prevLine.setScaleEnabled(false);
            prevLine.setPinchZoom(false);
            prevLine.setDoubleTapToZoomEnabled(false);
            prevLine.setDragDecelerationEnabled(false);
            prevLine.setDescription(null);
            YAxis left = prevLine.getAxisLeft();
            left.setDrawLabels(false); // no axis labels
            left.setDrawAxisLine(false); // no axis line
            left.setDrawGridLines(false);
            prevLine.getAxisRight().setEnabled(false);
            prevLine.getXAxis().setEnabled(false);

            prevLine.invalidate();
        }else {
            prevLine.setNoDataText("No data from training");
            prevLine.setNoDataTextColor(R.color.red_btn_bg_color);
            prevLine.invalidate();
        }
        return view;
    }
}*/
