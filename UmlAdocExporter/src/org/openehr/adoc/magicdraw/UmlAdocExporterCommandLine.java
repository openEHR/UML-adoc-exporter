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
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.lang.String.join;

/**
 * Command-line entry point for UML extractor
 * @author Bostjan Lah
 */
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class UmlAdocExporterCommandLine extends CommandLine {

    private static final UmlExportConfig exportConfig = UmlExportConfig.getInstance();
    private File projectFile;
    private File outFolder;
    private boolean helpOnly;

    public static void main(String[] args) throws InstantiationException
    {
        new UmlAdocExporterCommandLine().launch(args);
    }

    protected byte execute() {
        ProjectDescriptor projectDescriptor = ProjectDescriptorsFactory.createProjectDescriptor(projectFile.toURI());
        if (projectDescriptor == null) {
            throw new UmlAdocExporterException("Project descriptor not created for " + projectFile.getAbsolutePath() + '!');
        }
        ProjectsManager projectsManager = Application.getInstance().getProjectsManager();
        projectsManager.loadProject(projectDescriptor, true);
        Project project = projectsManager.getActiveProject();

        UmlAdocExporter exporter = new UmlAdocExporter ();
        try {
            exporter.exportProject(outFolder, project);
            return (byte)0;
        } catch (Exception e) {
            throw new UmlAdocExporterException("Export failed: " + e.getMessage(), e);
        }
    }

    protected void parseArgs (String[] cmdLineArgs) {
        for (Iterator<String> iterator = Arrays.asList(cmdLineArgs).iterator(); iterator.hasNext(); ) {
            String arg = iterator.next();
            switch (arg) {
                case "-c":  // short component name(s) - packages under root package to include
                    exportConfig.getComponentPackageNames().addAll (Pattern.compile(",").splitAsStream (getParameterValue (iterator, "-c")).collect(Collectors.toList()));
                    break;

                case "-P":  // component name prefix to use in links
                    exportConfig.setComponentPackageNamePrefix(getParameterValue (iterator, "-P"));
                    break;

                case "-d":  // Asciidoctor only: Diagram file formats
                    String imageFormat = getParameterValue(iterator, "-d").toLowerCase();
                    if (!UmlExporterDefinitions.defaultImageFormats.containsKey(imageFormat)) {
                        throw new UmlAdocExporterException("Invalid argument for -d: " + imageFormat + " (expected one of " +
                                join("|", UmlExporterDefinitions.defaultImageFormats.keySet()) + "!");
                    }
                    else {
                        exportConfig.getImageFormats().put(imageFormat, UmlExporterDefinitions.defaultImageFormats.get(imageFormat));
                    }
                    break;

                case "-k":  // link template
                    exportConfig.setSpecLinkTemplate(getParameterValue(iterator, "-k"));
                    break;

                case "-l":  // Asciidoctor only: heading level
                    String level = getParameterValue(iterator, "-l");
                    try {
                        exportConfig.setHeadingLevel(Integer.parseInt(level));
                    } catch (NumberFormatException ignored) {
                        throw new UmlAdocExporterException("Invalid argument for -l: " + level + " (expected numeric)!");
                    }
                    break;

                case "-o":  // Output folder
                    String outputFolder = getParameterValue(iterator, "-o");
                    Path outputPath = Paths.get(outputFolder);
                    if (!Files.isDirectory(outputPath))
                        throw new UmlAdocExporterException("Output folder " + outputFolder + " doesn't exist!");
                    outFolder = outputPath.toFile();
                    break;

                case "-q":  // flag to include class-name qualifiers in class file names, i.e. pkg.pkg.class_name.ext
                    exportConfig.setQualifiedClassNames(true);
                    break;

                case "-p":  // UML package depth to guarantee uniquely qualified classes
                    String depth = getParameterValue(iterator, "-p");
                    try {
                        exportConfig.setPackageDepth(Integer.parseInt(depth));
                    } catch (NumberFormatException ignored) {
                        throw new UmlAdocExporterException("Invalid argument for -p: " + depth + " (expected numeric)!");
                    }
                    break;

                case "-r":  // Root package
                    exportConfig.setRootPackageName (getParameterValue (iterator, "-r"));
                    break;

                case "-?":
                case "-h":
                    System.out.println("Usage: uml_generate [-c component_pkg_names] [-P link_component_prefix] [-k link_template] [-d image_formats] [-o output_folder] [-l heading_level]  [-p uml_pkg_depth] [-q] [-r root_package_name] [-i index_release] <project file>");
                    System.out.println("       -c: component package name(s) under root package to export (comma-separated)");
                    System.out.println("       -P: component name prefix to use in links");
                    System.out.println("       -d: image format: " + join("|", UmlExporterDefinitions.defaultImageFormatNames()) + " (default = all)");
                    System.out.println("       -k: spec URL template (default = \"" + UmlExporterDefinitions.DEFAULT_SPEC_LINK_TEMPLATE + "\"");
                    System.out.println("       -o: output folder (default = current folder)");
                    System.out.println("       -l: class headings level (default = 3)");
                    System.out.println("       -p: UML package depth for uniqueness (default = " + exportConfig.getPackageDepth() + ")");
                    System.out.println("       -q: if set, use package-qualified class-names in output files");
                    System.out.println("       -r: root package name to export (default = openehr)");
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
            if (exportConfig.getHeadingLevel() <= 0)
                exportConfig.setHeadingLevel(3);
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
