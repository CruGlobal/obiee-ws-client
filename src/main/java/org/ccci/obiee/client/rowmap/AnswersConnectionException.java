package org.ccci.obiee.client.rowmap;

import javax.xml.ws.soap.SOAPFaultException;

import org.ccci.obiee.client.rowmap.util.SoapFaults;

public class AnswersConnectionException extends RuntimeException
{

    private static final long serialVersionUID = 1L;

    private final String username;
    private final String faultDetail;

    public AnswersConnectionException(String username, SOAPFaultException e)
    {
        super("unable to connect to Answers", e);
        this.username = username;
        this.faultDetail = SoapFaults.getDetailsAsString(e.getFault());
    }

    @SuppressWarnings("unused") // to be used in rollbar reports
    public String getUsername()
    {
        return username;
    }

    @SuppressWarnings("unused") // see getUsername()
    public String getFaultDetail()
    {
        return faultDetail;
    }
}
