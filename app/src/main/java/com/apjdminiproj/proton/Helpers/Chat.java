package com.apjdminiproj.proton.Helpers;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Chat
{
    private String message;
    private int messageType;
    public static final int MSG_TYPE_LEFT=0;
    public static final int MSG_TYPE_RIGHT=1;
    private String dateOfSending;
    public Chat(String message, int messageType)
    {
        this.message = message;
        this.messageType=messageType;
        SimpleDateFormat formatter=new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        this.dateOfSending=formatter.format(new Date());
    }
    public Chat()
    {

    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getMessageType() {
        return messageType;
    }

    public void setMessageType(int messageType) {
        this.messageType = messageType;
    }

    public String getDateOfSending() {
        return dateOfSending;
    }

    public void setDateOfSending(String dateOfSending) {
        this.dateOfSending = dateOfSending;
    }
}
