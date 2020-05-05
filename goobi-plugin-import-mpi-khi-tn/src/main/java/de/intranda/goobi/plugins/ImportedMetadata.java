package de.intranda.goobi.plugins;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.jdom2.Element;

import lombok.Data;
import ugh.dl.Metadata;
import ugh.dl.MetadataType;
import ugh.dl.Person;
import ugh.exceptions.UGHException;

@Data
public class ImportedMetadata {

    private String dmdId;
    private List<Metadata> metadataList = new ArrayList<>();
    private List<Person> personList = new ArrayList<>();

    public ImportedMetadata(Element modsSection, Map<String, MetadataType> typeMap) throws UGHException {

        for (Element element : modsSection.getChildren()) {
            switch (element.getName()) {
                case "titleInfo":
                    String title = null;
                    String nonSort = null;
                    String partNumber = null;
                    String subTitle = null;

                    for (Element sub : element.getChildren()) {
                        switch (sub.getName()) {
                            case "nonSort":
                                nonSort = sub.getValue();
                                break;
                            case "title":
                                title = sub.getValue();
                                break;
                            case "partNumber":
                                partNumber = sub.getValue();
                                break;
                            case "subTitle":
                                subTitle = sub.getValue();
                                break;
                        }
                    }
                    if (StringUtils.isNotBlank(title)) {
                        Metadata sortTitle = new Metadata(typeMap.get("TitleDocMainShort"));
                        sortTitle.setValue(title);
                        metadataList.add(sortTitle);

                        if (StringUtils.isNotBlank(nonSort)) {
                            title = nonSort + " " + title;
                        }
                        Metadata md = new Metadata(typeMap.get("TitleDocMain"));
                        md.setValue(title);
                        metadataList.add(md);

                        // nonSort?
                    }
                    if (StringUtils.isNotBlank(partNumber)) {
                        //always <mods:partNumber>, Nr.</mods:partNumber>, ignore it
                    }
                    if (StringUtils.isNotBlank(subTitle)) {
                        Metadata md = new Metadata(typeMap.get("TitleDocSub1"));
                        md.setValue(subTitle);
                        metadataList.add(md);
                    }
                    break;
                case "abstract":
                    if (!element.getValue().trim().isEmpty()) {
                        Metadata md = new Metadata(typeMap.get("Information"));
                        md.setValue(element.getValue().trim());
                        metadataList.add(md);
                    }

                    break;
                case "name":
                    Element namePart = null; // split on last white space
                    Element description = null;

                    List<Element> roleTermList = null;

                    for (Element subfield : element.getChildren()) {
                        if (subfield.getName().equals("namePart")) {
                            namePart = subfield;
                        } else if (subfield.getName().equals("description")) {
                            description = subfield;
                        } else if (subfield.getName().equals("role")) {
                            roleTermList = subfield.getChildren();
                        }
                    }

                    if (namePart != null) {
                        String name = namePart.getValue();
                        String firstname = name.substring(0, name.indexOf(" "));
                        String lastname = name.substring(name.indexOf(" ") + 1);

                        for (Element roleTerm : roleTermList) {
                            String role = roleTerm.getValue().trim();
                            Person p = new Person(typeMap.get(role));
                            p.setFirstname(firstname);
                            p.setLastname(lastname);
                            if (description != null) {
                                String identifier = description.getValue();
                                identifier = identifier.replace("PND:", "").trim();
                                if (StringUtils.isNumeric(identifier)) {
                                    p.setAutorityFile("gnd", "http://d-nb.info/gnd/", identifier);

                                }
                            }
                            personList.add(p);
                        }
                    }

                    break;
                case "originInfo":
                    for (Element subfield : element.getChildren()) {
                        if (subfield.getName().equals("place")) {
                            Metadata md = new Metadata(typeMap.get("PlaceOfPublication"));
                            for (Element placeTerm : subfield.getChildren()) {
                                if (placeTerm.getAttributeValue("type").equals("text")) {
                                    md.setValue(placeTerm.getValue());
                                } else {
                                    md.setAutorityFile("gnd", "http://d-nb.info/gnd/", placeTerm.getValue().replace("SWD:", ""));
                                }
                            }
                            metadataList.add(md);
                        } else if (subfield.getName().equals("publisher")) {
                            Metadata md = new Metadata(typeMap.get("PublisherName"));
                            md.setValue(element.getValue());
                            metadataList.add(md);
                        } else if (subfield.getName().equals("dateIssued")) {
                            Metadata md = new Metadata(typeMap.get("PublicationYear"));
                            md.setValue(element.getValue());
                            metadataList.add(md);
                        }
                    }

                    break;
                case "genre":
                    // book, ignore id
                    break;
                case "typeOfResource":
                    // mixed material, ignore id
                    break;
                case "subject":
                    // empty
                    break;
                case "note":
                    System.out.println(element.getValue().trim());
                    break;
                case "identifier":
                    break;
                case "physicalDescription":
                    break;
                case "relatedItem":
                    break;

            }

        }
    }

}
