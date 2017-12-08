package main.java;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;

// TODO (in this class or other): systematically replace DTD URL in XSLTs with local path

public class MusicXMLPreprocess {

    public static void main(String[] args) {
        if (args.length != 4) {
            System.out.println("Usage: java MusicXMLPreprocess [location of original MusicXMLs] " +
                    "[location of partwise MusicXMLs] [location of timewise MusicXMLs] " +
                    "[location of XSLT files]");
            return;
        }
        preprocessMusicXMLs(args[0], args[1], args[2], args[3]);
    }

    public static void preprocessMusicXMLs(String origLoc, String partwiseLoc, String timewiseLoc, String defsLoc) {

        origLoc = normalizeDirectoryPath(origLoc);
        partwiseLoc = normalizeDirectoryPath(partwiseLoc);
        timewiseLoc = normalizeDirectoryPath(timewiseLoc);

        File origLocDir = new File(origLoc);
        File defsLocDir = new File(defsLoc);

        String partwiseDtd = new File(FilenameUtils.concat(defsLoc, "partwise.dtd")).getAbsolutePath();
        String timewiseDtd = new File(FilenameUtils.concat(defsLoc, "timewise.dtd")).getAbsolutePath();
        File partToTimeFile = new File(FilenameUtils.concat(defsLoc, "parttime.xsl"));
        File timeToPartFile = new File(FilenameUtils.concat(defsLoc, "timepart.xsl"));

        File[] musicXMLs = origLocDir.listFiles((dir, name) -> FilenameUtils.isExtension(name, "xml"));
        if (musicXMLs == null) {
            throw new RuntimeException("Failed to get files in " + origLoc);
        }

        // create fresh empty directories to put results in

        File partwiseLocDir = new File(partwiseLoc);
        File timewiseLocDir = new File(timewiseLoc);

        if (partwiseLocDir.exists()) {
            try {
                FileUtils.deleteDirectory(partwiseLocDir);
            } catch (IOException e) {
                throw new RuntimeException("Failed to delete partwise directory");
            }
        }
        if (!partwiseLocDir.mkdirs()) {
            throw new RuntimeException("Failed to make partwise directory");
        }

        if (timewiseLocDir.exists()) {
            try {
                FileUtils.deleteDirectory(timewiseLocDir);
            } catch (IOException e) {
                throw new RuntimeException("Failed to delete timewise directory");
            }
        }
        if (!timewiseLocDir.mkdirs()) {
            throw new RuntimeException("Failed to make timewise directory");
        }

        for (File musicXML: musicXMLs) {
            String partwiseName = FilenameUtils.getBaseName(musicXML.getName()) + "_partwise" + "."
                    + FilenameUtils.getExtension(musicXML.getName());
            String partwisePath = partwiseLoc + partwiseName;

            String timewiseName = FilenameUtils.getBaseName(musicXML.getName()) + "_timewise" + "."
                    + FilenameUtils.getExtension(musicXML.getName());
            String timewisePath = timewiseLoc + timewiseName;

            File tempFile = replaceDtdLocations(musicXML, partwiseDtd, timewiseDtd);
            convert(tempFile, partToTimeFile, timewisePath);
            convert(tempFile, timeToPartFile, partwisePath);
            tempFile.delete();
        }
    }

    private static File replaceDtdLocations(File musicXML, String partwiseDtdPath, String timewiseDtdPath) {
        final String partwiseDtdURL = "http://www.musicxml.org/dtds/partwise.dtd";
        final String timewiseDtdURL = "http://www.musicxml.org/dtds/timewise.dtd";

        Charset charset = Charset.forName("UTF-8");
        try {
            BufferedReader bufferedReader = Files.newBufferedReader(musicXML.toPath(), charset);
            File tempFile = File.createTempFile("modified_music_xml", null);
            BufferedWriter bufferedWriter = Files.newBufferedWriter(tempFile.toPath());

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                line = line.replaceAll(partwiseDtdURL, partwiseDtdPath);
                line = line.replaceAll(timewiseDtdURL, timewiseDtdPath);
                bufferedWriter.write(line);
                bufferedWriter.newLine();
            }
            bufferedReader.close();
            bufferedWriter.close();
            return tempFile;

        } catch (IOException e) {
            throw new RuntimeException(e.toString());
        }
    }

    private static void convert(File musicXML, File xsltFile, String outputFilePath) {
        TransformerFactory factory = TransformerFactory.newInstance();
        Source xsltSource = new StreamSource(xsltFile);
        Transformer transformer = null;
        try {
            transformer = factory.newTransformer(xsltSource);
        } catch (TransformerConfigurationException e) {
            System.err.println(e.getMessage());
        }
        Source musicXMLSource = new StreamSource(musicXML);
        try {
            transformer.transform(musicXMLSource, new StreamResult(new File(outputFilePath)));
        } catch (TransformerException e) {
            System.err.println(e.getMessage());
        }
    }

    private static String normalizeDirectoryPath(String dirName) {
        // TODO: make compatible with Windows
        if (dirName.charAt(dirName.length()-1) != '/') {
            return dirName + '/';
        }
        else return dirName;
    }
}
