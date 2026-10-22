package id.depthlock.hios;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.DisplayMetrics;
import android.view.View;

import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import de.robv.android.xposed.callbacks.XC_MethodHook;

/**
 * Efek depth wallpaper di lock screen HiOS 15.
 * Setelah jam digambar (KeyguardClockFrame.dispatchDraw), potongan karakter
 * digambar lagi di atasnya pada posisi yang sama dengan wallpaper.
 */
public class Hook implements IXposedHookZygoteInit, IXposedHookLoadPackage {

    private static final String TAG = "DepthLock: ";
    private static final String CLOCK_FRAME = "com.android.keyguard.KeyguardClockFrame";

    // Ukuran gambar potongan (sama dengan wallpaper asli)
    private static final float IMG_W = 736f;

    // Kalibrasi hasil pengukuran: wallpaper asli 736x1636 -> lock screen 720x1612
    // (skala 1.2653, geser x -101.2 px, geser y -1.8 px)
    private static final float WIDTH_RATIO = 1.29341f;  // lebar gambar di layar / lebar layar
    private static final float TX_RATIO = -0.140564f;   // geser x / lebar layar
    private static final float TY_RATIO = -0.001135f;   // geser y / tinggi layar

    private static String modulePath;
    private static Bitmap cutout;
    private static boolean loadTried;
    private static boolean loggedFirstDraw;
    private static boolean loggedError;
    private static final Paint PAINT = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);

    @Override
    public void initZygote(StartupParam startupParam) {
        modulePath = startupParam.modulePath;
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!"com.android.systemui".equals(lpparam.packageName)) return;
        try {
            XposedHelpers.findAndHookMethod(CLOCK_FRAME, lpparam.classLoader, "dispatchDraw",
                    Canvas.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            drawCutout((View) param.thisObject, (Canvas) param.args[0]);
                        }
                    });
            XposedBridge.log(TAG + "hook " + CLOCK_FRAME + ".dispatchDraw terpasang");
        } catch (Throwable t) {
            XposedBridge.log(TAG + "gagal memasang hook");
            XposedBridge.log(t);
        }
    }

    private void drawCutout(View v, Canvas canvas) {
        try {
            int w = v.getWidth();
            int h = v.getHeight();
            if (w <= 0 || h <= 0) return;

            DisplayMetrics dm = v.getResources().getDisplayMetrics();
            float sw = dm.widthPixels;
            float sh = dm.heightPixels;
            if (sw > sh) return; // hanya mode potret

            Bitmap bmp = getCutout();
            if (bmp == null) return;

            int alpha = 255;
            try {
                alpha = XposedHelpers.getIntField(v, "drawAlpha");
            } catch (Throwable ignored) {
            }
            if (alpha <= 0) return;

            int[] loc = new int[2];
            v.getLocationOnScreen(loc);

            float scale = WIDTH_RATIO * sw / IMG_W;
            float tx = TX_RATIO * sw;
            float ty = TY_RATIO * sh;

            if (!loggedFirstDraw) {
                loggedFirstDraw = true;
                XposedBridge.log(TAG + "gambar pertama: frame " + w + "x" + h + " di (" + loc[0] + ","
                        + loc[1] + "), layar " + (int) sw + "x" + (int) sh + ", skala " + scale);
            }

            PAINT.setAlpha(alpha);
            canvas.save();
            canvas.clipRect(0, 0, w, h);
            canvas.translate(-loc[0] + tx, -loc[1] + ty);
            canvas.scale(scale, scale);
            canvas.drawBitmap(bmp, 0f, 0f, PAINT);
            canvas.restore();
        } catch (Throwable t) {
            if (!loggedError) {
                loggedError = true;
                XposedBridge.log(TAG + "error saat menggambar");
                XposedBridge.log(t);
            }
        }
    }

    private static synchronized Bitmap getCutout() {
        if (cutout != null || loadTried) return cutout;
        loadTried = true;
        String path = modulePath;
        if (path == null) path = pathFromClassLoader();
        if (path == null) {
            XposedBridge.log(TAG + "path modul tidak ditemukan");
            return null;
        }
        try (ZipFile zf = new ZipFile(path)) {
            ZipEntry e = zf.getEntry("assets/depth_cutout.png");
            if (e == null) {
                XposedBridge.log(TAG + "depth_cutout.png tidak ada di APK modul");
                return null;
            }
            try (InputStream is = zf.getInputStream(e)) {
                cutout = BitmapFactory.decodeStream(is);
            }
            XposedBridge.log(TAG + "potongan dimuat: " + (cutout == null ? "null" : cutout.getWidth() + "x" + cutout.getHeight()));
        } catch (Throwable t) {
            XposedBridge.log(TAG + "gagal memuat potongan");
            XposedBridge.log(t);
        }
        return cutout;
    }

    /** Cadangan bila modulePath dari initZygote kosong: ambil dari toString ClassLoader. */
    private static String pathFromClassLoader() {
        try {
            String s = String.valueOf(Hook.class.getClassLoader());
            int i = s.indexOf("zip file \"");
            if (i < 0) return null;
            int start = i + "zip file \"".length();
            int end = s.indexOf('"', start);
            if (end < 0) return null;
            return s.substring(start, end);
        } catch (Throwable t) {
            return null;
        }
    }
}
