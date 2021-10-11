package de.intranda.goobi.plugins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.jdom2.Element;
import org.junit.Before;
import org.junit.Test;

import ugh.dl.DocStruct;

public class ImportedDocStructTest {


    private Element div;

    @Before
    public void setUp() throws Exception {
        div = new Element("div");


        div.setAttribute("DMDID", "DMDID");

        div.setAttribute("ID", "ID");
        div.setAttribute("LABEL", "LABEL");
        div.setAttribute("ORDER", "ORDER");
        div.setAttribute("ORDERLABEL", "ORDERLABEL");
        div.setAttribute("TYPE", "TYPE");


        Element fptr = new Element("fptr");
        fptr.setAttribute("FILEID", "FILEID");
        div.addContent(fptr);
    }


    @Test
    public void testConstructor() {
        DocStruct ds = new DocStruct();
        ImportedDocStruct ids = new ImportedDocStruct(div, ds);
        assertNotNull(ids);
    }

    @Test
    public void testGetter() {
        DocStruct ds = new DocStruct();
        ImportedDocStruct ids = new ImportedDocStruct(div, ds);

        assertEquals(ids.getDmdId(), "DMDID");
        assertEquals(ids.getId(), "ID");
        assertEquals(ids.getLabel(), "LABEL");
        assertEquals(ids.getOrder(), "ORDER");
        assertEquals(ids.getOrderLabel(), "ORDERLABEL");
        assertEquals(ids.getType(), "TYPE");
        assertEquals(ids.getDocstruct(),ds);
        assertEquals(ids.getRelatedPages().size(), 1);

    }



}
