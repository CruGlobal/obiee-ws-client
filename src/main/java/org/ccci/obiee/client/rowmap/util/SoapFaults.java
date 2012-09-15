package org.ccci.obiee.client.rowmap.util;

import java.io.StringWriter;

import javax.xml.soap.Detail;
import javax.xml.soap.SOAPFault;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class SoapFaults
{
    public static String getDetailsAsString(SOAPFault fault)
    {
        try
        {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setAttribute("indent-number", 2);
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            StringWriter writer = new StringWriter();
            Detail detail = fault.getDetail();
            transformer.transform(new DOMSource(detail), new StreamResult(writer));
            return writer.toString();
        }
        catch (Exception e)
        // avoid throwing exceptions if we can, since this code is being run in the context of handling another exception
        {
            return "unable to get details due to xslt problem: " + e;
        }
    }

}
