package io.github.jark006.weather;

import static android.content.Context.NOTIFICATION_SERVICE;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.CRC32;

import io.github.jark006.weather.qweather.rain.Rain;
import io.github.jark006.weather.qweather.realtime.RealTime;
import io.github.jark006.weather.qweather.threeDay.ThreeDay;
import io.github.jark006.weather.qweather.warning.Warning;
import io.github.jark006.weather.utils.LocationStruct;
import io.github.jark006.weather.utils.NetworkUtils;
import io.github.jark006.weather.utils.Utils;

/**
 * 天气小部件
 */
public abstract class WidgetBase extends AppWidgetProvider {
    final static String TAG = "JARK_Widget";
    final static String REQUEST_MANUAL = "jark_weather_REQUEST_MANUAL";
    final static String[] APIKEY_LIST = {
            "71a91885a2524ca8801c67bc9b3d354c",
            "e35f96422e814236a133a38fc8f25d7c"
    }; // qweather.com 和风天气 APIKEY

    final Map<String, Integer> warnColorMap = Map.of(
            "White", 0,
            "Blue", 1,
            "Green", 1,
            "Yellow", 2,
            "Orange", 3,
            "Red", 4,
            "Black", 4
    );

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        Log.d(TAG, "onEnabled: 创建小部件");
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // widget1_info.xml     android:updatePeriodMillis 定时1小时
        getWeatherData(context, "定时刷新...");
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        super.onReceive(context, intent);
        if (REQUEST_MANUAL.equals(intent.getAction())) {
            getWeatherData(context, "手动刷新...");
        }
    }


    /**
     * 获取天气数据
     */
    @SuppressLint("DefaultLocale")
    private void getWeatherData(Context context, @NonNull String tips) {

        showTips(context, tips);

        new Thread(() -> {
            LocationStruct locationStruct = (LocationStruct) Utils.readObj(context, "locationStruct");
            if (locationStruct == null) {
                locationStruct = new LocationStruct();
            }

            String districtName = locationStruct.districtName;
            if (districtName.isEmpty())
                districtName = locationStruct.cityName;

            if (Math.abs(locationStruct.latitude) > 88.0) {  // 靠近南北极就是位置异常
                locationStruct.longitude = Utils.defLongitude;
                locationStruct.latitude = Utils.defLatitude;
            }

            long nowTime = System.currentTimeMillis() / 1000;

            //一个KEY免费请求量太少， 多备几个随机选
            var APIKEY = APIKEY_LIST[(int) (nowTime % APIKEY_LIST.length)];

            try {

                String link = String.format("https://devapi.qweather.com/v7/grid-weather/now?location=%f,%f&key=%s",
                        locationStruct.longitude, locationStruct.latitude, APIKEY);
                String realTimeData = NetworkUtils.getData(link);
                if (realTimeData == null) {
                    Thread.sleep(2000);
                    realTimeData = NetworkUtils.getData(link); // 重试
                }

                RealTime realTime = new Gson().fromJson(realTimeData, RealTime.class);
                if (realTime != null && realTime.code != null && !realTime.code.equals("200")) {
                    if (realTime.code.equals("402"))
                        throw new Exception("今日天气数据额度已用完，明日恢复");
                    else throw new Exception("异常返回码 " + realTime.code);
                }


                link = String.format("https://devapi.qweather.com/v7/minutely/5m?location=%f,%f&key=%s",
                        locationStruct.longitude, locationStruct.latitude, APIKEY);
                String rainData = NetworkUtils.getData(link);
                if (rainData == null) {
                    Thread.sleep(2000);
                    rainData = NetworkUtils.getData(link); // 重试
                }
                Rain rain = new Gson().fromJson(rainData, Rain.class);
                if (rain != null && rain.code != null && !rain.code.equals("200")) {
                    if (rain.code.equals("402"))
                        throw new Exception("今日天气数据额度已用完，明日恢复");
                    else throw new Exception("异常返回码 " + rain.code);
                }


                SharedPreferences sf3day = context.getSharedPreferences("threeDayCache", Context.MODE_PRIVATE);
                long cacheTime = sf3day.getLong("timestamp", 0);

                String threeDayData;
                if (nowTime < (cacheTime + 3600 * 6)) {
                    threeDayData = sf3day.getString("data", "");
                } else {  // 缓存数据 过时6小时
                    link = String.format("https://devapi.qweather.com/v7/grid-weather/3d?location=%f,%f&key=%s",
                            locationStruct.longitude, locationStruct.latitude, APIKEY);
                    threeDayData = NetworkUtils.getData(link);
                    if (threeDayData == null) {
                        Thread.sleep(2000);
                        threeDayData = NetworkUtils.getData(link); // 重试
                    }

                    if (threeDayData != null && threeDayData.length() > 10) {
                        var ed = sf3day.edit();
                        ed.clear();
                        ed.putLong("timestamp", nowTime);
                        ed.putString("data", threeDayData);
                        ed.apply();
                    }
                }
                ThreeDay threeDay = new Gson().fromJson(threeDayData, ThreeDay.class);


                updateAppWidget(context, realTime, threeDay, rain, districtName);


                link = String.format("https://devapi.qweather.com/v7/warning/now?location=%f,%f&key=%s",
                        locationStruct.longitude, locationStruct.latitude, APIKEY);
                String warningData = NetworkUtils.getData(link);
                if (warningData == null) {
                    Thread.sleep(2000);
                    warningData = NetworkUtils.getData(link); // 重试
                }

                Warning warning = new Gson().fromJson(warningData, Warning.class);
                if (warning != null && Objects.equals(warning.code, "200") &&
                        warning.warning != null && !warning.warning.isEmpty()) {
                    notify(context, warning.warning);
                }
            } catch (Exception e) {
                showTips(context, "发生异常 " + e);
            }
        }).start();
    }

    @SuppressWarnings("unchecked")
    public void notify(Context context, @NonNull List<Warning.WarningItem> warnInfo) {
        final String setFileName = "hasNotifyHeFeng.set";
        HashSet<String> hasNotify = null;
        try {
            FileInputStream fis = context.openFileInput(setFileName);
            ObjectInputStream ois = new ObjectInputStream(fis);
            hasNotify = (HashSet<String>) ois.readObject();
            ois.close();
            fis.close();
        } catch (Exception e) {
            Utils.saveLog(context, "读取 " + setFileName + " 失败\n" + e);
        }

        if (hasNotify == null || hasNotify.size() > 100)
            hasNotify = new HashSet<>();

        boolean addItem = false;
        for (Warning.WarningItem info : warnInfo) {
            if (info.status.equals("Cancel"))
                continue;
            if (hasNotify.contains(info.id))
                continue;

            hasNotify.add(info.id);
            addItem = true;

            Integer warnLevel; // 0(白色预警) ~ 4(红色预警)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                warnLevel = warnColorMap.getOrDefault(info.severityColor, 0);
            }else {
                warnLevel = warnColorMap.get(info.severityColor);
            }
            if (warnLevel == null)
                warnLevel = 1;

            String channelId = Utils.warnLevelStr[warnLevel];

            RemoteViews cusRemoveExpandView = new RemoteViews(context.getPackageName(), R.layout.layout_notify_large);
            cusRemoveExpandView.setTextViewText(R.id.title, info.title);
            cusRemoveExpandView.setTextViewText(R.id.content, info.text);
            cusRemoveExpandView.setImageViewResource(R.id.icon, Utils.warnIconIndex[warnLevel]);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                var notification = new Notification.Builder(context, channelId)
                        .setWhen(System.currentTimeMillis())
                        .setSmallIcon(R.drawable.ic_sunny)
                        .setContentTitle(info.title)
                        .setContentText(info.text)
                        .setStyle(new Notification.DecoratedCustomViewStyle())
                        .setCustomBigContentView(cusRemoveExpandView)
                        .setOnlyAlertOnce(true) // 无效
                        .build();

                NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
                CRC32 crc32 = new CRC32();
                crc32.update(info.id.getBytes());
                notificationManager.notify((int) crc32.getValue(), notification);
            }
        }
        if (addItem) {
            try {
                FileOutputStream fos = context.openFileOutput(setFileName, Context.MODE_PRIVATE);
                ObjectOutputStream oos = new ObjectOutputStream(fos);
                oos.writeObject(hasNotify);
                oos.close();
                fos.close();
            } catch (Exception e) {
                Utils.saveLog(context, "保存 " + setFileName + " 失败\n" + e);
            }
        }
    }

    /**
     * 创建更新数据 PendingIntent
     */
    @SuppressLint("UnspecifiedImmutableFlag")
    protected PendingIntent createUpdatePendingIntent(Context context) {
        Intent updateIntent = new Intent(REQUEST_MANUAL);
        updateIntent.setClass(context, this.getClass());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            return PendingIntent.getBroadcast(context, 0, updateIntent, PendingIntent.FLAG_MUTABLE);
        else
            return PendingIntent.getBroadcast(context, 0, updateIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * 根据状态更新天气小部件
     *
     * @param context      上下文
     * @param realTime     实时天气
     * @param threeDay     未来三日数据
     * @param rain         分钟级降水预报
     * @param districtName 县、乡、区 名称
     */
    @SuppressLint("DefaultLocale")
    abstract public void updateAppWidget(Context context,
                                         RealTime realTime,
                                         ThreeDay threeDay,
                                         Rain rain,
                                         String districtName);

    abstract public void showTips(Context context, String tips);

}