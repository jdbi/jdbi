/* Copyright 2004-2007 Brian McCallister
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.skife.jdbi.v2.stringtemplate;

import antlr.CharScanner;
import org.antlr.stringtemplate.StringTemplateErrorListener;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.antlr.stringtemplate.StringTemplateGroupInterface;
import org.antlr.stringtemplate.StringTemplateGroupLoader;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A StringTemplateGroupLoader, appropriate for use with
 * {@see StringTemplateStatementLocator} which loads string template groups from the
 * classpath. It is essentially similar to the <code>CommonGroupLoader</code> in
 * StringTemplate except that it allows mor control over the template lexing, and accepts
 * root paths differently.
 */
public class ClasspathGroupLoader implements StringTemplateGroupLoader
{
    /**
     * A great deal of this class was  taken from PathGroupLoader in StringTemplate,
     * it makes osme bad assumptions though, so CommonGroupLoader is reimplemented here
     */

    private String fileCharEncoding = System.getProperty("file.encoding");

    private final ConcurrentMap<String, StringTemplateGroup> groupCache;
    private final ConcurrentMap<String, StringTemplateGroupInterface> interfaceCache;
    private final Class<? extends CharScanner> lexerClass;
    private final StringTemplateErrorListener errors;
    private final String[] dirs;

    /**
     * Create a new instance
     *
     * @param lexerClass CharScanner which will be used to lex the StringTemplateGroup file
     * @param errors Listener for errors while parsing or processing the templates
     * @param roots List of roots, relative to CLASSPATH root, which will be used
     *              to find template group files
     */
    public ClasspathGroupLoader(Class<? extends CharScanner> lexerClass,
                                StringTemplateErrorListener errors,
                                String... roots)
    {
        groupCache = new ConcurrentHashMap<String, StringTemplateGroup>();
        interfaceCache = new ConcurrentHashMap<String, StringTemplateGroupInterface>();
        this.lexerClass = lexerClass;
        this.errors = errors;
        this.dirs = roots;
    }

    /**
     * Create a new instance which uses the default lexer
     *
     * @param errors Listener for errors while parsing or processing the templates
     * @param roots List of roots, relative to CLASSPATH root, which will be used
     *              to find template group files
     */
    public ClasspathGroupLoader(StringTemplateErrorListener errors,
                                String... roots)
    {
        this(null, errors, roots);
    }

    /**
     * Create a new instance which uses the default lexer and looks for template
     * group files relative to the root of the classpath
     *
     * @param errors Listener for errors while parsing or processing the templates
     */
    public ClasspathGroupLoader(StringTemplateErrorListener errors)
    {
        this(errors, "/");
    }

    /**
     * Create a new instance which uses the default lexer and throws
     * IllegalStateExceptions when an error occurs during template processing
     *
     * @param roots List of roots, relative to CLASSPATH root, which will be used
     *              to find template group files
     */
    public ClasspathGroupLoader(String... roots)
    {
        this(new ExplodingStringTemplateErrorListener(), roots);
    }

    /**
     * Create a new instance which uses the default lexer, looks for files
     * relative to the root of the classpath, and throws
     * IllegalStateExceptions when an error occurs during template processing
     *
     */
    public ClasspathGroupLoader()
    {
        this("/");
    }

    /**
     * Create a new instance which throws IllegalStateExceptions when there is an error
     * processing templates
     *
     * @param lexerClass CharScanner which will be used to lex the StringTemplateGroup file
     * @param roots List of roots, relative to CLASSPATH root, which will be used
     *              to find template group files
     */
    public ClasspathGroupLoader(Class<? extends CharScanner> lexerClass, String... roots)
    {
        this(lexerClass, new ExplodingStringTemplateErrorListener(), roots);
    }


    private BufferedReader locate(String name)
    {
        for (String dir : dirs) {
            final String fileName = dir + "/" + name;
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            InputStream stream = loader.getResourceAsStream(fileName);
            if (stream == null) {
                loader = this.getClass().getClassLoader();
                stream = loader.getResourceAsStream(fileName);
            }
            if (stream != null) {
                return new BufferedReader(getInputStreamReader(stream));
            }
        }
        return null;
    }

    /**
     * Load the group called groupName from somewhere.  Return null
     * if no group is found.
     */
    public StringTemplateGroup loadGroup(String groupName)
    {
        if (groupCache.containsKey(groupName)) {
            return groupCache.get(groupName);
        }
        final BufferedReader br = locate(groupName + ".stg");
        if (br == null) {
            errors.error("no such group file " + groupName + ".stg", null);
            return null;
        }
        final StringTemplateGroup group = new StringTemplateGroup(br, lexerClass, errors);
        groupCache.putIfAbsent(groupName, group);

        return group;
    }

    /**
     * Load a group with a specified superGroup.  Groups with
     * region definitions must know their supergroup to find templates
     * during parsing.
     */
    public StringTemplateGroup loadGroup(String groupName, StringTemplateGroup superGroup)
    {
        final String key = new StringBuilder(groupName).append("!@#$%^&*()").append(superGroup).toString();
        if (groupCache.containsKey(key)) {
            return groupCache.get(key);
        }
        final BufferedReader br = locate(groupName + ".stg");
        if (br == null) {
            errors.error("no such group file " + groupName + ".stg", null);
            return null;
        }
        final StringTemplateGroup group = new StringTemplateGroup(br, lexerClass, errors, superGroup);
        groupCache.putIfAbsent(key, group);

        return group;
    }

    /**
     * Load the interface called interfaceName from somewhere.  Return null
     * if no interface is found.
     */
    public StringTemplateGroupInterface loadInterface(String interfaceName)
    {
        if (interfaceCache.containsKey(interfaceName)) {
            return interfaceCache.get(interfaceName);
        }
            final BufferedReader br = locate(interfaceName + ".sti");
            if (br == null) {
                errors.error("no such interface file " + interfaceName + ".sti", null);
                return null;
            }
            final StringTemplateGroupInterface iface = new StringTemplateGroupInterface(br, errors);
            interfaceCache.put(interfaceName, iface);

        return iface;
    }

    private InputStreamReader getInputStreamReader(InputStream in)
    {
        InputStreamReader isr = null;
        try {
            isr = new InputStreamReader(in, fileCharEncoding);
        }
        catch (UnsupportedEncodingException uee) {
            errors.error("Invalid file character encoding: " + fileCharEncoding, null);
        }
        return isr;
    }

    public void setFileCharEncoding(String fileCharEncoding)
    {
        this.fileCharEncoding = fileCharEncoding;
    }

    private static class ExplodingStringTemplateErrorListener implements StringTemplateErrorListener
    {

        public void error(String msg, Throwable e)
        {
            throw new IllegalStateException(msg, e);
        }

        public void warning(String msg)
        {
            throw new IllegalStateException(msg);
        }
    }
}
