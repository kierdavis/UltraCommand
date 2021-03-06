package com.kierdavis.ultracommand;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Server;
import org.bukkit.ChatColor;

public class CustomCommandContext {
    private static Pattern SUB_PATTERN = Pattern.compile("\\$([aAdDpP]|\\d+(\\+)?)");
    
    private Logger logger;
    private Player player;
    private String[] args;
    private List<String> text;
    private List<String> chat;
    private List<String> playerCommands;
    private List<String> consoleCommands;
    private String usage;
    private int reqArgs;
    
    public CustomCommandContext(Logger logger_, Player player_, String[] args_) {
        logger = logger_;
        player = player_;
        args = args_;
        text = null;
        chat = null;
        playerCommands = null;
        consoleCommands = null;
        usage = "";
        reqArgs = 0;
    }
    
    public void setText(List<String> l) {
        text = l;
    }
    
    public void setChat(List<String> l) {
        chat = l;
    }
    
    public void setPlayerCommands(List<String> l) {
        playerCommands = l;
    }
    
    public void setConsoleCommands(List<String> l) {
        consoleCommands = l;
    }
    
    public void addText(String s) {
        if (text == null) text = new ArrayList<String>();
        text.add(s);
    }
    
    public void addChat(String s) {
        if (chat == null) chat = new ArrayList<String>();
        chat.add(s);
    }
    
    public void addPlayerCommand(String s) {
        if (playerCommands == null) playerCommands = new ArrayList<String>();
        playerCommands.add(s);
    }
    
    public void addConsoleCommand(String s) {
        if (consoleCommands == null) consoleCommands = new ArrayList<String>();
        consoleCommands.add(s);
    }
    
    public void setUsage(String s) {
        usage = s;
    }
    
    public void execute() {
        doSubs(text);
        doSubs(chat);
        doSubs(playerCommands);
        doSubs(consoleCommands);
        
        if (args.length < reqArgs) {
            if (usage != null && usage.length() > 0) {
                player.sendMessage(ChatColor.YELLOW + "Usage: " + ChatColor.WHITE + ChatColor.translateAlternateColorCodes('&', usage));
            }
            else {
                player.sendMessage(ChatColor.YELLOW + "This command requires at least " + Integer.toString(reqArgs) + " argument" + (reqArgs == 1 ? "" : "s") + ".");
            }
            
            return;
        }
        
        execText();
        execChat();
        execPlayerCommands();
        execConsoleCommands();
    }
    
    private void execText() {
        if (text == null) return;
        
        for (int i = 0; i < text.size(); i++) {
            String s = text.get(i);
            s = ChatColor.translateAlternateColorCodes('&', s);
            player.sendMessage(s);
        }
    }
    
    private void execChat() {
        if (chat == null) return;
        
        for (int i = 0; i < chat.size(); i++) {
            String s = chat.get(i);
            s = ChatColor.translateAlternateColorCodes('&', s);
            player.chat(s);
        }
    }
    
    private void execPlayerCommands() {
        if (playerCommands == null) return;
        
        Server server = player.getServer();
        
        for (int i = 0; i < playerCommands.size(); i++) {
            String s = playerCommands.get(i);
            if (s.startsWith("/")) s = s.substring(1);
            logger.info("Command issued by " + player.getName() + " during processing: /" + s);
            server.dispatchCommand(player, s);
        }
    }
    
    private void execConsoleCommands() {
        if (consoleCommands == null) return;
        
        Server server = player.getServer();
        ConsoleCommandSender consoleSender = server.getConsoleSender();
        
        for (int i = 0; i < consoleCommands.size(); i++) {
            String s = consoleCommands.get(i);
            if (s.startsWith("/")) s = s.substring(1);
            logger.info("Command issued by console during processing: /" + s);
            server.dispatchCommand(consoleSender, s);
        }
    }
    
    private void doSubs(List<String> list) {
        if (list == null || list.size() == 0) return;
        
        StringBuffer outBuffer = new StringBuffer(); // For output
        StringBuffer argBuffer = new StringBuffer(); // For constructing arg lists
        
        for (int i = 0; i < list.size(); i++) {
            // Re-use outBuffer
            outBuffer.setLength(0);
            
            Matcher matcher = SUB_PATTERN.matcher(list.get(i));
            while (matcher.find()) {
                String subType = matcher.group(1);
                String subValue = "";
                
                if (subType.equalsIgnoreCase("a")) {
                    argBuffer.setLength(0);
                    for (int j = 0; j < args.length; j++) {
                        if (j > 0) argBuffer.append(" ");
                        argBuffer.append(args[j]);
                    }
                    subValue = argBuffer.toString();
                }
                else if (subType.equalsIgnoreCase("d")) {
                    subValue = player.getDisplayName();
                }
                else if (subType.equalsIgnoreCase("p")) {
                    subValue = player.getName();
                }
                else {
                    int argNum = 0;
                    
                    boolean isPlus = subType.charAt(subType.length()-1) == '+';
                    if (isPlus) {
                        subType = subType.substring(0, subType.length()-1);
                    }
                    
                    try {
                        argNum = Integer.parseInt(subType);
                    }
                    catch (NumberFormatException e) {} // This shouldn't happen as long as the regexp is valid.
                    
                    if (isPlus) {
                        argBuffer.setLength(0);
                        for (int j = argNum - 1; j < args.length; j++) {
                            if (j >= argNum) argBuffer.append(" ");
                            argBuffer.append(args[j]);
                        }
                        subValue = argBuffer.toString();
                    }
                    else {
                        try {
                            subValue = args[argNum - 1];
                        }
                        catch (ArrayIndexOutOfBoundsException e) {} // Leave subValue blank, the required arguments check will notify the user.
                    }
                    
                    //logger.fine("'" + subType + "'; " + Integer.toString(argNum) + "; '" + subValue + "'");
                    
                    if (argNum > reqArgs) {
                        reqArgs = argNum;
                    }
                }
                
                matcher.appendReplacement(outBuffer, subValue);
            }
            
            matcher.appendTail(outBuffer);
            list.set(i, outBuffer.toString());
        }
    }
}
