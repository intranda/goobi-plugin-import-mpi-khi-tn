package de.intranda.goobi.plugins;

import java.util.ArrayList;
import java.util.List;

import org.jdom2.Element;

import lombok.Data;
import ugh.dl.DocStruct;

@Data
public class ImportedDocStruct {

    private String type;

    private String order;
    private String orderLabel;
    private String label;
    private String id;
    private String dmdId;

    private DocStruct docstruct;

    private List<String> relatedPages = new ArrayList<>();

    public ImportedDocStruct(Element div, DocStruct docStrct) {

        setDmdId(div.getAttributeValue("DMDID"));
        setDocstruct(docStrct);
        setId(div.getAttributeValue("ID"));
        setLabel(div.getAttributeValue("LABEL"));
        setOrder(div.getAttributeValue("ORDER"));
        setOrderLabel(div.getAttributeValue("ORDERLABEL"));

        List<Element> fptrList = div.getChildren();
        for (Element fptr : fptrList) {
            if (fptr.getName().equals("fptr")) {
                relatedPages.add(fptr.getAttributeValue("FILEID"));
            }
        }
        setType(div.getAttributeValue("TYPE"));

    }

}
