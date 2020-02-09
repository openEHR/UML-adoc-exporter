package org.openehr.adoc.magicdraw;

import com.nomagic.magicdraw.commandline.CommandLine;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.core.project.ProjectDescriptor;
import com.nomagic.magicdraw.core.project.ProjectDescriptorsFactory;
import com.nomagic.magicdraw.core.project.ProjectsManager;
import org.openehr.adoc.magicdraw.exception.UmlAdocExporterException;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Bostjan Lah
 */
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class UmlAdocExporterCommandLine extends CommandLine {
    private int headingLevel;
    private final Set<String> rootPackageName = new HashSet<>();
    private String indexRelease;
    private File projectFile;
    private File outFolder;
    private boolean helpOnly;

    @Override
    protected byte execute() {
        ProjectDescriptor projectDescriptor = ProjectDescriptorsFactory.createProjectDescriptor(projectFile.toURI());
        if (projectDescriptor == null) {
            throw new UmlAdocExporterException("Project descriptor not created for " + projectFile.getAbsolutePath() + '!');
        }
        ProjectsManager projectsManager = Application.getInstance().getProjectsManager();
        projectsManager.loadProject(projectDescriptor, true);
        Project project = projectsManager.getActiveProject();

        UmlAdocExporter exporter = new UmlAdocExporter(headingLevel, rootPackageName, indexRelease);
        try {
            exporter.exportProject(outFolder, project);
            return (byte)0;
        } catch (Exception e) {
            throw new UmlAdocExporterException("Export failed: " + e.getMessage(), e);
        }
    }

    @Override
    @SuppressWarnings({"OverlyComplexMethod", "SwitchStatementDensity"})
    protected void parseArgs(String[] cmdLineArgs) {
        for (Iterator<String> iterator = Arrays.asList(cmdLineArgs).iterator(); iterator.hasNext(); ) {
            String arg = iterator.next();
            switch (arg) {
                case "-l":
                    String level = getParameterValue(iterator, "-l");
                    try {
                        headingLevel = Integer.valueOf(level);
                    } catch (NumberFormatException ignored) {
                        throw new UmlAdocExporterException("Invalid argument for -l: " + level + " (expected numeric)!");
                    }
                    break;
                case "-o":
                    String outputFolder = getParameterValue(iterator, "-o");
                    Path outputPath = Paths.get(outputFolder);
                    if (!Files.isDirectory(outputPath)) {
                        throw new UmlAdocExporterException("Output folder " + outputFolder + " doesn't exist!");
                    }
                    outFolder = outputPath.toFile();
                    break;
                case "-r":
                    rootPackageName.addAll(Pattern.compile(",").splitAsStream(getParameterValue(iterator, "-r")).collect(Collectors.toList()));
                    break;
                case "-i":
                    indexRelease = getParameterValue(iterator, "-i");
                    break;
                case "-?":
                case "-h":
                    System.out.println("Usage: uml_generate [-o output_folder] [-l heading_level] [-r root_package_name] [-i index_release] <project file>");
                    System.out.println("       -o: output folder (default = current folder)");
                    System.out.println("       -l: class headings level (default = 3)");
                    System.out.println("       -r: root package name to export (default = openehr)");
                    System.out.println("       -i: generate an index against a specific release, for example Release-1.0.3");
                    helpOnly = true;
                    break;
                default:
                    Path projectPath = Paths.get(arg);
                    if (!Files.isReadable(projectPath)) {
                        throw new UmlAdocExporterException("Project file " + arg + " doesn't exist!");
                    }
                    projectFile = projectPath.toFile();
            }
        }
        if (!helpOnly) {
            if (projectFile == null) {
                throw new UmlAdocExporterException("No project file specified!");
            }
            if (headingLevel <= 0) {
                headingLevel = 3;
            }
            if (outFolder == null) {
                outFolder = new File(".");
            }
            if (rootPackageName.isEmpty()) {
                rootPackageName.add("openehr");
            }
        }
    }

    private String getParameterValue(Iterator<String> iterator, String param) {
        if (iterator.hasNext()) {
            return iterator.next();
        } else {
            throw new UmlAdocExporterException("Missing parameter for " + param + '!');
        }
    }
}
