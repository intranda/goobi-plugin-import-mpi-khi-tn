package de.intranda.goobi.plugins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ugh.dl.DigitalDocument;
import ugh.dl.DocStruct;
import ugh.dl.Metadata;
import ugh.dl.Prefs;

public class TnImportPluginTest {

    private TNImportPlugin plugin;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    private File tempFolder;

    @Before
    public void setUp() throws Exception {
        Path path = Paths.get("src", "test", "resources", "ruleset-mpi.xml");
        Prefs prefs = new Prefs();
        prefs.loadPrefs(path.toString());

        plugin = new TNImportPlugin();
        plugin.setPrefs(prefs);

        plugin.setDataFolder("src/test/resources/data/xml/");
        plugin.setTeiFolder("src/test/resources/data/tei/");
        plugin.setImageFolder("src/test/resources/data/master/");
        plugin.setAreaFolder("src/test/resources/data/pagearea/");


        tempFolder = folder.newFolder("tmp");

        plugin.setImportFolder(tempFolder.getAbsolutePath());
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
    public void testFileGeneration() {
        List<Record> records = new ArrayList<>();
        Record fixture = new Record();
        fixture.setData("src/test/resources/data/xml/b304255f.xml");
        fixture.setId("b304255f");
        records.add(fixture);

        List<ImportObject> returnlist = plugin.generateFiles(records);

        assertEquals(1, returnlist.size());

        assertEquals("khi_tn_b304255f", returnlist.get(0).getProcessTitle());

        Path metsFile = Paths.get(tempFolder.getAbsolutePath(), "khi_tn_b304255f.xml");
        assertTrue(Files.exists(metsFile));

        Path teiFile = Paths.get(tempFolder.getAbsolutePath(), "khi_tn_b304255f", "images", "khi_tn_b304255f_source", "tei.xml");
        assertTrue(Files.exists(teiFile));

        Path imageFolder = Paths.get(tempFolder.getAbsolutePath(), "khi_tn_b304255f", "images", "master_khi_tn_b304255f_media");
        assertTrue(Files.exists(imageFolder));
        List<String> imageNames = plugin.list(imageFolder.toString());
        assertEquals(188, imageNames.size());
        int numberOfMasterFiles = 0;
        int numberOfPageAreas = 0;
        for (String imageName : imageNames) {
            if (imageName.startsWith("flb000008")) {
                numberOfMasterFiles++;
            } else {
                numberOfPageAreas++;
            }
        }
        assertEquals(120, numberOfMasterFiles);
        assertEquals(68, numberOfPageAreas);

    }

    @Test
    public void testGenerateImagesSubList() {
        List<Record> records = new ArrayList<>();
        Record fixture = new Record();
        fixture.setData("src/test/resources/data/xml/bd1060570br.xml");
        fixture.setId("bd1060570br");
        records.add(fixture);

        List<ImportObject> returnlist = plugin.generateFiles(records);

        assertEquals(1, returnlist.size());

        assertEquals("khi_tn_bd1060570br", returnlist.get(0).getProcessTitle());

    }

    @Test
    public void testParsePhysicalMap() {

        Element mets = plugin.readXmlDocument("src/test/resources/data/xml/foreign_sub_4_h_rom_2621.xml");
        Element physicalStructMap = null;

        for (Element element : mets.getChildren()) {
            if (element.getName().equals("structMap") && element.getAttributeValue("TYPE").equals("physical")) {
                physicalStructMap = element;
            }
        }

        DigitalDocument digDoc = new DigitalDocument();

        List<ImportedDocStruct> fixture = plugin.parsePhysicalMap(physicalStructMap, digDoc);

        assertEquals(351, fixture.size());

        ImportedDocStruct boundBook = fixture.get(0);
        assertEquals("BoundBook", boundBook.getDocstruct().getType().getName());
        assertEquals("foreign_sub_4_h_rom_2621_-_DM0000", boundBook.getDmdId());

        ImportedDocStruct firstPage = fixture.get(1);
        assertEquals("page", firstPage.getDocstruct().getType().getName());
        assertEquals("page", firstPage.getType());

        assertEquals("1", firstPage.getOrder());
        assertEquals("Physical Page Number: 1", firstPage.getOrderLabel());
        assertEquals(5, firstPage.getRelatedPages().size());

        assertEquals(350, boundBook.getDocstruct().getAllChildren().size());

        DocStruct lastPage = boundBook.getDocstruct().getAllChildren().get(320);
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

        assertEquals("321", phys.getValue());
        assertEquals("303", log.getValue());

    }

    @Test
    public void testMissingFilesInBetween() {
        List<Record> records = new ArrayList<>();
        Record fixture = new Record();
        fixture.setData("src/test/resources/data/xml/bd3610345bm.xml");
        fixture.setId("bd3610345bm");
        records.add(fixture);

        List<ImportObject> returnlist = plugin.generateFiles(records);

        assertEquals(1, returnlist.size());

        assertEquals("khi_tn_bd3610345bm", returnlist.get(0).getProcessTitle());

        Path imageFolder = Paths.get(tempFolder.getAbsolutePath(), "khi_tn_bd3610345bm", "images", "master_khi_tn_bd3610345bm_media");
        assertTrue(Files.exists(imageFolder));

        List<String> imageNames = plugin.list(imageFolder.toString());
        int numberOfMasterFiles = 0;
        int numberOfPageAreas = 0;
        for (String imageName : imageNames) {
            if (imageName.startsWith("flb000006")) {
                numberOfMasterFiles++;
            } else {
                numberOfPageAreas++;
            }
        }
        assertEquals(327, numberOfMasterFiles);
        assertEquals(311, numberOfPageAreas);
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


    public void generateTei() {
        plugin.setDataFolder("/opt/digiverso/tn/xml/");
        plugin.setTeiFolder("/opt/digiverso/tn/tei/");
        plugin.setImageFolder("/opt/digiverso/tn/");
        plugin.setAreaFolder("/opt/digiverso/tn/digital_tn_objects/");
        plugin.setImportFolder("/tmp");

        List<String> filesInFolder = listData();
        List<Record> records = new ArrayList<>();
        for (String file : filesInFolder) {
            Record fixture = new Record();
            fixture.setData("src/test/resources/data/xml/" + file);
            fixture.setId(file.replace(".xml", ""));
            records.add(fixture);
        }
        List<ImportObject> returnlist = plugin.generateFiles(records);
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
