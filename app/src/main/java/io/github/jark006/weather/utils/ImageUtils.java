package io.github.jark006.weather.utils;

import androidx.annotation.DrawableRes;

import io.github.jark006.weather.R;

public class ImageUtils {

    public static int getSkyconIcon(String weather) {
        return switch (weather) {
            // 多云天气
            case "PARTLY_CLOUDY_DAY", "PARTLY_CLOUDY_NIGHT" -> R.drawable.ic_cloud;

            // 阴天
            case "CLOUDY" -> R.drawable.ic_nosun;

            // 雾霾和雾
            case "HAZE", "LIGHT_HAZE", "MODERATE_HAZE", "HEAVY_HAZE", "FOG" -> R.drawable.ic_haze;

            // 各类降雨
            case "RAIN", "LIGHT_RAIN", "MODERATE_RAIN", "HEAVY_RAIN", "STORM_RAIN" ->
                    R.drawable.ic_rain;

            // 各类降雪
            case "SNOW", "LIGHT_SNOW", "MODERATE_SNOW", "HEAVY_SNOW", "STORM_SNOW" ->
                    R.drawable.ic_snow;

            // 沙尘天气
            case "DUST", "SAND" -> R.drawable.ic_sand;

            // 大风
            case "WIND" -> R.drawable.ic_wind;

            // 默认晴天（包括CLEAR_DAY/CLEAR_NIGHT）
            default -> R.drawable.ic_sunny;
        };
    }

    public static int getBgResourceId(String weather, boolean isDay) {
        return switch (weather) {
            case "CLEAR_DAY" -> R.drawable.bg_widget_sunny;
            case "CLEAR_NIGHT" -> R.drawable.bg_widget_sunny_night;
            case "PARTLY_CLOUDY_DAY" -> R.drawable.bg_widget_cloudy;
            case "PARTLY_CLOUDY_NIGHT" -> R.drawable.bg_widget_cloudy_night;
            case "CLOUDY" ->
                    isDay ? R.drawable.bg_widget_overcast : R.drawable.bg_widget_overcast_night;
            case "LIGHT_HAZE", "MODERATE_HAZE", "HEAVY_HAZE" ->
                    isDay ? R.drawable.bg_widget_haze : R.drawable.bg_widget_haze_night;
            case "LIGHT_RAIN" ->
                    isDay ? R.drawable.bg_widget_drizzle : R.drawable.bg_widget_drizzle_night;
            case "MODERATE_RAIN" ->
                    isDay ? R.drawable.bg_widget_rain : R.drawable.bg_widget_rain_night;
            case "HEAVY_RAIN" ->
                    isDay ? R.drawable.bg_widget_downpour : R.drawable.bg_widget_downpour_night;
            case "STORM_RAIN" ->
                    isDay ? R.drawable.bg_widget_rainstorm : R.drawable.bg_widget_rainstorm_night;
            case "FOG" -> isDay ? R.drawable.bg_widget_fog : R.drawable.bg_widget_fog_night;
            case "LIGHT_SNOW", "MODERATE_SNOW", "HEAVY_SNOW", "STORM_SNOW" ->
                    isDay ? R.drawable.bg_widget_snow : R.drawable.bg_widget_snow_night;
            case "DUST", "WIND" ->
                    isDay ? R.drawable.bg_widget_sandstorm : R.drawable.bg_widget_sandstorm_night;
            default -> isDay ? R.drawable.bg_widget_sunny : R.drawable.bg_widget_sunny_night;
        };
    }
}
