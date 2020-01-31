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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private EditText account,passwd;
    private Socket socket;
    private Context mContext;
    private ExecutorService mThreadPool;
    private PrintStream out;
    private DataInputStream in;
    InputStream is;
    OutputStream outputStream;
    Handler handler;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mContext =this;

       final ApplicationUtil applicationUtil = (ApplicationUtil) MainActivity.this.getApplication();
       socket = applicationUtil.getSocket();
        if(socket != null){
            System.out.println("SOCKET NO NULL");
            if(socket.isConnected()){
                try {
                    System.out.println("SOCKET CLOSE");
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        handler = new Handler(){
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                Log.v("HANDLE","收到消息"+msg.what);
                switch (msg.what){
                    case 1:
                        AlertDialog alertDialog = new AlertDialog.Builder(mContext)
                                .setTitle("登录").setMessage( msg.obj.toString()).create();
                        alertDialog.show();
                }
            }

        };


        mThreadPool = Executors.newCachedThreadPool();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        account = (EditText) findViewById(R.id.account);
        passwd = (EditText) findViewById(R.id.passwd);

        Button login = (Button)findViewById(R.id.login);
        login.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
             final String account_ =  account.getText().toString();
             final  String passwd_ = passwd.getText().toString();
                System.out.println("开始登录");
                    mThreadPool.execute(new Runnable() {
                        @Override
                        public void run() {
                            String loginstr ="";
                            try{
                                applicationUtil.init();

                                in = applicationUtil.getIn();
                                socket = applicationUtil.getSocket();
                                out = new PrintStream(socket.getOutputStream());
                            }catch (IOException e){
                                Message msg = handler.obtainMessage();
                                msg.what=1;
                                msg.obj="连接服务器失败";
                                handler.sendMessage(msg);
                                e.printStackTrace();

                                return;
                            }catch (Exception e){
                                Message msg = handler.obtainMessage();
                                msg.what=1;
                                msg.obj="连接服务器失败";
                                handler.sendMessage(msg);
                                e.printStackTrace();

                                return;
                            }
                            if(socket.isConnected()){
                                JSONObject login_content = new JSONObject();
                                try {
                                    login_content.put("TYPE",0);
                                    login_content.put("ACCOUNT",account_);
                                    login_content.put("PASSWD",passwd_);
                                    loginstr = login_content.toString();
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                            try{
                                out.println(loginstr);
                                out.flush();
                                System.out.println("发送消息:"+loginstr);
                                int r = in.available();
                                while (r == 0){
                                    r = in.available();
                                }
                                byte[] b = new byte[r];
                                in.read(b);
                                String result = new String(b,"utf-8");
                                System.out.println("收到消息:"+result);

                                JSONObject res = new JSONObject(result);
                                res.put("ACCOUNT",account_);
                                if(res.getInt("LOGIN_STATE") == 1){
                                    socket.close();
                                    in.close();
                                    out.close();
                                    Log.v("LOGIN","CLOSE");
                                    Intent intent = new Intent();
                                    intent.setClass(MainActivity.this,FriendList.class);
                                    intent.putExtra("LOGIN_RES",res.toString());

                                    startActivity(intent);
                                }
                            }
                            catch (UnknownHostException e){
                                e.printStackTrace();
                            }
                            catch (IOException e){
                                e.printStackTrace();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }
                    });

                }
        });




    }

}
