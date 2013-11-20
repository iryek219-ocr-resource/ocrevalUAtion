/*
 * Copyright (C) 2013 Universidad de Alicante
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package eu.digitisation.Unicode;

import eu.digitisation.util.MiniBrowser;
import eu.digitisation.util.Counter;
import java.io.*;
import java.util.HashMap;
import java.util.ArrayList;
import java.nio.channels.FileChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Transform the content of a file according to a mapping between (source,
 * target) character sequences. This can be useful, for example, to replace
 * Unicode characters which are not supported by the browser with printable
 * ones.
 *
 * @version 2012.06.20
 */
public class FileFilter extends HashMap<String, String> {

    /**
     * Converts the contents of a file into a CharSequence
     */
    private CharSequence toCharSequence(File file) {
        try {
            FileInputStream input = new FileInputStream(file);
            FileChannel channel = input.getChannel();
            java.nio.ByteBuffer buffer
                    = channel.map(FileChannel.MapMode.READ_ONLY, 0,
                            (int) channel.size());
            return java.nio.charset.Charset.forName("utf-8").newDecoder()
                    .decode(buffer);
        } catch (IOException ex) {
            System.err.println(ex);
        }
        return null;
    }

    /**
     * Find all occurrences of characters in a sequence and substitute them with
     * the replacement specified by the transformation map.
     *
     * @param s the string to be transformed
     * @return a new string with all the transformations performed
     *
     * Remark: No replacement priority is guaranteed in case of overlapping
     * matches.
     */
    private String translate(String s) {
        for (String pattern : keySet()) {
            s = s.replace(pattern, get(pattern));
        }
        return s;
    }

    /**
     * Translate all characters according to the transformation map
     *
     * @param infile the input file
     * @param outfile the file where the output must be written
     */
    public void translate(File infile, File outfile) {
        FileWriter writer = null;
        try {
            String input = toCharSequence(infile).toString();
            String output = translate(input);
            writer = new FileWriter(outfile);
            writer.write(output);
            writer.flush();
            writer.close();
        } catch (IOException ex) {
            Logger.getLogger(FileFilter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Translate (in place) all characters according to the transformation map
     *
     * @param file the input file
     *
     */
    public void translate(File file) {
        FileWriter writer = null;
        try {
            String input = toCharSequence(file).toString();
            String output = translate(input);
            writer = new FileWriter(file);
            writer.write(output);
            writer.flush();
            writer.close();
        } catch (IOException ex) {
            Logger.getLogger(FileFilter.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    /**
     * Load the transformation map from a file (one transformation per line).
     * The file contains Unicode values
     *
     * @param file the file to be transformed
     */
    public FileFilter(File file) {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(file));
            while (reader.ready()) {
                String line = reader.readLine();
                String[] tokens = line.split("\\p{Space}");
                if (tokens.length == 2) {
                    String left = Reader.codepointsToString(tokens[0]);
                    String right = Reader.codepointsToString(tokens[1]);
                    //		    System.out.println(left+"->"+right);
                    put(left, right);
                } else {
                    System.err.println("Wrong line" + line
                            + " at file " + file);
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(FileFilter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * TO BE REMOVED Escape special characters in XML
     *
     * @param s a string 
     * @return the string with characters <, >, &, " escaped
     */
    public static String encode(String s) {
        StringBuilder result = new StringBuilder();
        for (Character c : s.toCharArray()) {
            if (c.equals('<')) {
                result.append("&lt;");
            } else if (c.equals('>')) {
                result.append("&gt;");
            } else if (c.equals('"')) {
                result.append("&quot;");
            } else if (c.equals('&')) {
                result.append("&amp;");
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    /**
     * Identify characters in a file and their frequencies
     *
     * @param file the input file
     * @return a counter with the frequency for each character in the file
     */
    public static Counter<Character> stats(File file) {
        Counter<Character> counter = new Counter<Character>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));

            while (reader.ready()) {
                String line = reader.readLine();
                for (Character c : line.toCharArray()) {
                    counter.inc(c);
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(FileFilter.class.getName()).log(Level.SEVERE, null, ex);
        } 
        return counter;
    }

    /**
     * Display statistics for a collection
     *
     * @param files the array of files to be analysed
     */
    public static void showStats(File[] files) {
        Counter<Character> counter = new Counter<Character>();
        for (File file : files) {
            counter.add(stats(file));
            System.err.println("Processing file " + file);
        }
        for (Character c : counter.keySet()) {
            int code = (int) c;
            int n = counter.get(c);
            System.out.println(Integer.toHexString(code) + " " + n);
        }
    }

    /**
     * Read a file as an array of lines
     */
    private static String[] toArray(File file) {
        ArrayList<String> list = new ArrayList<String>();
        try {
            BufferedReader reader
                    = new BufferedReader(new FileReader(file));
            while (reader.ready()) {
                list.add(reader.readLine());
            }
        } catch (IOException ex) {
            System.err.println(ex);
        }
        return list.toArray(new String[list.size()]);
    }

    /**
     * Search for a Unicode sequence and highlight them in browser
     */
    public static void find(File[] files, String codepoints) {
        String pattern = Reader.codepointsToString(codepoints);
        MiniBrowser browser = new MiniBrowser();
        String outputDir = "output";
        try {
            for (File file : files) {
                String output = outputDir + "/"
                        + file.getName().replace(".xml", ".html");
                File outFile = new File(output);
                String[] lines = toArray(file);
                PrintWriter writer = new PrintWriter(outFile);
                Boolean found = false;
                writer.print("<p><font color='blue'>"
                        + file + "</font></p>");
                for (String line : lines) {
                    if (line.contains(pattern)) {
                        writer.print("<p><font color='red'>"
                                + encode(line) + "</font></p>");
                        found = true;
                    } else {
                        writer.print("<p>" + encode(line) + "</p>");
                    }
                }
                writer.close();
                if (found) {
                    browser.addPage("file:" + outFile);
                }
            }
            browser.setVisible(true);
        } catch (FileNotFoundException ex) {
            System.err.println(ex);
        }
    }

    /**
     * The main function
     * @param args see usage
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage:\n"
                    + "\tFileFilter -s unicode_pattern file1 file2 .... [search]"
                    + "\n\tFileFilter -f file1 file2 .....   [display statistics]"
                    + "\n\tFileFilter -r replacements input_file output_file");
        } else if (args[0].equals("-s")) {
            String codepoints = args[1];
            File[] files = new File[args.length - 2];
            for (int n = 2; n < args.length; ++n) {
                files[n - 2] = new File(args[n]);
            }
            FileFilter.find(files, codepoints);
        } else if (args.length > 0 && args[0].equals("-f")) {
            File[] files = new File[args.length - 1];
            for (int n = 1; n < args.length; ++n) {
                files[n - 1] = new File(args[n]);
            }
            FileFilter.showStats(files);
        } else {
            FileFilter map = new FileFilter(new File(args[0]));
            File infile = new File(args[1]);
            File outfile = new File(args[2]);
            map.translate(infile, outfile);
        }
    }
}
