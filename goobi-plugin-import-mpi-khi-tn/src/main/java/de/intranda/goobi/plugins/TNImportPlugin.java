package de.intranda.goobi.plugins;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
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
import de.sub.goobi.helper.StorageProvider;
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
import ugh.dl.Prefs;
import ugh.exceptions.MetadataTypeNotAllowedException;
import ugh.exceptions.PreferencesException;
import ugh.exceptions.TypeNotAllowedAsChildException;
import ugh.exceptions.TypeNotAllowedForParentException;
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
    private String dataFolder = "/home/robert/Downloads/xmldaten_khi_tn_projekt/xml";

    private String currentIdentifier;

    // ALTER VIEWER: http://tn.khi.fi.it/index.php?id=20&L=1&data=b181034r&type=content

    private DocStructType boundBookType;
    private DocStructType pageType;
    private MetadataType logicalPageNumberType;
    private MetadataType physPageNumberType;

    @Override
    public List<ImportObject> generateFiles(List<Record> recordList) {
        initializeTypes();
        List<ImportObject> importList = new ArrayList<>(recordList.size());
        for (Record record : recordList) {
            currentIdentifier = record.getId();
            ImportObject io = new ImportObject();
            io.setProcessTitle(getProcessTitle());

            Element metsElement = readXmlDocument(record.getData());

            try {
                Fileformat fileformat = new MetsMods(prefs);
                DigitalDocument digDoc = new DigitalDocument();
                fileformat.setDigitalDocument(digDoc);
                List<Element> structMaps = metsElement.getChildren("structMap", METS_NS);
                DocStruct logical = null;
                Element physicalStructMap = null;
                Element logicalStructMap = null;
                for (Element structMap : structMaps) {
                    if (structMap.getAttributeValue("TYPE").equals("physical")) {
                        physicalStructMap = structMap;
                    } else {
                        logicalStructMap = structMap;
                    }
                }
                List<ImportedDocStruct> physicalElements = parsePhysicalMap( physicalStructMap, digDoc);

                List<ImportedDocStruct> logicalElements = parseLogicalMap(logical, logicalStructMap, digDoc, physicalElements);

            } catch (PreferencesException e) {
                log.error(e);
            }

            importList.add(io);
        }

        return importList;
    }

    public List<ImportedDocStruct> parsePhysicalMap(Element physicalStructMap, DigitalDocument digDoc) {
        List<ImportedDocStruct> physicalList = new ArrayList<>();
        Element div = physicalStructMap.getChild("div", METS_NS);
        DocStruct physical =null;
        //        <METS:fptr FILEID='b181034r_-_0000-transcript'/>
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

                Metadata physPageNumber = new Metadata(physPageNumberType);
                physPageNumber.setValue(page.getOrder());
                pageStruct.addMetadata(physPageNumber);
                if (StringUtils.isNotBlank(page.getOrderLabel())) {
                    Metadata logicalPageNumber = new Metadata(logicalPageNumberType);
                    logicalPageNumber.setValue(page.getOrderLabel());
                    pageStruct.addMetadata(logicalPageNumber);
                }
                physical.addChild(pageStruct);
            } catch (TypeNotAllowedForParentException | TypeNotAllowedAsChildException | MetadataTypeNotAllowedException e) {
                log.error(e);
            }
        }

        return physicalList;
    }

    private List<ImportedDocStruct> parseLogicalMap(DocStruct logical, Element logicalStructMap, DigitalDocument digDoc,
            List<ImportedDocStruct> physicalElements) {

        // Collection -> Translatio nummorum

        // TODO Auto-generated method stub
        return null;
    }

    private void initializeTypes() {
        if (boundBookType == null) {
            boundBookType = prefs.getDocStrctTypeByName("BoundBook");
            pageType = prefs.getDocStrctTypeByName("page");
            logicalPageNumberType = prefs.getMetadataTypeByName("logicalPageNumber");
            physPageNumberType = prefs.getMetadataTypeByName("physPageNumber");
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
        List<String> filenameList = StorageProvider.getInstance().list(dataFolder);
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
}
