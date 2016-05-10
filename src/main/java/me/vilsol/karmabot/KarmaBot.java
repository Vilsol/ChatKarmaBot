package me.vilsol.karmabot;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import pro.zackpollard.telegrambot.api.TelegramBot;
import pro.zackpollard.telegrambot.api.chat.message.send.SendableTextMessage;
import pro.zackpollard.telegrambot.api.event.Listener;
import pro.zackpollard.telegrambot.api.event.chat.message.TextMessageReceivedEvent;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KarmaBot {

    public static void main(String[] args) throws IOException{
        new KarmaBot(args[0]);
    }

    private HashMap<String, HashMap<String, User>> chatUsers = new HashMap<>();

    private Pattern usernamePattern = Pattern.compile("^@[\\w_]{5,32}\\s*(—|--|\\+\\+)$");
    private Pattern checkPattern = Pattern.compile("<meta property=\"og:title\" content=\"(.+?)\">");

    private long delayTime = 1000 * (60 * 60 * 6); // 6 Hours

    public KarmaBot(String apiKey) throws IOException{
        TelegramBot telegramBot = TelegramBot.login(apiKey);

        if(telegramBot == null){
            System.exit(-1);
        }

        telegramBot.getEventsManager().register(new Listener() {
            @Override
            public void onTextMessageReceived(TextMessageReceivedEvent event){
                String content = event.getContent().getContent();
                if(usernamePattern.matcher(content).find()){
                    String chatId = event.getChat().getId();

                    if(!chatUsers.containsKey(chatId)){
                        chatUsers.put(chatId, new HashMap<>());
                    }

                    SendableTextMessage.SendableTextMessageBuilder message = SendableTextMessage.builder().replyTo(event.getMessage());

                    String username = event.getMessage().getSender().getUsername();

                    if(!chatUsers.get(chatId).containsKey(username.toLowerCase())){
                        chatUsers.get(chatId).put(username.toLowerCase(), new User(username.toLowerCase()));
                    }

                    String target = content.substring(1);
                    target = target.substring(0, target.length() - 2).trim();

                    if(username.toLowerCase().equals(target.toLowerCase())){
                        message.message("You can't karma yourself!");
                        event.getChat().sendMessage(message.build());
                        return;
                    }

                    if(!userExists(target.toLowerCase())){
                        message.message("This user doesn't exist!");
                        event.getChat().sendMessage(message.build());
                        return;
                    }

                    User user = chatUsers.get(chatId).get(username.toLowerCase());
                    Long lastVote = user.getLastKarma().getOrDefault(target.toLowerCase(), 0L);
                    if(lastVote + delayTime > System.currentTimeMillis()){
                        message.message("You have already voted for this user in past 6 hours!");
                        event.getChat().sendMessage(message.build());
                        return;
                    }

                    user.getLastKarma().put(target.toLowerCase(), System.currentTimeMillis());

                    if(!chatUsers.get(chatId).containsKey(target.toLowerCase())){
                        chatUsers.get(chatId).put(target.toLowerCase(), new User(target.toLowerCase()));
                    }

                    User tUser = chatUsers.get(chatId).get(target.toLowerCase());

                    if(content.endsWith("++")){
                        tUser.setKarma(tUser.getKarma() + 1);
                        message.message("@" + target + " Karma has been increased to " + tUser.getKarma());
                        event.getChat().sendMessage(message.build());
                    }else{
                        tUser.setKarma(tUser.getKarma() - 1);
                        message.message("@" + target + " Karma has been decreased to " + tUser.getKarma());
                        event.getChat().sendMessage(message.build());
                    }

                    try{
                        saveKarma();
                    }catch(IOException e){
                        System.exit(1);
                    }
                }
            }
        });

        telegramBot.startUpdates(false);

        loadKarma();
    }

    private void loadKarma() throws IOException {
        File f = new File("karma.json");
        if(f.exists()){
            JSONObject k = new JSONObject(Files.readAllLines(f.toPath()).get(0));
            k.keySet().forEach(chat -> {
                chatUsers.put(chat, new HashMap<>());
                JSONObject chatObject = k.getJSONObject(chat);
                chatObject.keySet().forEach(u -> chatUsers.get(chat).put(u, new User(chatObject.getJSONObject(u))));
            });
        }
    }

    private void saveKarma() throws IOException {
        File f = new File("karma.json");
        JSONObject result = new JSONObject();
        chatUsers.forEach((k, v) -> {
            JSONObject chat = new JSONObject();
            v.forEach((k1, v1) -> {
                JSONObject user = new JSONObject();
                user.put("username", v1.getUsername());
                user.put("karma", v1.getKarma());
                user.put("lastKarma", v1.getLastKarma());
                chat.put(v1.getUsername(), user);
            });
            result.put(k, chat);
        });
        Files.write(f.toPath(), result.toString().getBytes());
    }

    private boolean userExists(String username){
        try{
            URL url = new URL("http://telegram.me/" + username);
            String result = IOUtils.toString(url.openStream());
            Matcher matcher = checkPattern.matcher(result);
            if(matcher.find()){
                if(!matcher.group(1).startsWith("Telegram: Contact")){
                    return true;
                }
            }
        }catch(IOException e){
            e.printStackTrace();
        }

        return false;
    }

}
