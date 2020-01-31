package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class FriendList extends AppCompatActivity {

    JSONObject friendsObj;
    String [] data;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_list);
        Intent intent = getIntent();
        String friends = intent.getStringExtra("LOGIN_RES");
        ArrayAdapter<String> adapter ;

        try {
             friendsObj = new JSONObject(friends);
            JSONArray friendArray = friendsObj.getJSONArray("FRIENDS");

            data = new String[friendArray.length()];


            for(int i=0;i<friendArray.length();i++){
                JSONObject value = friendArray.getJSONObject(i);
                data[i] = value.getString("ACCOUNT");
            }

            adapter = new ArrayAdapter<String>(FriendList.this,android.R.layout.simple_list_item_1,data);
            ListView friendListView = findViewById (R.id.friends_list);
            friendListView.setAdapter(adapter);
            friendListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Intent intent = new Intent();
                    JSONObject talkabout = new JSONObject();
                    try {
                        talkabout.put("ME",friendsObj.getString("ACCOUNT"));
                        talkabout.put("TO",data[position]);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    intent.setClass(FriendList.this,talk_list.class);
                    intent.putExtra("TALKABOUT",talkabout.toString());
                    startActivity(intent);
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }


}
