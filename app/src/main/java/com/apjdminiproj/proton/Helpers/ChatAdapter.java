package com.apjdminiproj.proton.Helpers;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.apjdminiproj.proton.R;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder>
{
    public static final int MSG_TYPE_LEFT=0;
    public static final int MSG_TYPE_RIGHT=1;
    private final Context mContext;
    private final List<Chat> messages;
    private final Animation bounceAnimation;
    public ChatAdapter(Context context,List<Chat> msgs)
    {
        mContext=context;
        messages=msgs;
        bounceAnimation=AnimationUtils.loadAnimation(mContext,R.anim.bounce_anim);
    }
    @NonNull
    @Override
    public ChatAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        if(viewType==MSG_TYPE_RIGHT)
        {
            View view = LayoutInflater.from(mContext).inflate(R.layout.chat_item_right,parent,false);
            return new ViewHolder(view);
        }
        else
        {
            View view = LayoutInflater.from(mContext).inflate(R.layout.chat_item_left,parent,false);
            return new ViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ChatAdapter.ViewHolder holder, int position)
    {
        Chat chat=messages.get(position);
        if(chat.getMessageType()==MSG_TYPE_LEFT)
        {
            holder.showMessageLeft.setText(chat.getMessage());
            holder.itemView.findViewById(R.id.chatItemLeft).setOnLongClickListener(v -> {
                ClipboardManager clipboardManager=(ClipboardManager)mContext.getSystemService(Context.CLIPBOARD_SERVICE);
                if(clipboardManager==null)
                    return false;
                holder.itemView.findViewById(R.id.chatItemLeft).startAnimation(bounceAnimation);
                ClipData clipData=ClipData.newPlainText("messageLeft:"+chat.getDateOfSending(),chat.getMessage());
                clipboardManager.setPrimaryClip(clipData);
                Toast.makeText(mContext,"Message copied to Clipboard successfully !",Toast.LENGTH_LONG).show();
                return true;
            });
        }
        else
        {
            holder.showMessageRight.setText(chat.getMessage());
            holder.itemView.findViewById(R.id.chatItemRight).setOnLongClickListener(v -> {
                ClipboardManager clipboardManager=(ClipboardManager)mContext.getSystemService(Context.CLIPBOARD_SERVICE);
                if(clipboardManager==null)
                    return false;
                holder.itemView.findViewById(R.id.chatItemRight).startAnimation(bounceAnimation);
                ClipData clipData=ClipData.newPlainText("messageRight:"+chat.getDateOfSending(),chat.getMessage());
                clipboardManager.setPrimaryClip(clipData);
                Toast.makeText(mContext,"Message copied to Clipboard successfully !",Toast.LENGTH_LONG).show();
                return true;
            });
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    @Override
    public int getItemViewType(int position)
    {
        return messages.get(position).getMessageType();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder
    {
        private final TextView showMessageLeft;
        private final TextView showMessageRight;
        public ViewHolder(@NonNull View itemView)
        {
            super(itemView);
            showMessageLeft=itemView.findViewById(R.id.show_msg_left);
            showMessageRight=itemView.findViewById(R.id.show_msg_right);
        }
    }
}
