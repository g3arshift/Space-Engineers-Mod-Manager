package com.gearshiftgaming.se_mod_manager;

import com.gearshiftgaming.se_mod_manager.domain.ModService;
import com.gearshiftgaming.se_mod_manager.models.utility.FileChooserAndOption;
import com.gearshiftgaming.se_mod_manager.models.Mod;
import com.gearshiftgaming.se_mod_manager.ui.ModController;
import com.gearshiftgaming.se_mod_manager.ui.ModView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

//TODO:Move to a spring boot framework. Create a startup class
public class SpaceEngineersModManager {

    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {

        final Logger log = LoggerFactory.getLogger(SpaceEngineersModManager.class);
        log.info("Started application...");
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (UnsupportedLookAndFeelException | ClassNotFoundException | InstantiationException |
                 IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        List<Mod> modList = new ArrayList<>();
        ModView modView = new ModView();
        ModService modService = new ModService();
        FileChooserAndOption fileChooserAndOption = new FileChooserAndOption();

        final String DESKTOP_PATH = System.getProperty("user.home") + "/Desktop";
        final String APP_DATA_PATH = System.getenv("APPDATA") + "/SpaceEngineers/Saves";

        ModController modController = new ModController(modList, modView, modService, fileChooserAndOption, DESKTOP_PATH, APP_DATA_PATH);

        modController.injectModList();
    }

    /*
    //TODO: Rewrite and move to the controller
    private static void saveFile(JFileChooser fc) throws IOException {

        FileNameExtensionFilter sbcFileFilter = new FileNameExtensionFilter("SBC Files", "sbc");

        FileNameExtensionFilter textFileFilter = new FileNameExtensionFilter("Text Files (.txt, .doc)", "txt", "doc");
        ModList modList = new ModList(fc.getSelectedFile());
        int result = -1;

        do {
            JOptionPane.showMessageDialog(null, "Select a location to save your mod list", "Save file", JOptionPane.INFORMATION_MESSAGE);
            fc = new JFileChooser(System.getProperty("user.home") + "/Desktop");
            fc.setFileFilter(textFileFilter);
            fc.addChoosableFileFilter(fc.getAcceptAllFileFilter());
            int option = fc.showSaveDialog(null);

            if (option == JFileChooser.APPROVE_OPTION) {
                try {
                    result = writeModList(fc, modList.getModListUrls());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        } while (result == 1); //This is equal to selecting "No" in the overwrite dialog.

        if (result == 2) { //This is equal to selecting "Cancel" in the overwrite dialog.
            JOptionPane.showMessageDialog(null, "Mod list extraction cancelled. Your files have not been modified.", "Cancelled", JOptionPane.INFORMATION_MESSAGE);
        } else if (result == JFileChooser.APPROVE_OPTION)
            JOptionPane.showMessageDialog(null, "Mod list saved!", "Save Complete", JOptionPane.INFORMATION_MESSAGE);
    }

    //TODO: Rewrite and move to controller
    private static int writeModList(JFileChooser fc, String completedSandboxFile) throws IOException {
        String savePath;

        if (!fc.getSelectedFile().toPath().toString().contains(".")) {
            savePath = fc.getSelectedFile().toPath() + ".txt";
        } else
            savePath = fc.getSelectedFile().toPath().toString();


        File file = new File(savePath);

        if (!file.exists()) {
            boolean fileWriteResult = file.createNewFile();
            if (!fileWriteResult)
                JOptionPane.showMessageDialog(null, "Mod list extraction failed. Your files have not been modified.", "File write error", JOptionPane.WARNING_MESSAGE);
        } else {
            int option = JOptionPane.showOptionDialog(null, "File already exists! Overwrite?", "File exists", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null, null, "No");
            if (option != JFileChooser.APPROVE_OPTION) {
                return option;
            }
        }

        FileWriter fw = new FileWriter(file);

        BufferedWriter output = new BufferedWriter(fw);

        output.write(completedSandboxFile);

        output.flush();
        output.close();
        return 0;
    }
     */
}