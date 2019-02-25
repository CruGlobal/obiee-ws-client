package org.ccci.obiee.client.rowmap.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Doms
{

    public static Iterable<Node> each(final NodeList nodeList)
    {
        return () -> new Iterator<Node>()
        {
            int index = 0;

            public boolean hasNext()
            {
                return nodeList.getLength() > index;
            }

            public Node next()
            {
                if (index == nodeList.getLength())
                    throw new NoSuchElementException();
                Node next = nodeList.item(index);
                index++;
                return next;
            }

            public void remove()
            {
                throw new UnsupportedOperationException();
            }
        };
    }

}
