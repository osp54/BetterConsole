package osp;

import arc.Core;
import arc.util.CommandHandler;
import arc.util.Log;
import arc.util.Reflect;

import static arc.util.ColorCodes.*;
import static arc.util.Log.format;
import static arc.util.Log.formatColors;

import mindustry.mod.*;
import mindustry.net.Administration;
import mindustry.server.ServerControl;

import org.jline.reader.*;
import org.jline.terminal.*;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.widget.TailTipWidgets;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Main extends Plugin{
    public ServerControl serverControl;
    public static CommandHandler handler;
    public static List<String> commandList = new ArrayList<>();
    private Method logToFile;
    private Terminal terminal;
    private LineReader lineReader;
    private String[] tags;
    private DateTimeFormatter dateTime;
    @Override
    public void init() {
        // получаем серверКонтрол и хандлер комманд(если создать новый ничего не получится)
        serverControl = (ServerControl) Core.app.getListeners().find(listener -> listener instanceof ServerControl);
        handler = serverControl.handler;

        // получаем приватные переменные из ServerControl
        try {
            logToFile = serverControl.getClass().getDeclaredMethod("logToFile", String.class);
            logToFile.setAccessible(true);

            dateTime = Reflect.get(ServerControl.class, "dateTime");
            tags = Reflect.get(ServerControl.class, "tags");
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        // помещаем все имена команд в список для автодополнения команды через tab(работает костыльно, нужно доработать)
        handler.getCommandList().forEach(command -> {
            commandList.add(command.text);
        });
        // создание терминала
        try {
            terminal = TerminalBuilder.builder().jna(true).system(true).build();
            lineReader = LineReaderBuilder
                    .builder()
                    .completer(new StringsCompleter(commandList))
                    .highlighter(new CommandHighlighter())
                    .terminal(terminal)
                    .build();
            new TailTipWidgets(lineReader, AutoSuggestions.get(), 0, TailTipWidgets.TipType.TAIL_TIP).enable();
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
                try {
                    logToFile.invoke(serverControl,"[" + dateTime.format(LocalDateTime.now()) + "] " + formatColors(tags[level1.ordinal()] + " " + text + "&fr", false));
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        };
        serverControl.serverInput = () -> {
            while(true){
                try {
                    String line = lineReader.readLine("> ");
                    if (!line.isEmpty()) {
                        Core.app.post(() -> serverControl.handleCommandString(line));
                    }
                } catch (UserInterruptException e) {
                    Core.app.exit();
                } catch (EndOfFileException e) {
                    Core.app.exit();
                } catch (Exception e) {
                    Log.err(e);
                    Core.app.exit();
                }
            }
        };
    }
}
