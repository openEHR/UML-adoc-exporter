package org.openehr.adoc.magicdraw;

import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.export.image.ImageExporter;
import com.nomagic.magicdraw.uml.Finder;
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement;
import com.nomagic.uml2.ext.magicdraw.classes.mdinterfaces.Interface;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Enumeration;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.NamedElement;
import org.openehr.adoc.magicdraw.exception.UmlAdocExporterException;
import org.openehr.adoc.magicdraw.imm.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Action which displays its name.
 *
 * @author Bostjan Lah
 */
public class UmlAdocExporter {

    private static final String ADOC_FILE_EXTENSION = ".adoc";
    private static final String DIAGRAMS_FOLDER = "diagrams";
    private static final String CLASSES_FOLDER = "classes";

    // component, release, html file, subref classname + type, description
    private static final String INDEX_LINK_FORMAT = "[.xcode]\n* %s\n";

    private final Formatter formatter = new AsciidocFormatter();
    private final UmlExportConfig exportConfig;


    // map of all ClassInfo keyed by class key
    private final Map<String, ImmClass> allEntitiesMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    private Finder.ByTypeRecursivelyFinder umlElementsFinder;
    private Finder.ByQualifiedNameFinder umlQualifiedNameFinder;

    private Project project;

    public UmlAdocExporter()
    {
        this.exportConfig = UmlExportConfig.getInstance();
    }

    /**
     * Export a UML project as a set of files.
     * @param outputFolder Directory in which to write the files.
     * @param project MD descriptor for a project.
     * @exception IOException on fail to write to file.
     */
    public void exportProject(File outputFolder, Project project) throws Exception {
        File classesFolder = new File(outputFolder, CLASSES_FOLDER);
        if (!classesFolder.exists()) {
            if (!classesFolder.mkdir()) {
                throw new UmlAdocExporterException("Unable to create folder: " + classesFolder);
            }
        }

        // Save the project reference
        this.project = project;

        // Get a Finder object
        umlElementsFinder = Finder.byTypeRecursively();
        umlQualifiedNameFinder = Finder.byQualifiedName();

        // Gather UML classes, enumerations and interfaces, run through a pipeline that does:
        // * cast to an MD class object
        // * retain only classes within the root package specified on the command line, which
        //   will generally be more than what is to be published; we do this so as to be able
        //   to generate links from those classes being published to those in other components
        // * convert to ClassInfo objects (local representation used here)
        // Then export each ClassInfo object as an output file
        ImmClassBuilder immClassBuilder = new ImmClassBuilder(formatter, this::getUMLClassByQualifiedName);

        // -------- get the UML model classes -------
        Collection<? extends Element> umlClasses = umlElementsFinder.find(
                project.getPrimaryModel(),
                new java.lang.Class[]{com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class.class},
                true);
        List<ImmClass> classes = umlClasses.stream()
                .map(e -> (com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class)e)
                .filter(c -> ! c.getName().contains("<"))// ignore classes with names simulating template type names
                .filter(this::matchesRootPackage)
                .map(immClassBuilder::build)
                .collect(Collectors.toList());

        // -------- get the UML model interfaces -------
        Collection<? extends Element> umlInterfaces = umlElementsFinder.find(
                project.getPrimaryModel(),
                new java.lang.Class[]{Interface.class},
                true);
        ImmInterfaceBuilder immInterfaceBuilder = new ImmInterfaceBuilder(formatter, this::getUMLClassByQualifiedName);
        List<ImmClass> interfaces = umlInterfaces.stream()
                .map(e -> (Interface)e)
                .filter(c -> ! c.getName().contains("<"))// ignore classes with names simulating template type names
                .filter(this::matchesRootPackage)
                .map(immInterfaceBuilder::build)
                .collect(Collectors.toList());

        // -------- get the UML model enumerations -------
        Collection<? extends Element> umlEnumerations = umlElementsFinder.find(
                project.getPrimaryModel(),
                new java.lang.Class[]{Enumeration.class},
                true);

        ImmEnumerationBuilder immEnumerationBuilder = new ImmEnumerationBuilder(formatter, this::getUMLClassByQualifiedName);
        List<ImmClass> enumerations = umlEnumerations.stream()
                .map(e -> (Enumeration)e)
                .filter(this::matchesRootPackage)
                .map(immEnumerationBuilder::build)
                .collect(Collectors.toList());

        // -------- get the UML model state machines -------
//        Collection<? extends Element> umlStateMachines = umlElementsFinder.find(
//                project.getPrimaryModel(),
//                new Class[]{com.nomagic.uml2.ext.magicdraw.classes.mdkernel.???},
//                true);

        // --------- build a global map of ClassInfo keyed by class name --------
        classes.forEach (classInfo -> {
            if (!allEntitiesMap.containsKey (classInfo.getClassKey()) || !matchesComponents (allEntitiesMap.get (classInfo.getClassKey())))
                allEntitiesMap.put (classInfo.getClassKey(), classInfo);
        });
        interfaces.forEach (classInfo -> {
            if (!allEntitiesMap.containsKey (classInfo.getClassKey()) || !matchesComponents (allEntitiesMap.get (classInfo.getClassKey())))
                allEntitiesMap.put(classInfo.getClassKey(), classInfo);
        });
        enumerations.forEach (classInfo -> {allEntitiesMap.put (classInfo.getClassKey(), classInfo);});

        // iterate through the whole lot and add an override for the spec document, if it is
        // different from the sub-package inferred from the package structure
        for (ImmClass immClass : allEntitiesMap.values())
            if (UmlExporterDefinitions.classSpecMapExceptions.containsKey (immClass.getClassPackage()))
                immClass.setSpecName (UmlExporterDefinitions.classSpecMapExceptions.get (immClass.getClassPackage()));

        // -------------------------- do the publishing ----------------------------

        // Output the entities, but only those components that were requested to publish,
        // which equates to some selection of sub-packages of the root package, or maybe all
        if (!exportConfig.getComponentPackageNames().isEmpty())
            allEntitiesMap.values()
                    .stream()
                    .filter (this::matchesComponents)
                    .forEach (ci -> exportClass (ci, classesFolder));
        else
            allEntitiesMap.values().forEach (ci -> exportClass (ci, classesFolder));

        // Generate the index file
        generateIndex (outputFolder,
                allEntitiesMap.values()
                .stream()
                .filter (this::matchesComponents)
                .collect(Collectors.toList())
        );

        // obtain and generate the diagrams
        File diagramsFolder = new File(outputFolder, DIAGRAMS_FOLDER);
        if (!diagramsFolder.exists()) {
            if (!diagramsFolder.mkdir())
                throw new UmlAdocExporterException ("Unable to create folder: " + diagramsFolder);
        }

        List<DiagramPresentationElement> diagrams = project.getDiagrams().stream()
                .filter(this::diagMatchesRootPackages)
                .collect(Collectors.toList());
        diagrams.forEach(d -> exportDiagram(diagramsFolder, d));
    }

    /**
     * Export a UML diagram in PNG and SVG format to the export folder.
     * @param outputFolder target folder on file system.
     * @param diag UML diagram representation.
     */
    private void exportDiagram(File outputFolder, DiagramPresentationElement diag) {
        // iterate over image formats
        exportConfig.getImageFormats().forEach((k,v)->doExportDiagram(k, v, outputFolder, diag));
    }

    private void doExportDiagram(String formatName, Integer formatCode, File outputFolder, DiagramPresentationElement diag) {
        String name = diag.getName();
        try {
            // iterate over image formats
            ImageExporter.export(diag, formatCode, new File(outputFolder, formatDiagramName(name) + "." + formatName));
        }
        catch (IOException e) {
            throw new UmlAdocExporterException("Unable to export diagrams for " + name + '!', e);
        }
    }


    /**
     * Export a class as an Asciidoctor (.adoc) file to the output folder on the file system.
     * @param targetFolder Directory in which to write the file.
     * @param immClass info object for the class.
     * @exception IOException on fail to write to file.
     */
    private void exportClass (ImmClass immClass, File targetFolder) {
        try (PrintWriter printWriter = new PrintWriter(Files.newBufferedWriter(
                targetFolder.toPath().resolve(fileName(exportConfig.hasQualifiedClassNames()? immClass.getQualifiedClassName().toLowerCase(): immClass.getClassName().toLowerCase()) + ADOC_FILE_EXTENSION), Charset.forName("UTF-8")))) {
            printWriter.println(formatter.heading(immClass.getClassName() + ' ' + immClass.getMetaType(), exportConfig.getHeadingLevel()));
            printWriter.println();

            printWriter.println(formatter.tableDefinition ("1,3,5"));
            printWriter.println(formatter.tableDelimiter());
            printWriter.println(formatter.tableColHeader (immClass.getMetaType(), 1));
            printWriter.println(formatter.tableColHeaderCentred (
                    (immClass.isAbstractClass()
                            ? formatter.italic(immClass.getClassTypeName() + " (abstract)")
                            : immClass.getClassTypeName()),
                    2)
            );
            printWriter.println();

            printWriter.println(formatter.tableColHeader ("Description", 1));

            printWriter.println(formatter.tableCellPassthrough (immClass.getDocumentation(), 2));
            printWriter.println();

            // inheritance parents
            if (!immClass.getQualifiedParentClassNames().isEmpty()) {
                printWriter.println (formatter.tableColHeader ("Inherit", 1));
                StringBuilder sb = new StringBuilder();
                for (String qualifiedParentClassName: immClass.getQualifiedParentClassNames())
                    sb.append (formatter.monospace (linkClassName (immClass, qualifiedParentClassName))).append(", ");

                String parentsString = "";
                // remove any trailing ", "
                if (sb.length() > 0)
                    parentsString = sb.substring(0, sb.length() - 2);
                printWriter.println (formatter.tableCell (parentsString, 2));
                printWriter.println ();
            }

            // constants
            if (!immClass.getConstants().isEmpty()) {
                printWriter.println (formatter.tableColHeader ("Constants", 1));
                printWriter.println (formatter.tableColHeaderCentred ("Signature", 1));
                printWriter.println (formatter.tableColHeaderCentred ("Meaning", 1));

                for (ImmProperty immConstant : immClass.getConstants())
                    printWriter.print (postProcess (immClass, formatFeature (immConstant.getRepresentation())));
            }

            // attributes
            if (!immClass.getAttributes().isEmpty()) {
                printWriter.println (formatter.tableColHeader ("Attributes", 1));
                printWriter.println (formatter.tableColHeaderCentred ("Signature", 1));
                printWriter.println (formatter.tableColHeaderCentred ("Meaning", 1));

                for (ImmProperty immProperty : immClass.getAttributes())
                    printWriter.print (postProcess (immClass, formatFeature (immProperty.getRepresentation())));
            }

            // operations
            if (!immClass.getOperations().isEmpty()) {
                printWriter.println (formatter.tableColHeader ("Functions", 1));
                printWriter.println (formatter.tableColHeaderCentred ("Signature", 1));
                printWriter.println (formatter.tableColHeaderCentred ("Meaning", 1));

                for (ImmOperation immOperation : immClass.getOperations())
                    printWriter.print (postProcess (immClass, formatFeature (immOperation.getRepresentation())));
            }

            // invariants
            if (!immClass.getConstraints().isEmpty())
                printWriter.print (postProcess (immClass, formatConstraints  (immClass)));

            printWriter.println(formatter.tableDelimiter());

        } catch (IOException e) {
            throw new UmlAdocExporterException(e);
        }
    }


    /**
     * Generate an HTML file containing a clickable index of Class names that contain links to the location of
     * the class within the relevant specification.
     * @param targetFolder Directory in which to write the file.
     * @param allTypes classes, interfaces, and enumerations to include in index.
     * @exception IOException on fail to write to file.
     */
    private void generateIndex(File targetFolder, List<ImmClass> allTypes) {
        Collections.sort(allTypes);

        Path targetPath = targetFolder.toPath().resolve("class_index" + ADOC_FILE_EXTENSION);
        try (PrintWriter printWriter = new PrintWriter(Files.newBufferedWriter(targetPath, Charset.forName("UTF-8")))) {
            String specComponent = "";
            String specPackage = "";
            String specSubPackage = "";

            for (ImmClass immClass : allTypes) {
                // The test for className > 2 is to avoid generic parameters like 'T', and
                // occasionally 'TT' or similar.
                if (immClass.getClassName().length() > 2) {

                    // if Component of class has changed since last iteration, output a new header line
                    if (!specComponent.equals(immClass.getSpecComponent())) {
                        printWriter.println();
                        printWriter.println(formatter.heading ("Component " + immClass.getSpecComponent(), 2));
                        specComponent = immClass.getSpecComponent();
                    }

                    // if Package of class has changed since last iteration, output a new header line
                    if (!specPackage.equals(immClass.getComponentPackage())) {
                        printWriter.println();
                        printWriter.println(formatter.heading ("Model " + immClass.getComponentPackage(), 3));
                        specPackage = immClass.getComponentPackage();
                    }

                    // if Sub-package of class has changed since last iteration, output a new header line
                    if (!specSubPackage.equals(immClass.getClassPackage())) {
                        printWriter.println();
                        printWriter.println(formatter.heading ("Package " + immClass.getClassPackage(), 4));
                        printWriter.println();
                        specSubPackage = immClass.getClassPackage();
                    }

                    // Output the class as a linked text line of the form:
                    //   [.xcode]
                    //   * link:/releases/AM/{am_release}/AOM2.html#_c_object_class[C_OBJECT^]
                    // from the sprintf template string: "[.xcode]\n* %s\n"
                    printWriter.printf(INDEX_LINK_FORMAT, formatter.externalLink(immClass.getClassName(),
                            immClass.getSpecUrlPath (exportConfig.getSpecLinkTemplate(), exportConfig.getComponentPackageNamePrefix())));
                }
            }
        } catch (IOException e) {
            throw new UmlAdocExporterException("Unable to write to " + targetPath + '!', e);
        }
    }

    private Class getUMLClassByQualifiedName (String aName) {
        return umlQualifiedNameFinder.find(project, aName);
    }

    private boolean matchesRootPackage (NamedElement namedElement) {
        return namedElement.getQualifiedName().contains(exportConfig.getRootPackageName() + "::");
    }

    // diagram names follow the pattern COMPONENT-xxxx, e.g. RM-common etc. This returns True if
    // a diagram matches any of the components being requested.
    private boolean diagMatchesRootPackages (DiagramPresentationElement diagElement) {
        return exportConfig.getComponentPackageNames().stream().anyMatch (rn -> diagElement.getName().contains(rn + "-"));
    }

    // note: returns false for empty list - need to check for empty case before using this filter
    private boolean matchesComponents (ImmClass immClass) {
        return exportConfig.getComponentPackageNames().stream().anyMatch (cn -> immClass.getSpecComponent().equalsIgnoreCase (cn));
    }

    /**
     * Convert a targetClassName like "ELEMENT" that is referenced from originClass
     * to a link. If the target is in the same package, then it's the same spec,
     * so use a local ref, else use a full external URL link
     * @param originImmClass
     * @param targetQualifiedClassName
     * @return
     */
    private String linkClassName (ImmClass originImmClass, String targetQualifiedClassName) {
        ImmClass targetImmClass = allEntitiesMap.get (targetQualifiedClassName);
        if (targetImmClass != null) {
            if (!targetImmClass.getSpecName().equals (originImmClass.getSpecName()))
                return formatter.externalLink (targetImmClass.getClassName(),
                        targetImmClass.getSpecUrlPath (exportConfig.getSpecLinkTemplate(), exportConfig.getComponentPackageNamePrefix()));
            else
                return formatter.internalRef (targetImmClass.getClassName(), targetImmClass.localRef());
        }
        else
            return targetQualifiedClassName.substring(targetQualifiedClassName.lastIndexOf(".")+1);
    }

    /**
     * Post-process a formatted String:
     * - replace "@TypeName@" with linked Typenames (removing the @@)
     */
    private String postProcess (ImmClass immClass, String classText) {
        Pattern p = Pattern.compile (UmlExporterDefinitions.TYPE_QUOTE_REGEX);
        Matcher m = p.matcher (classText);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String matched = m.group();
            m.appendReplacement(sb, linkClassName (immClass, matched.substring(1, matched.length()-1)));
        }
        m.appendTail(sb);

        return sb.toString();
    }


    /**
     * Export all elements of a feature in a class as text in an Asciidoctor (.adoc) file.
     * @param immFeature info object for the class.
     */
    private String formatFeature (ImmFeature immFeature) {
        StringBuilder sb = new StringBuilder();
        sb.append (System.lineSeparator());
        sb.append (formatter.tableColHeader (immFeature.getCardinality() +
                (immFeature.getStatus().isEmpty()? "" : " +" + System.lineSeparator() + immFeature.getStatus()), 1));
        sb.append (System.lineSeparator());
        sb.append (formatter.tableCell (immFeature.getSignature(), 1) + System.lineSeparator());
        sb.append (formatter.tableCellPassthrough (immFeature.getDocumentation(), 1) + System.lineSeparator());

        return sb.toString();
    }

    private String formatOperation (ImmOperation immOperation) {
        StringBuilder sb = new StringBuilder();
        sb.append (System.lineSeparator());
        sb.append (formatter.tableColHeader (immOperation.getCardinality() +
                (immOperation.getStatus().isEmpty()? "" : " +" + System.lineSeparator() + immOperation.getStatus()), 1));
        sb.append (System.lineSeparator());
        sb.append (formatter.tableCell (immOperation.getSignature(), 1) + System.lineSeparator());
        sb.append (formatter.tableCellPassthrough (immOperation.getDocumentation(), 1) + System.lineSeparator());

        return sb.toString();
    }

    /**
     * Export all constraints in a class as text (invariants) in an Asciidoctor (.adoc) file.
     * @param immClass info object for the class.
     */
    private String formatConstraints (ImmClass immClass) {
        StringBuilder sb = new StringBuilder();

        String title = "Invariants";
        for (ImmConstraint immConstraint : immClass.getConstraints()) {
            sb.append (System.lineSeparator());
            sb.append (formatter.tableColHeader(title, 1));
            sb.append (System.lineSeparator());

            sb.append (formatter.tableCellPassthrough (immConstraint.getDocumentation(), 2));
            sb.append (System.lineSeparator());
            title = "";
        }

        return sb.toString();
    }

    /**
     * Convert a class name to a legal file name.
     * @param className name of class.
     * @return filename..
     */
    private String fileName(String className) {
        String name = className.replaceAll("[^a-z0-9.]", "_");
        return name.replaceAll("^_+", "");
    }

    private static String formatDiagramName(String name) {
        return name;
    }
}
