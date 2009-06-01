/*
 * Copyright 2004-2007 Brian McCallister
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.skife.jdbi.v2;

import org.skife.jdbi.v2.tweak.Argument;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the arguments bound to a particular statement
 */
public class Binding
{
    private Map<Integer, Argument> positionals = new HashMap<Integer, Argument>();
    private Map<String, Argument> named = new HashMap<String, Argument>();
    private List<LazyArguments> lazyArguments = new ArrayList<LazyArguments>();

    void addPositional(int position, Argument parameter) {
        positionals.put(position, parameter);
    }

    /**
     * Look up an argument by name
     *
     * @param name the key to lookup the value of
     *
     * @return the bound Argument
     */
    public Argument forName(String name) {
        if (named.containsKey(name)) {
            return named.get(name);
        }
        else {
            for (LazyArguments arguments : lazyArguments) {
                Argument arg = arguments.find(name);
                if (arg != null) {
                    return arg;
                }
            }
        }
        return null;
    }

    /**
     * Look up an argument by position
     *
     * @param position starts at 0, not 1
     *
     * @return arfument bound to that position
     */
    public Argument forPosition(int position) {
        return positionals.get(position);
    }

    void addNamed(String name, Argument argument) {
        this.named.put(name, argument);
    }

    void addLazyNamedArguments(LazyArguments args) {
        lazyArguments.add(args);
    }

    public String toString() {
        boolean wrote = false;
        StringBuilder b = new StringBuilder();
        b.append("{ positional:{");
        for (Map.Entry<Integer, Argument> entry : positionals.entrySet()) {
            wrote = true;
            b.append(entry.getKey()).append(":").append(entry.getValue()).append(",");
        }
        if (wrote) {
            wrote = false;
            b.deleteCharAt(b.length() - 1);
        }
        b.append("}");

        b.append(", named:{");
        for (Map.Entry<String, Argument> entry : named.entrySet()) {
            wrote = true;
            b.append(entry.getKey()).append(":").append(entry.getValue()).append(",");
        }
        if (wrote) {
            wrote = false;
            b.deleteCharAt(b.length() - 1);
        }
        b.append("}");

        b.append(", lazy:[");
        for (LazyArguments argument : lazyArguments) {
            wrote = true;
            b.append(argument).append(",");
        }
        if (wrote) {
            b.deleteCharAt(b.length() - 1);
        }
        b.append("]");

        b.append("}");
        return b.toString();
    }
}
