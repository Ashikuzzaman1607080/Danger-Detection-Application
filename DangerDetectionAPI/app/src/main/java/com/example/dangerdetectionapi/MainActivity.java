package com.example.dangerdetectionapi;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.telephony.SmsManager;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;


public class MainActivity extends AppCompatActivity {

    private static final int RECORDER_BPP = 16;
    private static final String AUDIO_RECORDER_FOLDER = "AudioRecorder";
    private static final String AUDIO_RECORDER_TEMP_FILE = "record_temp.raw";
    private static final int RECORDER_SAMPLERATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    short[] audioData;

    private AudioRecord recorder = null;
    private int bufferSize = 0;
    private Thread recordingThread = null;
    private boolean isRecording = false;
    int[] bufferData;
    int bytesRecorded;

    private String output;
    private String emailAddress = "";
    private boolean isEmailSubmitted = false;
    private  boolean isDangerDetectionMode = false;
    private String initial_msg = "Provide a valid phone number to turn on danger detection mode. Notification will be sent to that phone number address once danger is detected. You may provide phone number of someone else whom you want to notify about the danger";
    private String after_email_msg = "Now you can turn on danger detection mode. Notification will be sent to given phone number";
    private String in_progress_msg = "Audio recording and danger detection is on progress. Keep your internet connection on";

    private static int MICROPHONE_PERMISSION_CODE = 200;
    MediaRecorder mediaRecorder;
    MediaPlayer mediaPlayer;
    EditText emailEditBox;
    Button emailSubmitButton, dangerDetectionButton;
    boolean button_controller = true;
    TextView emailTextView, dangerDetectionTextView;
    CheckBox callCheckbox, smsCheckbox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (isMicrophonePresent()) {
            microphonePermission();
        }


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public void onSubmitEmailClick(View v) {
        emailTextView = (TextView) findViewById(R.id.emailTextView);
        emailSubmitButton = (Button) findViewById(R.id.emailSubmitButton);
        emailEditBox = (EditText) findViewById(R.id.editTextTextEmailAddress);
        dangerDetectionButton = (Button) findViewById(R.id.dangerDetectionButton);
        dangerDetectionTextView = (TextView) findViewById(R.id.dangerDetectionTextView);

        if(!isEmailSubmitted) {
            emailAddress = emailEditBox.getText().toString();
            emailEditBox.setVisibility(View.INVISIBLE);
            emailSubmitButton.setText("EDIT");
            emailTextView.setText(emailAddress);
            isEmailSubmitted = true;
            dangerDetectionButton.setVisibility(View.VISIBLE);
            dangerDetectionTextView.setText(after_email_msg);
        }
        else {
            emailEditBox.setVisibility(View.VISIBLE);
            emailSubmitButton.setText("SUBMIT");
            emailTextView.setText(emailAddress);
            emailTextView.setText("Input your phone no");
            isEmailSubmitted = false;
            dangerDetectionButton.setVisibility(View.INVISIBLE);
            dangerDetectionTextView.setText(initial_msg);
        }
    }

    public void onDangerDetectionClick(View v) {
        emailTextView = (TextView) findViewById(R.id.emailTextView);
        emailSubmitButton = (Button) findViewById(R.id.emailSubmitButton);
        emailEditBox = (EditText) findViewById(R.id.editTextTextEmailAddress);
        dangerDetectionButton = (Button) findViewById(R.id.dangerDetectionButton);
        dangerDetectionTextView = (TextView) findViewById(R.id.dangerDetectionTextView);

        Toast.makeText(this, "Danger detection turned onn/off", Toast.LENGTH_LONG).show();

        if(!isDangerDetectionMode) {
            isDangerDetectionMode = true;
            dangerDetectionTextView.setText(in_progress_msg);
            dangerDetectionButton.setText("Turn off Danger Detection Mode");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        callRestAPILoop();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

        }
        else {
            isDangerDetectionMode = false;
            dangerDetectionTextView.setText(after_email_msg);
            dangerDetectionButton.setText("Turn on Danger Detection Mode");
        }
    }

    private void callRestAPILoop() throws IOException {
        while (isDangerDetectionMode) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            callRestAPI();
        }
    }

    private  void callRestAPI() throws IOException {
        final TextView textView = (TextView) findViewById(R.id.dangerDetectionTextView);

        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "http://192.168.0.105:8000/status/1/get_status/";

        StringRequest stringRequest = new StringRequest(Request.Method.POST,
                url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        Log.d("Response is: " , response.toString());
                        if (response.toString().equals("2") || response.toString().equals("0")) {
                            Log.d("Response is: " , "If a  dhukche");
                            smsCheckbox=(CheckBox)findViewById(R.id.smsCheckBox);
                            callCheckbox=(CheckBox)findViewById(R.id.callCheckBox);

                            if (callCheckbox.isChecked()) {
                                makeCall();
                            }

                            if (smsCheckbox.isChecked()) {
                                getGPSLocation();
                            }



                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("Response is error: " ,error.toString());

                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                try {
                    Map<String, String> params = new HashMap<String, String>();
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    recordAudio(1000);

                    try {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }


                    String encodedFile = null;
                    try {
                        File file = new File(getRecordingFilePath());
                        FileInputStream fileInputStreamReader = new FileInputStream(file);
                        byte[] bytes = new byte[(int) file.length()];
                        try {
                            fileInputStreamReader.read(bytes);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        encodedFile = Base64.encodeToString(bytes, Base64.DEFAULT);
                        String finalEncodedFile = encodedFile;
                        params.put("encodedMP3", finalEncodedFile);
                    } catch (FileNotFoundException e) {
                        System.err.println("Exception: " + e.getMessage());
                    }
                    return params;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return  null;
            }
        };

        queue.add(stringRequest);
    }

    public void makeCall() {
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse("tel:"+emailAddress));//change the number
        startActivity(callIntent);
    }

    public void sendSMS(Location location) {
        String message = "Danger! Danger! See Location: "+ "www.google.com/maps/place/"+location.getLatitude()+','+location.getLongitude();
        Log.d("Response is: " ,message + emailAddress);

        SmsManager smsManager
                = SmsManager.getDefault();

        smsManager.sendTextMessage( emailAddress, null, message, null, null);
    }
    public void getGPSLocation() {
        Log.d("Response is: " , "Tracking location");
        FusedLocationProviderClient fusedLocationProviderClient;
        LocationRequest locationRequest;

        locationRequest = new LocationRequest();
        locationRequest.setInterval(30000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MainActivity.this);
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    Log.d("Response is: " , location.toString());
                    sendSMS(location);
                }
            });
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String [] {Manifest.permission.ACCESS_FINE_LOCATION}, 99);
            }
        }
    }
    public void recordAudio(int length) {

        try {
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setOutputFile(getRecordingFilePath());
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setAudioSamplingRate(16000);
            mediaRecorder.prepare();
            mediaRecorder.start();
            try {
                Thread.sleep(length);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            try {
                stopRecording();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopRecording() throws InterruptedException {
        mediaRecorder.stop();
        mediaRecorder.release();
        mediaRecorder = null;

    }

    private boolean isMicrophonePresent() {
        if (this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_MICROPHONE)) {
            return true;
        } else return false;
    }

    private void microphonePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, MICROPHONE_PERMISSION_CODE);
        }
    }

    private String getRecordingFilePath() {
        ContextWrapper contextWrapper = new ContextWrapper(getApplicationContext());
        File musicDirectory = contextWrapper.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        File file = new File(musicDirectory, "testRecordingFle" + ".mp3");
        return file.getPath();
    }

    private String getTextFilePath() {
        ContextWrapper contextWrapper = new ContextWrapper(getApplicationContext());
        File textDirectory = contextWrapper.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        File file = new File(textDirectory, "test" + ".txt");
        return file.getPath();
    }

    public void WavRecorder(String path) {
        bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
                RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING) * 3;

        audioData = new short[bufferSize]; 
        output = path;

    }

    private String getFilename() {
        return (output);
    }

    private String getTempFilename() {
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath, AUDIO_RECORDER_FOLDER);

        if (!file.exists()) {
            file.mkdirs();
        }

        File tempFile = new File(filepath, AUDIO_RECORDER_TEMP_FILE);

        if (tempFile.exists())
            tempFile.delete();

        return (file.getAbsolutePath() + "/" + AUDIO_RECORDER_TEMP_FILE);
    }

    public void startRecording() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING, bufferSize);
        int i = recorder.getState();
        if (i == 1)
            recorder.startRecording();

        isRecording = true;

        recordingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                writeAudioDataToFile();
            }
        }, "AudioRecorder Thread");

        recordingThread.start();
    }

    private void writeAudioDataToFile() {
        byte data[] = new byte[bufferSize];
        String filename = getTempFilename();
        FileOutputStream os = null;

        try {
            os = new FileOutputStream(filename);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        int read = 0;
        if (null != os) {
                read = recorder.read(data, 0, bufferSize);
                if (read > 0) {
                }

                if (AudioRecord.ERROR_INVALID_OPERATION != read) {
                    try {
                        os.write(data);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }


            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void deleteTempFile() {
        File file = new File(getTempFilename());
        file.delete();
    }

    private void copyWaveFile(String inFilename, String outFilename) {
        FileInputStream in = null;
        FileOutputStream out = null;
        long totalAudioLen = 0;
        long totalDataLen = totalAudioLen + 36;
        long longSampleRate = RECORDER_SAMPLERATE;
        int channels = ((RECORDER_CHANNELS == AudioFormat.CHANNEL_IN_MONO) ? 1
                : 2);
        long byteRate = RECORDER_BPP * RECORDER_SAMPLERATE * channels / 8;

        byte[] data = new byte[bufferSize];

        try {
            in = new FileInputStream(inFilename);
            out = new FileOutputStream(outFilename);
            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;

            WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
                    longSampleRate, channels, byteRate);

            while (in.read(data) != -1) {
                out.write(data);
            }

            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void WriteWaveFileHeader(FileOutputStream out, long totalAudioLen,
                                     long totalDataLen, long longSampleRate, int channels, long byteRate)
            throws IOException {
        byte[] header = new byte[44];

        header[0] = 'R'; // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f'; // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1; // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (((RECORDER_CHANNELS == AudioFormat.CHANNEL_IN_MONO) ? 1
                : 2) * 16 / 8); // block align
        header[33] = 0;
        header[34] = RECORDER_BPP; // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

        out.write(header, 0, 44);
    }
}