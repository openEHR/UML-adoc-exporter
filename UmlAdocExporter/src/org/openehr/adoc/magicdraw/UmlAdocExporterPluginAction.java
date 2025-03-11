package org.openehr.adoc.magicdraw;

import com.nomagic.magicdraw.actions.MDAction;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.ui.dialogs.MDDialogParentProvider;

import javax.annotation.CheckForNull;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Plug-in entry point (i.e. invoked from running UI tool) for UML extractor
 *
 * @author Bostjan Lah
 */
class UmlAdocExporterPluginAction extends MDAction {
    private static final long serialVersionUID = 1L;

    UmlAdocExporterPluginAction(@CheckForNull String id, String name) {
        super(id, name, null, null);
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
     */
    @SuppressWarnings("OverlyBroadCatchBlock")
    @Override
    public void actionPerformed(ActionEvent e) {
        File outputFolder = chooseFolder();
        if (outputFolder != null) {
            try {
                UmlAdocExporter exporter = new UmlAdocExporter(
                        3,
                        "openehr",
                        4,
                        new HashSet<>(),
                        "",
                        false,
                        null,
                        new HashMap<>());
                exporter.exportProject(outputFolder, Application.getInstance().getProject());

                JOptionPane.showMessageDialog(MDDialogParentProvider.getProvider().getDialogOwner(), "Export complete.", "Export",
                                              JOptionPane.INFORMATION_MESSAGE);
            }
            catch (Exception ex) {
                JOptionPane.showMessageDialog(MDDialogParentProvider.getProvider().getDialogOwner(), "Unable to export data: " + ex.getMessage());
            }
        }
    }

    private File chooseFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(null);
        chooser.setDialogTitle("Select Export Folder");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);

        File chosen;
        if (chooser.showDialog(MDDialogParentProvider.getProvider().getDialogOwner(), "OK") == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile();
            chosen = !Files.exists(selectedFile.toPath()) && Files.exists(selectedFile.toPath().getParent())
                    ? selectedFile.toPath().getParent().toFile()
                    : selectedFile.toPath().toFile();
        }
        else
            chosen = null;

        return chosen;
    }
}
