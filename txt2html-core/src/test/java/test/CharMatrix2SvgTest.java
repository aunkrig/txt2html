
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

package test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import de.unkrig.commons.io.Readers;
import de.unkrig.txt2html.CharMatrix;
import de.unkrig.txt2html.CharMatrix2Svg;

public
class CharMatrix2SvgTest {

    @Ignore @Test public void
    fig1Test() throws IOException {
        CharMatrix cm = CharMatrix.read(this.getClass().getResource("fig1.txt"));
        StringWriter sw = new StringWriter();
        new CharMatrix2Svg(sw).convert(cm.copy());
        String actual = sw.toString();
        
        String expected = read(this.getClass().getResource("fig1.html"));
        
        Assert.assertEquals(expected, actual);
    }

    private String
    read(URL resource) throws IOException {
        
        URLConnection conn = resource.openConnection();
        
        String charsetName = conn.getContentEncoding();
        if (charsetName == null) charsetName = "UTF-8";
        
//        try (Reader r = new InputStreamReader(conn.getInputStream(), charsetName)) {
//            return Readers.readAll(r);
//        }
        Reader r = new InputStreamReader(conn.getInputStream(), charsetName);
        try {
            return Readers.readAll(r);
        } finally {
            try { r.close(); } catch (Exception e) {}
        }
    }
}
