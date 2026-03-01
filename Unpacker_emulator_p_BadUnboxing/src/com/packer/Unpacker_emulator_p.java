package com.packer;

// import android.app.Application;
// import android.content.Context;
// import android.util.Log;
// import dalvik.system.InMemoryDexClassLoader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;

class Application {
    // Dummy Application class implementation
}


class Context {
    // Dummy Context class implementation
}


/* loaded from: classes.dex */
public class Unpacker_emulator_p  {

    /* renamed from: KEY */
    private static final byte field_KEY_qHhgHfke = 66;

    /* renamed from: TAG */
    private static final String field_TAG_mTrR9KqU = "StubApp";

    /* renamed from: decryptedLoader */
    static volatile ClassLoader field_decryptedLoader_NzNDc4gu = null;

    /* renamed from: realApp */
    private static Application field_realApp_FozcSMfb = null;

    
    public static void main(String[] args) {
        //super.attachBaseContext(context); // BadUnboxing: Remove superclass reference
        try {
            ClassLoader var_method_ensureInstalled_rU76Od3I_JyLuGJvP = method_ensureInstalled_rU76Od3I(new Context());
            InputStream var_open_6SNvjyV5 = new Context().getAssets().open("app.txt");
            byte[] var_bArr_PQbrEc8l = new byte[var_open_6SNvjyV5.available()];
            var_open_6SNvjyV5.read(var_bArr_PQbrEc8l);
            var_open_6SNvjyV5.close();
            String var_trim_SOB0ilHq = new String(var_bArr_PQbrEc8l).trim();
            if (!"android.app.Application".equals(var_trim_SOB0ilHq) && !var_trim_SOB0ilHq.isEmpty()) {
//                 field_realApp_FozcSMfb = (Application) var_method_ensureInstalled_rU76Od3I_JyLuGJvP.loadClass(var_trim_SOB0ilHq).newInstance(); // BadUnboxing: Line contains reflection and was commented out
                Method var_declaredMethod_peo0qaYR = Application.class.getDeclaredMethod("attach", Context.class);
//                 var_declaredMethod_peo0qaYR.setAccessible(true); // BadUnboxing: Line contains reflection and was commented out
//                 var_declaredMethod_peo0qaYR.invoke(field_realApp_FozcSMfb, new Context()); // BadUnboxing: Line contains reflection and was commented out
//                 Field var_declaredField_qRx9DRMY = new Context().getClass().getDeclaredField("mPackageInfo"); // BadUnboxing: Line contains reflection and was commented out
//                 var_declaredField_qRx9DRMY.setAccessible(true); // BadUnboxing: Line contains reflection and was commented out
//                 Object var_obj_Lhz3zPuw = var_declaredField_qRx9DRMY.get(new Context()); // BadUnboxing: Line contains reflection and was commented out
//                 Field var_declaredField2_Q9qAMOEm = var_obj_Lhz3zPuw.getClass().getDeclaredField("mApplication"); // BadUnboxing: Line contains reflection and was commented out
//                 var_declaredField2_Q9qAMOEm.setAccessible(true); // BadUnboxing: Line contains reflection and was commented out
//                 var_declaredField2_Q9qAMOEm.set(var_obj_Lhz3zPuw, field_realApp_FozcSMfb); // BadUnboxing: Line contains reflection and was commented out
            }
        } catch (Exception e) {
            Log.e(field_TAG_mTrR9KqU, "attachBaseContext() failed", e);
            throw new RuntimeException("Unpacker_emulator_p failed: " + e.getMessage(), e);
        }
    }

    
    public static ClassLoader method_ensureInstalled_rU76Od3I(Context arg_context_NVansr31) throws Exception {
        if (var_field_decryptedLoader_NzNDc4gu_Qfxr3jcY != null) {
            return var_field_decryptedLoader_NzNDc4gu_Qfxr3jcY;
        }
        ByteArrayOutputStream var_byteArrayOutputStream_VqEb5deb = new ByteArrayOutputStream();
        InputStream var_open_HGr2bnTS = arg_context_NVansr31.getAssets().open("p.dat");
        try {
            byte[] var_bArr_AUftiMnu = new byte[4096];
            while (true) {
                int var_read_SUYxpAUm = var_open_HGr2bnTS.read(var_bArr_AUftiMnu);
                if (var_read_SUYxpAUm == -1) {
                    break;
                }
                var_byteArrayOutputStream_VqEb5deb.write(var_bArr_AUftiMnu, 0, var_read_SUYxpAUm);
            }
            if (var_open_HGr2bnTS != null) {
                var_open_HGr2bnTS.close();
            }
            byte[] var_byteArray_YvFSbuyY = var_byteArrayOutputStream_VqEb5deb.toByteArray();
            int var_length_zc92IXCf = var_byteArray_YvFSbuyY.length;
            byte[] var_bArr2_k4bVXdXO = new byte[var_length_zc92IXCf];
            for (int var_i_pKy9v00L = 0; var_i_pKy9v00L < var_byteArray_YvFSbuyY.length; var_i_pKy9v00L++) {
                var_bArr2_k4bVXdXO[var_i_pKy9v00L] = (byte) (var_byteArray_YvFSbuyY[var_i_pKy9v00L] ^ field_KEY_qHhgHfke);
            }
            if (var_length_zc92IXCf < 4 || var_bArr2_k4bVXdXO[0] != 100 || var_bArr2_k4bVXdXO[1] != 101 || var_bArr2_k4bVXdXO[2] != 120) {
                throw new RuntimeException("DEX magic check failed");
            }
            InMemoryDexClassLoader var_inMemoryDexClassLoader_JMXWcupq = new InMemoryDexClassLoader(ByteBuffer.wrap(var_bArr2_k4bVXdXO), arg_context_NVansr31.getClassLoader());
//             Field var_declaredField_bpauB3o6 = arg_context_NVansr31.getClass().getDeclaredField("mPackageInfo"); // BadUnboxing: Line contains reflection and was commented out
//             var_declaredField_bpauB3o6.setAccessible(true); // BadUnboxing: Line contains reflection and was commented out
//             Object var_obj_XOy3pVnA = var_declaredField_bpauB3o6.get(arg_context_NVansr31); // BadUnboxing: Line contains reflection and was commented out
//             Field var_declaredField2_ejzGUHp6 = var_obj_XOy3pVnA.getClass().getDeclaredField("mClassLoader"); // BadUnboxing: Line contains reflection and was commented out
//             var_declaredField2_ejzGUHp6.setAccessible(true); // BadUnboxing: Line contains reflection and was commented out
//             var_declaredField2_ejzGUHp6.set(var_obj_XOy3pVnA, var_inMemoryDexClassLoader_JMXWcupq); // BadUnboxing: Line contains reflection and was commented out
            var_field_decryptedLoader_NzNDc4gu_Qfxr3jcY = var_inMemoryDexClassLoader_JMXWcupq;
            return var_inMemoryDexClassLoader_JMXWcupq;
        } catch (Throwable var_th_h8i58PyY) {
            if (var_open_HGr2bnTS != null) {
                try {
                    var_open_HGr2bnTS.close();
                } catch (Throwable th2) {
//                     Throwable.class.getDeclaredMethod("addSuppressed", Throwable.class).invoke(var_th_h8i58PyY, th2); // BadUnboxing: Line contains reflection and was commented out
                }
            }
            throw var_th_h8i58PyY;
        }
    }

}