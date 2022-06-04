package abcdra.app;

import abcdra.blockchain.Blockchain;
import abcdra.util.CryptUtil;
import org.codehaus.jackson.map.ObjectMapper;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

//TODO Нужно сильно обрафакторить этот класс, слишком много не учтено


public class AppConfig {
    public static String default_path = "data/blockchain_conf.json";
    private static Map<String, String> parsed;

    public static Blockchain safeBlockchainInit() {
        try {
            return new Blockchain(default_path);
        } catch (IOException exception) {
            JOptionPane.showMessageDialog(null, "Конфигурационный файл не найден");

            try {
                File newConfigFile = findConfigFile();
                if(newConfigFile == null) return null;
                return new Blockchain(newConfigFile.getPath());
            } catch (IOException e) {
                return manualInit();
            }
        }
    }

    private static File findConfigFile() {
        JFileChooser fileChooser = new JFileChooser(System.getProperty("user.dir"));
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "Json", "json");
        fileChooser.setFileFilter(filter);
        int response = fileChooser.showOpenDialog(null);
        if(JFileChooser.APPROVE_OPTION == response) {
            return fileChooser.getSelectedFile();
        }
        else {
            String[] option = new String[]{"Выбрать другой файл", "Создать вручную", "Создать автоматически"};
            int chosen = JOptionPane.showOptionDialog(null,"Что делать дальше ?",
                    "Проблемы ?", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
                    null, option, option[0]);
            if(chosen==0) {
                return findConfigFile();
            }
            if(chosen==1) {
                return createConfigFile();
            }
            if(chosen==2) {
                try {
                    File newFile = new File(default_path);
                    newFile.createNewFile();
                    defaultFillConfigFile(newFile);
                    return newFile;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return null;
    }

    private static File createConfigFile() {
        JFileChooser fileChooser = new JFileChooser(System.getProperty("user.dir"));
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "Json", "json");
        fileChooser.setFileFilter(filter);
        int response = fileChooser.showSaveDialog(null);
        if(JFileChooser.APPROVE_OPTION == response) {
            File configFile = fileChooser.getSelectedFile();
            try {
                if(manualFillConfigFie(configFile)) return configFile;
            } catch (IOException e) {
                defaultFillConfigFile(configFile);
                return configFile;
            }
        }
        return null;
    }

    private static void defaultFillConfigFile(File file) {
        String currentPath = System.getProperty("user.dir");
        System.out.println(currentPath);

        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> configMap = new HashMap<>();

        configMap.put("BLOCKCHAIN_PATH", currentPath+"\\blockchain.json");

        configMap.put("MEMPOOL_PATH", currentPath+"\\mempool");

        configMap.put("NODES_IP", currentPath+"\\nodes_ip.ini");
        String json;
        try {
            parsed = configMap;
            json = mapper.writeValueAsString(configMap);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        CryptUtil.writeStringToFile(file, json);
    }

    private static boolean manualFillConfigFie(File file) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> configMap = new HashMap<>();
        JFileChooser fileChooser = new JFileChooser(System.getProperty("user.dir"));
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setDialogTitle("Выберите папку с блокчейном");


        int response = fileChooser.showOpenDialog(null);

        if(JFileChooser.APPROVE_OPTION != response)  return false;
        File blockchain_path = fileChooser.getSelectedFile();
        configMap.put("BLOCKCHAIN_PATH", blockchain_path.getAbsolutePath());

        fileChooser.setDialogTitle("Выберите папку с мепулом");

        response = fileChooser.showOpenDialog(null);
        if(JFileChooser.APPROVE_OPTION != response)  return false;
        File mempool_path = fileChooser.getSelectedFile();
        configMap.put("MEMPOOL_PATH", mempool_path.getAbsolutePath());

        fileChooser.setDialogTitle("Выберите файл с ip");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        response = fileChooser.showOpenDialog(null);
        if(JFileChooser.APPROVE_OPTION != response)  return false;
        File node_path = fileChooser.getSelectedFile();
        configMap.put("NODES_IP", node_path.getAbsolutePath());
        parsed = configMap;
        String json = mapper.writeValueAsString(configMap);
        CryptUtil.writeStringToFile(file, json);
        return true;
    }

    private static Blockchain manualInit() {
        return new Blockchain(parsed.get("BLOCKCHAIN_PATH"), parsed.get("MEMPOOL_PATH"), parsed.get("NODES_IP"));
    }
}
