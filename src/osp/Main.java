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
import org.jline.reader.impl.completer.StringsCompleter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Main extends Plugin{
    private ServerControl serverControl;
    private CommandHandler handler;
    private Fi currentLogFile;
    private Terminal terminal;
    private LineReader lineReader;
    // анюковские переменные которые приватны в serverControl
    protected static String[] tags = {"&lc&fb[D]&fr", "&lb&fb[I]&fr", "&ly&fb[W]&fr", "&lr&fb[E]", ""};
    protected static DateTimeFormatter dateTime = DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss");
    public final Fi logFolder = Core.settings.getDataDirectory().child("logs/");
    private static final int maxLogLength = 1024 * 1024 * 5;
    @Override
    public void init() {
        // получаем серверКонтрол и хандлер комманд(если создать новый ничего не получится)
        serverControl = (ServerControl) Core.app.getListeners().find(listener -> listener instanceof ServerControl);
        handler = serverControl.handler;

        // помещаем все имена команд в список для автодополнения команды через tab(работает костыльно, нужно доработать)
        List<String> cmds = new ArrayList();
        handler.getCommandList().forEach(cmd -> {
            cmds.add(cmd.text);
        });
        // создание терминала
        try {
            terminal = TerminalBuilder.builder().jna(true).system(true).build();
            lineReader = LineReaderBuilder
                    .builder()
                    .completer(new StringsCompleter(cmds))
                    .terminal(terminal)
                    .build();
        } catch (Exception e){
            Log.err(e);
            Core.app.exit();
        }
        // Это нужно для того чтобы логи не мешали вводу
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
    // анюковские методы которые нельзя получить из ServerControl т.к они приватны
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
            }else{
                Log.err("Invalid command. Type 'help' for help.");
            }
        }else if(response.type == ResponseType.fewArguments){
            Log.err("Too few command arguments. Usage: " + response.command.text + " " + response.command.paramText);
        }else if(response.type == ResponseType.manyArguments){
            Log.err("Too many command arguments. Usage: " + response.command.text + " " + response.command.paramText);}
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
