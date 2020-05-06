package de.intranda.goobi.plugins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.goobi.production.importer.ImportObject;
import org.goobi.production.importer.Record;
import org.jdom2.Element;
import org.junit.Before;
import org.junit.Test;

import ugh.dl.DigitalDocument;
import ugh.dl.DocStruct;
import ugh.dl.Metadata;
import ugh.dl.Prefs;

public class TnImportPluginTest {

    private TNImportPlugin plugin;

    @Before
    public void setUp() throws Exception {
        Path path = Paths.get("src", "test", "resources", "ruleset-mpi.xml");
        Prefs prefs = new Prefs();
        prefs.loadPrefs(path.toString());

        plugin = new TNImportPlugin();
        plugin.setPrefs(prefs);

        plugin.setDataFolder("src/test/resources/data/xml/");

        plugin.setImportFolder("/tmp");
    }

    @Test
    public void testGenerateRecordsFromFilenames() {
        List<String> selectedFileNames = new ArrayList<>();
        selectedFileNames.add("b181034r.xml");
        List<Record> fixture = plugin.generateRecordsFromFilenames(selectedFileNames);

        assertEquals(1, fixture.size());
        Record record = fixture.get(0);
        assertEquals("b181034r", record.getId());
        assertEquals("src/test/resources/data/xml/b181034r.xml", record.getData());

    }

    @Test
    public void testGenerateFiles() {
        List<Record> records = new ArrayList<>();
        Record fixture = new Record();
        fixture.setData("src/test/resources/data/xml/b181034r.xml");
        fixture.setId("b181034r");
        records.add(fixture);

        List<ImportObject> returnlist = plugin.generateFiles(records);

        assertEquals(1, returnlist.size());

        assertEquals("khi_tn_b181034r", returnlist.get(0).getProcessTitle());

    }

    @Test
    public void runAllFiles() {
        List<String> xmlFiles = listData();

        List<Record> records = new ArrayList<>();
        for (String xmlName : xmlFiles) {
            Record fixture = new Record();
            fixture.setData("src/test/resources/data/xml/" + xmlName);
            fixture.setId(xmlName.replace(".xml", ""));
            records.add(fixture);
        }
        List<ImportObject> returnlist = plugin.generateFiles(records);

    }

    @Test
    public void testParsePhysicalMap() {

        Element mets = plugin.readXmlDocument("src/test/resources/data/xml/b181034r.xml");
        Element physicalStructMap = null;

        for (Element element : mets.getChildren()) {
            if (element.getName().equals("structMap") && element.getAttributeValue("TYPE").equals("physical")) {
                physicalStructMap = element;
            }
        }

        DigitalDocument digDoc = new DigitalDocument();

        List<ImportedDocStruct> fixture = plugin.parsePhysicalMap(physicalStructMap, digDoc);

        assertEquals(427, fixture.size());

        ImportedDocStruct boundBook = fixture.get(0);
        assertEquals("BoundBook", boundBook.getDocstruct().getType().getName());
        assertEquals("b181034r_-_DM0000", boundBook.getDmdId());

        ImportedDocStruct firstPage = fixture.get(1);
        assertEquals("page", firstPage.getDocstruct().getType().getName());
        assertEquals("page", firstPage.getType());

        assertEquals("1", firstPage.getOrder());
        assertEquals("Physical Page Number: 1", firstPage.getOrderLabel());
        assertEquals(5, firstPage.getRelatedPages().size());

        assertEquals(426, boundBook.getDocstruct().getAllChildren().size());

        DocStruct lastPage = boundBook.getDocstruct().getAllChildren().get(425);
        Metadata log = null;
        Metadata phys = null;
        for (Metadata md : lastPage.getAllMetadata()) {
            if (md.getType().getName().equals("logicalPageNumber")) {
                log = md;
            } else if (md.getType().getName().equals("physPageNumber")) {
                phys = md;
            }
        }
        assertNotNull(log);
        assertNotNull(phys);

        assertEquals("426", phys.getValue());
        assertEquals("-", log.getValue());

    }

    @Test
    public void testParsePLogicalMap() {

        Element mets = plugin.readXmlDocument("src/test/resources/data/xml/b181034r.xml");
        Element physicalStructMap = null;
        Element logicalStructMap = null;
        for (Element element : mets.getChildren()) {
            if (element.getName().equals("structMap") && element.getAttributeValue("TYPE").equals("physical")) {
                physicalStructMap = element;
            } else {
                logicalStructMap = element;
            }
        }

        DigitalDocument digDoc = new DigitalDocument();
        List<ImportedDocStruct> physicalImportedDocStructList = plugin.parsePhysicalMap(physicalStructMap, digDoc);

        List<ImportedDocStruct> fixture = plugin.parseLogicalMap(logicalStructMap, digDoc, physicalImportedDocStructList);
        DocStruct logical = digDoc.getLogicalDocStruct();

        assertEquals("Monograph", logical.getType().getName());
        assertEquals(logical, fixture.get(0).getDocstruct());

        // 148 chapter + monograph
        assertEquals(149, fixture.size());

        ImportedDocStruct chapter = fixture.get(148);
        assertEquals("OtherDocStrct", chapter.getDocstruct().getType().getName());
        assertEquals("148", chapter.getOrder());
        assertEquals(" INDICE COPIOSISSIMO DELLE MATERIE CONTENVTE NELL'HISTORIA AVGVSTA", chapter.getLabel());

        // 18 pages assigned
        assertEquals(18, chapter.getDocstruct().getAllToReferences().size());
        DocStruct firstPage = chapter.getDocstruct().getAllToReferences().get(0).getTarget();
        // should be page 405
        Metadata log = null;
        Metadata phys = null;
        for (Metadata md : firstPage.getAllMetadata()) {
            if (md.getType().getName().equals("logicalPageNumber")) {
                log = md;
            } else if (md.getType().getName().equals("physPageNumber")) {
                phys = md;
            }
        }
        assertNotNull(log);
        assertNotNull(phys);

        assertEquals("405", phys.getValue());
        assertEquals("379", log.getValue());
    }

    private List<String> listData() {
        List<String> fileNames = new ArrayList<>();
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get("src/test/resources/data/xml"))) {
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

}
