package com.apjdminiproj.proton.Helpers;

public class Chat
{
    private String message;
    private int messageType;
    public static final int MSG_TYPE_LEFT=0;
    public static final int MSG_TYPE_RIGHT=1;
    public Chat(String message, int messageType)
    {
        this.message = message;
        this.messageType=messageType;
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
}
