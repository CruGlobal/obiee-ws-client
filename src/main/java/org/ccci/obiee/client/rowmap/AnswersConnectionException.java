package org.ccci.obiee.client.rowmap;

import javax.xml.ws.soap.SOAPFaultException;

import org.ccci.obiee.client.rowmap.util.SoapFaults;

public class AnswersConnectionException extends RuntimeException
{

    private static final long serialVersionUID = 1L;

    public AnswersConnectionException(String username, SOAPFaultException e)
    {
        super(String.format(
                "unable to connect to Answers with username %s; details follow:\n%s",
                username,
                SoapFaults.getDetailsAsString(e.getFault())));
    }

}
