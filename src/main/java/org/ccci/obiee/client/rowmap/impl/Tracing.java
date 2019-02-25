package org.ccci.obiee.client.rowmap.impl;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;

import static io.opentracing.tag.Tags.SPAN_KIND_CLIENT;

class Tracing
{
    private Tracing() {}

    static Span buildTopLevelSpan(Tracer tracer, String operationName)
    {
        final Span span = tracer.buildSpan(operationName).start();
        Tags.COMPONENT.set(span, "obiee-ws-client");
        Tags.SPAN_KIND.set(span, SPAN_KIND_CLIENT);
        Tags.PEER_SERVICE.set(span, "obiee-answers");

        // This tag is treated specially by DataDog,
        // but it would be useful by any other kind of impl:
        span.setTag("span.type", "web");

        // If the tracer is a datadog tracer, we have to use the same operation name everywhere,
        // since the UI only shows at most one 'top-level span name' per service name.
        // The datadog docs are not obvious about this, but the OpenTracing 'operation name'
        // gets mapped to the top-level span name, and it is not (easily) overrideable.
        // See https://docs.datadoghq.com/tracing/faq/resource-trace-doesn-t-show-up-under-correct-service/
        //
        // Tracer itself may be a proxy, so we'll inspect its scope manager
        // (which is probably not a proxy).
        if (tracer.scopeManager().getClass().getName().contains("datadog"))
        {
            final String topLevelSpanName = "answers.query";
            span.setOperationName(topLevelSpanName);
            span.setTag("resource.name", operationName);
        }
        return span;
    }

}
