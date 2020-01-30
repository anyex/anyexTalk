package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
    InputStream is;
    OutputStream outputStream;
    Handler handler;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mContext =this;

        ApplicationUtil applicationUtil = (ApplicationUtil) MainActivity.this.getApplication();

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
                                socket = new Socket("192.168.1.11",9999);
                                System.out.println("开始登录2");

                                System.out.println(socket.isConnected());
                            }catch (IOException e){
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
                                outputStream = socket.getOutputStream();
                                outputStream.write(loginstr.getBytes());

                                outputStream.flush();
                                socket.shutdownOutput();
                                System.out.println("发送消息:"+loginstr);

                                is = socket.getInputStream();

                                StringBuffer out = new StringBuffer();
                                byte[] b =new byte[4096];
                                int n;
                                while ((n=is.read(b))!=-1){
                                    out.append(new String(b,0,n));
                                }
                                Log.v("ABC",out.toString());
                                System.out.println("收到消息:"+out.toString());
                                Message msg = handler.obtainMessage();
                                msg.what=1;
                                msg.obj=out.toString();
                                handler.sendMessage(msg);


                                outputStream.close();
                                is.close();
                                socket.close();
                            }
                            catch (UnknownHostException e){
                                e.printStackTrace();
                            }
                            catch (IOException e){
                                e.printStackTrace();
                            }

                        }
                    });

                }
        });




    }

}
