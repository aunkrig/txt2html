
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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

import de.unkrig.commons.file.ExceptionHandler;
import de.unkrig.commons.file.contentstransformation.ContentsTransformer;
import de.unkrig.commons.file.filetransformation.FileTransformations;
import de.unkrig.commons.file.filetransformation.FileTransformations.ArchiveCombiner;
import de.unkrig.commons.file.filetransformation.FileTransformations.DirectoryCombiner;
import de.unkrig.commons.file.filetransformation.FileTransformer;
import de.unkrig.commons.file.filetransformation.FileTransformer.Mode;
import de.unkrig.commons.io.IoUtil;
import de.unkrig.commons.lang.protocol.PredicateUtil;
import de.unkrig.commons.text.pattern.Glob;

public
class Main {
    
    private Main() {}
    
    public static void
    main(String[] args) throws IOException {
        Mode                          fileTransformerMode = Mode.CHECK_AND_TRANSFORM;
        boolean                       keepOriginals       = true;
        boolean                       saveSpace           = true;
        Charset                       charset             = Charset.forName("UTF-8");
        ExceptionHandler<IOException> exceptionHandler    = ExceptionHandler.defaultHandler();

        // Create a ContentsTransformer that does the ASCII-art-to-SVG transformation.
        ContentsTransformer ct = new ContentsTransformer() {
            
            @Override public void
            transform(String path, InputStream is, OutputStream os) throws IOException {
                IoUtil.copy(
                    new InputStreamReader(is, charset),                           // reader
                    Txt2SvgFilterWriter.make(new OutputStreamWriter(os, charset)) // writer
                );
            }
        };

        // Recurse through archives and compressed files.
        FileTransformer ft = FileTransformations.recursiveCompressedAndArchiveFileTransformer(
            PredicateUtil.always(), // lookIntoFormat
            PredicateUtil.never(),  // archiveEntryRemoval
            Glob.ANY,               // archiveEntryRenaming
            ArchiveCombiner.NOP,    // archiveCombiner
            ct,                     // delegate
            keepOriginals,          // keepOriginals (keep copies of archives and compressed files)
            exceptionHandler        // exceptionHandler
        );

        // Recurse through directory trees.
        ft = FileTransformations.directoryTreeTransformer(
            null,                  // directoryMemberNameComparator
            PredicateUtil.never(), // directoryMemberRemoval
            Glob.ANY,              // directoryMemberRenaming
            DirectoryCombiner.NOP, // directoryCombiner
            ft,                    // regularFileTransformer
            saveSpace,             // saveSpace
            keepOriginals,         // keepOriginals
            exceptionHandler       // exceptionHandler
        );

        FileTransformations.transform(args, ft, fileTransformerMode, exceptionHandler);
    }
}
