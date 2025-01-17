package com.github.teranes10.androidutils.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class SpeechRecognitionUtil implements RecognitionListener {
    private static final String TAG = "SpeechRecognition";
    private final static int PERMISSIONS_REQUEST_RECORD_AUDIO = 345;
    private final boolean IS_CONTINUES_LISTEN = true;
    private SpeechRecognizer speechRecognizer;
    private Intent recognizerIntent;
    private final Activity _ctx;
    private final List<SpeechRecognitionResultListener> listeners = new ArrayList<>();

    public SpeechRecognitionUtil(Activity context) {
        _ctx = context;
    }

    public void setListeners(SpeechRecognitionResultListener listener) {
        if (listener == null) {
            return;
        }

        listeners.add(listener);
    }

    private void startListening() {
        if (speechRecognizer != null) {

            speechRecognizer.startListening(recognizerIntent);
        }
    }

    private void resetSpeechRecognizer() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(_ctx);

        boolean isAvailable = SpeechRecognizer.isRecognitionAvailable(_ctx);
        if (isAvailable) {
            speechRecognizer.setRecognitionListener(this);
        } else {
            finish();
        }
    }

    private void setRecogniserIntent() {
        // String selectedLanguage = getDefaultLanguage().toString();
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
    }

    private Locale getDefaultLanguage() {
        var availableLocales = Locale.getAvailableLocales();
        return Arrays.stream(availableLocales)
                .filter(x -> x.toString().equals("en")).findFirst().orElse(null);
    }

    public void start(Activity activity) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        setRecogniserIntent();
        resetSpeechRecognizer();
        startListening();
    }

    public void finish() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }

    @Override
    public void onReadyForSpeech(Bundle bundle) {
        Log.i(TAG, "onReadyForSpeech: ");
    }

    @Override
    public void onBeginningOfSpeech() {
        Log.i(TAG, "onBeginningOfSpeech: ");
    }

    @Override
    public void onRmsChanged(float v) {
        // Log.i(TAG, "onRmsChanged: " + v);
    }

    @Override
    public void onBufferReceived(byte[] bytes) {
        Log.i(TAG, "onBufferReceived: ");
    }

    @Override
    public void onEndOfSpeech() {
        Log.i(TAG, "onEndOfSpeech: ");
        speechRecognizer.stopListening();
    }

    @Override
    public void onError(int i) {
        Log.e(TAG, "onError: " + getError(i));

        // rest voice recogniser
        resetSpeechRecognizer();
        startListening();
    }

    @Override
    public void onResults(Bundle bundle) {
        Log.i(TAG, "onResults: ");

        var matches = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches == null) {
            return;
        }

        StringBuilder text = new StringBuilder();
        for (var match : matches) {
            text.append(match);
        }

        Log.i(TAG, "onResults: " + text);
        if (!text.toString().trim().isEmpty() && listeners.size() > 0) {
            for (SpeechRecognitionResultListener listener : listeners) {
                if (listener == null) {
                    continue;
                }

                listener.onResult(text.toString());
            }
        }

        if (IS_CONTINUES_LISTEN) {
            startListening();
        }
    }

    @Override
    public void onPartialResults(Bundle bundle) {
        Log.i(TAG, "onPartialResults: ");
    }

    @Override
    public void onEvent(int i, Bundle bundle) {
        Log.i(TAG, "onEvent: ");
    }

    private String getError(int error) {
        String mError = "";
        switch (error) {
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> mError = "network timeout";
            case SpeechRecognizer.ERROR_NETWORK -> mError = " network";
            case SpeechRecognizer.ERROR_AUDIO -> mError = " audio";
            case SpeechRecognizer.ERROR_SERVER -> mError = " server";
            case SpeechRecognizer.ERROR_CLIENT -> mError = " client";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> mError = " speech time out";
            case SpeechRecognizer.ERROR_NO_MATCH -> mError = " no match";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> mError = " recogniser busy";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                    mError = " insufficient permissions";
        }

        return mError;
    }

    public interface SpeechRecognitionResultListener {
        void onResult(String text);
    }
}
