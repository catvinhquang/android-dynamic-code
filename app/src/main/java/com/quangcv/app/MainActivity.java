package com.quangcv.app;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import dalvik.system.DexClassLoader;

public class MainActivity extends Activity {

    private static final String LIB_URL = "https://github.com/catvinhquang/android-dynamic-code/raw/master/app/src/main/assets/lib.zip";
    private static final String LIB_NAME = "lib.zip";
    private static final String CLASS_NAME = "com.quangcv.app.LibraryProvider";
    private static final int BUF_SIZE = 8 * 1024;

    private LibraryInterface lib;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // before the secondary dex file can be processed by the DexClassLoader,
        // it has to be first copied from asset resource to a storage location.
        final File dex = new File(getDir("dex", Context.MODE_PRIVATE), LIB_NAME);
        prepareDexFromAssets(dex);

        // internal storage where the DexClassLoader writes the optimized dex file to.
        final String opt = getDir("opt", Context.MODE_PRIVATE).getAbsolutePath();

        TextView v = new TextView(this);
        v.setText("Touch Me!");
        v.setGravity(Gravity.CENTER);
        v.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (lib == null) {
                    try {
                        DexClassLoader loader = new DexClassLoader(dex.getAbsolutePath(), opt, null, getClassLoader());
                        Class cls = loader.loadClass(CLASS_NAME);

                        // Cast the return object to the library interface so that the
                        // caller can directly invoke methods in the interface.
                        // Alternatively, the caller can invoke methods through reflection,
                        // which is more verbose and slow.
                        lib = (LibraryInterface) cls.newInstance();
                    } catch (Exception e) {
                        e.printStackTrace();
                        return;
                    }
                }

                lib.sayHi(MainActivity.this, "Quang Cat");
            }
        });
        setContentView(v);
    }

    void prepareDexFromAssets(final File output) {
        if (output.exists()) {
            return;
        }

        final ProgressDialog dlg = ProgressDialog.show(this, null, "Loading...", true, false);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    exportDex(output, getAssets().open(LIB_NAME));
                } catch (Exception e) {
                    e.printStackTrace();
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dlg.cancel();
                    }
                });
            }
        }).start();
    }

    void prepareDexFromServer(final File output) {
        if (output.exists()) {
            return;
        }

        final ProgressDialog dlg = ProgressDialog.show(this, null, "Loading...", true, false);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(LIB_URL);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.connect();
                    exportDex(output, connection.getInputStream());
                } catch (Exception e) {
                    e.printStackTrace();
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dlg.cancel();
                    }
                });
            }
        }).start();
    }

    void exportDex(final File output, final InputStream is) {
        BufferedInputStream bis = null;
        OutputStream dexWriter = null;
        try {
            bis = new BufferedInputStream(is);
            dexWriter = new BufferedOutputStream(new FileOutputStream(output));
            byte[] buf = new byte[BUF_SIZE];
            int len;
            while ((len = bis.read(buf, 0, BUF_SIZE)) > 0) {
                dexWriter.write(buf, 0, len);
            }
            dexWriter.close();
            bis.close();
        } catch (IOException e) {
            if (dexWriter != null) {
                try {
                    dexWriter.close();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        }
    }
}