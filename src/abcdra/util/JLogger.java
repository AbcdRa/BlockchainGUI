package abcdra.util;

import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;

//TODO Добавить цвета
public class JLogger {
    private final Document document;

    private final JTextPane textPane;
    private final int limit;
    private final AttributeSet attributeSet;



    public JLogger(JTextPane textPane) {
        this.textPane = textPane;
        this.document = textPane.getDocument();
        this.limit = 3000;
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
