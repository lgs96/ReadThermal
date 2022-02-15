package com.example.readtxt;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Environment;
import android.util.Log;
import android.view.View;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import org.w3c.dom.Text;

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
    TextView txtRead;
    final static String rootPath = "/sys/devices/virtual/thermal";

    private List<String> folders;
    private List<String> types;
    String types_csv = "";

    private String stat_time = "";

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

        txtRead = (TextView) findViewById(R.id.textview_first);
        ListFile(rootPath);
        mOnFileRead();
    }

    public void mOnFileRead() {
        Thread t = new Thread((Runnable) () -> {
            int time_step = 0;
            while(true) {
                try {
                    time_step += 1;
                    String temp_info = Integer.toString(time_step);
                    for (int i = 0; i < folders.size(); i++){
                        String read = ReadTextFile(folders.get(i)+"/temp");
                        temp_info = temp_info + "," + read;
                    }
                    temp_info += "\n";
                    Log.i(TAG, "Result: " + temp_info);
                    writeToFile(temp_info, "zts", stat_time+"_zts.csv");

                    Thread.sleep(1000);
                } catch (Exception e) {
                    Log.i(TAG, "Exception during read ");
                }
            }
        });
        t.start();
    }


    public void ListFile(String folderPath){

        folders = new ArrayList<String>();
        types = new ArrayList<String>();

        File dir = new File(folderPath);
        File files[] = dir.listFiles();

        for (int i = 0; i < files.length; i++) {
            String folderName = files[i].toString();
            if (folderName.contains("thermal_zone")){
                String current_type = ReadTextFile(folderName +"/type");

                folders.add(folderName);
                types.add(current_type);

                Log.i(TAG, " file: " + folderName +" "+current_type);
            }
        }

        for (int i = 0; i < types.size(); i++) {
            types_csv = types_csv +  "," + types.get(i);
        }
        types_csv += "\n";
    }

    public String ReadTextFile (String path){
        StringBuffer strBuffer = new StringBuffer();
        try{
            InputStream is = new FileInputStream(path);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line="";
            while((line=reader.readLine())!=null){
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