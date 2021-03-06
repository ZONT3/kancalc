package ru.zont.kancalc;

import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class KMParser {
	static final String kmlistDir = "/data/kanmusuList.xml";
	static final String ctlistDir = "/data/constructionTime.xml";
	
	static DocumentBuilder db;
	static Document kmlistFile;
	static Node root;
	static NodeList kms;
	static ArrayList<String> kmNodes;
	
	static ArrayList<Kanmusu> remodels = new ArrayList<>();
	
	public static ArrayList<Kanmusu> getKMList() throws ParserConfigurationException, SAXException, IOException {
		return getKMList(false);
	}
	
	public static ArrayList<Kanmusu> getKMList(boolean am) throws ParserConfigurationException, SAXException, IOException {
		db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		kmlistFile = db.parse(Core.class.getResourceAsStream(kmlistDir));
		root = kmlistFile.getDocumentElement();
		kmNodes = new ArrayList<>();
		for (int j=0; j<root.getChildNodes().getLength(); j++)
			if (root.getChildNodes().item(j).getNodeName() == "ship")
				kmNodes.add(j+"");
		kms = root.getChildNodes();
		ArrayList<Kanmusu> res = new ArrayList<>();
		for (int i=0; i<kmNodes.size(); i++)
			res.add(getKM(i, am));
		res.addAll(remodels);
		return res;
	}

	private static Kanmusu getKM(int i, boolean am) {
		Node km = kms.item(Integer.valueOf(kmNodes.get(i)));
		Element kmE = (Element) km;
		Kanmusu kanmusu = new Kanmusu(kmE.getAttribute("type"));
		kanmusu.cls = kmE.getAttribute("class");
		NodeList kmps = km.getChildNodes();
		for (int j = 0; j < kmps.getLength(); j++) {
			if (kmps.item(j).getNodeType() == Node.ELEMENT_NODE) {
				Element kmp = (Element) kmps.item(j);
				switch (kmp.getNodeName()) {
				case "id":
					kanmusu.id = Integer.valueOf(kmp.getTextContent());
					break;
				case "nid":
					kanmusu.nid = Integer.valueOf(kmp.getTextContent());
					break;
				case "name":
					kanmusu.name = kmp.getTextContent();
					break;
				case "nameJP":
					if (kmp.hasAttribute("original")) {
						kanmusu.oname = kmp.getAttribute("original");
						kanmusu.jpname = kmp.getTextContent();
					} else {
						kanmusu.jpname = kmp.getTextContent();
						kanmusu.oname = kanmusu.jpname;
					}
					break;
				case "craft":
					kanmusu.setCraft(kmp.getTextContent());
					break;
				case "img":
					kanmusu.image = kmp.getTextContent();
					break;
				case "stats":
					for (int k = 0; k<kmp.getChildNodes().getLength(); k++) {
						if (kmp.getChildNodes().item(k).getNodeType() == Node.ELEMENT_NODE) {
							Element kmpst = (Element) kmp.getChildNodes().item(k);
							switch (kmpst.getNodeName()) {
							case "fuel":
								kanmusu.fuel = Integer.valueOf(kmpst.getTextContent());
								break;
							case "ammo":
								kanmusu.ammo = Integer.valueOf(kmpst.getTextContent());
								break;
							case "slots":
								kanmusu.slots = new int[Integer.valueOf(kmpst.getAttribute("count"))];
								for (int l = 0; l < kmpst.getChildNodes().getLength(); l++) {
									if (kmpst.getChildNodes().item(l).getNodeType() == Node.ELEMENT_NODE) {
										Element kmsl = (Element) kmpst.getChildNodes().item(l);
										if (kmsl.getNodeName() == "slot")
											kanmusu.slots[Integer.valueOf(kmsl.getAttribute("id"))] = Integer.valueOf(kmsl.getTextContent());
									}
								}
								break;
							default:
								break;
							}
						}
					}
					break;
				case "remodel":
					if (!am)
						break;
					String type = kanmusu.type;
					if (kmp.hasAttribute("type"))
						type = kmp.getAttribute("type");
					
					Kanmusu remodel = new Kanmusu(type);
					remodel.name = kanmusu.name;
					remodel.jpname = kanmusu.jpname;
					remodel.oname = kanmusu.oname;
					remodel.type = kanmusu.type;
					remodel.cls = kanmusu.cls;
					
					if (kmp.hasAttribute("type"))
						remodel.type = kmp.getAttribute("type");
					if (kmp.hasAttribute("class"))
						remodel.cls = kmp.getAttribute("class");
					
					
					NodeList rmps = kmp.getChildNodes();
					for (int k = 0; k < rmps.getLength(); k++) {
						if (rmps.item(k).getNodeType() != Node.ELEMENT_NODE)
							continue;
						Element rmp = (Element) rmps.item(k);
						
						switch (rmp.getNodeName()) {
						case "id":
							kanmusu.remodels.add(Integer.valueOf(kmp.getAttribute("index")), Integer.valueOf(rmp.getTextContent()));
							break;
						case "name":
							remodel.name = rmp.getTextContent();
							break;
						case "nameJP":
							remodel.jpname = rmp.getTextContent();
							remodel.oname = remodel.jpname;
							if (rmp.hasAttribute("original"))
								remodel.oname = rmp.getAttribute("original");
							break;
						case "fuel":
							remodel.fuel = Integer.valueOf(rmp.getTextContent());
							break;
						case "ammo":
							remodel.ammo = Integer.valueOf(rmp.getTextContent());
							break;
						case "suffix":
							remodel.name = remodel.name+" "+rmp.getTextContent();
							break;
						case "suffixJP":
							remodel.jpname = remodel.jpname+rmp.getTextContent();
							if (rmp.getTextContent().equals("改"))
								remodel.oname = remodel.oname+rmp.getTextContent();
							else
								remodel.oname = remodel.oname+"・"+rmp.getTextContent();
							break;
						default:
							break;
						}
					}
					
					remodels.add(remodel);
					break;
				default:
					break;
				}
			}
		}
		return kanmusu;
	}
	
	
	public static String getConstTime(Kanmusu kanmusu) {
		if (kanmusu.craft.equals("unbuildable"))
			return null;
		
		try {
			DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document doc = db.parse(Core.class.getResourceAsStream(ctlistDir));
			Node root = doc.getDocumentElement();
			NodeList nodes = root.getChildNodes();
			ArrayList<Node> ships = new ArrayList<>();
			ArrayList<Node> classes = new ArrayList<>();
			for (int i=0; i<nodes.getLength(); i++) {
				if (nodes.item(i).getNodeName().equals("ship"))
					ships.add(nodes.item(i));
				else if (nodes.item(i).getNodeName().equals("class"))
					classes.add(nodes.item(i));
			}
			
			for (int i=0; i<ships.size(); i++) {
				Node node = ships.get(i);
				if (node.getNodeType() != Node.ELEMENT_NODE)
					continue;
				Element e = (Element) node;
				if (e.getAttribute("name").equals(kanmusu.name) || e.getAttribute("id").equals(kanmusu.id+""))
					return e.getTextContent();
			}
			for (int i=0; i<classes.size(); i++) {
				Node node = classes.get(i);
				if (node.getNodeType() != Node.ELEMENT_NODE)
					continue;
				Element e = (Element) node;
				if (e.getAttribute("name").equals(kanmusu.cls))
					return e.getTextContent();
			}
		} catch (ParserConfigurationException | SAXException | IOException e) {e.printStackTrace();}
		
		return null;
	}
	
	public static String getConstTime(String name) {
		return getConstTime(Core.getKanmusu(name, Core.kmlist));
	}
	
	public static String getConstTime(int id) {
		return getConstTime(Core.getKanmusu(id, Core.kmlist));
	}
}
