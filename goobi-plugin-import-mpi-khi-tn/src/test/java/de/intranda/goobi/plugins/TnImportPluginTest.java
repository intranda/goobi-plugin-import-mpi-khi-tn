package de.intranda.goobi.plugins;

import static org.junit.Assert.assertEquals;

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
        DocStruct physical = null;

        Element mets = plugin.readXmlDocument("src/test/resources/data/xml/b181034r.xml");
        Element physicalStructMap = null;

        for (Element element : mets.getChildren()) {
            if (element.getName().equals("structMap") && element.getAttributeValue("TYPE").equals("physical")) {
                physicalStructMap = element;
            }
        }

        DigitalDocument digDoc = new DigitalDocument();

        List<ImportedDocStruct> fixture = plugin.parsePhysicalMap(physical, physicalStructMap, digDoc);

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

    }

}
