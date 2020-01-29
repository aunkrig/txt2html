
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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.unkrig.commons.lang.AssertionUtil;
import de.unkrig.txt2html.text.CharMatrix;
import de.unkrig.txt2html.text.CharMatrix.Orientation;
import de.unkrig.txt2html.text.CharMatrix.Turtle;
import de.unkrig.txt2html.text.MutableCharMatrix;

public
class CharMatrix2Svg {
    
    static { AssertionUtil.enableAssertionsForThisClass(); }

    // Starting from a table's corner, finds the next table corner below.
    private static final Pattern PATTERN_TABLE_NEXT_CORNER_DOWN = Pattern.compile("\\|+\\+");
    // Starting from a table's corner, finds the next table corner to the right.
    private static final Pattern PATTERN_TABLE_NEXT_CORNER_RIGHT = Pattern.compile("-+\\+");

    // Starting at a corner of an arrow ('+'), finds the next corner (or the root) of the arrow.
    private static final Pattern PATTERN_NEXT_HORIZONAL_POLYGON_SEGMENT = Pattern.compile("[^|]*-\\+?");
    private static final Pattern PATTERN_NEXT_VERTICAL_POLYGON_SEGMENT  = Pattern.compile("[^\\-]*\\|\\+?");
    
    private int cellWidth  = 6;
    private int cellHeight = 15;

    interface ArtifactDetector { boolean detect(MutableCharMatrix mcm, int x, int y, CharMatrix2Svg cm2svg); }

    private static final ArtifactDetector NORMAL_TEXT = (mcm, x, y, cm2svg) -> {
        
        {
            final char c = mcm.charAt(x, y);
            if (c == ' ' || c == '+' || c == '|' || c == '^' || c == '-') return false;
        }
        
        int x2 = x;
        for (int i = x; i < mcm.width(); i++) {
            final char c = mcm.charAt(i, y);
            if (c == '+' || c == '|') break;
            if (c == ' ') {
                if (i == mcm.width() - 1 || " +".indexOf(mcm.charAt(i + 1, y)) != -1) break;
            } else {
                x2 = i + 1;
            }
        }
        cm2svg.text(x, y, mcm.horizontalSection(y).subSequence(x, x2));
        mcm.fill(x, y, x2 - x, 1, ' ');
        return true;
    };
    
    private static final ArtifactDetector DOCUMENT_SYMBOL = (cm, x, y, cm2svg) -> {
        if (cm.charAt(x, y) != '+' || x + 3 > cm.width() || y + 3 > cm.height()) return false;
        
        int[] hCorners = corners(cm.horizontalSection(y), x, PATTERN_TABLE_NEXT_CORNER_RIGHT);
        int[] vCorners = corners(cm.verticalSection(x),   y, PATTERN_TABLE_NEXT_CORNER_DOWN);

        if (hCorners.length != 2 || vCorners.length != 2) return false;
        int x2 = hCorners[1];
        int y2 = vCorners[1];
        if (
            x2 + 2 > cm.width()
            || cm.charAt(x2, y + 1) != '|'
            || cm.charAt(x2 + 1, y + 1) != '\\'
            || cm.charAt(x2,     y + 2) != '+'
            || cm.charAt(x2 + 1, y + 2) != '-'
            || cm.charAt(x2 + 2, y + 2) != '+'
            || cm.charAt(x2 + 2, y2)    != '+'
        ) return false;
        
        for (int xx = x + 1; xx < x2; xx++) {
            if (cm.charAt(xx, y) != '-') return false;
        }
        for (int yy = y + 3; yy < y2; yy++) {
            if (cm.charAt(x2 + 2, yy) != '|') return false;
        }
        for (int xx = x + 1; xx < x2 + 2; xx++) {
            if (cm.charAt(xx, y2) != '-') return false;
        }
        for (int yy = y + 1; yy < y2; yy++) {
            if (cm.charAt(x, yy) != '|') return false;
        }
        cm2svg.polyline(x2, y, x, y, x, y2, x2 + 2, y2, x2 + 2, y + 2, x2, y, x2, y + 2, x2 + 2, y + 2);
        
        cm2svg.convertSubmatrix(cm, x + 1, y + 1, x2 - x - 1, y2 - y - 1);
        
        cm.fill(x, y, x2 - x + 3, y2 - y + 1, ' ');
        
        return true;
    };
    
    private static final ArtifactDetector TABLE = (cm, x, y, cm2svg) -> {
        if (cm.charAt(x, y) != '+') return false;
        
        int[] hCorners = corners(cm.horizontalSection(y), x, PATTERN_TABLE_NEXT_CORNER_RIGHT);
        int[] vCorners = corners(cm.verticalSection(x),   y, PATTERN_TABLE_NEXT_CORNER_DOWN);

        if (hCorners.length < 2 || vCorners.length < 2) return false;
        
        int tableX1 = hCorners[0], tableX2 = hCorners[hCorners.length - 1];
        int tableY1 = vCorners[0], tableY2 = vCorners[vCorners.length - 1];
        
        for (int hci = 0; hci < hCorners.length; hci++) {
            for (int vci = 0; vci < vCorners.length; vci++) {
                if (cm.charAt(hCorners[hci], vCorners[vci]) != '+') return false;
            }
        }
        
        cm2svg.polyline(tableX1, tableY2, tableX1, tableY1, tableX2, tableY1);
        
        for (int hci = 0; hci < hCorners.length - 1; hci++) {
            for (int vci = 0; vci < vCorners.length - 1; vci++) {
                int cellX1 = hCorners[hci];
                int cellX2 = hCorners[hci + 1];
                int cellY1 = vCorners[vci];
                int cellY2 = vCorners[vci + 1];
                
                for (int x2 = cellX1 + 1; x2 < cellX2; x2++) {
                    if (cm.charAt(x2, cellY2) != '-') return false;
                }
                for (int y2 = cellY1 + 1; y2 < cellY2; y2++) {
                    if (cm.charAt(cellX2, y2) != '|') return false;
                }
                if (cm.charAt(cellX2, cellY2) != '+') return false;
                
                cm2svg.convertSubmatrix(cm, cellX1 + 1, cellY1 + 1, cellX2 - cellX1 - 1, cellY2 - cellY1 - 1);
                
                cm2svg.polyline(cellX2, cellY1, cellX2, cellY2, cellX1, cellY2);
            }
        }
        
        cm.fill(tableX1, tableY1, tableX2 - tableX1 + 1, tableY2 - tableY1 + 1, ' ');
        
        return true;
    };
    
    /**
     * Detects an arrow with its tip at {@code (x, y)}.
     */
    private static final ArtifactDetector DOWN_ARROW = (cm, x, y, cm2svg) -> {
        if (cm.charAt(x, y) != 'v') return false;
        Turtle turtle = cm.new Turtle(x, y, Orientation.NORTH);
        MatchResult mr = turtle.forward(PATTERN_NEXT_VERTICAL_POLYGON_SEGMENT);
        if (mr == null) return false;
        cm.charAt(x, y, ' ');
        for (int yy = y; yy < turtle.getY(); yy++) {
            if (cm.charAt(x, yy) == '|') cm.charAt(x, yy, ' ');
        }
        cm2svg.arrow(turtle.getX(), turtle.getY(), x, y);
        cookArrowSegments(cm2svg, turtle);
        return true;
    };
    
    /**
     * Detects an arrow with its tip at {@code (x, y)}.
     */
    private static final ArtifactDetector UP_ARROW = (cm, x, y, cm2svg) -> {
        if (cm.charAt(x, y) != '^') return false;
        Turtle turtle = cm.new Turtle(x, y, Orientation.SOUTH);
        MatchResult mr = turtle.forward(PATTERN_NEXT_VERTICAL_POLYGON_SEGMENT);
        if (mr == null) return false;
        cm.charAt(x, y, ' ');
        for (int yy = y; yy > turtle.getY(); yy--) {
            if (cm.charAt(x, yy) == '|') cm.charAt(x, yy, ' ');
        }
        cm2svg.arrow(turtle.getX(), turtle.getY(), x, y);
        cookArrowSegments(cm2svg, turtle);
        return true;
    };
    
    /**
     * Detects an arrow with its tip at {@code (x, y)}.
     */
    private static final ArtifactDetector LEFT_ARROW = (cm, x, y, cm2svg) -> {
        if (cm.charAt(x, y) != '<') return false;
        Turtle turtle = cm.new Turtle(x, y, Orientation.EAST);
        MatchResult mr = turtle.forward(PATTERN_NEXT_HORIZONAL_POLYGON_SEGMENT);
        if (mr == null) return false;
        cm.charAt(x, y, ' ');
        for (int xx = x; xx < turtle.getX(); xx++) {
            if (cm.charAt(xx, y) == '-') cm.charAt(xx, y, ' ');
        }
        cm2svg.arrow(turtle.getX(), turtle.getY(), x, y);
        cookArrowSegments(cm2svg, turtle);
        return true;
    };
    
    /**
     * Detects an arrow with its tip at {@code (x, y)}.
     */
    private static final ArtifactDetector RIGHT_ARROW = (cm, x, y, cm2svg) -> {
        if (cm.charAt(x, y) != '>') return false;
        Turtle turtle = cm.new Turtle(x, y, Orientation.WEST);
        MatchResult mr = turtle.forward(PATTERN_NEXT_HORIZONAL_POLYGON_SEGMENT);
        if (mr == null) return false;
        cm.charAt(x, y, ' ');
        for (int xx = x; xx > turtle.getX(); xx--) {
            if (cm.charAt(xx, y) == '-') cm.charAt(xx, y, ' ');
        }
        cm2svg.arrow(turtle.getX(), turtle.getY(), x, y);
        cookArrowSegments(cm2svg, turtle);
        return true;
    };
    
    private static void
    cookArrowSegments(CharMatrix2Svg cm2svg, final Turtle turtle) {

        if (turtle.charAt() != '+') return;
        
        for (Orientation orientation : Orientation.values()) {
            if (orientation == turtle.getOrientation().opposite()) continue;
            final Turtle turtle2 = turtle.clone();
            turtle2.setOrientation(orientation);
            MatchResult mr;
            switch (orientation) {
            case EAST:
            case WEST:
                mr = turtle2.forward(PATTERN_NEXT_HORIZONAL_POLYGON_SEGMENT);
                if (mr != null) {
                    cm2svg.line(turtle.getX(), turtle.getY(), turtle2.getX(), turtle2.getY());
                    cookArrowSegments(cm2svg, turtle2);
                }
                break;
            case NORTH:
            case SOUTH:
                mr = turtle2.forward(PATTERN_NEXT_VERTICAL_POLYGON_SEGMENT);
                if (mr != null) {
                    cm2svg.line(turtle.getX(), turtle.getY(), turtle2.getX(), turtle2.getY());
                    cookArrowSegments(cm2svg, turtle2);
                }
                break;
            }
        }
    }

    private final PrintWriter pw;
    private int               currentXOffset = 5, currentYOffset = 1;
    
    public
    CharMatrix2Svg(Writer w) {
        this.pw = w instanceof PrintWriter ? (PrintWriter) w : new PrintWriter(w);
    }

    public void
    convert(CharMatrix cm) {
        this.pw.printf(
            "<svg class=\"asciiart\" width=\"%d\" height=\"%d\" style=\"font-family:Consolas;font-size:11px\">%n",
            x2px(cm.width()) + 35,
            y2px(cm.height()) + 15
        );
        this.pw.printf(
            ""
            + "  <defs>%n"
            + "    <marker id=\"head\" markerWidth=\"4\" markerHeight=\"4\"%n"
            + "      style=\"fill:rgb(220,220,220)\"%n"
            + "      orient=\"auto\" refY=\"2\">%n"
            + "      <path d=\"M0,0 L4,2 0,4\" />%n"
            + "    </marker>%n"
            + "  </defs>%n"
        );
        this.convert(cm.copy());
        this.pw.printf("</svg>%n");
    }

    private void
    convertSubmatrix(MutableCharMatrix cm, int x, int y, int width, int height) {
        
        this.currentXOffset += x;
        this.currentYOffset += y;
        try {
            this.convert(cm.subMatrix(x, y, width, height));
        } finally {
            this.currentXOffset -= x;
            this.currentYOffset -= y;
        }
    }

    private void
    text(int x, int y, CharSequence cs) {
        this.pw.printf(
            "<text x=\"%d\" y=\"%d\">%s</text>%n",
            x2px(x) + cellWidth / 2,
            y2px(y) + (3 * cellHeight) / 4,
            sgmlEscape(cs)
        );
    }

    private String
    sgmlEscape(CharSequence cs) {
        int l = cs.length();
        
        int i = 0;
        for (;; i++) {
            if (i == l) return cs.toString();
            char c = cs.charAt(i);
            if (c == '<' || c == '>' || c == '"' || c == '&') break;
        }
        
        StringWriter sw = new StringWriter(l + 5);
        sw.append(cs.subSequence(0,  i));
        for (; i < l; i++) {
            char c = cs.charAt(i);
            if (c == '<') {
                sw.append("&lt;");
            } else
            if (c == '>') {
                sw.append("&gt;");
            } else
            if (c == '"') {
                sw.append("&quot;");
            } else
            if (c == '&') {
                sw.append("&amp;");
            } else
            {
                sw.append(c);
            }
        }
        return sw.toString();
    }

    private void
    polyline(int... xy) {
        this.pw.printf("<polyline points=\"");
        for (int i = 0; i < xy.length;) {
            if (i > 0) this.pw.append(' ');
            int x = xy[i++];
            int y = xy[i++];
            this.pw.printf("%d,%d", x2px(x) + cellWidth / 2, y2px(y) + cellHeight / 2);
        }
        this.pw.printf("\" style=\"fill:none;stroke:black;stroke-width:1\" />%n");
    }

    /**
     * Renders a line starting at {@code (x1, y1)} and ending at {@code (x2, y2)}.
     */
    private void
    line(int x1, int y1, int x2, int y2) {
        
        int x1px = x2px(x1);
        int y1px = y2px(y1);
        int x2px = x2px(x2);
        int y2px = y2px(y2);
        
        if (x1 == x2) {
            if (y2 > y1) {
                
                // Downward line.
                x1px += cellWidth;
                y1px -= cellHeight / 2;
                x2px += cellWidth;
                y2px -= cellHeight / 2;
            } else {
                
                // Upward line
                x1px += cellWidth;
                y1px += cellHeight / 2;
                x2px += cellWidth;
                y2px -= cellHeight / 2;
            }
        }
        
        if (y1 == y2) {
            if (x2 > x1) {
                
                // Right line.
                x1px -= cellWidth / 2;
                y1px += cellHeight / 2;
                x2px -= cellWidth * 2;
                y2px += cellHeight / 2;
            } else {
                
                // Left line.
                x1px += cellWidth / 2;
                y1px += cellHeight / 2;
                x2px += cellWidth * 2;
                y2px += cellHeight / 2;
            }
        }
        pw.printf(
            "<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" style=\"stroke:rgb(220,220,220);stroke-width:4\" />%n",
            x1px,
            y1px,
            x2px,
            y2px
        );
    }

    /**
     * Renders an arrow starting at {@code (x1, y1)} and ending with a tip at {@code (x2, y2)}.
     */
    private void
    arrow(int x1, int y1, int x2, int y2) {
        
        int x1px = x2px(x1);
        int y1px = y2px(y1);
        int x2px = x2px(x2);
        int y2px = y2px(y2);
        
        if (x1 == x2) {
            if (y2 > y1) {
                
                // Downward arrow.
                x1px += cellWidth;
                y1px -= cellHeight / 2;
                x2px += cellWidth;
                y2px += cellHeight / 2;
            } else {
                
                // Upward arrow
                x1px += cellWidth;
                y1px += 3 * cellHeight / 2;
                x2px += cellWidth;
                y2px += cellHeight / 2;
            }
        }
        
        if (y1 == y2) {
            if (x2 > x1) {
                
                // Right arrow.
                x1px += cellWidth;
                y1px += cellHeight / 2;
                x2px -= cellWidth;
                y2px += cellHeight / 2;
            } else {
                
                // Left arrow.
                x1px += cellWidth;
                y1px += cellHeight / 2;
                x2px += cellWidth * 2;
                y2px += cellHeight / 2;
            }
        }
        pw.printf(
            "<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" style=\"stroke:rgb(220,220,220);stroke-width:4\" marker-end='url(#head)' />%n", // SUPPRESS CHECKSTYLE LineLength
            x1px,
            y1px,
            x2px,
            y2px
        );
    }

    private int
    x2px(int x) { return this.cellWidth * (this.currentXOffset + x); }
    
    private int
    y2px(int y) { return this.cellHeight * (this.currentYOffset + y); }

    private static int[]
    corners(CharSequence cs, int x, Pattern nextCell) {
        List<Integer> corners = new ArrayList<>();
        corners.add(x);
        final Matcher m = nextCell.matcher(cs).region(x + 1, cs.length());
        while (m.lookingAt()) {
            corners.add(m.end() - 1);
            m.region(m.end(), cs.length());
        }
        int[] result = new int[corners.size()];
        for (int i = 0; i < result.length; i++) result[i] = corners.get(i);
        return result;
    }

    private void
    convert(MutableCharMatrix cm) {
        
        for (ArtifactDetector[] ads : new ArtifactDetector[][] {
            { DOWN_ARROW, UP_ARROW, LEFT_ARROW, RIGHT_ARROW },
            { DOCUMENT_SYMBOL, TABLE },
            { NORMAL_TEXT },
        }) {
            for (int x = 0; x < cm.width(); x++) {
                CELLS:
                for (int y = 0; y < cm.height(); y++) {
                    for (ArtifactDetector ad : ads) {
                        if (ad.detect(cm, x, y, this)) continue CELLS;
                    }
                }
            }
        }
        System.currentTimeMillis(); // TODO TMP
    }
}
