package com.apjdminiproj.proton.Activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import java.util.regex.*;
import com.apjdminiproj.proton.Helpers.AssistantUtils;
import com.apjdminiproj.proton.Helpers.PermissionHelper;
import com.apjdminiproj.proton.R;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private ConstraintLayout inputLayout, optLayout, speechRecognitionLayout;
    private ImageView speechOpt, textOpt, sendBtn;
    private EditText cmdInput;
    private String command;
    private PermissionHelper permissionHelper;
    private boolean hasCameraFlash;
    private SpeechRecognizer speechRecognizer;
    private TextToSpeech textToSpeech;
    private Intent speechRecognizerIntent;
    private AssistantUtils assistantUtils;
    private String numbersRegex,contactRegex;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        inputLayout = findViewById(R.id.input_layout);
        optLayout = findViewById(R.id.speechTextOptLayout);
        speechOpt = findViewById(R.id.speechInputOpt);
        textOpt = findViewById(R.id.textInputOpt);
        sendBtn = findViewById(R.id.send_button);
        cmdInput = findViewById(R.id.cmdInput);
        speechRecognitionLayout=findViewById(R.id.speechRecogLayout);
        assistantUtils=new AssistantUtils(this);
        hasCameraFlash = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
        numbersRegex="^(\\+\\d{1,3}( )?)?((\\(\\d{3}\\))|\\d{3})[- .]?\\d{3}[- .]?\\d{4}$"
                + "|^(\\+\\d{1,3}( )?)?(\\d{3}[ ]?){2}\\d{3}$"
                + "|^(\\+\\d{1,3}( )?)?(\\d{3}[ ]?)(\\d{2}[ ]?){2}\\d{2}$";
        speechOpt.setOnClickListener(v->{
            if(speechRecognitionLayout.getVisibility()==View.INVISIBLE)
            {
                optLayout.setVisibility(View.GONE);
                speechRecognitionLayout.setVisibility(View.VISIBLE);
                try
                {
                    Thread.sleep(2200);
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
                speechRecognizer.startListening(speechRecognizerIntent);
            }
        });
        textOpt.setOnClickListener(v -> {
            if (inputLayout.getVisibility() == View.INVISIBLE) {
                optLayout.setVisibility(View.GONE);
                inputLayout.setVisibility(View.VISIBLE);
                cmdInput.requestFocus();
            }
        });
        sendBtn.setOnClickListener(v -> {
            command = cmdInput.getText().toString();
            if (command.isEmpty() || command == null) {
                Toast.makeText(this, "No command was entered to be executed !", Toast.LENGTH_LONG).show();
            }
            else
                {
                executeCommand(command);
                cmdInput.setText(null);
                command=null;
            }
        });
        checkPermission();
    }

    private boolean executeCommand(String cmd)
    {
        if (cmd.isEmpty() || cmd == null)
            return false;
        cmd=cmd.toLowerCase();
        if(cmd.contains("hey typex"))
            cmd.replace("hey typex","");
        if (cmd.contains("call"))
        {
            cmd=cmd.replace("call","");
            Pattern numberPat = Pattern.compile(numbersRegex);
            if(numberPat.matcher(cmd).matches())
            {
                Toast.makeText(this, "Calling " + cmd, Toast.LENGTH_LONG).show();
                cmd = "tel:" + cmd.trim();
                Uri uri = Uri.parse(cmd);
                Intent intent = new Intent(Intent.ACTION_CALL);
                intent.setData(uri);
                startActivity(intent);
                return true;
            }
            else
                {
                    Toast.makeText(this,"Invalid Phone Number",Toast.LENGTH_LONG).show();
                return false;
            }
        }
        else if(cmd.contains("set alarm at"))
        {

        }
        else if(cmd.contains("delete alarm")){

        }
        else if (cmd.contains("google")){

        }
        else if(cmd.contains("play")){

        }
        else if(cmd.contains("ping")){
            //send sms
        }
        else if(cmd.contains("take a selfie"))
        {
            Intent intent=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra("android.intent.extras.LENS_FACING_FRONT", 1);
            intent.putExtra("android.intent.extra.USE_FRONT_CAMERA", true);
            Toast.makeText(MainActivity.this,"Opening Selfie Cam ! Say Cheese !",Toast.LENGTH_LONG).show();
            startActivity(intent);
        }
        else if(cmd.contains("take a picture"))
        {
            Intent intent=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            Toast.makeText(MainActivity.this,"Opening Back Cam !",Toast.LENGTH_LONG).show();
            startActivity(intent);
        }
        else if(cmd.contains("flashlight"))
        {
            cmd=cmd.replace("flashlight","");
            if(!hasCameraFlash)
            {
                Toast.makeText(this, "This phone does not support Flashlight", Toast.LENGTH_LONG).show();
                return false;
            }
            if(cmd.contains("on"))
            {
                CameraManager cameraManager=(CameraManager)getSystemService(Context.CAMERA_SERVICE);
                try
                {
                    String cameraId = cameraManager.getCameraIdList()[0];
                    cameraManager.setTorchMode(cameraId, true);
                    return true;
                }
                catch (CameraAccessException e)
                {
                    Toast.makeText(this,"Unable to Access Camera to turn ON Flashlight",Toast.LENGTH_LONG).show();
                    return false;
                }
            }
            else if(cmd.contains("off"))
            {
                CameraManager cameraManager=(CameraManager)getSystemService(Context.CAMERA_SERVICE);
                try
                {
                    String cameraId = cameraManager.getCameraIdList()[0];
                    cameraManager.setTorchMode(cameraId, false);
                    return true;
                }
                catch (CameraAccessException e)
                {
                    Toast.makeText(this,"Unable to Access Camera to turn OFF Flashlight",Toast.LENGTH_LONG).show();
                    return false;
                }
            }

        }
        else if(cmd.contains("increase brightness"))
        {
            int currentBrightness=Settings.System.getInt(getContentResolver(),Settings.System.SCREEN_BRIGHTNESS,0);
            if(currentBrightness+30<=255)
                Settings.System.putInt(getContentResolver(),Settings.System.SCREEN_BRIGHTNESS, currentBrightness+30);
            else if(currentBrightness==255)
                Toast.makeText(this,"The Phone is already set with Max Brightness",Toast.LENGTH_LONG).show();
            else
                Settings.System.putInt(getContentResolver(),Settings.System.SCREEN_BRIGHTNESS,
                        currentBrightness+(255-currentBrightness));
            return true;
        }
        else if(cmd.contains("decrease brightness"))
        {
            int currentBrightness=Settings.System.getInt(getContentResolver(),Settings.System.SCREEN_BRIGHTNESS,255);
            if(currentBrightness-30>=0)
                Settings.System.putInt(getContentResolver(),Settings.System.SCREEN_BRIGHTNESS, currentBrightness-30);
            else if(currentBrightness==0)
                Toast.makeText(this,"The Phone is already set with Min Brightness",Toast.LENGTH_LONG).show();
            else
                Settings.System.putInt(getContentResolver(),Settings.System.SCREEN_BRIGHTNESS,
                        currentBrightness+(currentBrightness-30));
            return true;
        }
        else if(cmd.contains("increase volume"))
        {

        }
        else if(cmd.contains("decrease volume"))
        {

        }

        return true;
    }
    private boolean callByContactName(String ct)
    {
        return true;
    }
    private void checkPermission()
    {
        PermissionHelper permissionHelper=new PermissionHelper();
        permissionHelper.checkAndRequestPermissions(this,Manifest.permission.SEND_SMS,
                Manifest.permission.CALL_PHONE,Manifest.permission.READ_CONTACTS,Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.RECORD_AUDIO);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
    {
        permissionHelper.onRequestPermissionsResult(this,requestCode,permissions,grantResults);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if (textToSpeech == null)
        {
            textToSpeech = assistantUtils.getTextToSpeechObj();
        }
        if (speechRecognizer == null)
        {
            if (assistantUtils.isCompatible())
            {
                speechRecognizer = assistantUtils.getSpeechRecognizerObj();
                speechRecognizerIntent = assistantUtils.getRecogniserIntent(getPackageName());
                speechRecognizer.setRecognitionListener(new RecognitionListener()
                {
                    @Override
                    public void onReadyForSpeech(Bundle params)
                    {
                        textToSpeech.speak("Listening to you",TextToSpeech.QUEUE_FLUSH,null,"ListeningToYou");
                        Toast.makeText(MainActivity.this,"Listening to you...",Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onBeginningOfSpeech()
                    {

                    }

                    @Override
                    public void onRmsChanged(float rmsdB)
                    {

                    }

                    @Override
                    public void onBufferReceived(byte[] buffer)
                    {

                    }

                    @Override
                    public void onEndOfSpeech() {

                    }

                    @Override
                    public void onError(int error) {
                        if (error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT)
                        {
                            textToSpeech.speak("No Response try again",
                                    TextToSpeech.QUEUE_FLUSH,null,"NoResponse");
                            speechRecognitionLayout.setVisibility(View.GONE);
                            optLayout.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onResults(Bundle results)
                    {
                        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                        String command = matches.get(0);
                        if (command.isEmpty())
                        {
                            textToSpeech.speak("No Response try again",
                                    TextToSpeech.QUEUE_FLUSH,null,"NoResponse");
                            speechRecognitionLayout.setVisibility(View.GONE);
                            optLayout.setVisibility(View.VISIBLE);
                        }
                        else
                            {
                            Log.d("recognitionResults", command);
                            executeCommand(command);
                        }
                    }

                    @Override
                    public void onPartialResults(Bundle partialResults) {

                    }

                    @Override
                    public void onEvent(int eventType, Bundle params) {

                    }
                });
            }
            else
            {
                Toast.makeText(this, "Speech Recognition is unavailable in this Device! You can't use Voice Mode!"
                        , Toast.LENGTH_LONG).show();
                speechRecognitionLayout.setVisibility(View.GONE);
                inputLayout.setVisibility(View.VISIBLE);
            }
        }
    }
    @Override
    protected void onPause()
    {
        if(textToSpeech!=null)
        {
            textToSpeech.stop();
        }
        if(speechRecognizer!=null)
        {
            speechRecognizer.stopListening();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy()
    {
        textToSpeech.shutdown();
        speechRecognizer.destroy();
        super.onDestroy();
    }
}
