package com.gearshiftgaming.se_mod_manager.ui;

import com.gearshiftgaming.se_mod_manager.models.utility.Result;
import com.gearshiftgaming.se_mod_manager.models.utility.ResultType;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;

public class ModManagerView {

    FileNameExtensionFilter textFileFilter = new FileNameExtensionFilter("Text Files (.txt, .doc)", "txt", "doc");
    FileNameExtensionFilter sbcFileFilter = new FileNameExtensionFilter("SBC Files", "sbc");

    public String getModListFromFile(String rootFilePath) {
        return getFilePath(rootFilePath, textFileFilter);
    }

    public String getSandboxConfigFromFile(String filePath) {
        return getFilePath(filePath, sbcFileFilter);
    }

    public String getSavePath(String filePath) {
        return getSaveLocation(filePath, sbcFileFilter);
    }

    private String getFilePath(String rootFilePath, FileNameExtensionFilter fileFilter) {
        JFileChooser fc = new JFileChooser(new File(rootFilePath));

        fc.setAcceptAllFileFilterUsed(false);
        fc.setFileFilter(fileFilter);

        int choice = fc.showOpenDialog(null);

        if (choice != JOptionPane.NO_OPTION) {
            return fc.getSelectedFile().toPath().toString();
        } else return "1";
    }

    private String getSaveLocation(String filePath, FileNameExtensionFilter fileFilter) {
        JFileChooser fc = new JFileChooser(filePath);

        fc.setAcceptAllFileFilterUsed(true);
        fc.setFileFilter(fileFilter);

        int choice = fc.showSaveDialog(null);

        if (choice != JOptionPane.NO_OPTION) {
            return fc.getSelectedFile().toPath().toString();
        } else return "1";
    }

    public void displayResult(Result<?> result) {
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

    public int getOverwriteOption() {
        return JOptionPane.showOptionDialog(null, "File already exists! Overwrite?", "File exists", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null, null, "No");
    }

    public void displayCancellationDialog() {
        JOptionPane.showMessageDialog(null, "Mod list injection cancelled. Your files have not been modified.", "Notification", JOptionPane.INFORMATION_MESSAGE);
    }

    public void displayWelcomeDialog() {
        JOptionPane.showMessageDialog(null, "Select a mod list to import. This should be a list of steam workshop urls with each url on its own line.", "Notification", JOptionPane.INFORMATION_MESSAGE);
    }

    public void displaySandboxInjectDialog() {
        JOptionPane.showMessageDialog(null, "Select a Sandbox_config.sbc to inject the mod list into.", "Notification", JOptionPane.INFORMATION_MESSAGE);
    }

    public void displaySaveLocationDialog() {
        JOptionPane.showMessageDialog(null, "Select a location to save the modified Sandbox_config file to.", "Save as", JOptionPane.INFORMATION_MESSAGE);
    }

    public int getConnectionErrorOption() {
        return JOptionPane.showOptionDialog(null, "Cannot connect to the Steam Workshop. This may cause unexpected behavior. Continue anyways?", "Workshop Error", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, null, "No");
    }

    public void displayOverwriteAbortDialog() {
        JOptionPane.showMessageDialog(null, "Sandbox_config.sbc saving aborted. Please select another file.", "Save aborted", JOptionPane.INFORMATION_MESSAGE);
    }

    public int getBadModListOverrideDialog() {
        Object[] options = {"Continue",
                "Close",
                "Clean"};

        return JOptionPane.showOptionDialog(null, "Entries were found in the selected mod list that were not mods. These will not load properly into a save, and will have \"_NOT_A_MOD\" appended to their name in the mod list. Do you wish to continue without cleaning, close the application, or attempt to clean the mod list of non-mod entries?", "Mod list error", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[1]);
    }
}
