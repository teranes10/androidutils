package com.github.teranes10.androidutils.helpers;

import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Handler;
import android.os.Looper;

import java.util.LinkedList;
import java.util.Queue;

public class Tone {
    private static final ToneGenerator toneGen;
    private static final Handler handler = new Handler(Looper.getMainLooper());
    private static final Queue<ToneItem> toneQueue = new LinkedList<>();
    private static boolean isPlaying = false;
    private static final int DELAY_BEFORE_NEXT_TONE = 100;

    static {
        toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 65);
    }

    private static class ToneItem {
        int toneType;
        int duration;

        ToneItem(int toneType, int duration) {
            this.toneType = toneType;
            this.duration = duration;
        }
    }

    private static void playNextTone() {
        if (!toneQueue.isEmpty()) {
            ToneItem toneItem = toneQueue.poll();
            if (toneItem == null) {
                return;
            }

            toneGen.startTone(toneItem.toneType, toneItem.duration);
            isPlaying = true;

            handler.postDelayed(() -> {
                isPlaying = false;
                playNextTone();
            }, toneItem.duration + DELAY_BEFORE_NEXT_TONE);
        }
    }

    public static void play(int toneType, int duration) {
        toneQueue.add(new ToneItem(toneType, duration));
        if (!isPlaying) {
            playNextTone();
        }
    }

    public static void play(int toneType) {
        play(toneType, 200);
    }

    public static void play() {
        play(ToneGenerator.TONE_PROP_BEEP);
    }
}
