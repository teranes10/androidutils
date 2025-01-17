package com.github.teranes10.androidutils.helpers;

import android.content.Context;
import android.content.SharedPreferences;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public abstract class ConstantsHelper {
    public String getPreferenceKey() {
        return getClass().getSimpleName();
    }

    public void load(Context ctx) throws IllegalAccessException {
        if (getPreferenceKey().isEmpty()) {
            throw new IllegalArgumentException("PreferenceKey is required.");
        }

        SharedPreferences preference = ctx.getSharedPreferences(getPreferenceKey(), Context.MODE_PRIVATE);

        Field[] fields = getClass().getDeclaredFields();
        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers())) {
                field.set(null, get(preference, field.getName(), field.getType()));
            }
        }
    }

    public void update(Context ctx) throws IllegalAccessException {
        if (getPreferenceKey().isEmpty()) {
            throw new IllegalArgumentException("PreferenceKey is required.");
        }

        SharedPreferences.Editor editor = ctx.getSharedPreferences(getPreferenceKey(), Context.MODE_PRIVATE).edit();

        Field[] fields = getClass().getDeclaredFields();
        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers())) {
                set(editor, field.getName(), field.getType(), field.get(null));
            }
        }
    }

    private void set(SharedPreferences.Editor editor, String key, Class<?> type, Object value) {
        if (type == String.class) {
            editor.putString(key, (String) value);
        } else if (type == int.class || type == Integer.class) {
            editor.putInt(key, (Integer) value);
        } else if (type == boolean.class || type == Boolean.class) {
            editor.putBoolean(key, (Boolean) value);
        } else if (type == long.class || type == Long.class) {
            editor.putLong(key, (Long) value);
        } else if (type == float.class || type == Float.class) {
            editor.putFloat(key, (Float) value);
        } else {
            throw new IllegalArgumentException("Unsupported type: " + type.getName());
        }
    }

    private Object get(SharedPreferences preference, String key, Class<?> type) {
        if (type == String.class) {
            return preference.getString(key, "");
        } else if (type == int.class || type == Integer.class) {
            return preference.getInt(key, 0);
        } else if (type == boolean.class || type == Boolean.class) {
            return preference.getBoolean(key, false);
        } else if (type == long.class || type == Long.class) {
            return preference.getLong(key, 0);
        } else if (type == float.class || type == Float.class) {
            return preference.getFloat(key, 0f);
        } else {
            throw new IllegalArgumentException("Unsupported type: " + type.getName());
        }
    }

    public void release() throws IllegalAccessException {
        Field[] fields = getClass().getDeclaredFields();
        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers())) {
                try {
                    Class<?> type = field.getType();
                    if (type == boolean.class) {
                        field.setBoolean(null, false);
                    } else if (type == int.class) {
                        field.setInt(null, 0);
                    } else if (type == long.class) {
                        field.setLong(null, 0L);
                    } else if (type == float.class) {
                        field.setFloat(null, 0f);
                    } else if (type == double.class) {
                        field.setDouble(null, 0.0);
                    } else if (type == char.class) {
                        field.setChar(null, '\u0000');
                    } else if (type == byte.class) {
                        field.setByte(null, (byte) 0);
                    } else if (type == short.class) {
                        field.setShort(null, (short) 0);
                    } else {
                        field.set(null, null);
                    }
                } catch (IllegalAccessException e) {
                    throw e;
                }
            }
        }
    }
}
