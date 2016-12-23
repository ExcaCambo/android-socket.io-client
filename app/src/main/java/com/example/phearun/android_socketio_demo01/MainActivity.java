package com.example.phearun.android_socketio_demo01;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class MainActivity extends AppCompatActivity implements ItemClickListener{

    private RecyclerView mRecyclerView;
    private List<Feed> mFeeds = new ArrayList<>();
    private MyFeedAdapter myFeedAdapter;

    EditText txtPost;
    Button btnPost;
    TextView tvTyping;

    private Socket socket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRecyclerView = (RecyclerView) findViewById(R.id.mRecycler);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        myFeedAdapter = new MyFeedAdapter(mFeeds, this);
        mRecyclerView.setAdapter(myFeedAdapter);


        txtPost = (EditText) findViewById(R.id.txtPost);
        btnPost = (Button) findViewById(R.id.btnPost);
        tvTyping = (TextView) findViewById(R.id.tvTyping);
        tvTyping.setVisibility(View.GONE);

        btnPost.setOnClickListener(this.onButtonPostClick);

        this.bindingEventListener();
    }

    //TODO: connect to server & binding event listener
    public void bindingEventListener(){

        try {
            IO.Options opts = new IO.Options();
            opts.forceNew = true;
            opts.reconnection = true;
            socket = IO.socket("http://192.168.178.128:3000/post", opts);

        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        socket.on(Socket.EVENT_CONNECT, this.onConnectEvent);
        socket.on(Socket.EVENT_DISCONNECT, this.onDisconnectEvent);
        socket.on("all posts", this.onAllPostEvent);
        socket.on("new post", this.onNewPostEvent);
        socket.on("removed post", this.onRemovePostEvent);
        socket.on("update like", this.onLikePostEvent);

        socket.connect();
    }

    private Emitter.Listener onConnectEvent = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.d("SOCKET", "connnected to server!");
        }
    };

    private Emitter.Listener onAllPostEvent = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.d("SOCKET", "all posts" + args.length);

            String jsonString = args[0].toString();

            List<Feed> feeds = new Gson().fromJson(jsonString, new TypeToken<List<Feed>>(){}.getType());
            mFeeds.addAll(feeds);

            /*JSONArray jsonArray = (JSONArray) args[1];
            for(int i=0; i<jsonArray.length(); i++){
                try {
                    String id = jsonArray.getJSONObject(i).getString("id");
                    String username = jsonArray.getJSONObject(i).getString("username");
                    String text = jsonArray.getJSONObject(i).getString("text");
                    int like = jsonArray.getJSONObject(i).getInt("like");
                    mFeeds.add(new Feed(id, username, text, like));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }*/
            Log.d("SOCKET", mFeeds + "");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    myFeedAdapter.notifyDataSetChanged();
                }
            });
        }
    };

    //TODO: on new post event handler
    private Emitter.Listener onNewPostEvent = new Emitter.Listener() {
        @Override
        public void call(Object... args) {

            String jsonString = args[0].toString();

            final Feed feed = new Gson().fromJson(jsonString, Feed.class);

            Log.e("SOCKET", "onMessage" + feed);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mFeeds.add(0, feed);
                    myFeedAdapter.notifyItemInserted(0);
                    scrollToTop();
                }
            });
        }
    };

    //TODO: on remove post event handler
    private Emitter.Listener onRemovePostEvent = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            final String id = (String) args[0];
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final int index = Feed.findIndexById(mFeeds, id);
                    mFeeds.remove(index);
                    myFeedAdapter.notifyItemRemoved(index);
                }
            });
        }
    };

    //TODO: onlike post event handler
    private Emitter.Listener onLikePostEvent = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.e("SOCKET", args[0] + ", " + args[1]);

            int like = Integer.parseInt(args[0].toString());
            String id = args[1].toString();
            updateLike(like, id);
        }
    };

    //TODO: on disconnect event handler
    private Emitter.Listener onDisconnectEvent = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.d("SOCKET", "disconnect");
        }
    };

    //TODO: Clear status text
    private void clearText(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtPost.setText("");
            }
        });
    }

    //TODO: button post clicked
    private View.OnClickListener onButtonPostClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            try {
                String text = txtPost.getText().toString();
                String username = "Me";
                Feed feed = new Feed(null, text, username, 0);
                String jsonString = new Gson().toJson(feed);
                JSONObject jsonObject = new JSONObject(jsonString);

                /* or
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("username", username);
                jsonObject.put("text", text);
                */

                socket.emit("new post", jsonObject, new Ack() {
                    @Override
                    public void call(Object... args) {
                        clearText();
                    }
                });

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    //TODO: update like
    public void updateLike(final int like, final String id){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int index = Feed.findIndexById(mFeeds, id);
                mFeeds.get(index).setLike(like);
                myFeedAdapter.notifyItemChanged(index);
            }
        });
    }

    //TODO: scroll recycler view to top
    private void scrollToTop(){
        mRecyclerView.scrollToPosition(0);
    }


    @Override
    public void onItemClick(int position) {
        String id = mFeeds.get(position).getId();

        socket.emit("remove post", id);

    }

    @Override
    public void onLikeClick(int position) {
        String id = mFeeds.get(position).getId();
        socket.emit("like post", id);


    }
}
