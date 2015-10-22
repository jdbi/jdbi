/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jdbi.v3;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.regex.Matcher;

import org.antlr.runtime.ANTLRInputStream;
import org.antlr.runtime.Token;
import org.jdbi.v3.exceptions.UnableToCreateStatementException;
import org.jdbi.v3.tweak.StatementLocator;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * looks for [name], then [name].sql on the classpath
 */
public class ClasspathStatementLocator implements StatementLocator
{
    private final Map<String, String> found = Collections.synchronizedMap(new WeakHashMap<>());

    /**
     * Very basic sanity test to see if a string looks like it might be sql
     */
    public static boolean looksLikeSql(String sql)
    {
        final String local = left(stripStart(sql), 8).toLowerCase();
        return local.startsWith("insert ")
               || local.startsWith("update ")
               || local.startsWith("select ")
               || local.startsWith("call ")
               || local.startsWith("delete ")
               || local.startsWith("create ")
               || local.startsWith("alter ")
               || local.startsWith("merge ")
               || local.startsWith("replace ")
               || local.startsWith("drop ");
    }

    /**
     * If the passed in name doesn't look like SQL it will search the classpath for a file
     * which looks like the provided name.
     * <p/>
     * The "looks like" algorithm is not very sophisticated, it basically looks for the string
     * to begin with insert, update, select, call, delete, create, alter, or drop followed
     * by a space.
     * <p/>
     * If no resource is found using the passed in string, the string s returned as-is
     *
     * @param name Name or statement literal
     *
     * @return SQL to execute (which will go to a StatementRRewrter first)
     *
     * @throws UnableToCreateStatementException
     *          if an IOException occurs reading a found resource
     */
    @Override
    @SuppressWarnings({ "PMD.EmptyCatchBlock", "resource" })
    @SuppressFBWarnings("DM_STRING_CTOR")
    public String locate(SqlName name, StatementContext ctx)
    {
        final String cache_key;
        if (ctx.getSqlObjectType() != null) {
            cache_key = '/' + mungify(ctx.getSqlObjectType().getName() + '.' + name) + ".sql";
        }
        else {
            cache_key = name.toString();
        }

        if (found.containsKey(cache_key)) {
            return found.get(cache_key);
        }

        if (looksLikeSql(name.toString())) {
            // No need to cache individual SQL statements that don't cause us to search the classpath
            return name.toString();
        }
        final ClassLoader loader = selectClassLoader();
        InputStream in_stream = null;
        try {
            in_stream = loader.getResourceAsStream(name.toString());
            if (in_stream == null) {
                in_stream = loader.getResourceAsStream(name + ".sql");
            }

            if (in_stream == null && ctx.getSqlObjectType() != null) {
                String filename = '/' + mungify(ctx.getSqlObjectType().getName() + '.' + name) + ".sql";
                in_stream = loader.getResourceAsStream(filename);
                if (in_stream == null) {
                    in_stream = ctx.getSqlObjectType().getResourceAsStream(filename);
                }
            }

            if (in_stream == null) {
                // Ensure we don't store an identity map entry which has a hard reference
                // to the key (through the value) by copying the value, avoids potential memory leak.
                found.put(cache_key, name.toString() == cache_key ? new String(name.toString()) : name.toString());
                return name.toString();
            }
            String sql;
            try {
                sql = SQL_SCRIPT_PARSER.parse(new ANTLRInputStream(in_stream));
            } catch (IOException e) {
                throw new UnableToCreateStatementException(e.getMessage(), e, ctx);
            }

            found.put(cache_key, sql);
            return sql;
        }
        finally {
            try {
                if (in_stream != null) {
                    in_stream.close();
                }
            }
            catch (IOException e) {
                // nothing we can do here :-(
            }
        }
    }

    /**
     * There *must* be a better place to put this without creating a helpers class just for it
     */
    private static ClassLoader selectClassLoader()
    {
        ClassLoader loader;
        if (Thread.currentThread().getContextClassLoader() != null) {
            loader = Thread.currentThread().getContextClassLoader();
        }
        else {
            loader = ClasspathStatementLocator.class.getClassLoader();
        }
        return loader;
    }

    private static final String SEP = "/"; // *Not* System.getProperty("file.separator"), which breaks in jars

    private static String mungify(String path)
    {
        return path.replaceAll("\\.", Matcher.quoteReplacement(SEP));
    }


    // (scs) Logic copied from commons-lang3 3.1 with minor edits, per discussion on commit 023a14ade2d33bf8ccfa0f68294180455233ad52
    private static String stripStart(String str)
    {
        int strLen;
        if (str == null || (strLen = str.length()) == 0) {
            return "";
        }
        int start = 0;
        while (start != strLen && Character.isWhitespace(str.charAt(start))) {
            start++;
        }
        return str.substring(start);
    }

    private static String left(String str, int len)
    {
        if (str == null || len < 0) {
            return "";
        }
        if (str.length() <= len) {
            return str;
        }
        return str.substring(0, len);
    }

    private static final SqlScriptParser SQL_SCRIPT_PARSER = new SqlScriptParser(new SqlScriptParser.TokenHandler() {
        @Override
        public void handle(Token t, StringBuilder sb) {
            sb.append(t.getText());
        }
    });
}
