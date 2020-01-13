
/*
 * txt2html - Converts text to an HTML document
 *
 * Copyright (c) 2020 Arno Unkrig. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *       following disclaimer.
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 *       following disclaimer in the documentation and/or other materials provided with the distribution.
 *    3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote
 *       products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package de.unkrig.txt2html;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

public
interface CharMatrix {

    public int width();
    
    public int height();

    /**
     * @throws IndexOutOfBoundsException <var>x</var> {@code < 0}
     * @throws IndexOutOfBoundsException <var>x</var> {@code >= width()}
     * @throws IndexOutOfBoundsException <var>y</var> {@code < 0}
     * @throws IndexOutOfBoundsException <var>y</var> {@code >= height()}
     */
    public char charAt(int x, int y);

    public default CharMatrix
    subMatrix(int x, int y, int width, int height) {
        
        if (x == 0 && y == 0 && width == this.width() && height == this.height()) return this;
        
        if (x + width  > this.width())  throw new IndexOutOfBoundsException();
        if (y + height > this.height()) throw new IndexOutOfBoundsException();
        if (width < 0)                  throw new IndexOutOfBoundsException();
        if (height < 0)                 throw new IndexOutOfBoundsException();
        
        int xOffset = x, yOffset = y;
        
        return new CharMatrix() {

            @Override public int
            width() { return width; }

            @Override public int
            height() { return height; }

            @Override
            public char charAt(int x, int y) {
                if (x < 0 || x >= width)  throw new IndexOutOfBoundsException();
                if (y < 0 || y >= height) throw new IndexOutOfBoundsException();
                return CharMatrix.this.charAt(xOffset + x, yOffset + y);
            }

            @Override
            public String toString() { return this.toString2(); }
        };
    }
    
    public default CharSequence
    horizontalSection(int y) {
        
        return new CharSequence() {
            
            @Override public CharSequence
            subSequence(int start, int end) { return CharSequences.subSequence(this, start, end); }
            
            @Override public int
            length() { return CharMatrix.this.width(); }
            
            @Override public char
            charAt(int index) {
                return CharMatrix.this.charAt(index, y);
            }
            
            @Override public String
            toString() { return CharSequences.toString(this); }
        };
    }
    
    public default CharSequence
    verticalSection(int x) {
        
        return new CharSequence() {
            
            @Override public CharSequence
            subSequence(int start, int end) { return CharSequences.subSequence(this, start, end); }
            
            @Override public int
            length() { return CharMatrix.this.height(); }
            
            @Override public char
            charAt(int index) { return CharMatrix.this.charAt(x, index); }
            
            @Override public String
            toString() { return CharSequences.toString(this); }
        };
    }
    
    /**
     * @return A deep copy of this {@link CharMatrix}
     */
    public default MutableCharMatrix
    copy() {
        
        char[][] caa = new char[this.height()][this.width()];
        for (int x = 0; x < this.width(); x++) {
            for (int y = 0; y < this.height(); y++) {
                caa[y][x] = this.charAt(x, y);
            }
        }
        
        return new MutableCharMatrix() {
            
            @Override public int
            width() { return CharMatrix.this.width(); }
            
            @Override public int
            height() { return CharMatrix.this.height(); }
            
            @Override public char
            charAt(int x, int y) {
                if (x < 0)              throw new IndexOutOfBoundsException("x=" + x);
                if (x >= this.width())  throw new IndexOutOfBoundsException("x=" + x + ", width=" + this.width());
                if (y < 0)              throw new IndexOutOfBoundsException("y=" + y);
                if (y >= this.height()) throw new IndexOutOfBoundsException("y=" + y + ", height=" + this.height());
                return caa[y][x];
            }
            
            @Override public void
            charAt(int x, int y, char c) {
                if (x < 0)              throw new IndexOutOfBoundsException("x=" + x);
                if (x >= this.width())  throw new IndexOutOfBoundsException("x=" + x + ", width=" + this.width());
                if (y < 0)              throw new IndexOutOfBoundsException("y=" + y);
                if (y >= this.height()) throw new IndexOutOfBoundsException("y=" + y + ", height=" + this.height());
                caa[y][x] = c;
            }

            @Override public String
            toString() { return this.toString2(); }
        };
    }

    public static CharMatrix
    read(URL resource) throws IOException {
        
        URLConnection conn = resource.openConnection();
        
        String charsetName = conn.getContentEncoding();
        if (charsetName == null) charsetName = "UTF-8";
        
        try (Reader r = new InputStreamReader(conn.getInputStream(), charsetName)) {
            return CharMatrix.read(r);
        }
    }
    
    public static CharMatrix
    read(Reader r) throws IOException {
        
        BufferedReader br = r instanceof BufferedReader ? (BufferedReader) r : new BufferedReader(r);
        
        int          width = 0;
        List<String> lines = new ArrayList<>();
        for (;;) {
            String line = br.readLine();
            if (line == null) break;
            if (line.length() > width) width = line.length();
            lines.add(line);
        }
        
        int width2 = width;
        return new CharMatrix() {
            
            @Override public int
            width() { return width2; }
            
            @Override public int
            height() { return lines.size(); }
            
            @Override public char
            charAt(int x, int y) {
                if (x < 0)              throw new IndexOutOfBoundsException("x=" + x);
                if (x >= this.width())  throw new IndexOutOfBoundsException("x=" + x + ", width=" + this.width());
                if (y < 0)              throw new IndexOutOfBoundsException("y=" + y);
                if (y >= this.height()) throw new IndexOutOfBoundsException("y=" + y + ", height=" + this.height());
                final String line = lines.get(y);
                return x < line.length() ? line.charAt(x) : ' ';
            }

            @Override public String
            toString() { return this.toString2(); }
        };
    }

    public default String
    toString2() {
        StringWriter sw = new StringWriter();
        for (int y = 0; y < this.height(); y++) {
            sw.append(this.horizontalSection(y)).append('\n');
        }
        return sw.toString();
    }
}
