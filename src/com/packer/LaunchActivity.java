package com.packer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import java.io.InputStream;

// Launcher activity that hands off to the real app
public class LaunchActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            
            // Decrypts the dex file and loads it into the class loader
            StubApplication.loadDex(getBaseContext());

            String entry = readAssetText("entry.txt").trim();
            if (entry.isEmpty()) {
                throw new RuntimeException("entry.txt is empty");
            }

            // Normalize .MainActivity into a full class name 
            String fqcn = entry;
            if (fqcn.startsWith(".")) {
                fqcn = getPackageName() + fqcn;
            } else if (fqcn.indexOf('.') == -1) {
                fqcn = getPackageName() + "." + fqcn;
            }

            if ("com.packer.LaunchActivity".equals(fqcn)) {
                throw new RuntimeException("entry.txt points to LaunchActivity");
            }

            Intent i = new Intent();
            i.setClassName(this, fqcn);

            // start the real app activity now that it is setup
            startActivity(i);
            finish();
        } catch (Exception e) {
            throw new RuntimeException("LaunchActivity failed", e);
        }
    }

    private String readAssetText(String name) throws Exception {
        try (InputStream is = getAssets().open(name)) {
            byte[] buf = new byte[is.available()];
            int n = is.read(buf);
            if (n <= 0) return "";
            return new String(buf, 0, n);
        }
    }
}

