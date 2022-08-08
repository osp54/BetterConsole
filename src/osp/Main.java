package osp;

import arc.Core;
import arc.files.Fi;
import arc.util.CommandHandler;
import arc.util.CommandHandler.*;
import arc.util.Log;
import arc.util.Strings;
import static arc.util.ColorCodes.*;
import static arc.util.Log.format;
import static arc.util.Log.formatColors;

import mindustry.mod.*;
import mindustry.net.Administration;
import mindustry.server.ServerControl;

import org.jline.reader.*;
import org.jline.terminal.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Main extends Plugin{
    private ServerControl serverControl;
    private CommandHandler handler;
    private String suggested;
    private Fi currentLogFile;
    private Terminal terminal;
    private LineReader lineReader;

    protected static String[] tags = {"&lc&fb[D]&fr", "&lb&fb[I]&fr", "&ly&fb[W]&fr", "&lr&fb[E]", ""};
    protected static DateTimeFormatter dateTime = DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss");
    public final Fi logFolder = Core.settings.getDataDirectory().child("logs/");
    private static final int maxLogLength = 1024 * 1024 * 5;
    @Override
    public void init() {
        serverControl = (ServerControl) Core.app.getListeners().find(listener -> listener instanceof ServerControl);
        handler = serverControl.handler;

        try {
            terminal = TerminalBuilder.builder().system(true).build();
            lineReader = LineReaderBuilder
                    .builder()
                    .terminal(terminal)
                    .build();
        } catch (Exception e){
            e.printStackTrace();
            Log.err("Console not loaded. Stopping...");
            Core.app.exit();
        }

        Log.logger = (level1, text) -> {
            //err has red text instead of reset.
            if(level1 == Log.LogLevel.err) text = text.replace(reset, lightRed + bold);

            String result = bold + lightBlack + "[" + dateTime.format(LocalDateTime.now()) + "] " + reset + format(tags[level1.ordinal()] + " " + text + "&fr");

            if(lineReader.isReading()){
                lineReader.callWidget(LineReader.CLEAR);
                lineReader.getTerminal().writer().println(result);
                lineReader.callWidget(LineReader.REDRAW_LINE);
                lineReader.callWidget(LineReader.REDISPLAY);
            }else{
                lineReader.getTerminal().writer().println(result);
            }

            if(Administration.Config.logging.bool()){
                logToFile("[" + dateTime.format(LocalDateTime.now()) + "] " + formatColors(tags[level1.ordinal()] + " " + text + "&fr", false));
            }

//            if(socketOutput != null){
//                try{
//                    socketOutput.println(formatColors(text + "&fr", false));
//                }catch(Throwable e1){
//                    err("Error occurred logging to socket: @", e1.getClass().getSimpleName());
//                }
//            }
        };
        serverControl.serverInput = () -> {
            while(true){
                try {
                    String line = lineReader.readLine("> ");
                    if(!line.isEmpty()){
                        Core.app.post(() -> handleCommandString(line));
                    }
                }catch (EndOfFileException e){
                    Core.app.exit();
                }catch (UserInterruptException e){
                    Core.app.exit();
                }catch (Exception e){
                    Log.err(e);
                    Core.app.exit();
                }
            }
        };

    }
    public void handleCommandString(String line){
        CommandHandler handler = serverControl.handler;
        CommandResponse response = handler.handleMessage(line);
        if(response.type == ResponseType.unknownCommand){
            int minDst = 0;
            Command closest = null;

            for(Command command : handler.getCommandList()){
                int dst = Strings.levenshtein(command.text, response.runCommand);
                if(dst < 3 && (closest == null || dst < minDst)){
                    minDst = dst;
                    closest = command;
                }
            }

            if(closest != null && !closest.text.equals("yes")){
                Log.err("Command not found. Did you mean \"" + closest.text + "\"?");
                suggested = line.replace(response.runCommand, closest.text);
            }else{
                Log.err("Invalid command. Type 'help' for help.");
            }
        }else if(response.type == ResponseType.fewArguments){
            Log.err("Too few command arguments. Usage: " + response.command.text + " " + response.command.paramText);
        }else if(response.type == ResponseType.manyArguments){
            Log.err("Too many command arguments. Usage: " + response.command.text + " " + response.command.paramText);
        }else if(response.type == ResponseType.valid){
            suggested = null;
        }
    }
    private void logToFile(String text){
        if(currentLogFile != null && currentLogFile.length() > maxLogLength){
            currentLogFile.writeString("[End of log file. Date: " + dateTime.format(LocalDateTime.now()) + "]\n", true);
            currentLogFile = null;
        }

        for(String value : values){
            text = text.replace(value, "");
        }

        if(currentLogFile == null){
            int i = 0;
            while(logFolder.child("log-" + i + ".txt").length() >= maxLogLength){
                i++;
            }

            currentLogFile = logFolder.child("log-" + i + ".txt");
        }

        currentLogFile.writeString(text + "\n", true);
    }

}