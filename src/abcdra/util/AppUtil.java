package abcdra.util;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Vector;

public class AppUtil {
    public static <T> void addToJList(JList<T> jList, T value ) {
        ListModel<T> model = jList.getModel();
        Vector<T> new_model = new Vector<>();
        for(int i=0; i < model.getSize(); i++) {
            new_model.add(model.getElementAt(i));
        }
        new_model.add(value);

        jList.setListData(new_model);
    }


    public static <T> void removeToJList(JList<T> jList, T value ) {
        ListModel<T> model = jList.getModel();
        Vector<T> new_model = new Vector<>();
        for(int i=0; i < model.getSize(); i++) {
            T element = model.getElementAt(i);
            if(element != value) new_model.add(element);
        }
        jList.setListData(new_model);
    }

    public static <T> ArrayList<T> getArrayFromJList(JList<T> jList) {
        ListModel<T> listModel = jList.getModel();
        ArrayList<T> arrayList = new ArrayList<>(listModel.getSize());
        for(int i =0; i < listModel.getSize(); i++) {
            arrayList.add(listModel.getElementAt(i));
        }
        //T[] result = new T[arrayList.size()];
        return arrayList;
    }
}
