package org.ccci.obiee.client.rowmap.impl;

public class RowmapConfiguration
{

    private String endpointBaseUrl;
    private String username;
    private String password;
    private Integer readTimeout;
    private Integer connectTimeout;

    public String getEndpointBaseUrl()
    {
        return endpointBaseUrl;
    }

    public void setEndpointBaseUrl(String endpointBaseUrl)
    {
        this.endpointBaseUrl = endpointBaseUrl;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    public void setReadTimeout(Integer readTimeout)
    {
        this.readTimeout = readTimeout;
    }

    public Integer getReadTimeout()
    {
        return readTimeout;
    }

    public Integer getConnectTimeout()
    {
        return connectTimeout;
    }

    public void setConnectTimeout(Integer connectTimeout)
    {
        this.connectTimeout = connectTimeout;
    }

}
