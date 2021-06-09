package com.apjdminiproj.proton.Helpers;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;

public class PreferenceUtils
{
    private static SharedPreferences sharedPreferences;
    private static SharedPreferences.Editor editor;
    private static final String chatListKey="chat_backup";
    private static PreferenceUtils instance=null;
    private PreferenceUtils(Context context)
    {
        sharedPreferences= PreferenceManager.getDefaultSharedPreferences(context);
    }
    public static synchronized PreferenceUtils getInstance(Context con)
    {
        if(instance==null)
        {
            instance=new PreferenceUtils(con.getApplicationContext());
        }
        return instance;
    }
    public ArrayList<Chat> getChatList()
    {
        ArrayList<Chat> output;
        String serialisedObj=sharedPreferences.getString(chatListKey,null);
        if(serialisedObj==null)
            return null;
        else
        {
            Gson gson=new Gson();
            Type type=new TypeToken<ArrayList<Chat>>(){}.getType();
            output=gson.fromJson(serialisedObj,type);
        }
        return output;
    }
    public void setChatList(ArrayList<Chat> value)
    {
        editor=sharedPreferences.edit();
        Gson gson=new Gson();
        String json=gson.toJson(value);
        editor.remove(chatListKey).apply();
        editor.putString(chatListKey,json);
        editor.apply();
    }
}
