package org.openehr.adoc.magicdraw;

import com.nomagic.magicdraw.commandline.CommandLine;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.core.project.ProjectDescriptor;
import com.nomagic.magicdraw.core.project.ProjectDescriptorsFactory;
import com.nomagic.magicdraw.core.project.ProjectsManager;
import com.nomagic.magicdraw.export.image.ImageExporter;
import org.openehr.adoc.magicdraw.exception.UmlAdocExporterException;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.join;

/**
 * Command-line entry point for UML extractor
 * @author Bostjan Lah
 */
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class UmlAdocExporterCommandLine extends CommandLine {
    private int headingLevel;
    private String rootPackageName = "openehr";
    private final Set<String> componentPackageNames = new HashSet<>();
    private String specRelease;

    // If not set, output PNG and SVG; else must be set to either "svg" or "png"
    private String imageFormat;

    private Map<String, Integer> defaultImageFormats = Stream.of(new Object[][] {
            { "svg", ImageExporter.SVG },
            { "png", ImageExporter.PNG }
    }).collect(Collectors.toMap(data -> (String) data[0], data -> (Integer) data[1]));

    private Map<String, Integer> imageFormats;

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

        UmlAdocExporter exporter = new UmlAdocExporter (headingLevel, rootPackageName, componentPackageNames, specRelease, imageFormats != null? imageFormats : defaultImageFormats);
        try {
            exporter.exportProject(outFolder, project);
            return (byte)0;
        } catch (Exception e) {
            throw new UmlAdocExporterException("Export failed: " + e.getMessage(), e);
        }
    }

    @Override
    @SuppressWarnings({"OverlyComplexMethod", "SwitchStatementDensity"})
    protected void parseArgs (String[] cmdLineArgs) {
        for (Iterator<String> iterator = Arrays.asList(cmdLineArgs).iterator(); iterator.hasNext(); ) {
            String arg = iterator.next();
            switch (arg) {
                case "-c":
                    componentPackageNames.addAll (Pattern.compile(",").splitAsStream (getParameterValue (iterator, "-c")).collect(Collectors.toList()));
                    break;

                case "-d":
                    String imageFormat = getParameterValue(iterator, "-d").toLowerCase();
                    if (!defaultImageFormats.containsKey(imageFormat)) {
                        throw new UmlAdocExporterException("Invalid argument for -d: " + imageFormat + " (expected one of " +
                                join("|", defaultImageFormats.keySet()) + "!");
                    }
                    else {
                        imageFormats = new HashMap<>();
                        imageFormats.put(imageFormat, defaultImageFormats.get(imageFormat));
                    }
                    break;

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
                    if (!Files.isDirectory(outputPath))
                        throw new UmlAdocExporterException("Output folder " + outputFolder + " doesn't exist!");
                    outFolder = outputPath.toFile();
                    break;

                case "-r":
                    rootPackageName = getParameterValue (iterator, "-r");
                    break;

                case "-i":
                    specRelease = getParameterValue (iterator, "-i");
                    break;

                case "-?":
                case "-h":
                    System.out.println("Usage: uml_generate [-o output_folder] [-l heading_level] [-r root_package_name] [-i index_release] <project file>");
                    System.out.println("       -c: component package name(s) under root package to export (comma-separated)");
                    System.out.println("       -d: image format: " + join("|", defaultImageFormats.keySet()) + " (default = all)");
                    System.out.println("       -o: output folder (default = current folder)");
                    System.out.println("       -l: class headings level (default = 3)");
                    System.out.println("       -r: root package name to export (default = openehr)");
                    System.out.println("       -i: generate an index against a specific release, for example Release-1.0.3");
                    helpOnly = true;
                    break;

                default:
                    Path projectPath = Paths.get(arg);
                    if (!Files.isReadable(projectPath))
                        throw new UmlAdocExporterException ("Project file " + arg + " doesn't exist!");
                    projectFile = projectPath.toFile();
            }
        }
        if (!helpOnly) {
            if (projectFile == null)
                throw new UmlAdocExporterException ("No project file specified!");
            if (headingLevel <= 0)
                headingLevel = 3;
            if (outFolder == null)
                outFolder = new File(".");
        }
    }

    private String getParameterValue (Iterator<String> iterator, String param) {
        if (iterator.hasNext())
            return iterator.next();
        else
            throw new UmlAdocExporterException ("Missing parameter for " + param + '!');
    }
}
