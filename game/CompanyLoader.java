/*
Rails: an 18xx game system.
Copyright (C) 2005 Brett Lentz

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
*/

package game;

import java.io.InputStream;

import org.xml.sax.*;
import org.xml.sax.helpers.*;

public class CompanyLoader extends DefaultHandler {

	protected Company newCompany = null;
	
	public CompanyLoader (String game) {
		System.setProperty(
			"org.xml.sax.driver",
			"org.apache.crimson.parser.XMLReaderImpl");
		loadCompanies(game);
	}
	private static ClassLoader classLoader = CompanyLoader.class.getClassLoader();

	/**
		 * Load the XML file that defines the companies.
		 * @param game Name of the game (e.g. "1830")
		 */
	public void loadCompanies(String game) {

		String file = new String(game + "companies.xml");

		try {
			XMLReader xr = XMLReaderFactory.createXMLReader();

			xr.setContentHandler(this);
			xr.setErrorHandler(this);
			InputStream i = classLoader.getResourceAsStream(file);
			if (i == null)
				throw new Exception(
					"Companies file " + file + " not found");
			xr.parse(new InputSource(i));
			System.out.println(
				"Companies file " + file + " read successfully");
		} catch (Exception e) {
			System.err.println(
				"Exception catched while parsing the Companies file");
			e.printStackTrace(System.err);
		}

	}

	/* SAX parser callback methods */
	/**
		* DefaultHandler callback method.
		*/
	public void startDocument() {
		System.out.println("Start of reading the Companies file");
	}

	/**
		* DefaultHandler callback method.
		*/
	public void endDocument() {
		System.out.println("End of reading the Companies file");
	}

	/**
		* DefaultHandler callback method.
		*/
	public void startElement(String uri,String name,String qName,Attributes atts)
	{
		String qname;
		String companyName = null;
		String bgcolour = "";
		String fgcolour = "";
		boolean belowLedge = false;
		boolean closesCompany = false;
		boolean endsGame = false;

		int index, i;
		int length;
		newCompany = null;

		if ("".equals(uri)) {
			if (qName.equals("companies")) {
				// Ignore type for now, we only consider rectangular stockmarkets yet.
				;
			} else if (qName.equals("company")) {
				length = atts.getLength();
				for (index = 0; index < length; index++) {
					qname = atts.getQName(index);
					if (qname.equals("name")) {
						companyName = atts.getValue(index);
					} else if (qname.equals("fgcolour")) {
						fgcolour = atts.getValue(index);
					} else if (qname.equals("bgcolour")) {
						bgcolour = atts.getValue(index);
					} else {
						System.err.println(
							"Unknown attribute: {" + uri + "}" + qname);
					}
				}
				if (Company.get(companyName) != null) {
					System.err.println(
						"STOCKMARKET ERROR: Duplicate location definition ignored: "
							+ companyName);
				} else {
					newCompany =
						new Company (companyName, fgcolour, bgcolour);
					Company.addCompany (newCompany);
				}
			} else {
				System.err.println("Unknown start element: " + name);
			}
		} else {
			System.err.println(
				"Unknown start element: {" + uri + "}" + name);
		}
	}

	/**
		* DefaultHandler callback method.
		*/
	public void endElement(String uri, String name, String qName) {
		StockPrice square;
		if ("".equals(uri)) {
			if (qName.equals("companies")) {
			} else if (qName.equals("company")) {
				newCompany = null;
			}
		} else {
			System.out.println(
				"Unknown end element:   {" + uri + "}" + name);
		}
	}

	/**
		* DefaultHandler callback method.
		*/
	public void characters(char ch[], int start, int length) {
	}
}