package com.apjdminiproj.proton.Activities;

import android.Manifest;
import android.app.Activity;
import android.app.SearchManager;
import android.app.TimePickerDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.AlarmClock;
import android.provider.MediaStore;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.telephony.SmsManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.apjdminiproj.proton.Helpers.Chat;
import com.apjdminiproj.proton.Helpers.ChatAdapter;
import com.apjdminiproj.proton.Helpers.PermissionHelper;
import com.apjdminiproj.proton.Helpers.PreferenceUtils;
import com.apjdminiproj.proton.R;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity
{
    private String numbersRegex;
    private ImageView sendBtn;
    private EditText cmdInput;
    private String command,smsRec;
    private boolean hasCameraFlash;
    private SpeechRecognizer speechRecognizer;
    private TextToSpeech textToSpeech;
    private Intent speechRecognizerIntent;
    private ChatAdapter chatAdapter;
    private ArrayList<Chat> mChat;
    private RecyclerView recyclerView;
    private LinearLayoutManager linearLayoutManager;
    private boolean waitingForInput;
    private PreferenceUtils preferenceUtils;
    private AlertDialog SpeechRecognitionDialog;
    private ActivityResultLauncher<Intent> launcherForPicture,launcherForSelfie;
    private Intent originalIntent;
    private String[] conversationResponses;
    private Random randomEngine;
    private boolean isTorchOn;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        recyclerView=findViewById(R.id.recyclerView);
        sendBtn=findViewById(R.id.send_button);
        cmdInput=findViewById(R.id.cmdInput);
        sendBtn.setTag(R.drawable.ic_speech);
        recyclerView.setHasFixedSize(true);
        linearLayoutManager=new LinearLayoutManager(MainActivity.this);
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);
        originalIntent=getIntent();
        hasCameraFlash = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
        isTorchOn=false;
        randomEngine=new Random();
        sendBtn.setOnClickListener(v -> {
            if((Integer)sendBtn.getTag()==R.drawable.ic_send) {
                command = cmdInput.getText().toString();
                if (command.isEmpty() || command == null) {
                    receiveMessage("No command was entered to be executed !",false);
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
        if(preferenceUtils.getChatList()==null)
            mChat=new ArrayList<>();
        else
            mChat=preferenceUtils.getChatList();
        chatAdapter=new ChatAdapter(MainActivity.this,mChat);
        recyclerView.setAdapter(chatAdapter);
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
        launcherForPicture=registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
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
        });
        launcherForSelfie=registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
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
        });
        conversationResponses=getResources().getStringArray(R.array.conversations);
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
            return true;
        }
        else if(cmd.contains("takeapicture"))
        {
            Intent intent=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra("android.intent.extras.CAMERA_FACING", 0);
            receiveMessage("Opening Back Cam !",false);
            launcherForPicture.launch(intent);
            return true;
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
                if(isTorchOn)
                    receiveMessage("The FlashLight has already been turned ON !",false);
                else
                    {
                    String cameraId = cameraManager.getCameraIdList()[0];
                    cameraManager.setTorchMode(cameraId, true);
                    receiveMessage("Turned ON FlashLight successfully !", false);
                    isTorchOn = true;
                }
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
                    if(!isTorchOn)
                        receiveMessage("The FlashLight has already been turned OFF !",false);
                    else
                        {
                        String cameraId = cameraManager.getCameraIdList()[0];
                        cameraManager.setTorchMode(cameraId, false);
                        receiveMessage("Turned OFF FlashLight successfully !", false);
                        isTorchOn=false;
                        return true;
                    }
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
        else if(cmd.contains("cleartheconversation"))
        {
            if(mChat!=null)
            {
                mChat.clear();
                receiveMessage("The Preserved Messages were cleared successfully !",false);
                return true;
            }
        }
        else if(cmd.contains("setanalarm"))
        {
            receiveMessage("Choose when to set an alarm",false);
            Calendar calendar=Calendar.getInstance();
            TimePickerDialog setAlarmTimePicker = new TimePickerDialog(MainActivity.this, R.style.TimePickerDialogTheme, (view, hourOfDay, minute) -> {
                Calendar calendar1 =Calendar.getInstance();
                calendar1.set(Calendar.HOUR_OF_DAY,hourOfDay);
                calendar1.set(Calendar.MINUTE,minute);
                calendar1.set(Calendar.SECOND,0);
                Intent intent=new Intent(AlarmClock.ACTION_SET_ALARM);
                intent.putExtra(AlarmClock.EXTRA_HOUR,hourOfDay);
                intent.putExtra(AlarmClock.EXTRA_MINUTES,minute);
                intent.putExtra(AlarmClock.EXTRA_SKIP_UI,true);
                startActivity(intent);
                receiveMessage("Alarm set at "+
                        DateFormat.getTimeInstance(DateFormat.SHORT).format(calendar1.getTime())+" successfully !",false);
            },calendar.get(Calendar.HOUR_OF_DAY),calendar.get(Calendar.MINUTE),true);
            setAlarmTimePicker.setCancelable(false);
            setAlarmTimePicker.setCanceledOnTouchOutside(false);
            setAlarmTimePicker.setOnDismissListener(dialog -> receiveMessage("Alarm Setting Task cancelled successfully !",false));
            setAlarmTimePicker.show();
        }
        else if(cmd.contains("deleteanalarm"))
        {
            receiveMessage("Opening Clock App where you can delete an alarm",false);
            Intent intent=new Intent(AlarmClock.ACTION_DISMISS_ALARM);
            startActivity(intent);
        }
        else if(cmd.contains("openphonesettings"))
        {
            Intent intent=new Intent(Settings.ACTION_SETTINGS);
            receiveMessage("Opening Phone Settings",false);
            startActivity(intent);
            return true;
        }
        else if(cmd.contains("howareyou")||cmd.contains("whatsup")
                ||cmd.contains("howsitgoing")||cmd.contains("hopeyouaredoingwell")
                ||cmd.contains("howisitgoing")||cmd.contains("hopeyouredoingwell")
                ||cmd.contains("whatisup"))
        {
            int randomReplyIndex=randomEngine.nextInt(3);
            receiveMessage(conversationResponses[randomReplyIndex],false);
            return true;
        }
        else if(cmd.contains("ilikeyou")||cmd.contains("iloveyou")
                ||cmd.contains("ilikeyourservice")||cmd.contains("iloveyourservice"))
        {
            int randomReplyIndex=randomEngine.nextInt(3)+3;
            receiveMessage(conversationResponses[randomReplyIndex],false);
            return true;
        }
        else if(cmd.contains("goodday")||cmd.contains("haveagoodday")
                ||cmd.contains("haveaniceday")||cmd.contains("niceday")||cmd.contains("hello"))
        {
            int randomReplyIndex=randomEngine.nextInt(2)+6;
            receiveMessage(conversationResponses[randomReplyIndex],false);
            return true;
        }
        else if(cmd.contains("goodmorning")||cmd.contains("haveagoodmorning")
                ||cmd.contains("haveanicemorning")||cmd.contains("nicemorning"))
        {
            int randomReplyIndex=randomEngine.nextInt(2)+8;
            receiveMessage(conversationResponses[randomReplyIndex],false);
            return true;
        }
        else if(cmd.contains("goodafternoon")||cmd.contains("haveagoodafternoon")
                ||cmd.contains("haveaniceafternoon")||cmd.contains("niceafternoon"))
        {
            int randomReplyIndex=randomEngine.nextInt(2)+10;
            receiveMessage(conversationResponses[randomReplyIndex],false);
            return true;
        }
        else if(cmd.contains("goodevening")||cmd.contains("haveagoodevening")
                ||cmd.contains("haveaniceevening")||cmd.contains("niceevening"))
        {
            int randomReplyIndex=randomEngine.nextInt(2)+12;
            receiveMessage(conversationResponses[randomReplyIndex],false);
            return true;
        }
        else if(cmd.contains("goodnight")||cmd.contains("goodnightsweetdreams"))
        {
            int randomReplyIndex=randomEngine.nextInt(2)+14;
            receiveMessage(conversationResponses[randomReplyIndex],false);
            return true;
        }
        else if(cmd.contains("goodbye")||cmd.contains("goodbyefornow")
                ||cmd.contains("bye")||cmd.contains("byefornow"))
        {
            int randomReplyIndex=randomEngine.nextInt(2)+16;
            receiveMessage(conversationResponses[randomReplyIndex],false);
            Handler handler=new Handler(getMainLooper());
            handler.postDelayed(() -> finish(),3000);
            return true;
        }
        else if(cmd.contains("whocreatedyou"))
        {
            receiveMessage(conversationResponses[18],false);
            return true;
        }
        else if(cmd.contains("canyouhelpme")||cmd.contains("howcanyouhelpme")
                ||cmd.contains("ineedyourhelp")||cmd.contains("iwantyourassistance")||cmd.equals("heyproton"))
        {
            receiveMessage(conversationResponses[19],false);
            return true;
        }
        else
        {
            receiveMessage("Invalid Command/Message",false);
            return false;
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
                Manifest.permission.CALL_PHONE,Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.RECORD_AUDIO);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        if (requestCode == 100) {
            Map<String, Integer> perms = new HashMap<>();
            for (String permission : permissions) {
                perms.put(permission, PackageManager.PERMISSION_GRANTED);
            }
            if (grantResults.length > 0) {
                for (int i = 0; i < permissions.length; i++)
                    perms.put(permissions[i], grantResults[i]);

                boolean allPermissionsGranted = true;
                for (String permission1 : permissions) {
                    allPermissionsGranted = allPermissionsGranted && (perms.get(permission1) == PackageManager.PERMISSION_GRANTED);
                }
                if (allPermissionsGranted) {
                    Log.d(PermissionHelper.class.getSimpleName(), "onRequestPermissionsResult: all permissions granted");
                } else {
                    for (String permission2 : perms.keySet())
                        if (perms.get(permission2) == PackageManager.PERMISSION_GRANTED)
                            perms.remove(permission2);

                    StringBuilder message = new StringBuilder("The app has not been granted the following permission(s):\n\n");
                    for (String permission : perms.keySet()) {
                        message.append(permission);
                        message.append("\n");
                    }
                    message.append("\nHence, it cannot function properly." +
                            "\nPlease consider granting it these permissions in the Phone Settings.");

                    final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, R.style.AlertDialogTheme);
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
                            SpeechRecognitionDialog.dismiss();
                            receiveMessage( "No response ! Try again !",false);
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
                receiveMessage("Speech Recognition is unavailable in this Device! You can't use Voice Mode!"
                        ,false);
                SpeechRecognitionDialog.dismiss();
            }
        }
        if(preferenceUtils.getIsFirstTime())
        {
            receiveMessage(conversationResponses[19], false);
            preferenceUtils.setIsFirstTime(false);
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
            SpeechRecognitionDialog.setOnDismissListener(dialog -> speechRecognizer.stopListening());
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
