package de.intranda.goobi.plugins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
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

        DocStruct lastPage =  boundBook.getDocstruct().getAllChildren().get(425);
        Metadata log=null;
        Metadata phys=null;
        for (Metadata md :lastPage.getAllMetadata()) {
            if (md.getType().getName().equals("logicalPageNumber")) {
                log=md;
            } else if (md.getType().getName().equals("physPageNumber")) {
                phys=md;
            }
        }
        assertNotNull(log);
        assertNotNull(phys);

        assertEquals("426", phys.getValue());
        assertEquals("Physical Page Number: 426", log.getValue());

    }

}
