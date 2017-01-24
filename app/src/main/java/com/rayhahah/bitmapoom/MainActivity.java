package com.rayhahah.bitmapoom;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.util.LruCache;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final String PATH_NAME = "a";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    /**
     * 释放Bitmap资源
     */
    public void recycleBitmap() {
        Bitmap bitmap = BitmapFactory.decodeFile(PATH_NAME);
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
            bitmap = null;
        }
        System.gc();
    }

    /**
     * 设置Options来压缩图片，实现内存优化
     *
     * @return
     */
    public Bitmap bitmapOptions() {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        Bitmap bitmap = BitmapFactory.decodeFile(PATH_NAME, opts);
        int realHeight = opts.outHeight;
        int realWidth = opts.outWidth;
        Log.e("ray", "bitmapOptions: realHeight=" + realHeight + ",realWidth=" + realWidth);
        opts.inJustDecodeBounds = false;//这次加载需要加载位图信息
        opts.inPreferredConfig = Bitmap.Config.RGB_565;
        opts.inSampleSize = 4;
        Bitmap realBitmap = BitmapFactory.decodeFile(PATH_NAME, opts);
        return realBitmap;
    }

    public Bitmap streamBitmap(int resId) {
        InputStream is = getResources().openRawResource(resId);
        Bitmap bitmap = BitmapFactory.decodeStream(is);
        return bitmap;
    }


    List<SoftReference<Bitmap>> bitmaps = new ArrayList<>();

    /**
     * 使用软引用持有图片资源来实现内存优化
     *
     * @param position 当前需要图片的位置，模拟List中使用图片的情况
     */
    public Bitmap bitmaoSoft(int position) {
        SoftReference<Bitmap> softBitmap = bitmaps.get(position);
        if (softBitmap.get() != null) {
            return softBitmap.get();
        } else {
            //移除已经释放掉的软引用资源
            bitmaps.remove(softBitmap);
            Bitmap bitmap = BitmapFactory.decodeFile(PATH_NAME);
            //重新构造有值软引用
            softBitmap = new SoftReference<>(bitmap);
            bitmaps.add(position, softBitmap);
            //释放最新加载的图片资源
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
                bitmap = null;
            }
            System.gc();
            return softBitmap.get();
        }
    }

    LruCache<String, Bitmap> lruCache = new LruCache<String, Bitmap>(10 * 1024 * 1024) {
        /**
         * 必须重写
         * 资源在缓存空间中分配的大小
         * @param key 当前放入缓存资源的Key
         * @param value 当前放入缓存的资源
         * @return 缓存资源所需要的缓存空间
         */
        @Override
        protected int sizeOf(String key, Bitmap value) {
            return value.getByteCount();
        }

        /**
         * 不一定重写
         * 当有对象从缓存中被移除时回调
         * @param evicted true=系统自动清理空间，false=清理动作是由外界调put(),remove()方法所触发
         * @param key 新的put进来的Bitmap的Key
         * @param oldValue 被移除的Bitmap
         * @param newValue 新的put进来的Bitmap
         */
        @Override
        protected void entryRemoved(boolean evicted, String key, Bitmap oldValue, Bitmap newValue) {
            super.entryRemoved(evicted, key, oldValue, newValue);
        }
    };

    public Bitmap lruCacheBitmap(Context context, String url) {
        String key = url;
        Bitmap lruBitmap = lruCache.get(key);
        //缓存中有图片就从缓存中拿
        if (lruBitmap != null) {
            return lruBitmap;
        } else {
            //缓存没有，就在文件中拿
            String cachePath = context.getExternalCacheDir().getAbsolutePath();
            Bitmap bitmap = MyFileUtil.readBitmap(context, cachePath, key);
            if (bitmap == null) {
                //文件中没有就在网络中下载，同时在缓存和文件中存一份
                bitmap = new BitmapDownloadAsyncTask(context, url).excute();
                MyFileUtil.writeBitmap(context, cachePath, key, bitmap);
            }
            lruCache.put(key, bitmap);
            return bitmap;
        }
    }
}
