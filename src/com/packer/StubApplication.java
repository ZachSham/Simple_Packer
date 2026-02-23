package com.packer;

import android.app.Application;
import android.content.Context;
import android.util.Log;
import dalvik.system.InMemoryDexClassLoader;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;

public class StubApplication extends Application {

    private static final byte KEY = 0x42;
    private static final String TAG = "StubApp";
    static volatile ClassLoader decryptedLoader = null;
    private Application realApp = null;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        try {
            ClassLoader newLoader = loadDex(base);

            // Read real app class name saved by packer (may be default android.app.Application)
            InputStream nameStream = base.getAssets().open("app.txt");
            byte[] nameBytes = new byte[nameStream.available()];
            nameStream.read(nameBytes);
            nameStream.close();
            String realAppClassName = new String(nameBytes).trim();
            // if the real app class name is not the default android.app.Application, load the class and instantiate it
            if (!"android.app.Application".equals(realAppClassName) && !realAppClassName.isEmpty()) {
                Class<?> realAppClass = newLoader.loadClass(realAppClassName);
                realApp = (Application) realAppClass.newInstance();

                Method attach = Application.class.getDeclaredMethod("attach", Context.class);
                attach.setAccessible(true);
                attach.invoke(realApp, base);

                Field packageInfoField = base.getClass().getDeclaredField("mPackageInfo");
                packageInfoField.setAccessible(true);
                Object loadedApk = packageInfoField.get(base);
                Field appField = loadedApk.getClass().getDeclaredField("mApplication");
                appField.setAccessible(true);
                appField.set(loadedApk, realApp);
            }

        } catch (Exception e) {
            Log.e(TAG, "attachBaseContext() failed", e);
            throw new RuntimeException("StubApplication failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (realApp != null) realApp.onCreate();
    }

    static ClassLoader loadDex(Context base) throws Exception {
        if (decryptedLoader != null) return decryptedLoader;

        // Read encrypted DEX from assets 
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (InputStream is = base.getAssets().open("p.dat")) {
            byte[] temp = new byte[4096];
            int n;
            while ((n = is.read(temp)) != -1) buffer.write(temp, 0, n);
        }
        byte[] encrypted = buffer.toByteArray();
        byte[] decrypted = decrypt(encrypted);

        if (decrypted.length < 4 || decrypted[0] != 'd' || decrypted[1] != 'e' || decrypted[2] != 'x') {
            throw new RuntimeException("DEX magic check failed");
        }

        ClassLoader newLoader = new InMemoryDexClassLoader(
            ByteBuffer.wrap(decrypted),
            base.getClassLoader()
        );

        Field packageInfoField = base.getClass().getDeclaredField("mPackageInfo");
        packageInfoField.setAccessible(true);
        Object loadedApk = packageInfoField.get(base);
        Field classLoaderField = loadedApk.getClass().getDeclaredField("mClassLoader");
        classLoaderField.setAccessible(true);
        // swap the original class loader with the new one to load the real apps classes
        classLoaderField.set(loadedApk, newLoader);

        decryptedLoader = newLoader;

        return newLoader;
    }

    private static byte[] decrypt(byte[] encrypted) {
        byte[] decrypted = new byte[encrypted.length];
        for (int i = 0; i < encrypted.length; i++) {
            decrypted[i] = (byte) (encrypted[i] ^ KEY);
        }
        return decrypted;
    }
}