package com.example.myapplication;

import android.app.Application;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ApplicationUtil extends Application {
    private Socket socket;
    private DataOutputStream out = null;
    private DataInputStream in = null;

    public void init() throws IOException ,Exception{
        this.socket = new Socket("192.168.1.11",9999);
        this.out = new DataOutputStream(socket.getOutputStream());
        this.in = new DataInputStream(socket.getInputStream());
    }

    public Socket getSocket(){
        return  socket;
    }

    public void setSocket(Socket socket){
        this.socket = socket;
    }

    public DataOutputStream getOut(){
        return  out;
    }

    public void setOut(DataOutputStream out){
        this.out = out;
    }

    public DataInputStream getIn(){
        return  in;
    }

    public void setIn(DataInputStream in){
        this.in = in;
    }
}
