package io.github.jark006.weather;

import static io.github.jark006.weather.utils.Utils.saveLog;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

/**
 * 短生命周期前台服务：负责在后台抓取天气数据并刷新小部件。
 * 前台服务不受后台联网限制，避免小部件刷新时出现连接超时。
 */
public class WeatherFetchService extends Service {
    private static final String CHANNEL_ID = "weather_fetch_service";
    private static final int NOTIFICATION_ID = 0x7710;

    @Override
    public void onCreate() {
        super.onCreate();
        Notification notification = buildNotification();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
            else
                startForeground(NOTIFICATION_ID, notification);
        } catch (Exception e) {
            saveLog(this, "startForeground 失败: " + e);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }
        String widgetClassName = intent.getStringExtra(WidgetBase.EXTRA_WIDGET_CLASS);
        String tips = intent.getStringExtra(WidgetBase.EXTRA_TIPS);

        new Thread(() -> {
            try {
                Class<?> cls = Class.forName(widgetClassName);
                if (WidgetBase.class.isAssignableFrom(cls)) {
                    @SuppressWarnings("unchecked")
                    Class<? extends WidgetBase> widgetClass = (Class<? extends WidgetBase>) cls;
                    WidgetBase.doFetch(getApplicationContext(), widgetClass, tips);
                }
            } catch (Exception e) {
                saveLog(getApplicationContext(), "WeatherFetchService 异常: " + e);
            } finally {
                stopSelf();
            }
        }).start();
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private Notification buildNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    getString(R.string.app_name) + " 刷新", NotificationManager.IMPORTANCE_MIN);
            channel.setDescription("后台刷新天气数据");
            channel.setShowBadge(false);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("正在刷新天气")
                .setSmallIcon(R.drawable.ic_sunny)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .build();
    }
}
