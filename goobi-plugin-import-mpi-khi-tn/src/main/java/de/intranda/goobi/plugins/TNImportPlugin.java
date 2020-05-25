package de.intranda.goobi.plugins;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.map.HashedMap;
import org.apache.commons.lang.StringUtils;
import org.goobi.production.enums.ImportReturnValue;
import org.goobi.production.enums.ImportType;
import org.goobi.production.enums.PluginType;
import org.goobi.production.importer.DocstructElement;
import org.goobi.production.importer.ImportObject;
import org.goobi.production.importer.Record;
import org.goobi.production.plugin.interfaces.IImportPluginVersion2;
import org.goobi.production.properties.ImportProperty;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.jdom2.input.sax.XMLReaders;

import de.sub.goobi.forms.MassImportForm;
import de.sub.goobi.helper.exceptions.ImportPluginException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import ugh.dl.DigitalDocument;
import ugh.dl.DocStruct;
import ugh.dl.DocStructType;
import ugh.dl.Fileformat;
import ugh.dl.Metadata;
import ugh.dl.MetadataType;
import ugh.dl.Person;
import ugh.dl.Prefs;
import ugh.exceptions.DocStructHasNoTypeException;
import ugh.exceptions.IncompletePersonObjectException;
import ugh.exceptions.MetadataTypeNotAllowedException;
import ugh.exceptions.PreferencesException;
import ugh.exceptions.TypeNotAllowedAsChildException;
import ugh.exceptions.TypeNotAllowedForParentException;
import ugh.exceptions.UGHException;
import ugh.exceptions.WriteException;
import ugh.fileformats.mets.MetsMods;

@PluginImplementation
@Log4j2
public class TNImportPlugin implements IImportPluginVersion2 {

    private Namespace METS_NS = Namespace.getNamespace("mets", "http://www.loc.gov/METS/");

    private Namespace MODS_NS = Namespace.getNamespace("mods", "http://www.loc.gov/mods/v3");

    private Namespace RIGHTS_NS = Namespace.getNamespace("rights", "http://cosimo.stanford.edu/sdr/metsrights/");

    private Namespace XLINK_NS = Namespace.getNamespace("xlink", "http://www.w3.org/1999/xlink");
    private Namespace MIX_NS = Namespace.getNamespace("mix", "http://www.loc.gov/standards/mix10/");
    private Namespace XSI_NS = Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
    private Namespace XS_NS = Namespace.getNamespace("xs", "http://www.w3.org/2001/XMLSchema");

    /*
    images korrigieren:
    E_5016_a/flb000006_0515.tif fehlt

    rm BSB_2_H_ant_34_t/2010-09-21-16-16-15_00465.jpg
    rm BSB_2_H_ant_34_t_Beibd_1/2010-09-21-16-21-43_00089.jpg
    rm BSB_Rar_23/2010-09-21-16-00-15_00053.jpg
    rm BSB_Rar_23/2010-09-21-16-00-15_00054.jpg
    rm BSB_Rar_23/2010-09-21-16-00-15_00563.jpg
    rm BSB_Res_2_H_ant_34_r/2010-09-21-16-04-21_00365.jpg
    rm SUB-2-NUM-3682/00000489.tif
    rm SUB-4-ANT-I-1088/00000541.tif
    rm SUB-4-ANT-I-1088/Log.txt
    rm SUB-4-ANT-I-1088/readme.txt
    rm SUB-4-H-ROM-2621/00000351.tif
    rm SUB-2-NUM-3983/00000379.tif
    rm SUB-8-NUM-3370/00000373.tif
    RM SUB-8-NUM-3575/00000165.tif
    rm SUB-8-NUM-3932/00000441.tif


     */

    @Getter
    private String title = "mpi-khi-tn";

    @Getter
    private PluginType type = PluginType.Import;

    private Prefs prefs;

    @Getter
    @Setter
    private String importFolder;

    @Setter
    private MassImportForm form;

    private List<ImportType> importTypes = null;

    @Setter
    private String dataFolder = "/opt/digiverso/tn/xml";

    @Setter
    private String teiFolder = "/opt/digiverso/tn/tei/";
    @Setter
    private String imageFolder = "/opt/digiverso/tn/";
    @Setter
    private String areaFolder = "/opt/digiverso/tn/digital_tn_objects/";

    private String currentIdentifier;

    // ALTER VIEWER: http://tn.khi.fi.it/index.php?id=20&L=1&data=b181034r&type=content

    private DocStructType boundBookType;
    private DocStructType pageType;
    private DocStructType monographType;
    private DocStructType otherType;

    private Map<String, MetadataType> metadataTypeMap = new HashedMap<>();
    @Getter
    private Map<String, String> imageFolderMap = new HashedMap<>();
    private Map<String, String> gndMap = new HashedMap<>();

    @Override
    public List<ImportObject> generateFiles(List<Record> recordList) {

        initializeTypes();
        List<ImportObject> importList = new ArrayList<>(recordList.size());
        for (Record record : recordList) {
            currentIdentifier = record.getId();
            ImportObject io = new ImportObject();
            io.setProcessTitle(getProcessTitle());
            Element metsElement = readXmlDocument(record.getData());
            if (!metsElement.getName().equals("mets")) {
                continue;
            }
            try {
                Fileformat fileformat = new MetsMods(prefs);
                DigitalDocument digDoc = new DigitalDocument();
                fileformat.setDigitalDocument(digDoc);
                List<Element> structMaps = metsElement.getChildren("structMap", METS_NS);
                Element physicalStructMap = null;
                Element logicalStructMap = null;
                for (Element structMap : structMaps) {
                    if (structMap.getAttributeValue("TYPE").equals("physical")) {
                        physicalStructMap = structMap;
                    } else {
                        logicalStructMap = structMap;
                    }
                }

                List<ImportedDocStruct> physicalElements = parsePhysicalMap(physicalStructMap, digDoc);
                List<ImportedDocStruct> logicalElements = parseLogicalMap(logicalStructMap, digDoc, physicalElements);

                List<Element> dmdSecList = metsElement.getChildren("dmdSec", METS_NS);
                Map<String, ImportedMetadata> dmdMap = new HashedMap<>();

                parseDmdSec(dmdSecList, dmdMap);

                addMetadataToDocstruct(logicalElements, dmdMap);

                addAdditionalMetadata(metsElement, digDoc);

                io.setMetsFilename(importFolder + "/" + getProcessTitle() + ".xml");

                Path processData = Paths.get(importFolder, getProcessTitle());

                if (!Files.exists(processData)) {
                    try {
                        Files.createDirectories(processData);
                    } catch (IOException e) {
                        log.error(e);
                    }
                }

                Path imagesFolder = Paths.get(processData.toString(), "images", "master_" + getProcessTitle() + "_media");
                Path sourceFolder = Paths.get(processData.toString(), "images", getProcessTitle() + "_source");

                if (!Files.exists(imagesFolder)) {
                    try {
                        Files.createDirectories(imagesFolder);
                    } catch (IOException e) {
                        log.error(e);
                    }
                }

                // copy tei file
                List<String> teiFilenameList = list(teiFolder);
                for (String teifile : teiFilenameList) {
                    if (teifile.startsWith(currentIdentifier)) {
                        try {
                            if (!Files.exists(sourceFolder)) {
                                try {
                                    Files.createDirectories(sourceFolder);
                                } catch (IOException e) {
                                    log.error(e);
                                }
                            }

                            Files.copy(Paths.get(teiFolder.toString(), teifile), Paths.get(sourceFolder.toString(), "tei.xml"));
                            // TODO convert graphic urls

                        } catch (IOException e) {
                            log.error(e);
                        }
                    }
                }
                // copy/move images
                String imageFolderName = imageFolderMap.get(currentIdentifier);
                Path imageFolderToImport;

                imageFolderToImport = Paths.get(imageFolder, imageFolderName);
                DocStruct physicalDocstruct = digDoc.getPhysicalDocStruct();
                //  add filename to phys object
                List<Path> files;
                switch (imageFolderName) {
                    case "E_5015_x_Band_1":
                        // 0001-0186
                        imageFolderToImport = Paths.get(imageFolder, "E_5015_x");
                        files = listFiles(imageFolderToImport);
                        for (int i = 0; i < 186; i++) {
                            Path fileToCopy = files.get(i);
                            DocStruct page = physicalDocstruct.getAllChildren().get(i);
                            page.setImageName(fileToCopy.getFileName().toString());
                            try {
                                Files.copy(fileToCopy, Paths.get(imagesFolder.toString(), fileToCopy.getFileName().toString()));
                            } catch (IOException e) {
                            }
                        }
                        break;
                    case "E_5015_x_Band_2":
                        // 0187 - 0446
                        imageFolderToImport = Paths.get(imageFolder, "E_5015_x");
                        files = listFiles(imageFolderToImport);
                        for (int i = 186; i < 446; i++) {
                            Path fileToCopy = files.get(i);

                            DocStruct page = physicalDocstruct.getAllChildren().get(i - 186);
                            page.setImageName(fileToCopy.getFileName().toString());
                            try {
                                Files.copy(fileToCopy, Paths.get(imagesFolder.toString(), fileToCopy.getFileName().toString()));
                            } catch (IOException e) {
                            }
                        }
                        break;
                    case "E_5016_Band_1":
                        // 1-188
                        imageFolderToImport = Paths.get(imageFolder, "E_5016");
                        files = listFiles(imageFolderToImport);

                        for (int i = 0; i < 188; i++) {
                            Path fileToCopy = files.get(i);
                            DocStruct page = physicalDocstruct.getAllChildren().get(i);
                            page.setImageName(fileToCopy.getFileName().toString());
                            try {
                                Files.copy(fileToCopy, Paths.get(imagesFolder.toString(), fileToCopy.getFileName().toString()));
                            } catch (IOException e) {
                            }
                        }
                        break;
                    case "E_5016_Band_2":
                        // 189-450
                        imageFolderToImport = Paths.get(imageFolder, "E_5016");
                        files = listFiles(imageFolderToImport);
                        for (int i = 188; i < 450; i++) {
                            Path fileToCopy = files.get(i);

                            DocStruct page = physicalDocstruct.getAllChildren().get(i - 189);
                            page.setImageName(fileToCopy.getFileName().toString());
                            try {
                                Files.copy(fileToCopy, Paths.get(imagesFolder.toString(), fileToCopy.getFileName().toString()));
                            } catch (IOException e) {
                            }
                        }
                        break;
                    case "E_5016_a_Band_1":
                        // 1-188
                        imageFolderToImport = Paths.get(imageFolder, "E_5016_a");
                        files = listFiles(imageFolderToImport);
                        for (int i = 0; i < 188; i++) {
                            Path fileToCopy = files.get(i);
                            DocStruct page = physicalDocstruct.getAllChildren().get(i);
                            page.setImageName(fileToCopy.getFileName().toString());
                            try {
                                Files.copy(fileToCopy, Paths.get(imagesFolder.toString(), fileToCopy.getFileName().toString()));
                            } catch (IOException e) {
                            }
                        }
                        break;
                    case "E_5016_a_Band_2":
                        //189-516
                        imageFolderToImport = Paths.get(imageFolder, "E_5016_a");
                        files = listFiles(imageFolderToImport);
                        for (int i = 188; i < 515; i++) {
                            Path fileToCopy = files.get(i);
                            DocStruct page = physicalDocstruct.getAllChildren().get(i - 188);
                            page.setImageName(fileToCopy.getFileName().toString());
                            try {
                                Files.copy(fileToCopy, Paths.get(imagesFolder.toString(), fileToCopy.getFileName().toString()));
                            } catch (IOException e) {
                            }
                        }
                        break;
                    default:
                        files = listFiles(imageFolderToImport);
                        for (int i = 0; i < files.size(); i++) {
                            Path fileToCopy = files.get(i);
                            DocStruct page = physicalDocstruct.getAllChildren().get(i);
                            page.setImageName(fileToCopy.getFileName().toString());
                            try {
                                Files.copy(fileToCopy, Paths.get(imagesFolder.toString(), fileToCopy.getFileName().toString()));
                            } catch (IOException e) {
                            }
                        }
                        break;
                }

                // Paths.get(areaFolder, currentIdentifier) -> img_obj*

                Path pageAreaFolder = Paths.get(areaFolder, currentIdentifier);
                List<Path> pageAreaNames = listFiles(pageAreaFolder);
                for (Path file : pageAreaNames) {
                    if (Files.isRegularFile(file) && file.getFileName().toString().startsWith("obj_img")) {
                        try {
                            Files.copy(file, Paths.get(imagesFolder.toString(), file.getFileName().toString()));
                        } catch (IOException e) {
                        }

                        //
                        try {
                            DocStruct pageStruct = digDoc.createDocStruct(pageType);
                            //
                            Metadata physPageNumber = new Metadata(metadataTypeMap.get("physPageNumber"));
                            physPageNumber.setValue("" + (physicalDocstruct.getAllChildren().size() + 1));
                            pageStruct.addMetadata(physPageNumber);
                            pageStruct.setImageName(file.getFileName().toString());
                            physicalDocstruct.addChild(pageStruct);
                        } catch (Exception e) {
                            log.error(e);
                        }

                    }
                }

                fileformat.write(io.getMetsFilename());
                io.setImportReturnValue(ImportReturnValue.ExportFinished);
            } catch (PreferencesException | WriteException e) {
                log.error(e);
                io.setImportReturnValue(ImportReturnValue.InvalidData);
            }

            importList.add(io);
        }

        return importList;
    }

    private void parseDmdSec(List<Element> dmdSecList, Map<String, ImportedMetadata> dmdMap) {
        for (Element dmdSec : dmdSecList) {
            Element mods = dmdSec.getChild("mdWrap", METS_NS).getChild("xmlData", METS_NS).getChild("mods", MODS_NS);
            try {
                ImportedMetadata im = new ImportedMetadata(mods, metadataTypeMap, gndMap);
                String id = dmdSec.getAttributeValue("ID");
                im.setId(id);
                dmdMap.put(id, im);
            } catch (UGHException e) {
                log.info(currentIdentifier + "  " + dmdSec.getAttributeValue("ID"));
                log.error(e);
            }
        }
    }

    private void addMetadataToDocstruct(List<ImportedDocStruct> logicalElements, Map<String, ImportedMetadata> dmdMap) {
        for (ImportedDocStruct ids : logicalElements) {
            if (StringUtils.isNotBlank(ids.getDmdId())) {
                ImportedMetadata im = dmdMap.get(ids.getDmdId());
                DocStruct ds = ids.getDocstruct();
                for (Metadata md : im.getMetadataList()) {
                    try {
                        ds.addMetadata(md);
                    } catch (MetadataTypeNotAllowedException | DocStructHasNoTypeException e) {
                        log.info(currentIdentifier + "  " + ids.getDmdId());
                        log.error(e);
                    }
                }
                for (Person p : im.getPersonList()) {
                    try {
                        ds.addPerson(p);
                    } catch (MetadataTypeNotAllowedException | IncompletePersonObjectException e) {
                        log.info(currentIdentifier + "  " + ids.getDmdId());
                        log.error(e);
                    }
                }
            }
        }
    }

    private void addAdditionalMetadata(Element metsElement, DigitalDocument digDoc) {
        try {
            Metadata mdForPath = new Metadata(metadataTypeMap.get("pathimagefiles"));
            mdForPath.setValue("file://" + getProcessTitle());
            digDoc.getPhysicalDocStruct().addMetadata(mdForPath);
        } catch (MetadataTypeNotAllowedException | DocStructHasNoTypeException e1) {
            log.error(e1);
        }

        DocStruct logical = digDoc.getLogicalDocStruct();
        List<Element> identifier = metsElement.getChild("metsHdr", METS_NS).getChildren("altRecordID", METS_NS);
        for (Element i : identifier) {
            if (i.getAttributeValue("TYPE").equals("AlephSys")) {
                try {
                    Metadata md = new Metadata(metadataTypeMap.get("CatalogIDSource"));
                    md.setValue(i.getValue());
                    logical.addMetadata(md);
                } catch (MetadataTypeNotAllowedException | DocStructHasNoTypeException e) {
                    log.error(e);
                }
            } else {
                try {
                    Metadata md = new Metadata(metadataTypeMap.get("CatalogIdentifier"));
                    md.setValue(i.getValue());
                    logical.addMetadata(md);
                } catch (MetadataTypeNotAllowedException | DocStructHasNoTypeException e) {
                    log.error(e);
                }
            }
        }
        try {
            Metadata md = new Metadata(metadataTypeMap.get("singleDigCollection"));
            md.setValue("Translatio nummorum");
            logical.addMetadata(md);
        } catch (MetadataTypeNotAllowedException | DocStructHasNoTypeException e) {
            log.error(e);
        }
        try {
            Metadata md = new Metadata(metadataTypeMap.get("CatalogIDDigital"));
            md.setValue("khi_tn_" + currentIdentifier);
            logical.addMetadata(md);
        } catch (MetadataTypeNotAllowedException | DocStructHasNoTypeException e) {
            log.error(e);
        }

    }

    public List<ImportedDocStruct> parsePhysicalMap(Element physicalStructMap, DigitalDocument digDoc) {
        List<ImportedDocStruct> physicalList = new ArrayList<>();
        Element div = physicalStructMap.getChild("div", METS_NS);
        DocStruct physical = null;

        try {
            physical = digDoc.createDocStruct(boundBookType);
            digDoc.setPhysicalDocStruct(physical);
            ImportedDocStruct boundBook = new ImportedDocStruct(div, physical);

            physicalList.add(boundBook);
        } catch (TypeNotAllowedForParentException e) {
            log.error(e);
        }

        List<Element> pages = div.getChildren("div", METS_NS);

        for (Element pageDiv : pages) {
            try {
                DocStruct pageStruct = digDoc.createDocStruct(pageType);
                ImportedDocStruct page = new ImportedDocStruct(pageDiv, pageStruct);
                physicalList.add(page);

                Metadata physPageNumber = new Metadata(metadataTypeMap.get("physPageNumber"));
                physPageNumber.setValue(page.getOrder());
                pageStruct.addMetadata(physPageNumber);
                if (StringUtils.isNotBlank(page.getOrderLabel())) {
                    Metadata logicalPageNumber = new Metadata(metadataTypeMap.get("logicalPageNumber"));
                    if (page.getOrderLabel().matches("^[\\w\\s]+: \\d+$")) {
                        logicalPageNumber.setValue("-");
                    } else {
                        logicalPageNumber.setValue(page.getOrderLabel().substring(page.getOrderLabel().lastIndexOf(":") + 1).trim());
                    }
                    pageStruct.addMetadata(logicalPageNumber);
                }
                physical.addChild(pageStruct);
            } catch (TypeNotAllowedForParentException | TypeNotAllowedAsChildException | MetadataTypeNotAllowedException e) {
                log.error(e);
            }
        }

        return physicalList;
    }

    public List<ImportedDocStruct> parseLogicalMap(Element logicalStructMap, DigitalDocument digDoc, List<ImportedDocStruct> physicalElements) {
        List<ImportedDocStruct> logicalList = new ArrayList<>();
        Element mainDiv = logicalStructMap.getChild("div", METS_NS);
        DocStruct logical = null;

        try {
            switch (mainDiv.getAttributeValue("TYPE")) {
                case "book":
                    logical = digDoc.createDocStruct(monographType);
                    break;

                default:
                    log.error("type " + mainDiv.getAttributeValue("TYPE") + " not mapped, use monograph");
                    logical = digDoc.createDocStruct(monographType);
            }

            digDoc.setLogicalDocStruct(logical);
        } catch (TypeNotAllowedForParentException e) {
            log.error(e);
        }

        ImportedDocStruct logicalElement = new ImportedDocStruct(mainDiv, logical);
        logicalList.add(logicalElement);

        // add all pages to logical
        for (ImportedDocStruct ids : physicalElements) {
            logical.addReferenceTo(ids.getDocstruct(), "logical_physical");
        }

        for (Element div : mainDiv.getChildren("div", METS_NS)) {
            try {
                DocStruct other = digDoc.createDocStruct(otherType);
                logical.addChild(other);
                ImportedDocStruct otherElement = new ImportedDocStruct(div, other);
                logicalList.add(otherElement);

                // read pagecontainer
                Element pagecontainer = div.getChild("div", METS_NS);
                for (Element pageDiv : pagecontainer.getChildren("div", METS_NS)) {
                    String pageName = pageDiv.getAttributeValue("ORDERLABEL");
                    // if not unique, get first fptr FILEID?
                    for (ImportedDocStruct ids : physicalElements) {
                        if (ids.getType().equals("page") && ids.getOrderLabel().equals(pageName)) {
                            other.addReferenceTo(ids.getDocstruct(), "logical_physical");
                        }
                    }
                }

            } catch (TypeNotAllowedForParentException | TypeNotAllowedAsChildException e) {
                log.error(e);
            }

        }

        return logicalList;
    }

    private void initializeTypes() {

        if (boundBookType == null) {
            boundBookType = prefs.getDocStrctTypeByName("BoundBook");
            pageType = prefs.getDocStrctTypeByName("page");
            monographType = prefs.getDocStrctTypeByName("Monograph");
            otherType = prefs.getDocStrctTypeByName("OtherDocStrct");
            metadataTypeMap.put("logicalPageNumber", prefs.getMetadataTypeByName("logicalPageNumber"));
            metadataTypeMap.put("physPageNumber", prefs.getMetadataTypeByName("physPageNumber"));

            metadataTypeMap.put("TitleDocSub1", prefs.getMetadataTypeByName("TitleDocSub1"));
            metadataTypeMap.put("TitleDocMain", prefs.getMetadataTypeByName("TitleDocMain"));
            metadataTypeMap.put("TitleDocMainShort", prefs.getMetadataTypeByName("TitleDocMainShort"));

            metadataTypeMap.put("Information", prefs.getMetadataTypeByName("Information"));
            metadataTypeMap.put("Author", prefs.getMetadataTypeByName("Author"));
            metadataTypeMap.put("Publisher", prefs.getMetadataTypeByName("PublisherPerson"));
            metadataTypeMap.put("Artist", prefs.getMetadataTypeByName("Artist"));
            metadataTypeMap.put("PlaceOfPublication", prefs.getMetadataTypeByName("PlaceOfPublication"));
            metadataTypeMap.put("PublisherName", prefs.getMetadataTypeByName("PublisherName"));
            metadataTypeMap.put("PublicationYear", prefs.getMetadataTypeByName("PublicationYear"));
            metadataTypeMap.put("HandwrittenNote", prefs.getMetadataTypeByName("HandwrittenNote"));
            metadataTypeMap.put("PhysicalLocation", prefs.getMetadataTypeByName("PhysicalLocation"));
            metadataTypeMap.put("Copyright", prefs.getMetadataTypeByName("Copyright"));
            metadataTypeMap.put("ContentDescription", prefs.getMetadataTypeByName("ContentDescription"));
            metadataTypeMap.put("CatalogIDSource", prefs.getMetadataTypeByName("CatalogIDSource"));
            metadataTypeMap.put("CatalogIDDigital", prefs.getMetadataTypeByName("CatalogIDDigital"));
            metadataTypeMap.put("singleDigCollection", prefs.getMetadataTypeByName("singleDigCollection"));
            metadataTypeMap.put("CatalogIdentifier", prefs.getMetadataTypeByName("CatalogIdentifier"));

            metadataTypeMap.put("pathimagefiles", prefs.getMetadataTypeByName("pathimagefiles"));

            //            metadataTypeMap.put("", prefs.getMetadataTypeByName(""));
        }
        if (gndMap.isEmpty()) {
            fillMaps();
        }
    }

    public Element readXmlDocument(String data) {
        SAXBuilder builder = new SAXBuilder(XMLReaders.NONVALIDATING);
        builder.setFeature("http://xml.org/sax/features/validation", false);
        builder.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
        builder.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

        Document doc;
        try {
            doc = builder.build(data);
            Element item = doc.getRootElement();
            return item;
        } catch (JDOMException | IOException e) {
            log.error(e);
        }
        return null;
    }

    @Override
    public List<Record> generateRecordsFromFile() {
        return null;
    }

    @Override
    public List<Record> generateRecordsFromFilenames(List<String> selectedFileNames) {
        List<Record> recordList = new ArrayList<>(selectedFileNames.size());
        for (String filename : selectedFileNames) {
            Record rec = new Record();
            rec.setId(filename.replace(".xml", ""));
            rec.setData(Paths.get(dataFolder, filename).toString());
            recordList.add(rec);
        }
        return recordList;
    }

    @Override
    public List<String> getAllFilenames() {
        List<String> filenameList = list(dataFolder);
        return filenameList;
    }

    @Override
    public String getProcessTitle() {
        return "khi_tn_" + currentIdentifier;
    }

    @Override
    public Fileformat convertData() throws ImportPluginException {
        return null;
    }

    @Override
    public List<ImportType> getImportTypes() {
        if (importTypes == null) {
            importTypes = new ArrayList<>();
            importTypes.add(ImportType.FOLDER);
        }
        return importTypes;
    }

    @Override
    public List<String> getPossibleDocstructs() {
        return null;
    }

    @Override
    public List<? extends DocstructElement> getCurrentDocStructs() {
        return null;
    }

    @Override
    public DocstructElement getDocstruct() {
        return null;
    }

    @Override
    public List<ImportProperty> getProperties() {
        return null;
    }

    @Override
    public void setData(Record arg0) {
    }

    @Override
    public void setDocstruct(DocstructElement arg0) {
    }

    @Override
    public void setFile(File arg0) {
    }

    @Override
    public List<String> splitIds(String arg0) {
        return null;
    }

    @Override
    public List<Record> splitRecords(String arg0) {
        return null;
    }

    @Override
    public boolean isRunnableAsGoobiScript() {
        return true;
    }

    @Override
    public String deleteDocstruct() {
        return null;
    }

    @Override
    public String addDocstruct() {
        return null;
    }

    @Override
    public void deleteFiles(List<String> arg0) {
    }

    @Override
    public void setPrefs(Prefs prefs) {
        this.prefs = prefs;
        initializeTypes();

    }

    private void fillMaps() {
        imageFolderMap.put("b181034r", "E_6119");
        imageFolderMap.put("b229517f", "E_6103");
        imageFolderMap.put("b277436f", "E_6115");
        imageFolderMap.put("b309785f", "E_5015_o");
        imageFolderMap.put("b258350f", "E_6118");
        imageFolderMap.put("b304253f", "E_6112");
        imageFolderMap.put("b304254f", "E_6110");
        imageFolderMap.put("b304255f", "E_6109");
        //        bd1060570cr     - was E_5015_x  --> Buch in entsprechende B<E4>nde geteilt, also Ordner existiert so nicht mehr, sondern so:
        //bd1060570br             - was E_5015_x_Band_1
        //bd1060570cr             - was E_5015_x_Band_2
        imageFolderMap.put("bd1060570br", "E_5015_x_Band_1"); // 1-186
        imageFolderMap.put("bd1060570cr", "E_5015_x_Band_2"); // 187 - 446
        imageFolderMap.put("bd1080404r", "E_5015_r");
        imageFolderMap.put("bd1260181r", "E_5015");
        imageFolderMap.put("bd3590238r", "E_5015_t");
        //        bd3610344am     - was E_5016    --> Buch in entsprechende B<E4>nde geteilt, also Ordner existiert so nicht mehr, sondern so:
        //bd3610344am             - was E_5016_Band_1
        //bd3610344bm             - was E_5016_Band_2
        imageFolderMap.put("bd3610344am", "E_5016_Band_1"); // 1-188
        imageFolderMap.put("bd3610344bm", "E_5016_Band_2"); // 189-450
        //        bd3610345bm     - was E_5016_a  --> Buch in entsprechende B<E4>nde geteilt, also Ordner existiert so nicht mehr, sondern so:
        //bd3610345am             - was E_5016_a_Band_1
        //bd3610345bm             - was E_5016_a_Band_2
        imageFolderMap.put("bd3610345am", "E_5016_a_Band_1"); // 1-188
        imageFolderMap.put("bd3610345bm", "E_5016_a_Band_2"); // 189 -516

        imageFolderMap.put("foreign_bsb_2_h_ant_34_t", "BSB_2_H_ant_34_t");
        imageFolderMap.put("foreign_sub_2_num_3682", "SUB-2-NUM-3682");
        imageFolderMap.put("foreign_bsb_2_h_ant_34_t_beibd_1", "BSB_2_H_ant_34_t_Beibd_1");
        imageFolderMap.put("foreign_sub_4_h_rom_2621", "SUB-4-H-ROM-2621");
        imageFolderMap.put("foreign_bsb_rar_23", "BSB_Rar_23");
        imageFolderMap.put("foreign_sub_4_num_3983_1", "SUB-2-NUM-3983");
        imageFolderMap.put("foreign_bsb_res_2_h_ant_34_r", "BSB_Res_2_H_ant_34_r");
        imageFolderMap.put("foreign_sub_8_num_3370", "SUB-8-NUM-3370");
        imageFolderMap.put("foreign_bsb_res_4_l_eleg_m_205", "BSB_Res_4_L_eleg_m_205");
        imageFolderMap.put("foreign_sub_8_num_3575", "SUB-8-NUM-3575");
        imageFolderMap.put("foreign_bsb_res_biogr_227_beibd_5", "BSB_Res_Biogr_227_Beibd_5");
        imageFolderMap.put("foreign_sub_8_num_3932", "SUB-8-NUM-3932");
        imageFolderMap.put("foreign_bsu_a_7_inv_26", "BSU_a_7_inv_26");
        imageFolderMap.put("foreign_bsu_h_1_inv_1215", "BSU_H_1_inv_1215");
        imageFolderMap.put("foreign_bbaw_va_7005", "BBAW_Va_7005");

        imageFolderMap.put("foreign_sub_4_ant_i_1088", "SUB-4-ANT-I-1088");
        // unklar:
        imageFolderMap.put("foreign_smb_gris_1598_1_mtl", "foreign_smb_gris_1598_1_mtl");
        imageFolderMap.put("foreign_flb_ubw_15a_015370", "flb_ubw_15a_015370");
        imageFolderMap.put("bd2830393r", "X_7920_t");
        imageFolderMap.put("b230031f", "X_6920");

        gndMap.put("SWD:4062501-1", "Venedig");
        gndMap.put("SWD:4050471-2", "Rom");
        gndMap.put("SWD:4036770-8", "Lyon");
        gndMap.put("SWD:4002364-3", "Antwerpen");
        gndMap.put("SWD:4057878-1", "Straßburg");
        gndMap.put("SWD:4001783-7", "Amsterdam");
        gndMap.put("SWD:4044660-8", "Paris");
        gndMap.put("SWD:4069688-1", "Brügge");
        gndMap.put("SWD:4004617-5", "Basel");
        gndMap.put("SWD:4059081-1", "Tarragona");
        gndMap.put("SWD:9999999-9", "s.l.");
    }

    public List<String> list(String folder) {
        List<String> fileNames = new ArrayList<>();
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(folder))) {
            for (Path path : directoryStream) {
                if (!path.getFileName().toString().startsWith(".")) {
                    fileNames.add(path.getFileName().toString());
                }
            }
        } catch (IOException ex) {
        }
        Collections.sort(fileNames);
        return fileNames;
    }

    public List<Path> listFiles(Path folder) {
        List<Path> fileNames = new ArrayList<>();
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(folder)) {
            for (Path path : directoryStream) {
                if (!path.getFileName().toString().startsWith(".")) {
                    fileNames.add(path);
                }
            }
        } catch (IOException ex) {
        }
        Collections.sort(fileNames);
        return fileNames;
    }
}
