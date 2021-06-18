package com.apjdminiproj.proton.Activities;


import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.Manifest;
import android.app.Activity;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.speech.tts.UtteranceProgressListener;
import android.telephony.SmsManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.*;
import com.apjdminiproj.proton.Helpers.Chat;
import com.apjdminiproj.proton.Helpers.ChatAdapter;
import com.apjdminiproj.proton.Helpers.PermissionHelper;
import com.apjdminiproj.proton.Helpers.PreferenceUtils;
import com.apjdminiproj.proton.R;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity
{
    private ConstraintLayout inputLayout;
    private ImageView sendBtn;
    private EditText cmdInput;
    private String command,smsRec;
    private boolean hasCameraFlash;
    private SpeechRecognizer speechRecognizer;
    private TextToSpeech textToSpeech;
    private Intent speechRecognizerIntent;
    private String numbersRegex,contactRegex;
    private ChatAdapter chatAdapter;
    private ArrayList<Chat> mChat;
    private RecyclerView recyclerView;
    private LinearLayoutManager linearLayoutManager;
    private boolean waitingForInput;
    private PreferenceUtils preferenceUtils;
    private AlertDialog SpeechRecognitionDialog;
    private ActivityResultLauncher<Intent> launcherForPicture,launcherForSelfie;
    private Intent originalIntent;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        recyclerView=findViewById(R.id.recyclerView);
        sendBtn=findViewById(R.id.send_button);
        cmdInput=findViewById(R.id.cmdInput);
        recyclerView.setHasFixedSize(true);
        linearLayoutManager=new LinearLayoutManager(MainActivity.this);
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);
        originalIntent=getIntent();
        hasCameraFlash = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
        sendBtn.setOnClickListener(v -> {
            if((Integer)sendBtn.getTag()==R.drawable.ic_send) {
                command = cmdInput.getText().toString();
                if (command.isEmpty() || command == null) {
                    Toast.makeText(this, "No command was entered to be executed !", Toast.LENGTH_LONG).show();
                } else {
                    if (!waitingForInput) {
                        sendMessage(command, false);
                        executeCommand(preprocessCommand(command));
                        cmdInput.setText(null);
                    } else {
                        sendMessage(command, true);
                        cmdInput.setText(null);
                    }
                    linearLayoutManager.scrollToPositionWithOffset(chatAdapter.getItemCount() - 1, 0);
                    recyclerView.post(() -> {
                        View target = linearLayoutManager.findViewByPosition(chatAdapter.getItemCount() - 1);
                        if (target != null) {
                            int offset = recyclerView.getMeasuredHeight() - target.getMeasuredHeight();
                            linearLayoutManager.scrollToPositionWithOffset(chatAdapter.getItemCount() - 1, offset);
                        }
                    });
                }
            }
            else
            {
                textToSpeech.speak("Listening to you",TextToSpeech.QUEUE_FLUSH,null,"ListeningToYou");
            }
        });
        if(!Settings.System.canWrite(this))
        {
            final AlertDialog.Builder builder2=new AlertDialog.Builder(MainActivity.this,R.style.AlertDialogTheme);
            final ActivityResultLauncher<Intent> launcher = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if(result.getResultCode() == Activity.RESULT_OK)
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
                                final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this,R.style.AlertDialogTheme);
                                builder.setTitle("Permission Required")
                                        .setMessage(message)
                                        .setPositiveButton("OK", (dialog, id) -> {
                                            dialog.cancel();
                                            MainActivity.this.finish();
                                        });
                                final AlertDialog alert = builder.create();
                                alert.show();
                            }
                            checkPermission();
                        }
                    }
            );
            builder2.setTitle("Needs Protected Permission").setCancelable(false)
                    .setMessage("This app needs the following permission to run properly:\n"
                            +Manifest.permission.WRITE_SETTINGS+"\n You will now be redirected to the Settings to grant the Permission")
                    .setPositiveButton("OK", (dialog,which) -> {
                        Intent intent=new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                        intent.setData(Uri.parse("package:"+getPackageName()));
                        launcher.launch(intent);
                    }).setNegativeButton("CANCEL",(dialog,which)->{
                dialog.cancel();
                MainActivity.this.finish();
            });
            final AlertDialog alertd=builder2.create();
            alertd.show();
        }
        else
            checkPermission();
        waitingForInput=false;
        preferenceUtils=PreferenceUtils.getInstance(getApplicationContext());
        Handler handler=new Handler(getMainLooper());
        handler.post(() -> {
            if(preferenceUtils.getChatList()==null)
                mChat=new ArrayList<>();
            else
                mChat=preferenceUtils.getChatList();
            chatAdapter=new ChatAdapter(MainActivity.this,mChat);
            recyclerView.setAdapter(chatAdapter);
        });
        cmdInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
                if (s == null || s.length() == 0) {
                    sendBtn.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_speech));
                    sendBtn.setTag(R.drawable.ic_speech);
                }
                else
                    {
                        sendBtn.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_send));
                        sendBtn.setTag(R.drawable.ic_send);
                    }
            }
            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        launcherForPicture=registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result)
            {
                if(result.getResultCode()==RESULT_OK)
                {
                    if(result.getData().getExtras()!=null)
                    {
                        Bitmap picture = (Bitmap) result.getData().getExtras().get("data");
                        startActivity(originalIntent);
                        try {
                            String path = saveImage(picture,"ProtonCapture"+Calendar.getInstance().getTimeInMillis());
                            receiveMessage("Stored the pic at "+path.replaceFirst("/","")+" successfully !",false);
                        }
                        catch (IOException e)
                        {
                            e.printStackTrace();
                            receiveMessage("Failed to save the pic ! Try again !",false);
                        }
                    }
                    else
                    {
                        startActivity(originalIntent);
                        receiveMessage("Failed to save the pic ! Try again !",false);
                    }
                }
                else
                {
                    startActivity(originalIntent);
                    receiveMessage("Failed to capture a pic ! Try again !",false);
                }
            }
        });
        launcherForSelfie=registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result)
            {
                if(result.getResultCode()==RESULT_OK)
                {
                    if(result.getData().getExtras()!=null)
                    {
                        Bitmap picture = (Bitmap) result.getData().getExtras().get("data");
                        startActivity(originalIntent);
                        try {
                            String path = saveImage(picture,"ProtonSelfie"+Calendar.getInstance().getTimeInMillis());
                            receiveMessage("Stored the Selfie pic at "+path.replaceFirst("/","")+" successfully !",false);
                        }
                        catch (IOException e)
                        {
                            e.printStackTrace();
                            receiveMessage("Failed to save the Selfie pic ! Try again !",false);
                        }
                    }
                    else
                    {
                        startActivity(originalIntent);
                        receiveMessage("Failed to save the Selfie pic ! Try again !",false);
                    }
                }
                else
                {
                    startActivity(originalIntent);
                    receiveMessage("Failed to capture a Selfie pic ! Try again !",false);
                }
            }
        });
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
                receiveMessage("Calling " + cmd,false);
                cmd = "tel:+91" + cmd.trim();
                Uri uri = Uri.parse(cmd);
                Intent intent = new Intent(Intent.ACTION_CALL);
                intent.setData(uri);
                startActivity(intent);
                return true;
            }
            else
                {
                    receiveMessage("Invalid Phone Number",false);
                    return false;
                }
        }
        else if (cmd.contains("googlethis"))
        {
            command=command.substring(command.toLowerCase().indexOf("google this"));
            String searchQuery=command.substring(command.toLowerCase().indexOf("google this")+11).trim();
            if(searchQuery.length()==0||searchQuery==null)
            {
                receiveMessage("Invalid search term",false);
                return false;
            }
            else
                { Intent intent=new Intent(Intent.ACTION_WEB_SEARCH);
                intent.putExtra(SearchManager.QUERY, searchQuery);
                receiveMessage("Opening Google App", false);
                startActivity(intent);
                return true;
            }
        }
        else if(cmd.contains("playthis"))
        {
            command=command.substring(command.toLowerCase().indexOf("play this"));
            String searchQuery=command.substring(command.toLowerCase().indexOf("play this")+9).trim();
            if(searchQuery.length()==0||searchQuery==null)
            {
                receiveMessage("Incomplete command",false);
                return false;
            }
            else
            {
                Intent intent=new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.youtube.com/results?search_query="+searchQuery));
                receiveMessage("Opening YouTube App", false);
                startActivity(intent);
                return true;
            }
        }
        else if(cmd.contains("ping"))
        {
            receiveMessage("Enter the number of the recipient",true);
        }
        else if(cmd.contains("takeaselfie"))
        {
            Intent intent=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra("android.intent.extras.CAMERA_FACING", 1);
            receiveMessage("Opening Selfie Cam ! Say Cheese !",false);
            launcherForSelfie.launch(intent);
        }
        else if(cmd.contains("takeapicture"))
        {
            Intent intent=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra("android.intent.extras.CAMERA_FACING", 0);
            receiveMessage("Opening Back Cam !",false);
            launcherForPicture.launch(intent);
        }
        else if(cmd.contains("turnonflashlight"))
        {
            if(!hasCameraFlash)
            {
                receiveMessage("This phone does not support Flashlight",false);
                return false;
            }
            CameraManager cameraManager=(CameraManager)getSystemService(Context.CAMERA_SERVICE);
            try
            {
                String cameraId = cameraManager.getCameraIdList()[0];
                cameraManager.setTorchMode(cameraId, true);
                receiveMessage("Turned ON FlashLight successfully !",false);
                return true;
            }
            catch (CameraAccessException e)
            {
                receiveMessage("Unable to Access Camera to turn ON Flashlight",false);
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
                    receiveMessage("Turned OFF FlashLight successfully !",false);
                    return true;
                }
                catch (CameraAccessException e)
                {
                    receiveMessage("Unable to Access Camera to turn OFF Flashlight",false);
                    return false;
                }
            }
        else if(cmd.contains("increasebrightness"))
        {
            int currentBrightness=Settings.System.getInt(getContentResolver(),Settings.System.SCREEN_BRIGHTNESS,0);
            if(currentBrightness+30<=255) {
                Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, currentBrightness + 30);
                receiveMessage("Increased Brightness successfully !",false);
            }else if(currentBrightness==255)
                receiveMessage("The Phone is already set with Max Brightness",false);
            else
                {
                Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS,
                        currentBrightness + (255 - currentBrightness));
                receiveMessage("Increased Brightness successfully !",false);
            }
            return true;
        }
        else if(cmd.contains("decreasebrightness"))
        {
            int currentBrightness=Settings.System.getInt(getContentResolver(),Settings.System.SCREEN_BRIGHTNESS,255);
            if(currentBrightness-30>=0)
            {
                Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, currentBrightness - 30);
                receiveMessage("Decreased Brightness successfully !",false);
            }
            else if(currentBrightness==0)
                receiveMessage("The Phone is already set with Min Brightness",false);
            else
            {
                Settings.System.putInt(getContentResolver(),Settings.System.SCREEN_BRIGHTNESS,
                        currentBrightness+(currentBrightness-30));
                receiveMessage("Decreased Brightness successfully !",false);
            }
            return true;
        }
        else if(cmd.contains("increasevolume"))
        {

        }
        else if(cmd.contains("decreasevolume"))
        {

        }
        else if(cmd.contains("cleartheconversation"))
        {
            if(mChat!=null)
            {
                mChat.clear();
                receiveMessage("The Preserved Messages were cleared successfully !",false);
            }
        }
        command=null;
        return true;
    }
    private String saveImage(Bitmap bitmap,String name) throws IOException
    {
        OutputStream fos;
        String path;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentResolver resolver = getContentResolver();
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name + ".jpg");
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg");
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);
            Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
            fos = resolver.openOutputStream(Objects.requireNonNull(imageUri));
            path=imageUri.getPath();
        } else {
            String imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
            File image = new File(imagesDir,name+".jpg");
            fos = new FileOutputStream(image);
            path=image.getAbsolutePath();
        }
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
        Objects.requireNonNull(fos).close();
        return path;
    }
    private boolean callByContactName(String ct)
    {
        return true;
    }
    private void sendSMS(String recipient)
    {
        smsRec=recipient;
        receiveMessage("Enter the Message to be sent",true);
    }
    private void sendSMS(String recipient,String message)
    {
        if(message.trim().toLowerCase().contains("stop pinging"))
        {
            receiveMessage("SMS Task cancelled successfully !",false);
        }
        else
            {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(recipient, null, message, null, null);
            receiveMessage("Message: " + message + "\nsent to " + recipient + " successfully !", false);
        }
        waitingForInput=false;
    }
    private void checkPermission()
    {
        PermissionHelper permissionHelper=new PermissionHelper();
        permissionHelper.checkAndRequestPermissions(this,Manifest.permission.SEND_SMS,
                Manifest.permission.CALL_PHONE,Manifest.permission.READ_CONTACTS,Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.RECORD_AUDIO);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
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

                        final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this,R.style.AlertDialogTheme);
                        builder.setTitle("Permission Required")
                                .setMessage(message)
                                .setPositiveButton("OK", (dialog, id) -> {
                                    dialog.cancel();
                                    MainActivity.this.finish();
                                });
                        final AlertDialog alert = builder.create();
                        alert.show();
                    }
                }
            }
        }
    }
    private void sendMessage(String message,boolean givingInput)
    {
        Chat newMsg = new Chat(message, Chat.MSG_TYPE_RIGHT);
        mChat.add(newMsg);
        chatAdapter.notifyDataSetChanged();
        if(givingInput)
        {
            String current=mChat.get(mChat.size()-1).getMessage();
            String preprocessedCurrent=preprocessCommand(current);
            String previous=mChat.get(mChat.size()-2).getMessage();
            if(previous.equals("Enter the number of the recipient"))
            {
                Pattern num=Pattern.compile(numbersRegex);
                if(num.matcher(preprocessedCurrent).matches())
                {
                    sendSMS(preprocessedCurrent);
                }
                else
                {
                    receiveMessage("Invalid Recipient",false);
                }
            }
            else if(previous.equals("Enter the Message to be sent"))
            {
                sendSMS(smsRec,current);
            }
        }
    }
    private void receiveMessage(String message,boolean needsInput)
    {
        textToSpeech.speak(message,TextToSpeech.QUEUE_FLUSH,null,"receiveMessage:"+message);
        Chat newMsg = new Chat(message, Chat.MSG_TYPE_LEFT);
        mChat.add(newMsg);
        chatAdapter.notifyDataSetChanged();
        scrollToRecentMessage();
        if(needsInput)
        {
            waitingForInput=true;
        }
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
            textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId)
                {

                }

                @Override
                public void onDone(String utteranceId)
                {
                    if(utteranceId.equals("ListeningToYou"))
                    {
                        runOnUiThread(() -> showSpeechRecognitionDialog());
                    }
                }

                @Override
                public void onError(String utteranceId)
                {

                }

                @Override
                public void onError(String utteranceId,int errorCode)
                {

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
                    public void onError(int error)
                    {
                        if(error==SpeechRecognizer.ERROR_SPEECH_TIMEOUT||error==SpeechRecognizer.ERROR_NO_MATCH) {
                            textToSpeech.speak("No Response try again",
                                    TextToSpeech.QUEUE_ADD, null, "NoResponse");
                            SpeechRecognitionDialog.dismiss();
                            Toast.makeText(MainActivity.this, "No response ! Try again !", Toast.LENGTH_LONG)
                                    .show();
                        }
                    }

                    @Override
                    public void onResults(Bundle results)
                    {
                        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                        command = matches.get(0);
                        Log.d("recognitionResults", command);
                        SpeechRecognitionDialog.dismiss();
                        if(waitingForInput)
                        {
                            sendMessage(command,true);
                        }
                        else
                            {
                                sendMessage(command, false);
                                executeCommand(preprocessCommand(command));
                            }
                            Log.d("preprocessedCmd", preprocessCommand(command));
                            scrollToRecentMessage();
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
                SpeechRecognitionDialog.dismiss();
                inputLayout.setVisibility(View.VISIBLE);
            }
        }
    }

    private void scrollToRecentMessage()
    {
        linearLayoutManager.scrollToPositionWithOffset(chatAdapter.getItemCount() - 1, 0);
        recyclerView.post(() -> {
            View target = linearLayoutManager.findViewByPosition(chatAdapter.getItemCount() - 1);
            if (target != null) {
                int offset = recyclerView.getMeasuredHeight() - target.getMeasuredHeight();
                linearLayoutManager.scrollToPositionWithOffset(chatAdapter.getItemCount() - 1, offset);
            }
        });
    }

    private void showSpeechRecognitionDialog()
    {
        if(SpeechRecognitionDialog==null)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            View view = LayoutInflater.from(this).inflate(R.layout.layout_speechrecognition,findViewById(R.id.speechRecognitionLayout));
            builder.setView(view);
            SpeechRecognitionDialog = builder.create();
            SpeechRecognitionDialog.setCancelable(false);
            SpeechRecognitionDialog.setOnShowListener(dialog ->
                    speechRecognizer.startListening(speechRecognizerIntent));
            SpeechRecognitionDialog.setOnDismissListener(dialog -> {
                speechRecognizer.stopListening();
            });
            if (SpeechRecognitionDialog.getWindow() != null)
            {
                SpeechRecognitionDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }
            view.findViewById(R.id.cancelSpeechBut).setOnClickListener(v->{
                SpeechRecognitionDialog.dismiss();
                textToSpeech.stop();
            });
        }
        SpeechRecognitionDialog.show();
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
        if(mChat!=null)
        {
            Iterator<Chat> iterator = mChat.iterator();
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
            String todayS = formatter.format(new Date());
            Date today;
            try
            {
                today = formatter.parse(todayS);
            } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
                if (today == null)
                    return;
                while (iterator.hasNext()) {
                    Chat c = iterator.next();
                    Date date;
                    try {
                        date = formatter.parse(c.getDateOfSending());
                    } catch (Exception e) {
                        e.printStackTrace();
                        break;
                    }
                    if (date == null)
                        continue;
                    long diffTime = today.getTime() - date.getTime();
                    long diffDays = (diffTime / (1000 * 60 * 60 * 24)) % 365;
                    if (diffDays > 7)
                        iterator.remove();
                }
                preferenceUtils.setChatList(mChat);
        }
        super.onPause();
    }
    @Override
    protected void onDestroy()
    {
        if(textToSpeech!=null)
            textToSpeech.shutdown();
        if(speechRecognizer!=null)
            speechRecognizer.destroy();
        super.onDestroy();
    }
}
