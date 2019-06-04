package org.ccci.obiee.client.rowmap;

import oracle.bi.web.soap.ReportParams;
import oracle.bi.web.soap.Variable;
import org.ccci.obiee.client.rowmap.util.SoapFaults;

import javax.xml.ws.soap.SOAPFaultException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Thrown when there is a problem retrieving data from the Answers server.  This could be due to an underlying network problem,
 * an authentication problem, a timeout problem, an unexpected response, etc. 
 * 
 * @author Matt Drees
 */
public class DataRetrievalException extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    private final String reportPath;
    private final String reportXml;
    private final Map<String, Object> reportParams;
    private final String faultDetail;

    public DataRetrievalException(String message, Exception cause)
    {
        super(message, cause);
        reportPath = null;
        reportParams = null;
        faultDetail = null;
        reportXml = null;
    }

    public DataRetrievalException(String message)
    {
        super(message);
        reportPath = null;
        reportParams = null;
        faultDetail = null;
        reportXml = null;
    }

    public DataRetrievalException(String message, String reportPath, ReportParams params, RuntimeException cause)
    {
        super(message + " for report " + reportPath, cause);
        this.reportPath = reportPath;
        reportParams = formatParamsAsString(params);
        faultDetail = getFaultDetail(cause);
        reportXml = null;
    }

    public DataRetrievalException(String message, String reportXml, RuntimeException cause)
    {
        super(message + " for xml query", cause);
        this.reportXml = reportXml;
        faultDetail = getFaultDetail(cause);
        reportParams = null;
        reportPath = null;
    }

    private String getFaultDetail(RuntimeException cause)
    {
        if (cause instanceof SOAPFaultException)
        {
            final SOAPFaultException soapFaultException = (SOAPFaultException) cause;
            return SoapFaults.getDetailsAsString(soapFaultException.getFault());
        } else {
            return null;
        }
    }

    private Map<String, Object> formatParamsAsString(ReportParams params)
    {
        return params == null ? null : asMap(params.getVariables());
    }

    private Map<String, Object> asMap(List<Variable> variables)
    {
        return variables.stream().collect(
            Collectors.toMap(Variable::getName, Variable::getValue));
    }

    @SuppressWarnings("unused") // to be used in rollbar reports
    public String getReportPath()
    {
        return reportPath;
    }

    @SuppressWarnings("unused") // see getReportPath()
    public Map<String, Object> getReportParams()
    {
        return reportParams;
    }

    @SuppressWarnings("unused") // see getReportPath()
    public String getFaultDetail()
    {
        return faultDetail;
    }

    @SuppressWarnings("unused") // see getReportPath()
    public String getReportXml()
    {
        return reportXml;
    }
}
