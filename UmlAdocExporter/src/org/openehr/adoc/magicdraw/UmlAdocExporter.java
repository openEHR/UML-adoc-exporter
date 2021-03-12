package org.openehr.adoc.magicdraw;

import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.export.image.ImageExporter;
import com.nomagic.magicdraw.uml.Finder;
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement;
import com.nomagic.uml2.ext.magicdraw.classes.mdinterfaces.Interface;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Enumeration;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.NamedElement;
import org.openehr.adoc.magicdraw.exception.UmlAdocExporterException;

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
public class UmlAdocExporter extends UmlExporterDefinitions {
    private static final String ADOC_FILE_EXTENSION = ".adoc";

    private static final String DIAGRAMS_FOLDER = "diagrams";
    private static final String CLASSES_FOLDER = "classes";

    // component, release, html file, subref classname + type, description
    private static final String INDEX_LINK_FORMAT = "[.xcode]\n* %s\n";

    private final Formatter formatter = new AsciidocFormatter();
    private final int headingLevel;
    private final String rootPackageName;
    private final Set<String> componentPackageNames;
    private final Map<String, Integer> imageFormats;

    // This is a printf pattern giving the form of an Asciidoctor variable name containing
    // a single '%s' for substitution, e.g. "{%s_release}", where the %s will be substituted
    // by the component name (lower case) of each class, some of which are in the core package
    // others of which are in other components.
    private final String specReleaseVarPattern;

    // map of all ClassInfo keyed by class name
    private Map<String, ClassInfo> allEntitiesMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    public UmlAdocExporter(int aHeadingLevel, String aRootPackageName, Set<String> aComponentPackageNames, String aSpecReleasePattern, Map<String, Integer> anImageFormats) {
        headingLevel = aHeadingLevel;
        rootPackageName = aRootPackageName;
        specReleaseVarPattern = aSpecReleasePattern;

        imageFormats = anImageFormats;
        componentPackageNames = aComponentPackageNames;
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

        // Get a Finder object
        Finder.ByTypeRecursivelyFinder umlElementsFinder = Finder.byTypeRecursively();

        // Gather UML classes, enumerations and interfaces, run through a pipeline that does:
        // * cast to an MD class object
        // * retain only classes within the root package specified on the command line, which
        //   will generally be more than what is to be published; we do this so as to be able
        //   to generate links from those classes being published to those in other components
        // * convert to ClassInfo objects (local representation used here)
        // Then export each ClassInfo object as an output file
        ClassInfoBuilder classInfoBuilder = new ClassInfoBuilder(formatter);

        // -------- get the UML model classes -------
        Collection<? extends Element> umlClasses = umlElementsFinder.find(
                project.getPrimaryModel(),
                new Class[]{com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class.class},
                true);
        List<ClassInfo> classes = umlClasses.stream()
                .map(e -> (com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class)e)
                .filter(c -> ! c.getName().contains("<"))// ignore classes with names simulating template type names
                .filter(this::matchesRootPackage)
                .map(classInfoBuilder::build)
                .collect(Collectors.toList());

        // -------- get the UML model interfaces -------
        Collection<? extends Element> umlInterfaces = umlElementsFinder.find(
                project.getPrimaryModel(),
                new Class[]{Interface.class},
                true);
        InterfaceInfoBuilder interfaceInfoBuilder = new InterfaceInfoBuilder(formatter);
        List<ClassInfo> interfaces = umlInterfaces.stream()
                .map(e -> (Interface)e)
                .filter(c -> ! c.getName().contains("<"))// ignore classes with names simulating template type names
                .filter(this::matchesRootPackage)
                .map(interfaceInfoBuilder::build)
                .collect(Collectors.toList());

        // -------- get the UML model enumerations -------
        Collection<? extends Element> umlEnumerations = umlElementsFinder.find(
                project.getPrimaryModel(),
                new Class[]{Enumeration.class},
                true);

        EnumerationInfoBuilder enumerationInfoBuilder = new EnumerationInfoBuilder(formatter);
        List<ClassInfo> enumerations = umlEnumerations.stream()
                .map(e -> (Enumeration)e)
                .filter(this::matchesRootPackage)
                .map(enumerationInfoBuilder::build)
                .collect(Collectors.toList());

        // -------- get the UML model state machines -------
//        Collection<? extends Element> umlStateMachines = umlElementsFinder.find(
//                project.getPrimaryModel(),
//                new Class[]{com.nomagic.uml2.ext.magicdraw.classes.mdkernel.???},
//                true);

        // --------- build a global map of ClassInfo keyed by class name --------
        classes.forEach (classInfo -> {
            if (!allEntitiesMap.containsKey (classInfo.getClassName()) || !matchesComponents (allEntitiesMap.get (classInfo.getClassName())))
                allEntitiesMap.put (classInfo.getClassName(), classInfo);
        });
        interfaces.forEach (classInfo -> {
            if (!allEntitiesMap.containsKey (classInfo.getClassName()) || !matchesComponents (allEntitiesMap.get (classInfo.getClassName())))
                allEntitiesMap.put(classInfo.getClassName(), classInfo);
        });
        enumerations.forEach (classInfo -> {allEntitiesMap.put (classInfo.getClassName(), classInfo);});

        // iterate through the whole lot and add an override for the spec document, if it is
        // different from the sub-package inferred from the package structure
        for (ClassInfo classInfo: allEntitiesMap.values())
            if (classSpecMapExceptions.containsKey (classInfo.getClassSubPackage()))
                classInfo.setSpecName (classSpecMapExceptions.get (classInfo.getClassSubPackage()));

        // -------------------------- do the publishing ----------------------------

        // Output the entities, but only those components that were requested to publish,
        // which equates to some selection of sub-packages of the root package, or maybe all
        if (!componentPackageNames.isEmpty())
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
        imageFormats.forEach((k,v)->doExportDiagram(k, v, outputFolder, diag));
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
     * @param classInfo info object for the class.
     * @exception IOException on fail to write to file.
     */
    private void exportClass (ClassInfo classInfo, File targetFolder) {
        try (PrintWriter printWriter = new PrintWriter(Files.newBufferedWriter(
                targetFolder.toPath().resolve(fileName(classInfo.getClassName().toLowerCase()) + ADOC_FILE_EXTENSION), Charset.forName("UTF-8")))) {
            printWriter.println(formatter.heading(classInfo.getClassName() + ' ' + classInfo.getMetaType(), headingLevel));
            printWriter.println();

            printWriter.println(formatter.tableDefinition ("1,3,5"));
            printWriter.println(formatter.tableDelimiter());
            printWriter.println(formatter.tableColHeader (classInfo.getMetaType(), 1));
            printWriter.println(formatter.tableColHeaderCentred (
                    (classInfo.isAbstractClass()
                            ? formatter.italic(classInfo.getClassTypeName() + " (abstract)")
                            : classInfo.getClassTypeName()),
                    2)
            );
            printWriter.println();

            printWriter.println(formatter.tableColHeader ("Description", 1));

            printWriter.println(formatter.tableCellPassthrough (classInfo.getDocumentation(), 2));
            printWriter.println();

            // inheritance parents
            if (!classInfo.getParentClassNames().isEmpty()) {
                printWriter.println (formatter.tableColHeader ("Inherit", 1));
                StringBuilder sb = new StringBuilder();
                for (String parentClass: classInfo.getParentClassNames())
                    sb.append (formatter.monospace (linkClassName (classInfo, parentClass))).append(", ");

                String parentsString = "";
                // remove any trailing ", "
                if (sb.length() > 0)
                    parentsString = sb.substring(0, sb.length() - 2);
                printWriter.println (formatter.tableCell (parentsString, 2));
                printWriter.println ();
            }

            // constants
            if (!classInfo.getConstants().isEmpty()) {
                printWriter.println (formatter.tableColHeader ("Constants", 1));
                printWriter.println (formatter.tableColHeaderCentred ("Signature", 1));
                printWriter.println (formatter.tableColHeaderCentred ("Meaning", 1));

                for (ClassFeatureInfo classFeatureInfo : classInfo.getConstants())
                    printWriter.print (postProcess (classInfo, formatFeature  (classFeatureInfo)));
            }

            // attributes
            if (!classInfo.getAttributes().isEmpty()) {
                printWriter.println (formatter.tableColHeader ("Attributes", 1));
                printWriter.println (formatter.tableColHeaderCentred ("Signature", 1));
                printWriter.println (formatter.tableColHeaderCentred ("Meaning", 1));

                for (ClassFeatureInfo classFeatureInfo : classInfo.getAttributes())
                    printWriter.print (postProcess (classInfo, formatFeature  (classFeatureInfo)));
            }

            // operations
            if (!classInfo.getOperations().isEmpty()) {
                printWriter.println (formatter.tableColHeader ("Functions", 1));
                printWriter.println (formatter.tableColHeaderCentred ("Signature", 1));
                printWriter.println (formatter.tableColHeaderCentred ("Meaning", 1));

                for (ClassFeatureInfo classFeatureInfo : classInfo.getOperations())
                    printWriter.print (postProcess (classInfo, formatFeature  (classFeatureInfo)));
            }

            // invariants
            if (!classInfo.getConstraints().isEmpty())
                printWriter.print (postProcess (classInfo, formatConstraints  (classInfo)));

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
    private void generateIndex(File targetFolder, List<ClassInfo> allTypes) {
        Collections.sort(allTypes);

        Path targetPath = targetFolder.toPath().resolve("class_index" + ADOC_FILE_EXTENSION);
        try (PrintWriter printWriter = new PrintWriter(Files.newBufferedWriter(targetPath, Charset.forName("UTF-8")))) {
            String specComponent = "";
            String specPackage = "";
            String specSubPackage = "";

            for (ClassInfo classInfo : allTypes) {
                // The test for className > 2 is to avoid generic parameters like 'T', and
                // occasionally 'TT' or similar.
                if (classInfo.getClassName().length() > 2) {

                    // if Component of class has changed since last iteration, output a new header line
                    if (!specComponent.equals(classInfo.getSpecComponent())) {
                        printWriter.println();
                        printWriter.println(formatter.heading ("Component " + classInfo.getSpecComponent(), 2));
                        specComponent = classInfo.getSpecComponent();
                    }

                    // if Package of class has changed since last iteration, output a new header line
                    if (!specPackage.equals(classInfo.getClassPackage())) {
                        printWriter.println();
                        printWriter.println(formatter.heading ("Model " + classInfo.getClassPackage(), 3));
                        specPackage = classInfo.getClassPackage();
                    }

                    // if Sub-package of class has changed since last iteration, output a new header line
                    if (!specSubPackage.equals(classInfo.getClassSubPackage())) {
                        printWriter.println();
                        printWriter.println(formatter.heading ("Package " + classInfo.getClassSubPackage(), 4));
                        printWriter.println();
                        specSubPackage = classInfo.getClassSubPackage();
                    }

                    // Output the class as a linked text line of the form:
                    //   [.xcode]
                    //   * link:/releases/AM/{am_release}/AOM2.html#_c_object_class[C_OBJECT^]
                    // from the sprintf template string: "[.xcode]\n* %s\n"
                    printWriter.printf(INDEX_LINK_FORMAT, formatter.externalLink(classInfo.getClassName(), classInfo.urlPath (specReleaseVarPattern)));
                }
            }
        } catch (IOException e) {
            throw new UmlAdocExporterException("Unable to write to " + targetPath + '!', e);
        }
    }

    private boolean matchesRootPackage (NamedElement namedElement) {
        return namedElement.getQualifiedName().contains(rootPackageName + "::");
    }

    private boolean diagMatchesRootPackages (DiagramPresentationElement diagElement) {
        return componentPackageNames.stream().anyMatch (rn -> diagElement.getName().contains(rn + "-"));
    }

    // note: returns false for empty list - need to check for empty case before using this filter
    private boolean matchesComponents (ClassInfo classInfo) {
        return componentPackageNames.stream().anyMatch (cn -> classInfo.getSpecComponent().equalsIgnoreCase (cn));
    }

    /**
     * Convert a targetClassName like "ELEMENT" that is referenced from originClass
     * to a link. If the target is in the same package, then it's the same spec,
     * so use a local ref, else use a full external URL link
     * @param originClass
     * @param targetClassName
     * @return
     */
    private String linkClassName (ClassInfo originClass, String targetClassName) {
        ClassInfo targetClass = allEntitiesMap.get (targetClassName);
        if (targetClass != null) {
            if (!targetClass.getSpecName().equals (originClass.getSpecName()))
                return formatter.externalLink (targetClassName, targetClass.urlPath (specReleaseVarPattern));
            else
                return formatter.internalRef (targetClassName, targetClass.localRef());
        }
        else
            return targetClassName;
    }

    /**
     * Post-process a formatted String:
     * - replace "@TypeName@" with linked Typenames (removing the @@)
     */
    private String postProcess (ClassInfo classInfo, String classText) {
        Pattern p = Pattern.compile (TYPE_QUOTE_REGEX);
        Matcher m = p.matcher (classText);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String matched = m.group();
            m.appendReplacement(sb, linkClassName (classInfo, matched.substring(1, matched.length()-1)));
        }
        m.appendTail(sb);

        return sb.toString();
    }

    /*
     * Handle exceptions to regular relationship between package name and
     * specification document name.
     */
    static Hashtable<String, String> classSpecMapExceptions = new Hashtable<>();

    static {
        classSpecMapExceptions.put("composition", "ehr");
        classSpecMapExceptions.put("aom2", "AOM2");
        classSpecMapExceptions.put("aom2_profile", "AOM2");
        classSpecMapExceptions.put("p_aom2", "AOM2");
    }

    /**
     * Export all elements of a feature in a class as text in an Asciidoctor (.adoc) file.
     * @param classFeatureInfo info object for the class.
     */
    private String formatFeature (ClassFeatureInfo classFeatureInfo) {
        StringBuilder sb = new StringBuilder();
        sb.append (System.lineSeparator());
        sb.append (formatter.tableColHeader (classFeatureInfo.getCardinality() +
                (classFeatureInfo.getStatus().isEmpty()? "" : " +" + System.lineSeparator() + classFeatureInfo.getStatus()), 1));
        sb.append (System.lineSeparator());
        sb.append (formatter.tableCell (classFeatureInfo.getSignature(), 1) + System.lineSeparator());
        sb.append (formatter.tableCellPassthrough (classFeatureInfo.getDocumentation(), 1) + System.lineSeparator());

        return sb.toString();
    }

    /**
     * Export all constraints in a class as text (invariants) in an Asciidoctor (.adoc) file.
     * @param classInfo info object for the class.
     */
    private String formatConstraints (ClassInfo classInfo) {
        StringBuilder sb = new StringBuilder();

        String title = "Invariants";
        for (ConstraintInfo constraintInfo : classInfo.getConstraints()) {
            sb.append (System.lineSeparator());
            sb.append (formatter.tableColHeader(title, 1));
            sb.append (System.lineSeparator());

            sb.append (formatter.tableCellPassthrough (constraintInfo.getDocumentation(), 2));
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
        String name = className.replaceAll("[^a-z0-9]", "_");
        return name.replaceAll("^_+", "");
    }

    private static String formatDiagramName(String name) {
        return name;
    }
}
