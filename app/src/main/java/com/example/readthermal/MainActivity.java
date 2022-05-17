package com.example.readthermal;

import static com.example.readthermal.utils.Constant.COOL_ID;
import static com.example.readthermal.utils.Constant.CPU_ID;
import static com.example.readthermal.utils.Constant.TEMP_ID;
import static com.example.readthermal.utils.Constant.cooling_path;
import static com.example.readthermal.utils.Constant.cpu_path;
import static com.example.readthermal.utils.Constant.temp_path;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.readthermal.utils.Config;
import com.example.readthermal.utils.RTLog;
import com.github.mikephil.charting.charts.LineChart;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "Main";

    private List<String> temp_types;
    private List<String> cooling_types;
    private List<String> cpu_types;

    private ArrayList<String> [] temp_values;
    private ArrayList<String> [] cooling_values;
    private ArrayList<String> [] cpu_values;

    TextView mThermalText;
    TextView mCPUText;
    LineChart mThermalGraph;
    LineChart mCoolingGraph;
    LineChart mCPUGraph;
    Button mStartButton;
    Button mStopButton;

    private List<String> folders;
    private List<String> types;
    String types_csv = "";

    private String stat_time = "";

    HandlerThread mTraceThread = null;
    Handler mTraceHandler = null;
    PlotGraph mPlotGraph_temp;
    PlotGraph mPlotGraph_cool;
    PlotGraph mPlotGraph_cpu;

    private boolean run_trace = false;
    private boolean is_paused = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, " App start");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        stat_time = f.format(new Date());

        requestPermission();

        File dir = new File("/sdcard/zts/");
        if(!dir.exists()){
            try{
                dir.mkdirs();
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }

        mThermalText = (TextView) findViewById(R.id.text_view_thermal);
        mCPUText = (TextView) findViewById(R.id.text_view_cpu);
        mThermalGraph = (LineChart) findViewById(R.id.thermal_graph);
        mCoolingGraph = (LineChart) findViewById(R.id.cooling_graph);
        mCPUGraph = (LineChart) findViewById(R.id.cpu_graph);
        mStartButton = (Button) findViewById(R.id.button_start);
        mStopButton = (Button) findViewById(R.id.button_stop);

        mStartButton.setOnClickListener(onButtonsClick);
        mStopButton.setOnClickListener(onButtonsClick);

        // Get the hardware specific configuration
        Config.getConfiguration();

    }

    View.OnClickListener onButtonsClick = (v)->{
        switch(v.getId()){
            case R.id.button_start:
                // Get the list of files to trace
                InitializeTrace();
                // Start tracing for every second
                RTLog.d(TAG, "Click start button");
                mTraceThread = new HandlerThread("Trace");
                mTraceThread.start();
                mTraceHandler = new Handler(mTraceThread.getLooper());

                mPlotGraph_temp = new PlotGraph(mThermalGraph, TEMP_ID);
                mPlotGraph_cool = new PlotGraph(mCoolingGraph, COOL_ID);
                mPlotGraph_cpu = new PlotGraph(mCPUGraph, CPU_ID);

                mTraceHandler.post(new StartTrace());
                break;
            case R.id.button_stop:
                // Stop tracing
                StopTrace();
                break;
        }
    };

    public void InitializeTrace (){
        // Get types of target traces. It will be used as a legend of a graph.
        temp_types = GetList (temp_path, Config.temp_index, "type");
        cooling_types = GetList (cooling_path, Config.cooling_index, "type");
        cpu_types = new ArrayList<String>();
        cpu_types.add("Little");
        cpu_types.add("Big1");
        cpu_types.add("Big2");

        temp_values = new ArrayList[Config.temp_index.size()];
        cooling_values = new ArrayList[Config.cooling_index.size()];
        cpu_values = new ArrayList[Config.cpu_index.size()];

        // Get 2-d list
        for (int i = 0; i < Config.temp_index.size(); i++){
            temp_values[i] = new ArrayList<String>();
            temp_values[i].add(temp_types.get(i));
        }
        for (int i = 0; i < Config.cooling_index.size(); i++){
            cooling_values[i] = new ArrayList<String>();
            cooling_values[i].add(cooling_types.get(i));
        }
        for (int i = 0; i < Config.cpu_index.size(); i++){
            cpu_values[i] = new ArrayList<String>();
            cpu_values[i].add(cpu_types.get(i));
        }
    }

    public class StartTrace implements Runnable {
        public void run() {
            run_trace = true;
            while(true) {
                RTLog.d(TAG, "Try tracing");
                try {
                    RTLog.d(TAG, "Tracing is working");
                    // Temp
                    List<String> temp_list = GetList(temp_path, Config.temp_index, "temp");
                    String temp_toShow = "Temp: ";
                    for (int i = 0; i < temp_values.length; i++) {
                        temp_values[i].add(temp_list.get(i));
                        temp_toShow += temp_list.get(i) + " ";
                    }
                    // Cooling
                    List<String> cooling_list = GetList(cooling_path, Config.cooling_index, "cur_state");
                    for (int i = 0; i < cooling_values.length; i++) {
                        cooling_values[i].add(cooling_list.get(i));
                    }
                    // CPU
                    List<String> cpu_list = GetList(cpu_path, Config.cpu_index, "scaling_cur_freq");
                    String cpu_toShow = "CPU freq: ";
                    for (int i = 0; i < cpu_values.length; i++) {
                        cpu_values[i].add(cpu_list.get(i));
                        cpu_toShow += cpu_list.get(i) + " ";
                    }
                    mThermalText.setText(temp_toShow);
                    mCPUText.setText(cpu_toShow);

                    RTLog.d(TAG, "================Traced================");
                    if (run_trace == false)
                        break;
                    else {
                        mPlotGraph_temp.run(temp_list, temp_types,TEMP_ID); //index 0 -> temp
                        mPlotGraph_cool.run(cooling_list, cooling_types,COOL_ID);
                        mPlotGraph_cpu.run(cpu_list, cpu_types,CPU_ID);
                        Thread.sleep(1000);
                    }
                }
                catch(Exception e){
                    RTLog.e(TAG, "Error during tracing");
                }
            }
            SaveTrace();
        }
    }

    public void StopTrace(){
        run_trace = false;
    }

    public void SaveTrace() {
        int list_length = temp_values[0].size();
        RTLog.d(TAG, list_length + " "+ temp_values.length + " " +cooling_values.length + " " + cpu_values.length);
        int time_step = 0;
        String temp_info = "";
        for (int i = 0; i < list_length; i++) {
            temp_info = Integer.toString(time_step);
            for (int j = 0; j < temp_values.length; j++) {
                RTLog.d(TAG, temp_values[j].get(i));
                temp_info = temp_info + "," + temp_values[j].get(i);
            }
            for (int j = 0; j < cooling_values.length; j++) {
                RTLog.d(TAG, temp_info + " " + cooling_values.length);
                temp_info = temp_info + "," + cooling_values[j].get(i);
            }
            for (int j = 0; j < cpu_values.length; j++) {
                temp_info = temp_info + "," + cpu_values[j].get(i);
            }
            time_step += 1;
            temp_info += "\n";
            //RTLog.d(TAG, temp_info);
            writeToFile(temp_info, "zts", stat_time+"_zts.csv");
        }
    }

    public List<String> GetList(String folderPath, List<Integer> index, String info){

        List <String> types = new ArrayList<String>();

        for (int i = 0; i < index.size(); i++){
            String temp_path = folderPath + Integer.toString(index.get(i));
            String current_type = ReadFile(temp_path + "/" + info);

            types.add(current_type);
        }

        return types;

        /*
        for (int i = 0; i < types.size(); i++) {
            types_csv = types_csv +  "," + types.get(i);
        }
        types_csv += "\n";
         */
    }

    public String ReadFile (String path){
        StringBuffer strBuffer = new StringBuffer();
        try{
            InputStream is = new FileInputStream(path);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line="";
            while((line=reader.readLine())!=null){
                RTLog.d(TAG, "Path: " + path);
                RTLog.d(TAG, "Line: " + line);
                strBuffer.append(line+"");
            }

            reader.close();
            is.close();
        }catch (IOException e){
            e.printStackTrace();
            return "";
        }
        return strBuffer.toString();
    }

    public void writeToFile(String content, String folder_name, String fileName) {

        FileWriter fileWriter = null;
        String path = Environment.getExternalStorageDirectory().getPath();
        boolean notFound = false;

        File file = new File("/sdcard/zts/" + fileName);

        Log.i("WriteToFile", path + "/" +folder_name +"/" + fileName);
        if (!file.exists()) {
            Log.i("StatusDataCollector", "Not found");
            notFound = true;
        }
        try {
            fileWriter = new FileWriter(file, true);
            if (notFound)
                fileWriter.append(types_csv);
            fileWriter.append(content);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fileWriter.flush();
                fileWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                if(shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                }

                requestPermissions(new String[] {
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.ACCESS_FINE_LOCATION}, 2);

            } else {
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
