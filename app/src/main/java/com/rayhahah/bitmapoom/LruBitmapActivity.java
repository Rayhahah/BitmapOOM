package com.rayhahah.bitmapoom;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.util.LruCache;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class LruBitmapActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lru_bitmap);
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