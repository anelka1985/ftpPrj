package cho.nico.com.ftpproject;

import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import java.io.File;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;

import cho.nico.com.ftpproject.R;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


    }


    public void click(View view) {


//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {


                File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "DCIM" + File.separator + "Test"+ File.separator );

//                Log.e("test","====>"+dir.exists());
                File[] files = dir.listFiles();

//                dir.list();
//                Log.e("test","files====>"+(files==null));
//                Log.e("test","files====>"+(files.length));
//                for(File file:files)
//                {
//                    Log.e("test","====>"+file.getAbsolutePath());
//                }

                for (File file : files) {
                    try {
                        FtpManager.getInstance().addTask(file.getAbsolutePath());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            }
        }).start();

    }



    public void click1(View view) {


//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {


                File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "DCIM" + File.separator + "Test1"+ File.separator );

//                Log.e("test","====>"+dir.exists());
                File[] files = dir.listFiles();
//
//                dir.list();
//                Log.e("test","files====>"+(files==null));
//                Log.e("test","files====>"+(files.length));
//                for(File file:files)
//                {
//                    Log.e("test","====>"+file.getAbsolutePath());
//                }

                for (File file : files) {
                    try {
                        FtpManager.getInstance().addTask(file.getAbsolutePath());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            }
        }).start();

    }

}
