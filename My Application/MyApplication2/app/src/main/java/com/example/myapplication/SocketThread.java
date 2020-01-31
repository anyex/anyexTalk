package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.Socket;
import android.os.Handler;
import android.os.Message;

import org.json.JSONObject;

public class SocketThread extends Thread {
    private String ip ="192.168.1.11";
    private int port = 9999;
    private String TAG = "soket thread";
    private Socket client;
    public boolean isRun=true;
    public Handler inHandler;
   public  Handler outHandler;
    Context ctx;
    SharedPreferences sp;
    DataInputStream  in;
    private PrintStream out = null;

    public SocketThread(Handler handlerin, Handler handerout, Context context){
        inHandler = handlerin;
        outHandler = handerout;
        ctx = context;
        Log.v(TAG,"创建socket线程");
    }

    public void conn(){
        try {
            initdate();
            client = new Socket(ip,port);
            in = new DataInputStream(client.getInputStream());
            out = new PrintStream(client.getOutputStream());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void initdate(){
       // sp = ctx.getSharedPreferences("SP",ctx.MODE_PRIVATE);

    }

    @Override
    public void run() {
        conn();
        String line;
        while (isRun){
          if(client != null){
              while (true){
                  try {
                      Log.v(TAG,"开始读取数据");
                      int r = in.available();
                      while (r == 0){
                          r = in.available();
                      }
                      byte[] b = new byte[r];
                      in.read(b);
                      String result = new String(b,"utf-8");
                     // if (((line = in.readUTF())!=null))
                      {
                          Message msg = inHandler.obtainMessage();
                          msg.obj = result;
                          inHandler.sendMessage(msg);
                          Log.v("RECV MESSAGE","MSG:"+result+"SEND TO HANDLER");
                      }
                      Log.v(TAG,"读取数据结束");

                  } catch (IOException e) {
                      e.printStackTrace();
                  }

              }

          }else {
              Log.v("SOCKET_THREAD","没有可用连接");
          }
        }
    }

    public void Send(String mess){
        try {
            if(client != null){
                Log.v(TAG,"SEND MESS:"+mess);
                AsyncTask asyncTask = new AsyncTask() {
                    @Override
                    protected Object doInBackground(Object[] objects) {
                        out.println(objects[0].toString());
                        out.flush();
                        return null;
                    }
                };
                asyncTask.execute(mess);

                JSONObject sendstat = new JSONObject(mess);
                sendstat.put("SEND_STAT",1);
                Message msg = outHandler.obtainMessage();
                msg.obj = sendstat.toString();
                msg.what =1;
                outHandler.sendMessage(msg);
            }else{
                Message msg = outHandler.obtainMessage();
                msg.obj = "网络未连接";
                msg.what =1;
                outHandler.sendMessage(msg);
                conn();
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public void close(){
        try {
            if(client != null){
                in.close();
                out.close();
                client.close();
            }

        }catch (Exception e){
            Log.v(TAG,"close err");
            e.printStackTrace();
        }

    }
}

