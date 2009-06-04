// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) radix(10) lradix(10) 
// Source File Name:   TuxHTTPRequest.java

package com.tuxisalive.api;

import hudson.ProxyConfiguration;

import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.Hashtable;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.codehaus.groovy.control.io.StringReaderSource;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

// Referenced classes of package com.tuxisalive.api:
//            SLock

public class TuxHTTPRequest
{

    public TuxHTTPRequest(String host, int port)
    {
        baseUrl = String.format("http://%s:%d", new Object[] {
            host, Integer.valueOf(port)
        });
        mutex = new SLock();
    }

    private Object getValueFromStructure(Hashtable struct, String valuePath)
    {
        String pathList[] = valuePath.split("\\.");
        Hashtable node = struct;
        Object result = (Object)null;
        for(int i = 0; i < pathList.length; i++)
        {
            String p = pathList[i];
            if(node.containsKey(p))
            {
                if(i == pathList.length - 1)
                {
                    result = node.get(p);
                    return result;
                }
                node = (Hashtable)node.get(p);
            } else
            {
                return result;
            }
        }

        return result;
    }

    public Hashtable request(String cmd)
    {
        return request(cmd, "GET");
    }

    public Hashtable request(String cmd, String method)
    {
        cmd = String.format("/%s", new Object[] {
            cmd
        });
        System.out.println(cmd);
        Hashtable xmlStruct = new Hashtable();
        String cCmd = String.format("%s%s", new Object[] {
            baseUrl, cmd
        });
        xmlStruct.put("result", "Failed");
        xmlStruct.put("data_count", Integer.valueOf(0));
        xmlStruct.put("server_run", "Failed");
        mutex.acquire();
        InputSource s;
        try
        {
        	URLConnection cnx = ProxyConfiguration.open(new URL(cCmd));
	    	cnx.connect();
            java.io.Reader r = new InputStreamReader(cnx.getInputStream());//, "ISO-8859-1");
            s = new InputSource(r);
        }
        catch(Exception e)
        {
            mutex.release();
            return xmlStruct;
        }
        xmlStruct = parseXml(s);
        mutex.release();
        return xmlStruct;
    }

    public Boolean request(String cmd, Hashtable varStruct, Hashtable varResult)
    {
        Hashtable xmlStruct = request(cmd);
        if(!xmlStruct.get("server_run").equals("Success"))
            return Boolean.valueOf(false);
        if(!xmlStruct.get("result").equals("Success"))
            return Boolean.valueOf(false);
        if(varStruct.size() > 0)
        {
            String valueName;
            Object value;
            for(Enumeration e = varStruct.keys(); e.hasMoreElements(); varResult.put(valueName, value))
            {
                valueName = (String)e.nextElement();
                String valuePath = (String)varStruct.get(valueName);
                value = getValueFromStructure(xmlStruct, valuePath);
            }

        } else
        {
            Object value;
            for(Enumeration e = xmlStruct.keys(); e.hasMoreElements(); varResult.put(value, xmlStruct.get(value)))
                value = e.nextElement();

        }
        return Boolean.valueOf(true);
    }

    private Hashtable parseXml(InputSource s)
    {
        Hashtable struct = new Hashtable();
        int dataCount = 0;
        String dataNodeName = "";
        struct.put("result", "Failed");
        struct.put("data_count", Integer.valueOf(0));
        struct.put("server_run", "Success");
        try
        {
        	
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(s);

            doc.getDocumentElement().normalize();
            Node root = doc.getFirstChild();
            root.getChildNodes().getLength();
            for(int iNode = 0; iNode < root.getChildNodes().getLength(); iNode++)
            {
                Node node = root.getChildNodes().item(iNode);
                if(node.getFirstChild().getNodeValue() != null)
                {
                    struct.put(node.getNodeName(), node.getFirstChild().getTextContent());
                    continue;
                }
                Hashtable sub_struct = new Hashtable();
                for(int jNode = 0; jNode < node.getChildNodes().getLength(); jNode++)
                {
                    Node node1 = node.getChildNodes().item(jNode);
                    sub_struct.put(node1.getNodeName(), node1.getFirstChild().getTextContent());
                }

                if(node.getNodeName() == "data")
                {
                    dataNodeName = String.format("data%d", new Object[] {
                        Integer.valueOf(dataCount)
                    });
                    dataCount++;
                } else
                {
                    dataNodeName = node.getNodeName();
                }
                struct.put(dataNodeName, sub_struct);
            }

            struct.put("data_count", Integer.valueOf(dataCount));
            struct.put("server_run", "Success");
        }
        catch(Exception e) { }
        return struct;
    }

    private String baseUrl;
    private SLock mutex;
}
