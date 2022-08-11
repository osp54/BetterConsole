package osp;

import org.jline.reader.Highlighter;
import org.jline.reader.LineReader;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import java.util.regex.Pattern;
import static osp.Main.commandList;

public class CommandHighlighter implements Highlighter {
    private String command;
    @Override
    public AttributedString highlight(LineReader reader, String buffer) {
        AttributedStringBuilder asb = new AttributedStringBuilder();
        asb.style(new AttributedStyle().foreground(AttributedStyle.RED));
        //int idx = buffer.indexOf(" ");
        try {
            command = buffer.split(" ")[0];
        } catch (Exception e) {
            command = buffer;
        }
        if (commandList.contains(command)) {
            asb.style(new AttributedStyle().foreground(AttributedStyle.GREEN));
            asb.append(buffer);
        //} else if (idx > 0) {
            //asb.append(buffer.substring(0, idx));
            //asb.style(AttributedStyle.DEFAULT);
            //asb.append(buffer.substring(idx));
        } else {
            asb.style(new AttributedStyle().foreground(AttributedStyle.RED));
            asb.append(buffer);
        }
        return asb.toAttributedString();
    }

    @Override
    public void setErrorPattern(Pattern errorPattern) {}

    @Override
    public void setErrorIndex(int errorIndex) {}
}