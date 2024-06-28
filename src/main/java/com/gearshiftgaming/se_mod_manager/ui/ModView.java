package com.gearshiftgaming.se_mod_manager.ui;

import com.gearshiftgaming.se_mod_manager.models.utility.FileChooserAndOption;
import com.gearshiftgaming.se_mod_manager.models.utility.Result;
import com.gearshiftgaming.se_mod_manager.models.utility.ResultType;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;

public class ModView {

    FileNameExtensionFilter textFileFilter = new FileNameExtensionFilter("Text Files (.txt, .doc)", "txt", "doc");
    FileNameExtensionFilter sbcFileFilter = new FileNameExtensionFilter("SBC Files", "sbc");

    public FileChooserAndOption getModListFromFile(String filePath) {
        return getFileChooserAndOption(filePath, textFileFilter);
    }

    public FileChooserAndOption getSandboxConfigFromFile(String filePath) {
        return getFileChooserAndOption(filePath, sbcFileFilter);
    }

    private FileChooserAndOption getFileChooserAndOption(String filePath, FileNameExtensionFilter sbcFileFilter) {
        JFileChooser fc = new JFileChooser(filePath);

        fc.setAcceptAllFileFilterUsed(false);
        fc.setFileFilter(sbcFileFilter);

        FileChooserAndOption fileChooserAndOption = new FileChooserAndOption();
        fileChooserAndOption.setOption(fc.showOpenDialog(null));
        fileChooserAndOption.setFc(fc);

        return fileChooserAndOption;
    }

    public void displayResult(Result<File> result) {
        switch (result.getType()) {
            case INVALID ->
                    JOptionPane.showMessageDialog(null, result.getMessages().getLast(), "Error", JOptionPane.ERROR_MESSAGE);
            case SUCCESS ->
                    JOptionPane.showMessageDialog(null, result.getMessages().getLast(), "Success", JOptionPane.INFORMATION_MESSAGE);
            default -> {
                result.addMessage("Failed to open mod list file.", ResultType.FAILED);
                JOptionPane.showMessageDialog(null, result.getMessages().getLast(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void displayCancellation() {
        JOptionPane.showMessageDialog(null, "Operation cancelled by user.", "Information", JOptionPane.INFORMATION_MESSAGE);
    }

    public void displayWelcome() {
        JOptionPane.showMessageDialog(null, "Select a mod list to import. This should be a list of steam workshop urls with each url on its own line.", "Notification", JOptionPane.INFORMATION_MESSAGE);
    }

    public void displaySandboxDialog() {
        JOptionPane.showMessageDialog(null, "Select a Sandbox_config.sbc to extract the mod list from.", "Notification", JOptionPane.INFORMATION_MESSAGE);
    }

    public void displayConnectionError() {
        JOptionPane.showMessageDialog(null, "Cannot connect to the steam workshop.", "Error", JOptionPane.ERROR_MESSAGE);
    }
}
