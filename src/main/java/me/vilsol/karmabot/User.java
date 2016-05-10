package me.vilsol.karmabot;

import org.json.JSONObject;

import java.util.HashMap;

public class User {

    private String username;
    private long karma = 0;
    private HashMap<String, Long> lastKarma = new HashMap<>();

    public User(final JSONObject user){
        this.username = user.getString("username");
        this.karma = user.getLong("karma");

        JSONObject last = user.getJSONObject("lastKarma");
        last.keySet().forEach(k -> this.lastKarma.put(k, last.getLong(k)));
    }

    public User(String username){
        this.username = username;
    }

    public String getUsername(){
        return username;
    }

    public long getKarma(){
        return karma;
    }

    public void setKarma(long karma){
        this.karma = karma;
    }

    public HashMap<String, Long> getLastKarma(){
        return lastKarma;
    }
}
