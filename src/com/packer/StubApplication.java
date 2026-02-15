package com.packer;

import android.app.Application;
import android.content.Context;
import dalvik.system.InMemoryDexClassLoader;
import java.io.*;
import java.nio.ByteBuffer;

public class StubApplication extends Application {

    private static final byte KEY = 0x42;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        try {
            // Read encrypted payload
            InputStream is = base.getAssets().open("p.dat");
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] temp = new byte[4096];
            int n;
            while ((n = is.read(temp)) != -1) {
                buffer.write(temp, 0, n);
            }
            is.close();
            byte[] encrypted = buffer.toByteArray();

            // Decrypt
            byte[] decrypted = new byte[encrypted.length];
            for (int i = 0; i < encrypted.length; i++) {
                decrypted[i] = (byte)(encrypted[i] ^ KEY);
            }

            // Load
            new InMemoryDexClassLoader(ByteBuffer.wrap(decrypted), getClassLoader());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
