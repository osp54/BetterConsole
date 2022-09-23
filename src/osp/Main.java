package osp;

import arc.Core;
import arc.struct.Seq;
import arc.util.CommandHandler;
import arc.util.Log;

import mindustry.mod.*;
import mindustry.server.ServerControl;

import org.jline.reader.*;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.widget.TailTipWidgets;

import static org.jline.utils.AttributedString.fromAnsi;

public class Main extends Plugin{
    public ServerControl serverControl;
    public static CommandHandler handler;
    public static Seq<String> commandList = new Seq<>();
    private LineReader lineReader;
    @Override
    public void init() {
        serverControl = (ServerControl) Core.app.getListeners().find(listener -> listener instanceof ServerControl);
        handler = serverControl.handler;

        handler.getCommandList().forEach(command -> commandList.add(command.text));

        try {
            lineReader = LineReaderBuilder
                    .builder()
                    .completer(new StringsCompleter(commandList))
                    .highlighter(new CommandHighlighter())
                    .build();
            new TailTipWidgets(lineReader, AutoSuggestions.get(), 0, TailTipWidgets.TipType.TAIL_TIP).enable();
            System.setOut(new BlockingPrintStream(string -> lineReader.printAbove(fromAnsi(string))));
        } catch (Exception e){
            Log.err(e);
            Core.app.exit();
        }
        serverControl.serverInput = () -> {
            while (true) {
                try {
                    String line = lineReader.readLine("> ");
                    if (!line.isEmpty()) {
                        Core.app.post(() -> serverControl.handleCommandString(line));
                    }
                } catch (UserInterruptException e) {
                    Core.app.exit();
                } catch (Exception e) {
                    Log.err(e);
                    Core.app.exit();
                }
            }
        };
    }
}
