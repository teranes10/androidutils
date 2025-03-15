package com.github.teranes10.androidutils.utils;

import android.annotation.SuppressLint;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class Formatter {
    public static final String HH_mm = "HH:mm";
    public static final String HH_mm_ss = "HH:mm:ss";
    public static final String yyyyMMdd = "yyyyMMdd";
    public static final String DOT_NET_DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    public static final String DOT_NET_DATE_FORMAT = "yyyy-MM-dd";
    public static final String DOT_NET_TIME_FORMAT = "HH:mm:ss";
    public static final String FILE_FORMAT = "yyyy_MM_dd_'T'_HH_mm_ss";
    public static final String DATETIME_FORMAT = "MMM dd - HH:mm";
    public static final String MAIN_DATETIME_FORMAT = "MMM dd HH:mm";
    public static final String DATE_FORMAT = "MMM dd, yyyy";
    public static final String TIME_FORMAT = HH_mm_ss;

    public static final String SIMPLE_DATE_TIME_FORMAT = "dd-M-yyyy hh:mm:ss";
    public static final String DAY_MONTH_FORMAT = "dd MMM";
    private static final String TAG = "Formatter";

    @SuppressLint("SimpleDateFormat")
    public static SimpleDateFormat getFormat(String format) {
        return new SimpleDateFormat(format);
    }

    public static String format(String format) {
        return getFormat(format).format(new Date());
    }

    public static String format(String format, Date date) {
        return getFormat(format).format(date);
    }

    public static Date getDate(String format) {
        try {
            return getFormat(format).parse(format(format));
        } catch (Exception e) {
            Log.e(TAG, "getDate: ", e);
        }
        return null;
    }

    public static Date getDate(String format, String date) {
        try {
            if (date != null) {
                return getFormat(format).parse(date);
            }
        } catch (Exception e) {
            Log.e(TAG, "getDate: " + e.getLocalizedMessage());
        }
        return null;
    }

    public static long diff(String startDateFormat, String startDate, String endDateFormat, String endDate) {
        Date start = getDate(startDateFormat, startDate);
        Date end = getDate(endDateFormat, endDate);
        return start != null && end != null ? end.getTime() - start.getTime() : 0;
    }


    public static long diff(Date start, Date end) {
        return start != null && end != null ? end.getTime() - start.getTime() : 0;
    }

    public static int compareUtc(String format, String date) {
        SimpleDateFormat formatter = getFormat(format);
        try {
            Date start = formatter.parse(date);
            Date end = formatter.parse(format(format, getUtcTime()));
            if (start != null && end != null) {
                return start.compareTo(end);
            }
        } catch (Exception e) {
            Log.e(TAG, "compare: ", e);
        }

        return -2;
    }

    public static int compare(String format, String date) {
        SimpleDateFormat formatter = getFormat(format);
        try {
            Date start = formatter.parse(date);
            Date end = formatter.parse(format(format));
            if (start != null && end != null) {
                return start.compareTo(end);
            }
        } catch (Exception e) {
            Log.e(TAG, "compare: ", e);
        }

        return -2;
    }

    public static int compare(String date1Format, String date1, String date2Format, String date2) {
        try {
            SimpleDateFormat _date1Format = getFormat(date1Format);
            Date start = _date1Format.parse(date1);
            SimpleDateFormat _date2Format = getFormat(date2Format);
            Date _date2 = _date2Format.parse(date2);
            if (start != null && _date2 != null) {
                Date end = _date1Format.parse(format(date1Format, _date2));
                if (end != null) {
                    return start.compareTo(end);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "compare: ", e);
        }

        return -2;
    }

    public static String getLocalDate(String utcFormat, String utcDate, String resultFormat) {
        try {
            if (utcDate == null || utcDate.trim().isEmpty()) {
                return "";
            }

            SimpleDateFormat _utcFormat = getFormat(utcFormat);
            _utcFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date _utcDate = _utcFormat.parse(utcDate);

            if (_utcDate != null) {
                SimpleDateFormat _resultFormat = getFormat(resultFormat);
                _resultFormat.setTimeZone(TimeZone.getDefault());
                return _resultFormat.format(_utcDate);
            }
        } catch (Exception e) {
            Log.e(TAG, "getLocalDate: ", e);
        }
        return "";
    }

    public static Boolean isWeekEnd() {
        Calendar c = Calendar.getInstance();
        return c.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY ||
                c.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY;
    }

    public static Date getUtcTime() {  // handling ParseException
        // create an instance of the SimpleDateFormat class
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss");
        // set UTC time zone by using SimpleDateFormat class
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        //create another instance of the SimpleDateFormat class for local date format
        @SuppressLint("SimpleDateFormat") SimpleDateFormat ldf = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss");
        // declare and initialize a date variable which we return to the main method
        Date d1 = null;
        // use try catch block to parse date in UTC time zone
        try {
            // parsing date using SimpleDateFormat class
            d1 = ldf.parse(sdf.format(new Date()));
        }
        // catch block for handling ParseException
        catch (java.text.ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
        // pass UTC date to main method.
        return d1;
    }

    public static Date localToUtcTime(Date time) {  // handling ParseException
        // create an instance of the SimpleDateFormat class
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss");
        // set UTC time zone by using SimpleDateFormat class
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        //create another instance of the SimpleDateFormat class for local date format
        @SuppressLint("SimpleDateFormat") SimpleDateFormat ldf = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss");
        // declare and initialize a date variable which we return to the main method
        Date d1 = null;
        // use try catch block to parse date in UTC time zone
        try {
            // parsing date using SimpleDateFormat class
            d1 = ldf.parse(sdf.format(time));
        }
        // catch block for handling ParseException
        catch (java.text.ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
        // pass UTC date to main method.
        return d1;
    }

    public static String getUtcTimeString() {  // handling ParseException
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            Date date = getUtcTime();
            return sdf.format(date);
        } catch (Exception e) {
            return "";
        }
    }

    public static String formatTime(long millis) {
        return formatTime(millis, true);
    }

    public static String formatTime(long millis, boolean includeHours) {
        if (millis <= 0) {
            return includeHours ? "00:00:00" : "00:00";
        }

        int seconds = (int) (millis / 1000) % 60;
        int minutes = (int) ((millis / (1000 * 60)) % 60);
        int hours = (int) ((millis / (1000 * 60 * 60)) % 24);

        return includeHours ? String.format("%02d:%02d:%02d", hours, minutes, seconds)
                : String.format("%02d:%02d", minutes, seconds);
    }
}
