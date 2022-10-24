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

    // Asciidoctor document heading level to generate for class texts
    private int headingLevel;

    // depth to build package name qualifiers for classes. Avoids having to use very
    // deep package hierarchies as qualifiers. Class qualifiers are used
    // a) as the key for classes in global hashmap for internal comparisons
    // b) to build filenames for writing out classes to.
    private int packageDepth = 4;

    // root package name to filter on - avoid packages not under this root
    private String rootPackageName = UmlExporterDefinitions.ROOT_PACKAGE_NAME_DEFAULT;

    // List of package names under root representing 'components'
    private final Set<String> componentPackageNames = new HashSet<>();

    // String pattern to insert that represents version id of a component, so that later
    // Asciidoctor substitution of version name will work
    private String specReleaseVarPattern = UmlExporterDefinitions.SPEC_RELEASE_PATTERN_DEFAULT;

    private Map<String, Integer> defaultImageFormats = Stream.of(new Object[][] {
            { "svg", ImageExporter.SVG },
            { "png", ImageExporter.PNG }
    }).collect(Collectors.toMap(data -> (String) data[0], data -> (Integer) data[1]));

    private Map<String, Integer> imageFormats;

    private boolean qualifiedClassNames;

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

        UmlAdocExporter exporter = new UmlAdocExporter (
                headingLevel,
                rootPackageName,
                packageDepth,
                componentPackageNames,
                qualifiedClassNames,
                specReleaseVarPattern,
                imageFormats != null? imageFormats : defaultImageFormats);
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
                case "-c":  // Component names - packages under root package to include
                    componentPackageNames.addAll (Pattern.compile(",").splitAsStream (getParameterValue (iterator, "-c")).collect(Collectors.toList()));
                    break;

                case "-d":  // Asciidoctor only: Diagram file formats
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

                case "-l":  // Asciidoctor only: heading level
                    String level = getParameterValue(iterator, "-l");
                    try {
                        headingLevel = Integer.valueOf(level);
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
                    qualifiedClassNames = true;
                    break;

                case "-p":  // UML package depth to guarantee uniquely qualified classes
                    String depth = getParameterValue(iterator, "-p");
                    try {
                        packageDepth = Integer.valueOf(depth);
                    } catch (NumberFormatException ignored) {
                        throw new UmlAdocExporterException("Invalid argument for -p: " + depth + " (expected numeric)!");
                    }
                    break;

                case "-r":  // Root package
                    rootPackageName = getParameterValue (iterator, "-r");
                    break;

                case "-i":  // Asciidoctor only: specification release variable pattern to insert in links
                    specReleaseVarPattern = getParameterValue (iterator, "-i");
                    break;

                case "-?":
                case "-h":
                    System.out.println("Usage: uml_generate [-c component_pkg_names] [-d image_formats] [-o output_folder] [-l heading_level]  [-p uml_pkg_depth] [-q] [-r root_package_name] [-i index_release] <project file>");
                    System.out.println("       -c: component package name(s) under root package to export (comma-separated)");
                    System.out.println("       -d: image format: " + join("|", defaultImageFormats.keySet()) + " (default = all)");
                    System.out.println("       -o: output folder (default = current folder)");
                    System.out.println("       -l: class headings level (default = 3)");
                    System.out.println("       -p: UML package depth for uniqueness (default = 4)");
                    System.out.println("       -q: if set, use package-qualified class-names in output files");
                    System.out.println("       -r: root package name to export (default = openehr)");
                    System.out.println("       -i: pass asciidoctor release var name pattern, e.g. '{%s_release}'");
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
