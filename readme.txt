When generating the OBIEE jax-ws classes, maven needs to talk directly to the server; it can't use the proxy.  I don't know why this is.

So, when running, use the following command line parameter:
-Dhttp.nonProxyHosts="hart-*|harta|*.ccci.org"

I hope to figure out a better way to do this automatically.