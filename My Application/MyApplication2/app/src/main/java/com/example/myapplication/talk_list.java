package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class talk_list extends AppCompatActivity {

    private EditText mess;
    private Socket socket;
    private Handler handler;
    private DataInputStream in;
    private DataOutputStream out;
    private String to;
    private String me;
    private Button button;
    private Context mContext;
    private TextView messagelog;
    private ApplicationUtil applicationUtil;
    private ExecutorService mThreadPool;
    private Context ctx;
    Handler mHandler;
    Handler mSendHandler;
    SocketThread socketThread;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_talk_list);
        Intent intent = getIntent();
        mThreadPool =  Executors.newCachedThreadPool();
        String talkabout = intent.getStringExtra("TALKABOUT");
        mContext = this;
        handler = new Handler(){
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                Log.v("HANDLE","收到消息"+msg.what);
                try {
                    JSONObject message = new JSONObject(msg.obj.toString());
                    String from = message.getString("FROM");
                    String Time = message.getString("TIME");
                    String msg_ = message.getString("MESSAGE");
                    String res = from+" "+Time+"\n"+msg_+"\n";
                    messagelog.append(res);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                //messagelog.setText(msg.obj.toString());
            }

        };

        mSendHandler = new Handler(){
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                Log.v("HANDLE","收到消息"+msg.what);
                try {
                    JSONObject message = new JSONObject(msg.obj.toString());
                    String from = message.getString("ME");
                    String Time = message.getString("TIME");
                    String msg_ = message.getString("MESSAGE");
                    String res = from+" "+Time+"\n"+msg_+"\n";
                    messagelog.append(res);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };


        try {
            JSONObject talkaboutObj = new JSONObject(talkabout);
            me = talkaboutObj.getString("ME");
            to = talkaboutObj.getString("TO");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        messagelog = findViewById(R.id.talk_list);
        mess  = findViewById(R.id.talk_text);

        button = findViewById(R.id.send);
        startSocket();
        button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                 String message = mess.getText().toString();
                JSONObject request = new JSONObject();
                try {
                    request.put("ME",me);
                    request.put("TO",to);
                    request.put("TYPE",1);
                    request.put("MESSAGE",message);
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss");
                    Date date = new Date(System.currentTimeMillis());
                    request.put("TIME",simpleDateFormat.format(date));
                    socketThread.Send(request.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });
    }



    void startSocket(){
        socketThread = new SocketThread(handler,mSendHandler,ctx);
        socketThread.start();

    }
    private void stopSocket(){
        socketThread.isRun = false;
        socketThread.close();
        socketThread= null;

    }
}
