
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

public
interface MutableCharMatrix extends CharMatrix {

    void charAt(int x, int y, char c);

    public default MutableCharMatrix
    subMatrix(int x, int y, int width, int height) {
        
        if (x == 0 && y == 0 && width == this.width() && height == this.height()) return this;
        
        if (x + width  > this.width())  throw new IndexOutOfBoundsException("x=" + x + ", width="  + width  + ", outer width="  + this.width());
        if (y + height > this.height()) throw new IndexOutOfBoundsException("y=" + y + ", height=" + height + ", outer height=" + this.height());
        if (width < 0)                  throw new IndexOutOfBoundsException("width=" + width);
        if (height < 0)                 throw new IndexOutOfBoundsException("height=" + height);
        
        int xOffset = x, yOffset = y;
        
        return new MutableCharMatrix() {

            @Override public int
            width() { return width; }

            @Override public int
            height() { return height; }

            @Override
            public char charAt(int x, int y) {
                if (x < 0)       throw new IndexOutOfBoundsException();
                if (x >= width)  throw new IndexOutOfBoundsException();
                if (y < 0)       throw new IndexOutOfBoundsException();
                if (y >= height) throw new IndexOutOfBoundsException();
                return MutableCharMatrix.this.charAt(xOffset + x, yOffset + y);
            }

            @Override
            public String toString() { return this.toString2(); }

            @Override public void
            charAt(int x, int y, char c) {
                if (x < 0)       throw new IndexOutOfBoundsException();
                if (x >= width)  throw new IndexOutOfBoundsException();
                if (y < 0)       throw new IndexOutOfBoundsException();
                if (y >= height) throw new IndexOutOfBoundsException();
                MutableCharMatrix.this.charAt(xOffset + x, yOffset + y, c);
            }
        };

    }

    default void
    fill(int x, int y, int width, int height, char c) {
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                this.charAt(x + i, y + j, c);
            }
        }
    }
}
