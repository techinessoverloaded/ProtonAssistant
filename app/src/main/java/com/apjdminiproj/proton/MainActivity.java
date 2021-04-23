package com.apjdminiproj.proton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity
{
    ConstraintLayout inputLayout,optLayout,speechRecognitionLayout;
    ImageView speechOpt,textOpt,sendBtn;
    EditText cmdInput;
    String command;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        inputLayout=findViewById(R.id.input_layout);
        optLayout=findViewById(R.id.speechTextOptLayout);
        speechOpt=findViewById(R.id.speechInputOpt);
        textOpt=findViewById(R.id.textInputOpt);
        sendBtn=findViewById(R.id.send_button);
        cmdInput=findViewById(R.id.cmdInput);
        textOpt.setOnClickListener(v->{
            if(inputLayout.getVisibility() == View.INVISIBLE)
            {
                optLayout.setVisibility(View.GONE);
                inputLayout.setVisibility(View.VISIBLE);
                cmdInput.requestFocus();
            }
        });
        sendBtn.setOnClickListener(v->{
            command=cmdInput.getText().toString();
            if(command.isEmpty()||command==null)
            {
                Toast.makeText(this,"No command was entered to be executed !",Toast.LENGTH_LONG).show();
            }
            else
            {
               executeCommand(command);
            }
        });
    }
    private boolean executeCommand(String cmd)
    {
        if(cmd.isEmpty()||cmd==null)
            return false;
        cmd=cmd.toLowerCase();
        if(cmd.contains("call"))
        {

        }
        return true;
    }
}