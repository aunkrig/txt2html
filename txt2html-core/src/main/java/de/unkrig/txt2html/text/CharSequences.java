
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

public class CharSequences {

    /**
     * A generic implementation of {@link CharSequence#subSequence(int, int)}.
     */
    public static CharSequence
    subSequence(CharSequence cs, int start, int end) {
        
        if (start < 0 || end < start || end > cs.length()) throw new IndexOutOfBoundsException();
        
        if (start == 0 && end == cs.length()) return cs;
        
        return new CharSequence() {
            
            @Override public CharSequence
            subSequence(int start2, int end2) {
                if (start2 < 0 || start + end2 > end) throw new IndexOutOfBoundsException();
                return CharSequences.subSequence(cs, start + start2, start + end2);
            }
            
            @Override public int
            length() { return end - start; }
            
            @Override public char
            charAt(int index) { return cs.charAt(start + index); }

            @Override public String
            toString() { return CharSequences.toString(this); }
        };
    }
    
    public static CharSequence
    reverseOf(CharSequence cs) {
        
        return new CharSequence() {
            
            @Override public CharSequence
            subSequence(int start, int end) { return CharSequences.subSequence(this, start, end); }
            
            @Override public int
            length() { return cs.length(); }
            
            @Override public char
            charAt(int index) { return cs.charAt(cs.length() - index - 1); }
            
            @Override public String
            toString() { return CharSequences.toString(this); }
        };
    }

    public static String
    toString(CharSequence cs) {
        int    l  = cs.length();
        char[] ca = new char[l];
        for (int i = 0; i < l; i++) ca[i] = cs.charAt(i);
        return new String(ca);
    }
}
