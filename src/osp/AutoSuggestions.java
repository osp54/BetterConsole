package osp;

import arc.util.CommandHandler.CommandParam;
import org.jline.console.ArgDesc;
import org.jline.console.CmdDesc;

import java.util.*;

public class AutoSuggestions {
    public static Map<String, CmdDesc> get() {
        Map<String, CmdDesc> tailTips = new HashMap<>();
        Main.handler.getCommandList().forEach(command -> {
            List<String> params = new ArrayList<>();
            for (int i = 0; i < command.params.length; i++) {
                CommandParam param = command.params[i];
                params.add(param.optional ? "[" + param.name + "]" : "<" + param.name + ">");
            }
            tailTips.put(command.text, new CmdDesc(ArgDesc.doArgNames(params)));
        });
        return tailTips;
    }

}
