package com.apjdminiproj.proton.Helpers;
import android.content.Context;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import java.util.Locale;

public class AssistantUtils
{
    private SpeechRecognizer speechRecognizer;
    private TextToSpeech textToSpeech;
    private Intent speechRecognizerIntent;
    private Context context;
    public AssistantUtils(Context context)
    {
        this.context = context;
    }
    public TextToSpeech getTextToSpeechObj()
    {
        if (textToSpeech == null)
        {
            textToSpeech = new TextToSpeech(context, status ->
            {
                if (status == TextToSpeech.SUCCESS)
                {
                    textToSpeech.setLanguage(Locale.UK);
                }
            });
        }
        return textToSpeech;
    }
    public SpeechRecognizer getSpeechRecognizerObj()
    {
        if(speechRecognizer==null)
        {
            speechRecognizer=SpeechRecognizer.createSpeechRecognizer(context);
        }
        return speechRecognizer;
    }
    public Intent getRecogniserIntent(String packageName)
    {
        if(speechRecognizerIntent==null)
        {
            speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName);
        }
        return speechRecognizerIntent;
    }
    public boolean isCompatible()
    {
        return SpeechRecognizer.isRecognitionAvailable(context);
    }
}

