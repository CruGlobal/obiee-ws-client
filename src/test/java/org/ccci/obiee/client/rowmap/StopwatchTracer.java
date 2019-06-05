package org.ccci.obiee.client.rowmap;

import com.google.common.base.Stopwatch;
import io.opentracing.Scope;
import io.opentracing.ScopeManager;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * A simplistic Tracer that logs a message when a span completes
 * indicating how long the span took.
 */
public class StopwatchTracer implements Tracer
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public SpanBuilder buildSpan(String operationName)
    {
        return new SpanBuilderImpl(operationName);
    }

    @Override
    public ScopeManager scopeManager()
    {
        return new ScopeManagerImpl();
    }

    private class ScopeManagerImpl implements ScopeManager
    {
        @Override
        public Scope activate(Span span, boolean finishSpanOnClose)
        {
            return new ScopeImpl();
        }

        @Override
        public Scope active()
        {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public Span activeSpan()
    {
        throw new UnsupportedOperationException();
    }

    private class ScopeImpl implements Scope
    {
        @Override
        public void close()
        {
        }

        @Override
        public Span span()
        {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public <C> void inject(SpanContext spanContext, Format<C> format, C carrier)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public <C> SpanContext extract(Format<C> format, C carrier)
    {
        throw new UnsupportedOperationException();
    }

    private class SpanBuilderImpl implements SpanBuilder
    {
        private final String operationName;

        SpanBuilderImpl(String operationName)
        {
            this.operationName = operationName;
        }

        @Override
        public SpanImpl start()
        {
            return new SpanImpl();
        }

        private class SpanImpl implements Span
        {
            private Stopwatch stopwatch = Stopwatch.createStarted();


            @Override
            public void finish()
            {
                stopwatch.stop();
                log.debug("completed operation " + operationName + " in " + stopwatch);
            }


            @Override
            public Span setTag(String key, String value)
            {
                return this;
            }


            // everything else is unsupported

            @Override
            public SpanContext context()
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public Span setTag(String key, boolean value)
            {
                // ignore
                return this;
            }

            @Override
            public Span setTag(String key, Number value)
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public Span log(Map<String, ?> fields)
            {
                // ignore
                return this;
            }

            @Override
            public Span log(long timestampMicroseconds, Map<String, ?> fields)
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public Span log(String event)
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public Span log(long timestampMicroseconds, String event)
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public Span setBaggageItem(String key, String value)
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public String getBaggageItem(String key)
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public Span setOperationName(String operationName)
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public void finish(long finishMicros)
            {
                throw new UnsupportedOperationException();
            }
        }

        @Override
        public SpanBuilder asChildOf(SpanContext parent)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public SpanBuilder asChildOf(Span parent)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public SpanBuilder addReference(String referenceType, SpanContext referencedContext)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public SpanBuilder ignoreActiveSpan()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public SpanBuilder withTag(String key, String value)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public SpanBuilder withTag(String key, boolean value)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public SpanBuilder withTag(String key, Number value)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public SpanBuilder withStartTimestamp(long microseconds)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        @Deprecated
        public Span startManual()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public Scope startActive(boolean finishSpanOnClose)
        {
            throw new UnsupportedOperationException();
        }
    }
}
