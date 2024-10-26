package com.github.teranes10.androidutils.helpers;

import static android.app.Activity.RESULT_OK;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.DataChannel;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.RtpTransceiver;
import org.webrtc.ScreenCapturerAndroid;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.nio.ByteBuffer;
import java.util.List;

public class WebRtcClient {
    private static final String TAG = "WebRTC";
    private final Context _ctx;
    private final EglBase.Context _eglBaseContext = EglBase.create().getEglBaseContext();
    private final ActivityResultLauncher<Intent> _screenCaptureLauncher;
    private PeerConnectionFactory _peerConnectionFactory;
    private PeerConnection _peerConnection;
    private DataChannel _dataChannel;
    private WebRTCListener _listener;
    private final List<PeerConnection.IceServer> _iceServers;
    private final MediaConstraints _mediaConstraints;
    private VideoCapturer _videoCapturer;
    private MediaStream _mediaStream;

    private AudioTrack _audioTrack;
    private AudioSource _audioSource;

    public WebRtcClient(AppCompatActivity context) {
        _ctx = context;

        _screenCaptureLauncher = context.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        onCapturingPermissionGranted(result.getData());
                    } else {
                        Log.e(TAG, "screenCaptureLauncher: result is null");
                    }
                }
        );

        _mediaConstraints = new MediaConstraints();
        _mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"));
        _mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));

        _iceServers = List.of(
                PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
                PeerConnection.IceServer.builder("turn:170.64.173.147:3478")
                        .setUsername("softclient1")
                        .setPassword("softclient1")
                        .createIceServer()
        );

        PeerConnectionFactory.InitializationOptions options = PeerConnectionFactory.InitializationOptions.builder(_ctx)
                .setEnableInternalTracer(true)
                .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
                .createInitializationOptions();

        PeerConnectionFactory.initialize(options);

        _peerConnectionFactory = PeerConnectionFactory.builder()
                .setVideoDecoderFactory(new DefaultVideoDecoderFactory(_eglBaseContext))
                .setVideoEncoderFactory(new DefaultVideoEncoderFactory(_eglBaseContext, true, true))
                .setOptions(new PeerConnectionFactory.Options())
                .createPeerConnectionFactory();
    }

    public void startConnection() {
        _peerConnection = _peerConnectionFactory.createPeerConnection(_iceServers, new CustomPeerConnectionObserver() {
            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                super.onIceCandidate(iceCandidate);
                if (_listener != null) {
                    _listener.onIceCandidate(iceCandidate);
                }
            }

            @Override
            public void onAddStream(MediaStream mediaStream) {
                super.onAddStream(mediaStream);
                if (!mediaStream.audioTracks.isEmpty()) {
                    AudioTrack remoteAudioTrack = mediaStream.audioTracks.get(0);
                    remoteAudioTrack.setEnabled(true);
                }
            }

            @Override
            public void onIceConnectionChange(PeerConnection.IceConnectionState newState) {
                super.onIceConnectionChange(newState);
                if (newState == PeerConnection.IceConnectionState.CLOSED || newState == PeerConnection.IceConnectionState.FAILED) {
                    Log.d(TAG, "Peer connection closed or failed");
                    stopConnection();
                }
            }

            @Override
            public void onDataChannel(DataChannel dataChannel) {
                super.onDataChannel(dataChannel);

                dataChannel.registerObserver(new DataChannel.Observer() {
                    @Override
                    public void onMessage(DataChannel.Buffer buffer) {
                        ByteBuffer data = buffer.data;
                        byte[] bytes = new byte[data.remaining()];
                        data.get(bytes);

                        String command = new String(bytes);
                        if (_listener != null) {
                            _listener.onDataReceived(command);
                        }
                    }

                    @Override
                    public void onBufferedAmountChange(long l) {
                    }

                    @Override
                    public void onStateChange() {
                        Log.d("DataChannel", "State: " + dataChannel.state().toString());
                    }
                });
            }
        });

        if (_peerConnection != null) {
            DataChannel.Init dataChannelInit = new DataChannel.Init();
            _dataChannel = _peerConnection.createDataChannel("control", dataChannelInit);

            if (_mediaStream != null) {
                _peerConnection.addStream(_mediaStream);

                if (_listener != null) {
                    _listener.onMediaStreamAdded();
                }
            } else {
                startCapture();
            }
        }
    }

    public void stopConnection() {
        if (_peerConnection != null) {
            if (_dataChannel != null) {
                _dataChannel.close();
                _dataChannel.dispose();
                _dataChannel = null;
            }

            //_peerConnection.close();
            _peerConnection = null;
        }
    }

    public void release() {
        stopCapture();
        stopConnection();

        if (_peerConnectionFactory != null) {
            _peerConnectionFactory.dispose();
            _peerConnectionFactory = null;
        }
    }

    public void setListener(WebRTCListener listener) {
        _listener = listener;
    }

    public void handleOffer(SessionDescription offer) {
        _peerConnection.setRemoteDescription(new CustomSdpObserver() {
            @Override
            public void onSetSuccess() {
                super.onSetSuccess();

                createAnswer();
            }
        }, offer);
    }

    public void handleIceCandidate(IceCandidate iceCandidate) {
        Log.i(TAG, "handleIceCandidate: ");
        _peerConnection.addIceCandidate(iceCandidate);
    }

    private void createAnswer() {
        _peerConnection.createAnswer(new CustomSdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription answer) {
                super.onCreateSuccess(answer);

                _peerConnection.setLocalDescription(new CustomSdpObserver() {
                    @Override
                    public void onSetSuccess() {
                        super.onSetSuccess();
                        if (_listener != null) {
                            _listener.onAnswer(answer);
                        }
                    }
                }, answer);
            }
        }, _mediaConstraints);
    }

    private void startCapture() {
        ForegroundService.startService(_ctx, ScreenCastService.class);

        MediaProjectionManager mediaProjectionManager =
                (MediaProjectionManager) _ctx.getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        if (mediaProjectionManager != null) {
            _screenCaptureLauncher.launch(mediaProjectionManager.createScreenCaptureIntent());
        } else {
            Log.e(TAG, "initializeWebRTC: projection manager is null");
        }
    }

    private void stopCapture() {
        if (_videoCapturer != null) {
            try {
                _videoCapturer.stopCapture();
            } catch (Exception e) {
                Log.e(TAG, "stopCapture: ", e);
            }

            _videoCapturer.dispose();
            _videoCapturer = null;
        }

        if (_mediaStream != null) {
            _mediaStream.dispose();
            _mediaStream = null;
        }

        ForegroundService.stopService(_ctx, ScreenCastService.class);
    }

    private void onCapturingPermissionGranted(Intent permissionIntent) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) _ctx.getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        int screenWidth = displayMetrics.widthPixels;
        int screenHeight = displayMetrics.heightPixels;
        int frameRate = 30;

        _videoCapturer = new ScreenCapturerAndroid(permissionIntent, new MediaProjection.Callback() {
            @Override
            public void onStop() {
                super.onStop();
                Log.d("TAG", "onStop: stopped screen casting permission");
            }
        });

        VideoSource videoSource = _peerConnectionFactory.createVideoSource(false);

        SurfaceTextureHelper surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", _eglBaseContext);
        _videoCapturer.initialize(surfaceTextureHelper, _ctx, videoSource.getCapturerObserver());
        _videoCapturer.startCapture(screenWidth, screenHeight, frameRate);

        VideoTrack videoTrack = _peerConnectionFactory.createVideoTrack("videoTrack", videoSource);
        _mediaStream = _peerConnectionFactory.createLocalMediaStream("localStream");
        _mediaStream.addTrack(videoTrack);

        _audioSource = _peerConnectionFactory.createAudioSource(new MediaConstraints());
        _audioTrack = _peerConnectionFactory.createAudioTrack("audioTrack", _audioSource);
        _mediaStream.addTrack(_audioTrack); // Add the audio track to the media stream

        _peerConnection.addStream(_mediaStream);

        if (_listener != null) {
            _listener.onMediaStreamAdded();
        }
    }

    private static class CustomPeerConnectionObserver implements PeerConnection.Observer {
        @Override
        public void onIceCandidate(IceCandidate iceCandidate) {
            Log.i(TAG, "onIceCandidate: ");
        }

        @Override
        public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
            Log.i(TAG, "onIceCandidatesRemoved: ");
        }

        @Override
        public void onAddStream(MediaStream mediaStream) {
            Log.i(TAG, "onAddStream: ");
        }

        @Override
        public void onRemoveStream(MediaStream mediaStream) {
            Log.i(TAG, "onRemoveStream: ");
        }

        @Override
        public void onDataChannel(DataChannel dataChannel) {
            Log.i(TAG, "onDataChannel: ");
        }

        @Override
        public void onRenegotiationNeeded() {
            Log.i(TAG, "onRenegotiationNeeded: ");
        }

        @Override
        public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
            Log.i(TAG, "onAddTrack: ");
        }

        @Override
        public void onSignalingChange(PeerConnection.SignalingState signalingState) {
            Log.i(TAG, "onSignalingChange: ");
        }

        @Override
        public void onIceConnectionChange(PeerConnection.IceConnectionState newState) {
            Log.i(TAG, "onIceConnectionChange: " + newState);
        }

        @Override
        public void onIceConnectionReceivingChange(boolean receiving) {
            Log.i(TAG, "onIceConnectionReceivingChange: ");
        }

        @Override
        public void onIceGatheringChange(PeerConnection.IceGatheringState newState) {
            Log.i(TAG, "onIceGatheringChange: ");
        }

        @Override
        public void onConnectionChange(PeerConnection.PeerConnectionState newState) {
            Log.i(TAG, "onConnectionChange: ");
        }

        @Override
        public void onStandardizedIceConnectionChange(PeerConnection.IceConnectionState newState) {
            Log.i(TAG, "onStandardizedIceConnectionChange: ");
        }

        @Override
        public void onTrack(RtpTransceiver transceiver) {
            Log.i(TAG, "onTrack: ");
        }
    }

    private static class CustomSdpObserver implements SdpObserver {

        @Override
        public void onCreateSuccess(SessionDescription sessionDescription) {
            Log.i(TAG, "onCreateSuccess: " + sessionDescription.type);
        }

        @Override
        public void onSetSuccess() {
            Log.i(TAG, "onSetSuccess: ");
        }

        @Override
        public void onCreateFailure(String s) {
            Log.i(TAG, "onCreateFailure: " + s);
        }

        @Override
        public void onSetFailure(String s) {
            Log.i(TAG, "onSetFailure: " + s);
        }
    }

    public interface WebRTCListener {
        void onAnswer(SessionDescription answer);

        void onIceCandidate(IceCandidate iceCandidate);

        void onMediaStreamAdded();

        void onDataReceived(String data);
    }

    public static class ScreenCastService extends ForegroundService {

        @Override
        protected int getServiceId() {
            return 2;
        }

        @RequiresApi(api = Build.VERSION_CODES.Q)
        @Override
        protected int getServiceType() {
            return ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION;
        }

        @Override
        protected void onStartService(Context context) {

        }

        @Override
        protected void onStopService() {

        }
    }
}
