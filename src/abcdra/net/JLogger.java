package abcdra.net;

import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;

public class JLogger {
    private final Document document;
    private final boolean isAppend;
    private JTextPane textPane;
    private int limit;
    private AttributeSet attributeSet;



    public JLogger(JTextPane textPane) {
        this.textPane = textPane;
        this.document = textPane.getDocument();
        this.isAppend = true;
        this.limit = 1000;
        this.attributeSet = new SimpleAttributeSet();
        textPane.setEditable( false );
    }

    public void write(String str) {

        try {
            if(document.getLength() < limit) document.insertString(document.getLength(), str+"\n", attributeSet);
            if (document.getLength() >= limit) document.remove(0, document.getLength()-str.length());
            textPane.setCaretPosition(document.getLength());
        } catch (BadLocationException e) {
            throw new RuntimeException(e);
        }


    }
}
