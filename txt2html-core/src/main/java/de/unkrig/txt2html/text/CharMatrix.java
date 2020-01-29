
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

package de.unkrig.txt2html.text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.unkrig.commons.lang.AssertionUtil;
import de.unkrig.commons.nullanalysis.Nullable;

public abstract
class CharMatrix {
    
    static { AssertionUtil.enableAssertionsForThisClass(); }

    public enum Orientation {
        NORTH, EAST, SOUTH, WEST,
        ;
        
        private Orientation      opposite;
        private Set<Orientation> others;
        
        static {
            NORTH.opposite = SOUTH;
            EAST.opposite  = WEST;
            SOUTH.opposite = NORTH;
            WEST.opposite  = EAST;
            NORTH.others = Collections.unmodifiableSet(EnumSet.complementOf(EnumSet.of(NORTH)));
            EAST.others  = Collections.unmodifiableSet(EnumSet.complementOf(EnumSet.of(EAST)));
            SOUTH.others = Collections.unmodifiableSet(EnumSet.complementOf(EnumSet.of(SOUTH)));
            WEST.others  = Collections.unmodifiableSet(EnumSet.complementOf(EnumSet.of(WEST)));
        }
        
        public Orientation
        opposite() { return this.opposite; }
        
        public Set<Orientation>
        others() { return this.others; }
    }

    public
    class Turtle {

        private int         x, y;
        private Orientation orientation = Orientation.EAST;

        public
        Turtle(int x, int y, Orientation orientation) { this.x = x; this.y = y; this.orientation = orientation; }

        public Turtle
        clone() { return new Turtle(this.x, this.y, this.orientation); }

        public int
        getX() { return this.x; }
        
        public int
        getY() { return this.y; }
        
        public Orientation
        getOrientation() { return this.orientation; }
        
        public char
        charAt() { return CharMatrix.this.charAt(this.x, this.y); }

        public CharMatrix
        getCharMatrix() { return CharMatrix.this; }

        public void
        setX(int x) {
            assert x >= 0;
            assert x < CharMatrix.this.width();
            this.x = x;
        }
        
        public void
        setY(int y) {
            assert y >= 0;
            assert y < CharMatrix.this.height();
            this.y = y;
        }

        public void
        setOrientation(Orientation orientation) { this.orientation = orientation; }
        
        public void
        forward(int n) {
            switch (this.orientation) {
            case NORTH: this.setY(this.y - n); return; 
            case EAST:  this.setX(this.x + n); return;
            case SOUTH: this.setY(this.y + n); return;
            case WEST:  this.setX(this.x - n); return;
            default:    throw new AssertionError(this);
            }
        }
        
        @Nullable public MatchResult
        forward(Pattern pattern) {
            switch (this.orientation) {
                case NORTH: 
                {
                    Matcher m = pattern.matcher((
                        CharSequences.reverseOf(CharMatrix.this.verticalSection(this.x).subSequence(0, this.y))
                        .toString()
                    ));
                    if (!m.lookingAt()) return null;
                    this.y -= m.end();
                    return m;
                }
            case EAST:
                {
                    CharSequence hs = CharMatrix.this.horizontalSection(this.y);
                    Matcher      m  = pattern.matcher(hs).region(this.x + 1, hs.length());
                    if (!m.lookingAt()) return null;
                    this.x = m.end() - 1;
                    return m;
                }
            case SOUTH:
                {
                    CharSequence vs = CharMatrix.this.verticalSection(this.x);
                    Matcher      m  = pattern.matcher(vs).region(this.y + 1, vs.length());
                    if (!m.lookingAt()) return null;
                    this.y = m.end() - 1;
                    return m;
                }
            case WEST:
                {
                    Matcher m = pattern.matcher((
                        CharSequences.reverseOf(CharMatrix.this.horizontalSection(this.y).subSequence(0, this.x))
                        .toString()
                    ));
                    if (!m.lookingAt()) return null;
                    this.x -= m.end();
                    return m;
                }
            default:    throw new AssertionError(this);
            }
        }

        @Override public String
        toString() { return "x=" + this.x + ", y=" + this.y + ", orientation=" + this.orientation; }
    }
    
    public abstract int width();
    
    public abstract int height();

    /**
     * @throws IndexOutOfBoundsException <var>x</var> {@code < 0}
     * @throws IndexOutOfBoundsException <var>x</var> {@code >= width()}
     * @throws IndexOutOfBoundsException <var>y</var> {@code < 0}
     * @throws IndexOutOfBoundsException <var>y</var> {@code >= height()}
     */
    public abstract char charAt(int x, int y);

    public CharMatrix
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
        };
    }
    
    public CharSequence
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
    
    public CharSequence
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
    public MutableCharMatrix
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
        };
    }

    /**
     * @return The text of this {@link CharMatrix}, with lines suffixed with {@code '\n'}
     */
    @Override public String
    toString() {
        StringWriter sw = new StringWriter();
        for (int y = 0; y < this.height(); y++) {
            sw.append(this.horizontalSection(y)).append('\n');
        }
        return sw.toString();
    }
}
