package org.apache.cordova.geolocation;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import android.net.Uri;


public class XmlParser {
	
	public XmlParser(String content){
		//setXmlFileName(content);
		mContent = content;
		parseXml();
	}
	
//	public void setXmlFileName(String fileName){
//		mFileName = fileName;
//		if(fileName.length() > 0){
//			parseXmlFile();
//		}
//		
//	}
//	public void setXMLContent(String content){
//		if(content.length() > 0){
//			SVFile.createFile(SVDir.getTempFilePath("pushresult.xml"), content, true);
//			mFileName = SVDir.getTempFilePath("pushresult.xml");
//			parseXmlFile();
//		}
//	}
	private void parseXml(){
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			mDOMObject = db.parse(new InputSource(new ByteArrayInputStream(mContent.getBytes("utf-8"))));
		}catch(ParserConfigurationException pce) {
			pce.printStackTrace();
		}catch(SAXException se) {
			se.printStackTrace();
		}catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}
	public ArrayList<Element> parseDocument(Element parentElement, String tag){
		if(mDOMObject == null)
			return null;
		ArrayList<Element> returnValue = new ArrayList<Element>();
		NodeList nl = parentElement.getElementsByTagName(tag);
		if(nl != null && nl.getLength() > 0) {
			for(int i = 0 ; i < nl.getLength();i++) {
				Element childElement = (Element)nl.item(i);
				returnValue.add(childElement);
			}
		}
		return returnValue;
	}
	
	public Element getRootElement(){
		if(mDOMObject == null)
			return null;
		return mDOMObject.getDocumentElement();
	}
	
	public String getAttribute(Element element, String attribute){
		return element.getAttribute(attribute);
	}
	
	public String getAttribute(Element parent, String tag, String attribute){
		ArrayList<Element> elements = parseDocument(parent, tag);
		return getAttribute(elements.get(0), attribute);
	}
	
	public String getStringValue(Element element){
		return element.getChildNodes().item(0).getNodeValue();
	}
	public String getNodeValue(Element parent, String tag){
		try{
			ArrayList<Element> elements = parseDocument(parent, tag);
			return getStringValue(elements.get(0));
		}catch(Exception e){
			return "";
		}
	}
	
	public void destroy(){
		mDOMObject = null;
	}
	private String mContent = "";
	private Document mDOMObject = null;
}
