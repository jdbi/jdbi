package org.skife.jdbi.v2.sqlobject.stringtemplate;

import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.antlr.stringtemplate.language.AngleBracketTemplateLexer;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.StatementLocator;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.regex.Matcher;

public class StringTemplate3StatementLocator implements StatementLocator
{
    private final StringTemplateGroup group;
    private final StringTemplateGroup literals = new StringTemplateGroup("literals", AngleBracketTemplateLexer.class);

    private boolean treatLiteralsAsTemplates;

    public StringTemplate3StatementLocator(Class baseClass)
    {
        this(mungify("/" + baseClass.getName()) + ".sql.stg", false, false);
    }

    public StringTemplate3StatementLocator(Class baseClass,
                                           boolean allowImplicitTemplateGroup,
                                           boolean treatLiteralsAsTemplates)
    {
        this(mungify("/" + baseClass.getName()) + ".sql.stg", allowImplicitTemplateGroup, treatLiteralsAsTemplates);
    }

    public StringTemplate3StatementLocator(String templateGroupFilePathOnClasspath)
    {
        this(templateGroupFilePathOnClasspath, false, false);
    }

    public StringTemplate3StatementLocator(String templateGroupFilePathOnClasspath,
                                           boolean allowImplicitTemplateGroup,
                                           boolean treatLiteralsAsTemplates)
    {
        this.treatLiteralsAsTemplates = treatLiteralsAsTemplates;
        InputStream ins = getClass().getResourceAsStream(templateGroupFilePathOnClasspath);
        if (allowImplicitTemplateGroup && ins == null) {
            this.group = new StringTemplateGroup("empty template group", AngleBracketTemplateLexer.class);
        }
        else if (ins == null) {
            throw new IllegalStateException("unable to find group file "
                                            + templateGroupFilePathOnClasspath
                                            + " on classpath");
        }
        else {
            InputStreamReader reader = new InputStreamReader(ins);
            try {
                this.group = new StringTemplateGroup(reader, AngleBracketTemplateLexer.class);
                reader.close();
            }
            catch (IOException e) {
                throw new IllegalStateException("unable to load string template group " + templateGroupFilePathOnClasspath,
                                                e);
            }
        }
    }

    public String locate(String name, StatementContext ctx) throws Exception
    {
        if (group.isDefined(name)) {
            // yeah, found template for it!
            StringTemplate t = group.lookupTemplate(name);
            for (Map.Entry<String, Object> entry : ctx.getAttributes().entrySet()) {
                t.setAttribute(entry.getKey(), entry.getValue());
            }
            return t.toString();
        }
        else if (treatLiteralsAsTemplates) {
            // no template in the template group, but we want literals to be templates
            final String key = new String(new Base64().encode(name.getBytes()));
            if (!literals.isDefined(key)) {
                literals.defineTemplate(key, name);
            }
            StringTemplate t = literals.lookupTemplate(key);
            for (Map.Entry<String, Object> entry : ctx.getAttributes().entrySet()) {
                t.setAttribute(entry.getKey(), entry.getValue());
            }
            return t.toString();
        }
        else {
            // no template, no literals as template, just use the literal as sql
            return name;
        }
    }

    private final static String sep = "/"; // *Not* System.getProperty("file.separator"), which breaks in jars

    private static String mungify(String path)
    {
        return path.replaceAll("\\.", Matcher.quoteReplacement(sep));
    }

}
