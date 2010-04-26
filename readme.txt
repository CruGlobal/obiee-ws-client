When generating the OBIEE jax-ws classes, maven needs to talk directly to the server; it can't use the proxy.  I don't know why this is.

One way to achieve this is to use the following command line parameter when running maven:
-Dhttp.nonProxyHosts="hart-*|harta|*.ccci.org"


To avoid having to specify this parameter for every build, you can either edit your network settings to skip *.ccci.org (see 'bypass proxy settings' 
on a mac; not sure what the config would be in windows) or configure your MAVEN_OPTS variable to permanently set this parameter.
