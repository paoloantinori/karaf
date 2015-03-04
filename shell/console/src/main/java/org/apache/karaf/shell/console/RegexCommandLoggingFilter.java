/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.karaf.shell.console;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.karaf.shell.console.CommandLoggingFilter;

/**
 *
 */
public class RegexCommandLoggingFilter implements CommandLoggingFilter {

    private int group=1;
    private Pattern pattern;
    private String replacement = "*****";

    public CharSequence filter(CharSequence command) {
        if( pattern!=null ) {
            Matcher m = pattern.matcher(command);
            if( m.matches() ) {
                String replace = replace(m.group(group));
                command = new StringBuilder(command).replace(m.start(group), m.end(group), replace).toString();
            }
        }
        return command;
    }

    protected String replace(String value) {
        return replacement;
    }

    public String getPattern() {
        return pattern.toString();
    }

    public void setPattern(String pattern) {
        this.pattern = Pattern.compile(pattern);
    }

    public String getReplacement() {
        return replacement;
    }

    public void setReplacement(String replacement) {
        this.replacement = replacement;
    }

    public int getGroup() {
        return group;
    }

    public void setGroup(int group) {
        this.group = group;
    }
}
