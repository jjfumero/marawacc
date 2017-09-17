/*
 * Copyright (c) 2013, 2017, The University of Edinburgh. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package uk.ac.ed.accelerator.ocl;

public class CodeBuffer {

    private StringBuffer sourceBuffer;
    private static final String NEWLINE = System.getProperty("line.separator");
    private StringBuffer spaces;
    private static final String SPACE_TYPE = "    ";
    private boolean commentsEnabled;
    private String lastLine;

    public CodeBuffer(boolean comments) {
        sourceBuffer = new StringBuffer();
        spaces = new StringBuffer(" ");
        commentsEnabled = comments;
    }

    public void beginBlock() {
        incSpaces();
    }

    public void endBlock() {
        decSpaces();
    }

    public void emitComment(String s) {
        if (commentsEnabled) {
            sourceBuffer.append(spaces + "/* " + s + " */" + NEWLINE);
        }
    }

    public void beginBlockComment() {
        if (commentsEnabled) {
            sourceBuffer.append(spaces + "/*" + NEWLINE);
            incSpaces();
        }
    }

    public void endBlockComment() {
        if (commentsEnabled) {
            decSpaces();
            sourceBuffer.append(spaces + "*/" + NEWLINE);
        }
    }

    public void emitCode(String code) {
        lastLine = code;
        sourceBuffer.append(spaces + code + NEWLINE);
    }

    public StringBuffer getSpaces() {
        return spaces;
    }

    public int size() {
        return sourceBuffer.length();
    }

    public void compareAndEmitString(String s) {
        if (!s.equals(lastLine)) {
            lastLine = s;
            sourceBuffer.append(spaces + s + NEWLINE);
        }
    }

    public void compareNotReturnAndEmit(String s) {
        if (!lastLine.startsWith("return")) {
            sourceBuffer.append(spaces + s + NEWLINE);
        }
    }

    public void emitStringNoNL(String s) {
        sourceBuffer.append(spaces + s);
    }

    public void emitStringNoSpaces(String code) {
        sourceBuffer.append(code);
    }

    public void append(CodeBuffer codeBuffer) {
        sourceBuffer.append(codeBuffer.sourceBuffer);
    }

    public String getCode() {
        return sourceBuffer.toString();
    }

    private void incSpaces() {
        spaces.append(SPACE_TYPE);
    }

    private void decSpaces() {
        spaces = new StringBuffer(spaces.substring(0, spaces.length() - 4));
    }

    public void removeLastNLines(int nlines) {
        String[] split = sourceBuffer.toString().split("\\n");
        int size = split.length - nlines;

        sourceBuffer = new StringBuffer();
        for (int i = 0; i < size; i++) {
            String s = split[i];
            sourceBuffer.append(s + "\n");
        }
    }
}
