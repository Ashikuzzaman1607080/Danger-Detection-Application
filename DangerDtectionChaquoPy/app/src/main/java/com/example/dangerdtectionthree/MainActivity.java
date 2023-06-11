package com.example.dangerdtectionthree;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


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

    private static int MICROPHONE_PERMISSION_CODE = 200;
    MediaRecorder mediaRecorder;
    MediaPlayer mediaPlayer;
    EditText et1, et2;
    Button button4;
    TextView tv,tv2;
    boolean button_controller = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (isMicrophonePresent()) {
            microphonePermission();
        }

        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }

        boolean isMountend = Environment.getExternalStorageState().
                equals(android.os.Environment.MEDIA_MOUNTED);

        Python py = Python.getInstance();
        PyObject pyobj = py.getModule("script");
        PyObject obj = pyobj.callAttr("main","10");
        Toast.makeText(this, obj.toString(), Toast.LENGTH_LONG).show();
        tv = (TextView) findViewById(R.id.tv);
        tv.setText(obj.toString());
    }


    public void btnRecordPress(View v) {
        try {
            if (button_controller) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (button_controller) {
                            WavRecorder(getRecordingFilePath());
                            startRecording();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    tv = (TextView) findViewById(R.id.tv);
                                    tv.setText("Audio Recording");
                                }
                            });
                            try {
                                Thread.sleep(5000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            stopRecording();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    tv = (TextView) findViewById(R.id.tv);
                                    tv.setText("Audio Processing");
                                }
                            });
                            PlayPress();
                            try {
                                Thread.sleep(5000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }).start();
            }


        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    public void reccordAudio() {
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setOutputFile(getRecordingFilePath());
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        try {
            mediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mediaRecorder.start();
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void btnStopPress(View v) {
        button_controller = false;
        Toast.makeText(this, "Recording Stopped", Toast.LENGTH_LONG).show();

    }

    public void StopPress() {
        mediaRecorder.stop();
        mediaRecorder.release();
        mediaRecorder = null;


    }

    public void btnPlayPress(View v) {
        button_controller = true;
    }

    public void PlayPress() {
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(getRecordingFilePath());
            mediaPlayer.prepare();
            mediaPlayer.start();
            Thread.sleep(5000);
            mediaPlayer.stop();

        } catch (Exception e) {
            e.printStackTrace();
        }

        ContextWrapper contextWrapper = new ContextWrapper(getApplicationContext());
        File musicDirectory = contextWrapper.getExternalFilesDir(Environment.DIRECTORY_MUSIC);

        Python py = Python.getInstance();
        PyObject pyobj = py.getModule("script");
        PyObject obj = pyobj.callAttr("main", musicDirectory.getPath());
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tv2 = (TextView) findViewById(R.id.tv2);
                tv2.setText(obj.toString());
            }
        });
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse("tel:"+"+8801754328378"));
        startActivity(callIntent);

    }

    public void btnSubmitPress(View v) {

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
        File file = new File(musicDirectory, "testRecordingFle" + ".wav");
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
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

            }
        });
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
            while (isRecording) {
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
            }

            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void stopRecording() {
        if (null != recorder) {
            isRecording = false;

            int i = recorder.getState();
            if (i == 1)
                recorder.stop();
            recorder.release();

            recorder = null;
            recordingThread = null;
        }

        copyWaveFile(getTempFilename(), getFilename());
        deleteTempFile();
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