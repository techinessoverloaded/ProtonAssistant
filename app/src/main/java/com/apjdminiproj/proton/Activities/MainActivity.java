package com.apjdminiproj.proton.Activities;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
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
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.*;
import com.apjdminiproj.proton.Helpers.Chat;
import com.apjdminiproj.proton.Helpers.ChatAdapter;
import com.apjdminiproj.proton.Helpers.PermissionHelper;
import com.apjdminiproj.proton.R;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity
{
    private ConstraintLayout inputLayout, optLayout, speechRecognitionLayout;
    private ImageView speechOpt, textOpt, sendBtn;
    private EditText cmdInput;
    private String command;
    private PermissionHelper permissionHelper;
    private boolean hasCameraFlash;
    private SpeechRecognizer speechRecognizer;
    private TextToSpeech textToSpeech;
    private Intent speechRecognizerIntent;
    private String numbersRegex,contactRegex;
    private ChatAdapter chatAdapter;
    private List<Chat> mChat;
    private RecyclerView recyclerView;
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
        numbersRegex="\\d{10}";
        recyclerView=findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager=new LinearLayoutManager(MainActivity.this);
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);
        mChat=new ArrayList<>();
        chatAdapter=new ChatAdapter(MainActivity.this,mChat);
        recyclerView.setAdapter(chatAdapter);
        hasCameraFlash = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
        speechOpt.setOnClickListener(v->{
            if(speechRecognitionLayout.getVisibility()==View.INVISIBLE)
            {
                optLayout.setVisibility(View.GONE);
                speechRecognitionLayout.setVisibility(View.VISIBLE);
                textToSpeech.speak("Listening to you",TextToSpeech.QUEUE_FLUSH,null,"ListeningToYou");
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
                    sendMessage(command);
                    executeCommand(preprocessCommand(command));
                    cmdInput.setText(null);
                    command=null;
            }
        });
        if(!Settings.System.canWrite(this))
        {
            final AlertDialog.Builder builder2=new AlertDialog.Builder(MainActivity.this);
            builder2.setTitle("Needs Protected Permission").setCancelable(false)
                    .setMessage("This app needs the following permission to run properly:\n"
                            +Manifest.permission.WRITE_SETTINGS+"\n You will now be redirected to the Settings to grant the Permission")
                    .setPositiveButton("OK", (dialog,which) -> {
                        Intent intent=new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                        intent.setData(Uri.parse("package:"+getPackageName()));
                        startActivityForResult(intent,2);
                    }).setNegativeButton("CANCEL",(dialog,which)->{
                dialog.cancel();
            });
            final AlertDialog alertd=builder2.create();
            alertd.show();
        }
        else
            checkPermission();
    }
    private String preprocessCommand(String cmd)
    {
        cmd=cmd.toLowerCase();
        cmd=cmd.replaceAll("\\p{Punct}","");
        cmd=cmd.replaceAll("\\s","");
        if(cmd.contains("heyproton"))
            cmd=cmd.replace("heyproton","");
        return cmd;
    }
    private boolean executeCommand(String cmd)
    {
        if (cmd.isEmpty() || cmd == null)
            return false;
        if (cmd.contains("callthisnumber"))
        {
            cmd=cmd.replace("callthisnumber","");
            Pattern numberPat = Pattern.compile(numbersRegex);
            if(numberPat.matcher(cmd).matches())
            {
                receiveMessage("Calling " + cmd);
                cmd = "tel:+91" + cmd.trim();
                Uri uri = Uri.parse(cmd);
                Intent intent = new Intent(Intent.ACTION_CALL);
                intent.setData(uri);
                startActivity(intent);
                return true;
            }
            else
                {
                    receiveMessage("Invalid Phone Number");
                return false;
            }
        }
        else if (cmd.contains("google")){

        }
        else if(cmd.contains("play")){

        }
        else if(cmd.contains("ping")){
            //send sms
        }
        else if(cmd.contains("takeaselfie"))
        {
            Intent intent=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra("android.intent.extras.LENS_FACING_FRONT", 1);
            intent.putExtra("android.intent.extra.USE_FRONT_CAMERA", true);
            receiveMessage("Opening Selfie Cam ! Say Cheese !");
            startActivity(intent);
        }
        else if(cmd.contains("takeapicture"))
        {
            Intent intent=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            receiveMessage("Opening Back Cam !");
            startActivity(intent);
        }
        else if(cmd.contains("turnonflashlight"))
        {
            if(!hasCameraFlash)
            {
                receiveMessage("This phone does not support Flashlight");
                return false;
            }
            CameraManager cameraManager=(CameraManager)getSystemService(Context.CAMERA_SERVICE);
            try
            {
                String cameraId = cameraManager.getCameraIdList()[0];
                cameraManager.setTorchMode(cameraId, true);
                receiveMessage("Turned ON FlashLight successfully !");
                return true;
            }
            catch (CameraAccessException e)
            {
                receiveMessage("Unable to Access Camera to turn ON Flashlight");
                return false;
            }
        }
        else if(cmd.contains("turnoffflashlight"))
            {
                CameraManager cameraManager=(CameraManager)getSystemService(Context.CAMERA_SERVICE);
                try
                {
                    String cameraId = cameraManager.getCameraIdList()[0];
                    cameraManager.setTorchMode(cameraId, false);
                    receiveMessage("Turned OFF FlashLight successfully !");
                    return true;
                }
                catch (CameraAccessException e)
                {
                    receiveMessage("Unable to Access Camera to turn OFF Flashlight");
                    return false;
                }
            }
        else if(cmd.contains("increasebrightness"))
        {
            int currentBrightness=Settings.System.getInt(getContentResolver(),Settings.System.SCREEN_BRIGHTNESS,0);
            if(currentBrightness+30<=255) {
                Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, currentBrightness + 30);
                receiveMessage("Increased Brightness successfully !");
            }else if(currentBrightness==255)
                receiveMessage("The Phone is already set with Max Brightness");
            else
                {
                Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS,
                        currentBrightness + (255 - currentBrightness));
                receiveMessage("Increased Brightness successfully !");
            }
            return true;
        }
        else if(cmd.contains("decreasebrightness"))
        {
            int currentBrightness=Settings.System.getInt(getContentResolver(),Settings.System.SCREEN_BRIGHTNESS,255);
            if(currentBrightness-30>=0)
            {
                Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, currentBrightness - 30);
                receiveMessage("Decreased Brightness successfully !");
            }
            else if(currentBrightness==0)
                receiveMessage("The Phone is already set with Min Brightness");
            else
            {
                Settings.System.putInt(getContentResolver(),Settings.System.SCREEN_BRIGHTNESS,
                        currentBrightness+(currentBrightness-30));
                receiveMessage("Decreased Brightness successfully !");
            }
            return true;
        }
        else if(cmd.contains("increasevolume"))
        {

        }
        else if(cmd.contains("decreasevolume"))
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
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        switch (requestCode)
        {
            case 100:
            {
                Map<String, Integer> perms = new HashMap<>();
                for (String permission : permissions)
                {
                    perms.put(permission, PackageManager.PERMISSION_GRANTED);
                }
                if (grantResults.length > 0)
                {
                    for (int i = 0; i < permissions.length; i++)
                        perms.put(permissions[i], grantResults[i]);

                    boolean allPermissionsGranted = true;
                    for (String permission1 : permissions)
                    {
                        allPermissionsGranted = allPermissionsGranted && (perms.get(permission1) == PackageManager.PERMISSION_GRANTED);
                    }
                    if (allPermissionsGranted)
                    {
                        Log.d(PermissionHelper.class.getSimpleName(), "onRequestPermissionsResult: all permissions granted");
                    }
                    else
                    {
                        for (String permission2 : perms.keySet())
                            if (perms.get(permission2) == PackageManager.PERMISSION_GRANTED)
                                perms.remove(permission2);

                        StringBuilder message = new StringBuilder("The app has not been granted the following permission(s):\n\n");
                        for (String permission : perms.keySet())
                        {
                            message.append(permission);
                            message.append("\n");
                        }
                        message.append("\nHence, it cannot function properly." +
                                "\nPlease consider granting it these permissions in the Phone Settings.");

                        final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setTitle("Permission Required")
                                .setMessage(message)
                                .setPositiveButton("OK", (dialog, id) -> dialog.cancel());
                        final AlertDialog alert = builder.create();
                        alert.show();
                    }
                }
            }
        }
    }
    private void sendMessage(String message)
    {
        Chat newMsg=new Chat(message,Chat.MSG_TYPE_RIGHT);
        mChat.add(newMsg);
        chatAdapter.notifyDataSetChanged();
    }
    private void receiveMessage(String message)
    {
        Chat newMsg=new Chat(message,Chat.MSG_TYPE_LEFT);
        mChat.add(newMsg);
        chatAdapter.notifyDataSetChanged();
    }
    @Override
    protected void onResume()
    {
        super.onResume();
        if (textToSpeech == null)
        {
            textToSpeech = new TextToSpeech(MainActivity.this, status ->
            {
                if (status == TextToSpeech.SUCCESS)
                {
                    textToSpeech.setLanguage(Locale.UK);
                }
            });
        }
        if (speechRecognizer == null)
        {
            if(SpeechRecognizer.isRecognitionAvailable(MainActivity.this))
            {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(MainActivity.this);
                speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
                speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,getPackageName());
                speechRecognizer.setRecognitionListener(new RecognitionListener()
                {
                    @Override
                    public void onReadyForSpeech(Bundle params)
                    {
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
                            sendMessage(command);
                            executeCommand(preprocessCommand(command));
                            Log.d("preprocessedCmd",preprocessCommand(command));
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==2)
        {
            if(Settings.System.canWrite(MainActivity.this))
                Log.d("ProtectedPermissions","Write Settings Permission granted");
            else
            {
                StringBuilder message = new StringBuilder("The app has not been granted the following permission(s):\n\n");
                message.append(Manifest.permission.WRITE_SETTINGS);
                message.append("\n");
                message.append("\nHence, it cannot function properly." +
                        "\nPlease consider granting it these permissions in the Phone Settings.");
                final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Permission Required")
                        .setMessage(message)
                        .setPositiveButton("OK", (dialog, id) -> dialog.cancel());
                final AlertDialog alert = builder.create();
                alert.show();
            }
            checkPermission();
        }
    }
}
